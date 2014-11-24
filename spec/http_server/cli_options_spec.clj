(ns http-server.cli-options-spec
  (:require [speclj.core :refer :all]
            [http-server.cli-options :as cli]))

(describe "cli options"
  
  (it "Defaults port to 5000"
    (should= 5000 ((cli/parse []) :port)))
  
  (it "Converts port string to int"
    (should= 8080
             ((cli/parse ["-p", "8080"]) :port)))
  
  (it "Sets directory to ./public by default"
    (should= "./public"
             ((cli/parse []) :directory)))
  
  (it "Sets directory to the supplied string"
    (should= "test"
             ((cli/parse ["-d", "test"]) :directory)))
  
  (it "Sets both directory and port"
    (let [cli-opts (cli/parse ["-p", "8080", "-d", "test"])]
      (should= 8080 (cli-opts :port))
      (should= "test" (cli-opts :directory)))))
