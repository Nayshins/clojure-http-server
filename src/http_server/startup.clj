(ns http-server.startup
  (:require [http-server.cli-options :refer [parse]]
            [http-server.server :as server])
  (:gen-class))

(def directory "./public")

(def handlers [])

(defn -main [& args]
  (let [cli-options (parse args)]
    (prn "starting server")
    (server/serve (server/create (:port cli-options)) handlers)))
