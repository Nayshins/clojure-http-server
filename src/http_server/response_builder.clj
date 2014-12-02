(ns http-server.response-builder
  (:require [clojure.java.io :as io]))

(def response-code { 200 " OK\r\n"
                     204 " NO CONTENT\r\n"
                     206 " PARTIAL CONTENT\r\n"
                     301 " MOVED PERMANENTLY\r\n"
                     400 " BAD REQUEST\r\n"
                     401 " UNAUTHORIZED\r\n"
                     404 " NOT FOUND\r\n"
                     405 " METHOD NOT ALLOWED\r\n"
                     409 " CONFLICT\r\n"
                     500 " INTERNAL SERVER ERROR\r\n"})

(defn build-code [code]
  (byte-array (.getBytes (str "HTTP/1.1 " code (response-code code)))))

(defn build-headers [headers]
  (if (not-empty headers)
    (as-> headers __
         (map #(str (key %) ": " (val %) "\r\n") __)
         (apply str __)
         (.getBytes ^String __)
         (byte-array __))))

(defn build-response [status-map]
  (let [response-byte-arrays
        [(build-code (status-map :status))
         (build-headers (status-map :headers)) 
         (.getBytes "\r\n") 
         (byte-array (status-map :body))]]
    (byte-array (mapcat seq response-byte-arrays)))) 
