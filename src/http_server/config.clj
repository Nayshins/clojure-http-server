(ns http-server.config
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(def allowed-keys [:directory :accept-parameters :authenticate 
                   :credentials :redirect :protected])

(defn split-values [values]
  (if (empty? values)
    ""
  (map string/trim  (string/split values #","))))


(defn combine-into-hashmap [line]
  (let [hash-key (keyword (first line))
        value (second line)
        values-list (split-values value)]
    (if (empty? values-list)
      {}
      (hash-map hash-key values-list))))

(defn config-line-parser [config-seq]
  (as-> config-seq __
    (remove empty? __)
    (map #(string/split % #": ") __)
    (map combine-into-hashmap __)
    (apply merge __)
    (select-keys __ allowed-keys)))

(defn read-config-file [file]
  (try
  (with-open [reader (io/reader (io/file file))]
    (let [config-opts (config-line-parser (doall (line-seq reader)))]
      (if (nil? config-opts)
        {}
        config-opts)))
  (catch Exception e 
    (prn e)
    {}))) 

