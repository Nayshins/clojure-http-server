(ns http-server.server
  (:import [java.io BufferedReader DataOutputStream
            InputStreamReader BufferedOutputStream]
           [java.net ServerSocket Socket SocketException]
           [java.lang Integer]
           [java.util.concurrent.CountDownLatch])
  (:require [clojure.java.io :as io]
            [http-server.request-processor :as request-processor]
            [http-server.response-builder :as response-builder]
            [clojure.tools.logging :as log]
            [http-server.handlers :as handlers]))

(set! *warn-on-reflection* true)

(def connection-count (atom 0N))

(def server-latch (java.util.concurrent.CountDownLatch. 1))

(def socket-latch (java.util.concurrent.CountDownLatch. 1))

(defn accept-connection [^ServerSocket server]
  (try
    (.accept server)
    (catch SocketException e)))

(defn create [port]
  (let [ss (ServerSocket. port)]
    (.countDown ^java.util.concurrent.CountDownLatch server-latch)
    ss))

(defn socket-reader [socket]
  (let [reader (BufferedReader. (InputStreamReader. (.getInputStream ^Socket socket)))]
    reader))

(defn socket-writer [socket]
  (let [writer (DataOutputStream.
                 (BufferedOutputStream.
                   (.getOutputStream ^Socket socket)))]
    writer))

(defn write-response [^DataOutputStream out response]
  (with-open [out out] 
    (.write out response 0 (count response))
    (.flush out)))

(defn request-handler [^Socket socket handlers]
  (let [in (socket-reader socket)
        out (socket-writer socket)
        rri (request-processor/read-request in)
        request-map (request-processor/process rri)
        response-map (handlers/try-handlers handlers request-map)
        http-response (response-builder/build-response response-map)]
    (write-response out http-response)))

(defn serve [^ServerSocket server-socket handlers]
  (loop []
    (let [connection (accept-connection server-socket)]
      (future
        (with-open [socket ^Socket connection]
          (swap! connection-count inc)
          (.countDown ^java.util.concurrent.CountDownLatch socket-latch)
          (request-handler connection handlers))))
    (if (.isClosed server-socket)
      (reset! connection-count 0N)
      (recur))))
