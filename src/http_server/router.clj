(ns http-server.router
  (:require [http_server.response-builder :as response-builder]
            [http_server.fileio :as fileio]
            [http-server.config :refer :all]
            [clojure.java.io :as io]
            [base64-clj.core :as base64]
            [pantomime.mime :refer [mime-type-of]]
            [pandect.core :as pandect]))

(def config-options (read-config-file
                         (str 
                           (-> 
                             (java.io.File. "")
                             .getAbsolutePath) "/config")))

(def special-routes (concat (config-options :directory) 
                          (config-options :accept-parameters)
                          (config-options :redirect)
                          (config-options :authenticate)))

(defn to-byte-array [string]
  (->> ^String string
       (.getBytes)
       (byte-array)))

(defn build-directory-links [directory]
  (let [directory (io/file directory)
        files (.list directory)]
    (str "<!DOCTYPE html>"
         "<html>"
         "<head>"
         "<title>directory</title>"
         "</head>"
         "<body>"
         (apply str 
                (map #(str "<a href=\"/" % "\">" % "</a><br>") files))
         "</body>"
         "</html>")))

(defn build-directory [directory]
  (let [directory-links (to-byte-array 
                          (build-directory-links directory))]
    (response-builder/build-response 200 
                    {"Content-Length" (count directory-links)} 
                    directory-links)))

(defn check-auth [auth]
  (if auth
    (= (config-options :credentials) (base64/decode (second auth))) 
    false))

(defn get-trimmed-body [body-bytes begin end]
  (->> body-bytes
       (seq)
       (drop begin)
       (take end)
       (byte-array)))

(defn parse-byte-range [byte-header]
  (map #(.replaceAll ^String % "[^0-9]" "")
       (clojure.string/split byte-header #"-")))

(defn get-body-range [body-bytes range-header path]
  (let [byte-range (parse-byte-range range-header)
        begin (Integer. ^String (first byte-range))
        end (+ 1 (Integer. ^String (second byte-range)))
        body (get-trimmed-body body-bytes begin end)]
    (response-builder/build-response 206 {"Content-Type"
                          (mime-type-of (io/file path))
                          "Content-Length"
                          (count body)}
                    body)))

(defn get-file-data [directory location headers]
  (try
    (let [path (str directory location)
          body-data (fileio/binary-slurp path)]
      (if (contains? headers :Range)
        (get-body-range body-data (headers :Range) path)
        (response-builder/build-response 200
                        {"Content-Type" 
                         (mime-type-of (io/file path))
                         "Content-Length" 
                         (count body-data) } 
                        body-data)))
    (catch Exception e
      (response-builder/build-response 404 {}))))

(defn authenticate [directory location headers]
  (let [no-auth (.getBytes "Authentication required")]
    (if (headers :Authorization) 
      (get-file-data directory location headers) 
      (response-builder/build-response 401 {} no-auth))))

(defn decode-params [params]
  (let [params (clojure.string/replace params #"=" " = ")]
    (->> params
         (java.net.URLDecoder/decode)
         (.getBytes)
         (byte-array))))

(defn handle-query [params]
  (let [decoded-params (decode-params params)]
    (response-builder/build-response 200 
                    {:Content-Length (count decoded-params)} 
                    decoded-params)))

(defn contain-route? [config-option location]
  (some (partial = location) config-option))

(defn handle-special-route [location directory headers params]
  (cond
    (contain-route? (config-options :directory) location) 
      (build-directory directory)
    (contain-route? (config-options :authenticate) location) 
      (authenticate directory location headers)
    (contain-route? (config-options :accept-parameters) location)
      (handle-query params)
    (contain-route? (config-options :redirect) location) 
      (response-builder/build-response 301 {"Location" "http://localhost:5000/"})
    :else (response-builder/build-response 500 {})))

(defn get-route [location directory headers] 
  (let [query (clojure.string/split location #"\?") 
        location (first query)
        params (second query)]
    (cond 
      (some (partial = location) special-routes)
        (handle-special-route location directory headers params)
      (= "/" location)
        (get-file-data directory "/index.html" headers)
      :else (get-file-data directory location headers))))

(defn options-route [location directory]
  (response-builder/build-response 200 {"Allow" "GET,HEAD,POST,OPTIONS,PUT"}))

(defn not-valid-file [location]
  (some (partial = location) (config-options :protected)))

(defn patch-route [body location directory headers]
  (cond 
    (not-valid-file location) (response-builder/build-response 405 {})
    :else
    (let [path (str directory location)
          file-data (slurp path)
          encoded-file-data (pandect/sha1 file-data)
          etag (headers :If-Match)]
      (fileio/overwrite-file path body)
      (response-builder/build-response 204 {}))))

(defn post-route [body location directory]
  (cond
    (not-valid-file location) (response-builder/build-response 405 {})
    :else (do 
            (fileio/append-to-file (str directory location) body)
            (response-builder/build-response 200 {}))))

(defn put-route [body location directory]
  (cond
    (not-valid-file location) (response-builder/build-response 405 {})
    :else (do 
            (spit (str directory location) body)
            (response-builder/build-response 200 {}))))

(defn delete-route [location directory]
  (spit (str directory location) "")
  (response-builder/build-response 200 {}))

(defn head-route [location directory]
  (response-builder/build-response 200 {}))

(defmulti router (fn [directory parsed-request headers & body]
                   (parsed-request :action)))

(defmethod router "GET" [directory parsed-request headers & body]
  (let [action (parsed-request :action)
        location (parsed-request :location)]
    (get-route location directory headers)))

(defmethod router "OPTIONS" [directory parsed-request headers & body]
  (let [action (parsed-request :action)
        location (parsed-request :location)]
    (options-route location directory)))

(defmethod router "POST" [directory parsed-request headers & body]
  (let [action (parsed-request :action)
        location (parsed-request :location)]
    (post-route (first body) location directory)))

(defmethod router "PATCH" [directory parsed-request headers & body]
  (let [action (parsed-request :action)
        location (parsed-request :location)]
    (patch-route (first body) location directory headers)))

(defmethod router "PUT" [directory parsed-request headers & body]
  (let [action (parsed-request :action)
        location (parsed-request :location)]
    (put-route (first body) location directory)))

(defmethod router "DELETE" [directory parsed-request headers & body]
  (let [action (parsed-request :action)
        location (parsed-request :location)]
    (delete-route location directory)))

(defmethod router "HEAD" [directory parsed-request headers & body]
  (let [action (parsed-request :action)
        location (parsed-request :location)]
    (head-route location directory)))

(defmethod router :default [directory parsed-request headers & body]
  (response-builder/build-response 400 {}))
