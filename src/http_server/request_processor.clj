(ns http-server.request-processor
  (:import[java.io BufferedReader])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn parse-request-line [request]
  (let [request-line (zipmap [:action :location :http] 
                             (string/split request #" "))]
   request-line))

(defn get-content-length [headers]
  (if-let [content-length (headers :Content-Length)]
    (Integer. ^String content-length)
    0))

(defn convert-headers-to-hashmap [headers]
  (as-> headers __
    (map #(string/split % #": ") __)
    (map #(hash-map (keyword (first %1)) (second %1)) __)
    (apply merge __)))

(defn process [request]
  (let [parsed-request-line (parse-request-line (request :request-line))
        request-map (dissoc request :request-line)]
        (merge request-map parsed-request-line)))

(defn read-headers [in]
  (take-while
    (partial not= "")
    (line-seq in)))

(defn read-body [^BufferedReader in content-length]
  (let [body (char-array content-length)]
    (.read in body 0 content-length)
    (apply str body)))

(defn read-request [in]
  (let [request (read-headers in) 
        request-line (first request)
        headers (convert-headers-to-hashmap (rest request))
        content-length (get-content-length headers)
        request {:request-line request-line :headers headers}]
    (log/info request-line)
    (if (> content-length 0)
      (assoc request :body (read-body in content-length))
      request)))
