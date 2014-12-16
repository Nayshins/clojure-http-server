(ns http-server.request-processor-spec
  (:require [http-server.request-processor :refer :all]
            [speclj.core :refer :all])
  (:import [java.io InputStreamReader BufferedReader StringReader]))

(describe "Parse request line"

  (it "returns the action from the request"
    (should=
      "GET"
      (:action
        (parse-request-line "GET / HTTP/1.1headerContent-Length: 4"))))

  (it "returns the location of the request"
    (should=
      "/"
      (:location
        (parse-request-line "GET / HTTP/1.1headerContent-Length: 4"))))
  (it "returns the HTTP version"
    (should= "HTTP/1.1" ((parse-request-line "GET / HTTP/1.1") :http))))

(describe "get content length"
  (it "gets the length from header"
    (should= 4 (get-content-length {:Content-Length "4"})))

  (it "should return 0 for headers without content length"
    (should= 0 (get-content-length {:Content-Length nil}))))

(describe "convert headers to hashmap"

  (it "converts header lazy seq to hashmap"
    (should= 
      "value"
      (let [string-seq
            (line-seq (BufferedReader.
                        (StringReader. "key: value\nx-ray: foxtrot")))]
        ((convert-headers-to-hashmap string-seq) :key)))))

(describe "process"
  (it "creates a hashmap of the full request"
    (should= 
      {:http "HTTP/1.1"
       :location "/"
       :action "GET"
       :headers {:Content-Length "0"}}
             (process
               {:request-line "GET / HTTP/1.1"
                :headers {:Content-Length "0"}}))))

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
