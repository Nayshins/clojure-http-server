(ns http-server.startup
  (:require [http-server.cli-options :refer [parse]]
            [http-server.server :as server]
            [http-server.handlers :as handlers-helper])
  (:gen-class))

(def directory "./public")

(def routes [["GET" "/" {:status 200 :body "Hello World"}]])

(defn demo-app-router  [request]
    (some #(handlers-helper/check-route request %) routes))

(def handlers [demo-app-router])

(defn -main [& args]
  (let [cli-options (parse args)]
    (println (str "Starting server on port: " (:port cli-options)))
    (server/serve (server/create (:port cli-options)) handlers)))
