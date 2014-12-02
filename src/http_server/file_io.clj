(ns http-server.file-io
  (:require [clojure.java.io :as io]))

(defn binary-slurp [path]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream path) out)
    (.toByteArray out)))

(defn append-to-file [path body]
  (spit path body :append true))

(defn overwrite-file [path body]
  (spit path body))
