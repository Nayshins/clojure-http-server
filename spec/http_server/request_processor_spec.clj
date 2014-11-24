(ns http-server.request-processor-spec
  (:require [http-server.request-processor :refer :all]
            [speclj.core :refer :all])
  (:import [java.io BufferedReader StringReader]))

(describe "Parse request line"
  (it "returns the action from the request"
    (should= "GET" ((parse-request-line "GET / HTTP/1.1headerContent-Length: 4") :action)))
  (it "returns the location of the request"
    (should= "/" ((parse-request-line "GET / HTTP/1.1headerContent-Length: 4") :location)))
  (it "returns the HTTP version"
    (should= "HTTP/1.1" ((parse-request-line "GET / HTTP/1.1") :http))))

(describe "get content length"
  (it "gets the length from header"
    (should= 4 (get-content-length {:Content-Length "4"})))
  
  (it "should return 0 for headers without content length"
    (should= 0 (get-content-length {:Content-Length nil}))))

(describe "convert headers to hashmap"
  (it "converts header lazy seq to hashmap"
    (should= "value"
             (let [string-seq
                   (line-seq (BufferedReader. 
                               (StringReader. "key: value\nx-ray: foxtrot")))]
               ((convert-headers-to-hashmap string-seq) :key)))))

(describe "process"
  (it "creates a hashmap of the full request"
    (should= {:http "HTTP/1.1" :location "/" :action "GET" :headers {:Content-Length "0"}}
             (process {:request-line "GET / HTTP/1.1" :headers {:Content-Length "0"}}))))
