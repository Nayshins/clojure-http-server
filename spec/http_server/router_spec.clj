(ns http-server.router-spec
  (:require [http-server.router :refer :all]
            [speclj.core :refer :all])
  (:import [java.io File]))

(def path (str (-> (java.io.File. "") .getAbsolutePath) "/public"))

(def test-path 
  (str (-> (java.io.File. "") .getAbsolutePath) "/test"))

(def query-params "/parameters?variable_1=Operators%20%3C%2C%20%3E%2C%20%3D%2C%20!%3D%3B%20%2B%2C%20-%2C%20*%2C%20%26%2C%20%40%2C%20%23%2C%20%24%2C%20%5B%2C%20%5D%3A%20%22is%20that%20all%22%3F&variable_2=stuff")

(def ok "HTTP/1.1 200 OK\r\n\r\n")

(defn write-to-test [text]
  (spit (str path "/test") text))

(describe "Router"
  (after (write-to-test ""))

  (it "returns file contnets from a GET /file request"
    (should= 
      "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 15\r\n\r\nfile1 contents\n"
             (String. (router path {:action "GET" :location "/file1"} {}))))

  (it "returns directory links from GET /"
    (should= 
      "HTTP/1.1 200 OK\r\nContent-Length: 114\r\n\r\n<!DOCTYPE html><html><head><title>directory</title></head><body><a href=\"/file.txt\">file.txt</a><br></body></html>" 
             (String. (router test-path {:action "GET" :location "/"} {}))))

  (it "returns 404 when trying to read nonexistent file"
    (should= "HTTP/1.1 404 NOT FOUND\r\n\r\n" 
             (String. (router path {:action "GET" :location "/foobar"} {}))))

  (it "returns 206 partial when given a range header"
    (should-contain "HTTP/1.1 206 PARTIAL CONTENT\r\n"
                    (String. (router path {:action "GET" :location "/partial_content.txt"}
                                     {:Range "bytes=0-4"}))))

  (it "returns 301 on GET /redirect"
    (should-contain "HTTP/1.1 301"
                    (String. (router path {:action "GET" :location "/redirect"} {})))) 

  (it "returns 200 when give query params"
    (should-contain "HTTP/1.1 200 OK\r\n"
                    (String. (router path {:action "GET" :location query-params} {}))))

  (it "should contain query params in the body of the response"
    (should-contain "variable_1 = Operators <, >, =, !=; +, -, *, &, @, #, $, [, ]:"
                    (String. (router path {:action "GET" :location query-params} {}))))

  (it "returns allow header with GET POST OPTIONS PUT HEAD from options"
    (should=
      "HTTP/1.1 200 OK\r\nAllow: GET,HEAD,POST,OPTIONS,PUT\r\n\r\n"
      (String. (router path  {:action "OPTIONS" :location "/"} {} "test"))))

  (it "returns 204 no content on PATCH"
    (should-contain "HTTP/1.1 204"
                    (String. (router path {:action "PATCH" :location "/test"} {} ))))

  (it "appends body of the request to the requested file POST"
    (write-to-test "test")
    (should= ok 
             (String. (router path {:action "POST" :location "/test"} {} "test")))
    (should= "testtest" (slurp (str path "/test"))))

  (it "PUT overwrites current file content"
    (write-to-test "FAIL")
    (should= ok 
             (String. (router path {:action "PUT" :location "/test"} {} "PUT test")))
    (should= "PUT test" (slurp (str path "/test"))))

  (it "should not put to protected file"
    (should= "HTTP/1.1 405 METHOD NOT ALLOWED\r\n\r\n"
             (String.
               (router path
                       {:action "PUT" :location "/file1"} {} "file1 contents"))))

  (it "deletes file contents with DELETE"
    (write-to-test "FAIL")
    (should= ok 
             (String. 
               (router path {:action "DELETE" :location "/test"} {})))
    (should= "" (slurp (str path "/test"))))

  (it "returns 200 ok for HEAD request"
    (should= ok (String. (router path {:action "HEAD" :location "/"} {}))))

  (it "returns 500 for bad reuest"
    (should-contain "HTTP/1.1 500"
                    (String. (router path {:action "BAD" :location "/"} {})))))
