(ns http-server.server
  (:import [java.io BufferedReader DataOutputStream
            InputStreamReader BufferedOutputStream]
           [java.net ServerSocket Socket SocketException]
           [java.lang Integer])
  (:require [clojure.java.io :as io]
            [http-server.request-parser :refer :all]
            [http-server.router :refer :all]
            [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)

(def connection-count (atom 0N))

(defn accept-connection [^ServerSocket server]
  (try
    (.accept server)
    (catch SocketException e)))

(defn create-server-socket [port]
  (ServerSocket. port))

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
        headers (convert-headers-to-hashmap (rest request))
        content-length (get-content-length headers)
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
          parsed-request (parse-request-line (rri :request-line))]
      (let [response (router 
                       directory parsed-request 
                       (rri :headers)(rri :body))]
        (write-response out response))))

(defn server [^ServerSocket server-socket directory]
  (loop []
    (let [connection (accept-connection server-socket)]
      (future
        (with-open [socket ^Socket connection]
          (swap! connection-count inc)
          (socket-handler connection directory))))
    (if (.isClosed server-socket)
      (reset! connection-count 0N)
      (recur))))



