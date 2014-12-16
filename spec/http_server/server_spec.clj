(ns http-server.server-spec
  (:require [speclj.core :refer :all]
            [http-server.server :refer :all]
            [http-server.handlers :as handlers-helper]
            [clojure.java.io :refer [reader writer]])
  (:import [java.net Socket]
           [java.io BufferedReader InputStreamReader StringReader]))


(def routes [["GET" "/" {:status 200}]])

(def directory "./public")

(defn app-router [request]
  (some #(handlers-helper/check-route request %) routes))

(defn not-found [request]
  {:status 404})

(def handlers [app-router not-found])

(defn connect []
   (with-open [socket (Socket. "localhost"  5000)]))

(defn multiple-connect [connections]
  (dotimes [n connections]
    (connect)))

(defn test-input-output [request]
  (with-open [socket (Socket. "localhost" 5000)
              out (writer socket)
              in (reader socket)]
    (.write out request)
    (.flush out)
    (.readLine in)))

(describe "create-server"
  (it "creates a ServerSocket"
    (with-open [server-socket (create 5000)]
      (should-be-a java.net.ServerSocket server-socket))))

(describe "server"
  (it "accepts a connection"
    (with-open [ss (create 5000)]
      (future (serve ss handlers))
      (.await server-latch)
      (connect)
      (.await socket-latch)
      (should (> @connection-count 0)))))

(describe "request handler"
  (it "returns 200 OK on GET / request"
    (with-open [ss (create 5000)]
      (future (serve ss handlers))
      (.await server-latch)
      (should= "HTTP/1.1 200 OK" 
               (test-input-output 
                 "GET / HTTP/1.1\r\nContent-Length: 0\r\n\r\n"))))

  (it "returns a 404 if route not found"
    (with-open [ss (create 5000)]
      (future (serve ss handlers))
      (.await server-latch)
      (should= "HTTP/1.1 404 NOT FOUND"
               (test-input-output "GET /foo HTTP/1.1\r\nContent-Length: 0\r\n\r\n")))))
