(ns http-server.request-processor
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
