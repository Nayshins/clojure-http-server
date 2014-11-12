(ns http-server.request-parser
 (:require [clojure.string :as str]
           [clojure.tools.logging :as log]))

(defn parse-request-line [request]
  (let [request-line (zipmap [:action :location :http] 
                             (str/split request #" "))]
   request-line))

(defn get-content-length [headers]
  (if-let [content-length (headers :Content-Length)]
    (Integer. ^String content-length)
    0))

(defn convert-headers-to-hashmap [headers]
  (as-> headers __
    (map #(clojure.string/split % #": ") __)
    (map #(hash-map (keyword (first %1)) (second %1)) __)
    (apply merge __)))