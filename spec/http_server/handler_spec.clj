(ns http-server.handler-spec
  (:require [speclj.core :refer :all]
            [http-server.handlers :refer :all]))

(defn equals-one [request]
  (if (= 1 request)
    request))

(defn equals-two [request]
  (if (= 2 request)
    request
    nil))

(def simple-fun
  {:status 200})

(defn detailed-fun [request]
  (if (= "GET" (request :action))
    {:status 200}))

(def simple-handlers [equals-one equals-two])

(def routes [["GET" "/" {:status 200}]
             ["GET" "/foobar" simple-fun]
             ["GET" "/function" detailed-fun]
             ["GET" #"\/\d" {:status 200}]])

(defn app-router [request]
  (some #(check-route request %) routes))

(def route-handlers [app-router not-found])

(def request {:action "GET" :location "/"})

(def function-request {:action "GET" :location "/foobar"})

(def detailed-function-request {:action "GET" :location "/function"})

(def bad-request {:action "GET" :location "foo"})

(describe "check-route"
  (it "returns body if routes method and path match the request"
    (should= {:status 200}
             (check-route request (first routes))))
  
  (it "matches a regex route"
    (should= {:status 200}
             (check-route {:action "GET" :location "/1"} (last routes))))

  (it "returns nil if there is no match"
    (should-be-nil (check-route bad-request (first routes)))))

(describe "Try handlers"
  (it "returns the response from the first handler the returns true"
    (should= 1
             (try-handlers simple-handlers 1)))
  
  (it "returns response when route and path match"
    (should= {:status 200}
             (try-handlers route-handlers request)))
 
 (it "evaluates the function of the route"
   (should= {:status 200}
            (try-handlers route-handlers function-request)))

 (it "evaluates the function of a route if it requires the request"
   (should= {:status 200}
            (try-handlers route-handlers detailed-function-request)))

  (it "moves to next handler if no match is found in the first"
    (should= {:status 404}
             (try-handlers route-handlers bad-request))))

