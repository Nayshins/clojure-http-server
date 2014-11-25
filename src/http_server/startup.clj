(ns http-server.startup
  (:require [http-server.cli-options :refer [parse]]
            [http-server.server :as server]
            [http_server.handlers :as handlers-helper])
  (:gen-class))

(def directory "./public")

(defn authenticate [request]
  (let [headers (request :headers)] 
    (if (headers :Authorization)
      (http-server.router/get-route request directory)
      {:status 401 :body (byte-array (.getBytes "Authentication required"))})))



(def directory-links (http-server.router/build-directory directory))

(def routes [["GET" "/" directory-links]
             ["PUT" "/file1" {:status 405}]
             ["POST" "/text-file.txt" {:status 405}]
             ["GET" "/redirect" {:status 301
                                 :headers {"Location" "http://localhost:5000/"}}]
             ["GET" "/logs" authenticate]])

(defn app-router [request]
  (some #(handlers-helper/check-route request %) routes))

(defn parameters-router [request]
  (prn request)
  (let [query (clojure.string/split (request :location) #"\?")
        location (first query)
        params (second query)]
    (prn params)
    (if (nil? params)
      nil
      {:status 200 :body (http-server.router/decode-params params)})))

(defn resource-router [request]
  (http-server.router/router request directory))

(defn not-found [request]
  {:status 404})

(def handlers [app-router parameters-router resource-router not-found])
(defn -main [& args]
  (let [cli-options (parse args)]
    (prn "starting server")
    (server/serve 
      (server/create (cli-options :port)) 
      handlers-helper/try-handlers handlers)))
