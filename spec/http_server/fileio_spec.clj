(ns http-server.fileio-spec
  (:require [http_server.fileio :refer :all]
            [speclj.core :refer :all]))

(def test-path "/tmp/test")

(describe "file interactor"
  (after (spit test-path ""))

  (it "binary slurp reads file contents into byte array"
    (spit test-path "hello world")
    (should= "hello world"  (String. (binary-slurp test-path))))
  
  (it "append to file appends to the file"
    (spit test-path "test")
    (append-to-file test-path "test")
    (should= "testtest" (slurp test-path)))
  
  (it "overwrite-file overwrites file contents"
    (spit test-path "FAIL")
    (overwrite-file test-path "PASS")
    (should= "PASS" (slurp test-path))))




