(ns http-server.startup
  (:require [http-server.cli-options :refer [parse]]
            [http-server.server :as server])
  (:gen-class))

(def directory (atom ""))

(defn -main [& args]
  (let [cli-options (parse args)]
    (reset! directory (cli-options :directory))
    (server/server (server/create-server-socket (cli-options :port))
                   (cli-options :directory))))
