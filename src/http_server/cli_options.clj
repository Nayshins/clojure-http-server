(ns http-server.cli-options
  (:require [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  [["-p" "--port PORT" "Port Number"
    :id :port
    :default 5000
    :parse-fn #(Integer/parseInt %)]

   ["-d" "--directory DIRECTORY" "Directory of public folder"
    :id :directory
    :default "./public"]])

(defn parse [args]
  (let [{:keys [options arguments summary]} (parse-opts args cli-options)]
    options))
