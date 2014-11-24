(ns http-server.startup
  (:require [http-server.cli-options :refer [parse]]
            [http-server.server :as server]
            [http_server.handlers :as handlers-helper]
            [http-server.router :as resource-router])
  (:gen-class))

(def directory (atom ""))

(defn -main [& args]
  (let [cli-options (parse args)]
    (prn "starting server")
    (reset! directory (cli-options :directory))
    (server/serve 
      (server/create (cli-options :port)) 
      handlers-helper/try-handlers handlers)))
