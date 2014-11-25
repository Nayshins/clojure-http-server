(ns http-server.startup
  (:require [http-server.cli-options :refer [parse]]
            [http-server.server :as server]
            [http_server.handlers :as handlers-helper])
  (:gen-class))

(def directory "./public")

(def handlers [app-router parameters-router resource-router not-found])
(defn -main [& args]
  (let [cli-options (parse args)]
    (prn "starting server")
    (server/serve 
      (server/create (cli-options :port)) 
      handlers-helper/try-handlers handlers)))
