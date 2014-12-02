(ns http-server.static-router
  (:require 
    [pandect.core :as pandect]
    [http-server.file-io :as fileio]
    [http-server.resource-handler :as resource-handler]))

(defn get-route [request directory] 
  (let [query (clojure.string/split (request :location) #"\?") 
        location (first query)
        params (second query)]
    (resource-handler/get-file-data directory location (request :headers))))

(defn options-route [location directory]
  {:status 200 :headers {"Allow" "GET,HEAD,POST,OPTIONS,PUT"}})

(defn patch-route [request directory]
    (let [path (str directory (request :location))
          headers (request :headers)
          file-data (slurp path)
          encoded-file-data (pandect/sha1 file-data)
          etag (headers :If-Match)]
      (fileio/overwrite-file path (request :body))
      {:status 204}))

(defn post-route [request directory]
  (let [location (request :location)
        body (request :body)]
    (fileio/append-to-file (str directory location) body)
    {:status 200}))

(defn put-route [request directory]
  (let [location (request :location)
        body (request :body)] 
    (spit (str directory location) body)
    {:status 200}))

(defn delete-route [request directory]
  (spit (str directory (request :location)) "")
  {:status 200})

(defn head-route [location directory]
  {:status 200})

(defmulti router (fn [request directory]
                   (request :action)))

(defmethod router "GET" [request directory]
  (get-route request directory))

(defmethod router "OPTIONS" [request directory]
    (options-route request directory))

(defmethod router "POST" [request directory]
    (post-route request directory))

(defmethod router "PATCH" [request directory]
    (patch-route request directory))

(defmethod router "PUT" [request directory]
    (put-route request directory))

(defmethod router "DELETE" [request directory]
    (delete-route request directory))

(defmethod router "HEAD" [request directory]
    (head-route request directory))

(defmethod router :default [request directory]
  nil)
