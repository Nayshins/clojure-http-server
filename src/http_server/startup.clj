(ns http-server.startup
  (:require [clojure.tools.cli :refer [parse-opts]]
            [http-server.server :refer :all]
            [http-server.config :refer :all])
  (:import [java.io File])
  (:gen-class))

(set! *warn-on-reflection* true)

(def directory (atom ""))

(def cli-options
  [["-p" "--port PORT" "Port Number"
    :id :port
    :default 5000
    :parse-fn #(Integer/parseInt %)]

   ["-d" "--directory DIRECTORY" "Directory of public folder"
    :id :directory
    :default (str (->  (java.io.File. "") .getAbsolutePath) "/public")]])

(defn -main [& args]
  (let [{:keys [options arguments summary]} (parse-opts args cli-options)]
    (reset! directory (options :directory))
    (server (create-server-socket (options :port)) (options :directory))))
