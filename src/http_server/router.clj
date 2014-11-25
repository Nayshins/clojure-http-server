(ns http-server.router
  (:require [http_server.response-builder :as response-builder]
            [http_server.fileio :as fileio]
            [clojure.java.io :as io]
            [base64-clj.core :as base64]
            [pantomime.mime :refer [mime-type-of]]
            [pandect.core :as pandect]))

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
    {:status 200 
      :headers {"Content-Length" (count directory-links)} 
      :body directory-links}))

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
    {:status 206 
     :headers {"Content-Type" (mime-type-of (io/file path))
               "Content-Length" (count body)}
     :body body}))

(defn get-file-data [directory location headers]
  (try
    (let [path (str directory location)
          body-data (fileio/binary-slurp path)]
      (if (contains? headers :Range)
        (get-body-range body-data (headers :Range) path)
        {:status 200 
         :headers {"Content-Type" (mime-type-of (io/file path))
                   "Content-Length" (count body-data) } 
         :body body-data}))
    (catch Exception e
      {:status 404})))

(defn decode-params [params]
  (let [params (clojure.string/replace params #"=" " = ")]
    (->> params
         (java.net.URLDecoder/decode)
         (.getBytes)
         (byte-array))))

(defn handle-query [params]
  (let [decoded-params (decode-params params)]
    {:status 200 
     :headers {:Content-Length (count decoded-params)} 
     :body decoded-params}))

(defn get-route [request directory] 
  (let [query (clojure.string/split (request :location) #"\?") 
        location (first query)
        params (second query)]
    (get-file-data directory location (request :headers))))

(defn options-route [location directory]
{:status 200 :headers {"Allow" "GET,HEAD,POST,OPTIONS,PUT"}})

(defn patch-route [request directory]
    (let [path (str directory (request :location))
          headers (request :headers)
          file-data (slurp path)
          encoded-file-data (pandect/sha1 file-data)
          etag (headers :If-Match)]
      (fileio/overwrite-file path (request :body))
      {:status 204}))

(defn post-route [request directory]
  (let [location (request :location)
        body (request :body)]
    (fileio/append-to-file (str directory location) body)
    {:status 200}))

(defn put-route [request directory]
  (let [location (request :location)
        body (request :body)] 
    (spit (str directory location) body)
    {:status 200}))

(defn delete-route [request directory]
  (spit (str directory (request :location)) "")
  {:status 200})

(defn head-route [location directory]
  {:status 200})

(defmulti router (fn [request directory]
                   (request :action)))

(defmethod router "GET" [request directory]
  (get-route request directory))

(defmethod router "OPTIONS" [request directory]
  (let [action (request :action)
        location (request :location)]
    (options-route location directory)))

(defmethod router "POST" [request directory]
  (let [action (request :action)
        location (request :location)]
    (post-route request directory)))

(defmethod router "PATCH" [request directory]
  (let [action (request :action)
        location (request :location)]
    (patch-route request directory)))

(defmethod router "PUT" [request directory]
  (let [action (request :action)
        location (request :location)]
    (put-route request directory)))

(defmethod router "DELETE" [request directory]
  (let [action (request :action)
        location (request :location)]
    (delete-route request directory)))

(defmethod router "HEAD" [request directory]
  (let [action (request :action)
        location (request :location)]
    (head-route request directory)))

(defmethod router :default [request directory]
  nil)
