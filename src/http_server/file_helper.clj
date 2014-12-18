(ns http-server.file-helper
  (:require [http-server.file-io :as fileio]
            [clojure.java.io :as io]
            [base64-clj.core :as base64]
            [pantomime.mime :refer [mime-type-of]]))

(defn get-trimmed-body [body-bytes begin end]
  (->> body-bytes
       (seq)
       (drop begin)
       (take end)
       (byte-array)))

(defn drop-bytes [body-bytes drop-amount]
  (->> body-bytes
       (seq)
       (drop drop-amount)
       (byte-array)))

(defn parse-byte-range [byte-header]
  (clojure.string/split byte-header #"="))

(defn partial-response [body path]
  {:status 206 
   :headers {"Content-Type" (mime-type-of (io/file path))
             "Content-Length" (count body)}
   :body body})

(defn return-end-bytes [body-bytes byte-range path]
  (let [body-count (count body-bytes) 
        range-int (Integer. (second (clojure.string/split byte-range #"-")))
        amount-to-drop (- body-count range-int)
        body (drop-bytes body-bytes amount-to-drop)]
    (partial-response body path)))

(defn drop-first-bytes [body-bytes byte-range path]
  (let [range-int (Integer. (first (clojure.string/split byte-range #"-")))
        body (drop-bytes body-bytes range-int)]
    (partial-response body path)))

(defn get-partial-range [body-bytes byte-range path]
    (if (= "-" (first (clojure.string/split byte-range #"")))
      (return-end-bytes body-bytes byte-range path)
      (drop-first-bytes body-bytes byte-range path)))

(defn get-full-body-range [body-bytes byte-range path]
  (let [byte-range (clojure.string/split byte-range #"-")
        begin (Integer. ^String (first byte-range))
        end (+ 1 (Integer. ^String (second byte-range))) 
        body (get-trimmed-body body-bytes begin end)]
    (partial-response body path)))

(defn get-body-range [body-bytes range-header path]
  (let [byte-range (second (parse-byte-range range-header))] 
    (if (= 2 (count byte-range))
      (get-partial-range body-bytes byte-range path)
      (get-full-body-range body-bytes byte-range path))))

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
