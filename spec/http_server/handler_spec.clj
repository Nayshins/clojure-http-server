(ns http-server.handler-spec
  (:require [speclj.core :refer :all]
            [http_server.handlers :refer :all]
            [http-server.router :as resource-router]))

(defn equals-one [request]
  (if (= 1 request)
    request))

(defn equals-two [request]
  (if (= 2 request)
    request
    nil))

(def simple-handlers [equals-one equals-two])

(def routes '(["GET" "/" {:status 200}]))

(defn app-router [request]
  (some #(check-route request %) routes))

(defn file-router [request]
  (resource-router/router request))

(def route-handlers [app-router not-found])

(def request {:action "GET" :location "/"})
(def bad-request {:action "GET" :location "foo"})

(describe "check-route"
  (it "returns body if routes method and path match the request"
    (should= {:status 200}
             (check-route request (first routes))))
  
  (it "returns nil if there is no match"
    (should-be-nil (check-route bad-request (first routes)))))

(describe "Try handlers"
  (it "returns the response from the first handler the returns true"
    (should= 1
             (try-handlers simple-handlers 1)))
  
  (it "returns response when route and path match"
    (should= {:status 200}
             (try-handlers route-handlers request)))
  
  (it "moves to next handler if no match is found in the first"
    (should= {:status 404}
             (try-handlers route-handlers bad-request))))

