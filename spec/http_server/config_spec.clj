(ns http-server.config-spec
  (:require [http-server.config :refer :all]
            [speclj.core :refer :all]))

(describe "read-config-file"
  (it "reads a file and parses it"
    (spit "/tmp/test.txt" "authenticate: world, !\ndirectory: is, a, test")
    (should= {:authenticate '("world", "!")
              :directory '("is", "a", "test")} 
             (read-config-file "/tmp/test.txt"))))

(describe "config-line-parser"
  (it "creates a hashmap of values split by :"
    (should= {:credentials '("world")} 
             (config-line-parser '("credentials: world"))))
  
  (it "splits the values on , into a sequence"
    (should= {:authenticate '("world", "!")} 
             (config-line-parser '("authenticate: world, !"))))
  
  (it "parses multiple list elements"
    (should= {:directory '("world", "!")
              :authenticate '("is","a","new","line")}
             (config-line-parser 
               '("directory: world, !", "authenticate: is, a, new, line"))))
  (it "does not add a key that has a nil value"
    (should= {:credentials '("world")} 
             (config-line-parser
               '("credentials: world", "fail: "))))
  
  (it "filters unrecognized headers from config opts"
    (should= {:authenticate '("/logs")}
             (config-line-parser '("authenticate: /logs",
                                                  "goodnight: moon"))))

  (it "returns empty hashmap if config does not exist"
    (should= {} (read-config-file "/foo")))
  
  (it "retusn empty hashmap if gonfig is empty"
    (spit "/tmp/test.txt" "")
    (should= {} (read-config-file "/tmp/test.txt"))))
