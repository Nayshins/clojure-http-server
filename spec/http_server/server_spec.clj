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

(defn resource-router [request]
  (http-server.static-router/router request directory))

(defn not-found [request]
  {:status 404})

(def handlers [app-router resource-router not-found])

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
      (future (serve ss handlers-helper/try-handlers handlers))
      (.await server-latch)
      (connect)
      (.await socket-latch)
      (should (> @connection-count 0)))))

(describe "request reader"
  (it "reads all of the request headers"
     (let [reader (BufferedReader.
                    (InputStreamReader. 
                      (clojure.java.io/input-stream
                        (byte-array (.getBytes 
                          "GET / HTTP/1.1\r\nheader: hello\r\nContent-Length: 4\r\n\r\nbody\r\n\r\n")))))
           headers ((read-request reader) :headers)]
     (should= {:header "hello", :Content-Length "4"}
              headers)
     (should-not-contain "body" headers)))

  (it "reads the body of the request"
    (let [reader (BufferedReader.
                   (InputStreamReader.
                     (clojure.java.io/input-stream 
                       (byte-array
                         (.getBytes
                           "GET / HTTP/1.1\r\nContent-Length: 4\r\n\r\nbody\r\n\r\n")))))]
      (should= "body" ((read-request reader) :body)))))

(describe "request handler"
  (it "returns 200 OK on GET / request"
    (with-open [ss (create 5000)]
      (future (serve ss handlers-helper/try-handlers handlers))
      (.await server-latch)
      (should= "HTTP/1.1 200 OK" 
               (test-input-output 
                 "GET / HTTP/1.1\r\nContent-Length: 0\r\n\r\n"))))
  
  (it "returns 200 for file found with resource router" 
    (with-open [ss (create 5000)]
      (future (serve ss handlers-helper/try-handlers handlers))
      (.await server-latch)
      (should= "HTTP/1.1 200 OK" 
               (test-input-output 
                 "GET /logs HTTP/1.1\r\nContent-Length: 0\r\n\r\n"))))

  (it "returns a 404 if route not found"
    (with-open [ss (create 5000)]
      (future (serve ss handlers-helper/try-handlers handlers))
      (.await server-latch)
      (should= "HTTP/1.1 404 NOT FOUND"
               (test-input-output "GET /foo HTTP/1.1\r\nContent-Length: 0\r\n\r\n")))))
