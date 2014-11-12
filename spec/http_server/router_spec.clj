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
  (spit "/tmp/test" text))

(describe "Router"

  (after (write-to-test ""))

  (it "returns file contnets from a GET /file request"
    (should-contain 
      "HTTP/1.1 200 OK\r\n"
             (String. (router "/" {:action "GET" :location "tmp/test"} {}))))

  (it "returns 404 when trying to read nonexistent file"
    (should= "HTTP/1.1 404 NOT FOUND\r\n\r\n" 
             (String. (router path {:action "GET" :location "/foobar"} {}))))


  (it "returns allow header with GET POST OPTIONS PUT HEAD from options"
    (should=
      "HTTP/1.1 200 OK\r\nAllow: GET,HEAD,POST,OPTIONS,PUT\r\n\r\n"
      (String. (router path  {:action "OPTIONS" :location "/"} {} "test"))))

  (it "returns 204 no content on PATCH"
    (should-contain "HTTP/1.1 204"
                    (String. (router "/" {:action "PATCH" :location "tmp/test"} {} ))))

  (it "appends body of the request to the requested file POST"
    (write-to-test "test")
    (should= ok 
             (String. (router path {:action "POST" :location "/test"} {} "test")))
    (should= "test" (slurp (str path "/test"))))

  (it "PUT overwrites current file content"
    (write-to-test "FAIL")
    (should= ok 
             (String. (router path {:action "PUT" :location "/test"} {} "PUT test")))
    (should= "PUT test" (slurp (str path "/test"))))

  (it "deletes file contents with DELETE"
    (write-to-test "FAIL")
    (should= ok 
             (String. 
               (router path {:action "DELETE" :location "/test"} {})))
    (should= "" (slurp (str path "/test"))))

  (it "returns 405 when attempting to post to protected file"
    (with-redefs [http-server.router/config-options {:protected '("/test")}]
      (should-contain "HTTP/1.1 405"
                      (String. 
                        (router path 
                                {:action "PUT" :location "/test"}
                                {} "Hello")))))
  
  (it "should contain query params in the body of the response"
    (with-redefs [http-server.router/special-routes '("/parameters")
                  http-server.router/config-options {:accept-parameters 
                                                     '("/parameters")}] 
      (should-contain 
        "variable_1 = Operators <, >, =, !=; +, -, *, &, @, #, $, [, ]:"
                      (String. 
                        (router path 
                                {:action "GET" :location query-params} 
                                {})))))

 (it "should contain HTML directory in body on GET /"
   (with-redefs [http-server.router/config-options {:directory '("/")}
                 http-server.router/special-routes '("/")]
  (should-contain 
     "<!DOCTYPE html><html><head><title>directory</title></head>"  
             (String. (router path {:action "GET" :location "/"} {}))))) 

 (it "returns 301 for redirect request"
   (with-redefs [http-server.router/config-options {:redirect '("/redirect")}
                 http-server.router/special-routes '("/redirect")]
     (should-contain "HTTP/1.1 301"
                     (String. (router path {:action "GET"
                                           :location "/redirect"} {})))))

  (it "returns 200 ok for HEAD request"
    (should= ok (String. (router path {:action "HEAD" :location "/"} {}))))

  (it "returns 500 for bad request"
    (should-contain "HTTP/1.1 500"
                    (String. (router path {:action "BAD" :location "/"} {})))))
