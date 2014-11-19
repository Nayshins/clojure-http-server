(ns http-server.router-spec
  (:require [http-server.router :refer :all]
            [speclj.core :refer :all])
  (:import [java.io File]))

(def path (str (-> (java.io.File. "") .getAbsolutePath) "/public"))

(def test-path 
  "/tmp/test")

(def query-params "/parameters?variable_1=Operators%20%3C%2C%20%3E%2C%20%3D%2C%20!%3D%3B%20%2B%2C%20-%2C%20*%2C%20%26%2C%20%40%2C%20%23%2C%20%24%2C%20%5B%2C%20%5D%3A%20%22is%20that%20all%22%3F&variable_2=stuff")

(def ok {:status 200})

(defn write-to-test [text]
  (spit "/tmp/test" text))

(describe "Router"

  (after (write-to-test ""))

  (it "returns file contnets from a GET /file request"
    (should= 
      200
             (:status (router "/" {:action "GET" :location "tmp/test"} {}))))

  (it "returns 404 when trying to read nonexistent file"
    (should= {:status 404} 
             (router path {:action "GET" :location "/foobar"} {})))


  (it "returns allow header with GET POST OPTIONS PUT HEAD from options"
    (should=
      {:status 200 :headers {"Allow" "GET,HEAD,POST,OPTIONS,PUT"}}
      (router path  {:action "OPTIONS" :location "/"} {} "test")))

  (it "returns 204 no content on PATCH"
    (should= {:status 204}
             (router "/" {:action "PATCH" :location "tmp/test"} {})))

  (it "appends body of the request to the requested file POST"
    (write-to-test "test")
    (should= ok 
             (router "/" {:action "POST" :location "/tmp/test"} {} "test"))
    (should= "testtest" (slurp "/tmp/test")))

  (it "PUT overwrites current file content"
    (write-to-test "FAIL")
    (should= ok 
             (router "/" {:action "PUT" :location "/tmp/test"} {} "PUT test"))
    (should= "PUT test" (slurp "/tmp/test")))

  (it "deletes file contents with DELETE"
    (write-to-test "FAIL")
    (should= ok 
             (router "/" {:action "DELETE" :location "/tmp/test"} {}))
    (should= "" (slurp "/tmp/test")))

  (it "returns 200 ok for HEAD request"
    (should= ok (router path {:action "HEAD" :location "/"} {})))

  (it "returns 400 for bad request"
    (should= {:status 400}
                    (router path {:action "BAD" :location "/"} {}))))

