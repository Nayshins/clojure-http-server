(ns http-server.server
  (:import [java.io BufferedReader DataOutputStream
            InputStreamReader BufferedOutputStream]
           [java.net ServerSocket Socket SocketException]
           [java.lang Integer]
           [java.util.concurrent.CountDownLatch])
  (:require [clojure.java.io :as io]
            [http-server.request-parser :as request-parser]
            [http-server.router :as router]
            [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)

(def connection-count (atom 0N))

(def server-latch (java.util.concurrent.CountDownLatch. 1))

(def socket-latch (java.util.concurrent.CountDownLatch. 1))

(defn accept-connection [^ServerSocket server]
  (try
    (.accept server)
    (catch SocketException e)))

(defn create-server-socket [port]
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

(defn read-headers [in]
  (take-while
    (partial not= "")
    (line-seq in)))


(defn read-body [^BufferedReader in content-length]
  (let [body (char-array content-length)]
    (.read in body 0 content-length)
    (apply str body)))

(defn read-request [in]
  (let [request (read-headers in) 
        request-line (first request)
        headers (request-parser/convert-headers-to-hashmap (rest request))
        content-length (request-parser/get-content-length headers)
        request {:request-line request-line  :headers headers}]
    (log/info request-line)
    (if (> content-length 0)
      (assoc request :body (read-body in content-length))
      request)))


(defn write-response [^DataOutputStream out response]
  (with-open [out out] 
    (.write out response 0 (count response))
  (.flush out)))

(defn socket-handler [^Socket socket directory]
    (let [in (socket-reader socket)
          out (socket-writer socket)
          rri (read-request in)
          parsed-request (request-parser/parse-request-line (rri :request-line))]
      (let [response (router/router 
                       directory parsed-request 
                       (rri :headers)(rri :body))]
        (write-response out response))))

(defn server [^ServerSocket server-socket directory]
  (loop []
    (let [connection (accept-connection server-socket)]
      (future
        (with-open [socket ^Socket connection]
          (swap! connection-count inc)
          (.countDown ^java.util.concurrent.CountDownLatch socket-latch)
          (socket-handler connection directory))))
    (if (.isClosed server-socket)
      (reset! connection-count 0N)
      (recur))))



