(ns http_server.response-builder
  (:require [clojure.java.io :as io]))

(set! *warn-on-reflection* true)
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

(defn build-body [body]
  (if-not (nil? (first body))
  (first body)))

(defn build-response [code headers & body]
  (let [response-byte-arrays
        [(build-code code) (build-headers headers) (.getBytes "\r\n") (build-body body)]]
    (byte-array (mapcat seq response-byte-arrays))))
