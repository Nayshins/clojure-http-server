(ns http-server.startup
  (:require [http-server.cli-options :refer [parse]]
            [http-server.server :as server]
            [http_server.handlers :as handlers-helper]
            [http-server.router :as resource-router])
  (:gen-class))

(def directory (atom ""))

(def routes '(["GET" "/" {:status 200}]))

(defn app-router [request]
  (some #(handlers-helper/check-route request %) routes))

(defn file-router [request]
  (resource-router/router request directory))

(def handlers [handlers-helper/not-found file-router app-router])

(defn -main [& args]
  (let [cli-options (parse args)]
    (prn "starting server")
    (reset! directory (cli-options :directory))
    (server/serve 
      (server/create (cli-options :port)) 
      handlers-helper/try-handlers handlers)))
