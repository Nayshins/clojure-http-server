(ns http-server.startup
  (:require [http-server.cli-options :refer [parse]]
            [http-server.server :as server]
            [http-server.handlers :as handlers-helper])
  (:gen-class))

(def directory "./public")

(def handlers [])

(defn -main [& args]
  (let [cli-options (parse args)]
    (prn "starting server")
    (server/serve (server/create (:port cli-options)) handlers)))
