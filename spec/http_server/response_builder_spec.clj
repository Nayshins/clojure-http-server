(ns http-server.response-builder-spec
  (:require [http-server.response-builder :refer :all]
            [speclj.core :refer :all]))

(describe "Build Code"
  (it "returns a http response code line"
    (should= "HTTP/1.1 200 OK\r\n" (String. (build-code 200)))))

(describe "build headers"
  (it "converts headers from hashmap to byte-array"
    (should= "header-key: header-value\r\n" 
             (String. (build-headers {"header-key" "header-value"})))))

(describe "build-response"
  (it "builds the full response"
    (should= "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\nHelloWorld"
             (String. (build-response {:status 200
                                       :headers {"Content-Length" "0"}
                                       :body (byte-array (.getBytes "HelloWorld"))})))))
