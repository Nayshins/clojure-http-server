# Simple Http Server Written in Clojure
[![Build Status](https://travis-ci.org/Nayshins/clojure-http-server.svg?branch=master)](https://travis-ci.org/Nayshins/clojure-http-server)

A simple and extensible http server written in clojure. This server takes connections over TCP, and follows a middleware pattern similar to ring and compojure.

## Requirements 
Leiningen 2.5 is required in order to run the server. Installation instructions can be found [here](http://leiningen.org/)

## Initial Setup
1. Clone repository into directory of your chosing
2. cd into the clojure-http-server directory
3. To install dependencies, run:
```
$ lein deps 
```
## Configure Server
The server follows a middleware pattern similar to Ring and Compojure. The
server returns a hashmap of the response with the following keys: 
- :location String of the location of the request
- :method - The method of the http request
- :headers - Hashmap of headers from the request.
- :body - String of the request body.

The server expects a hashmap to contain a :status header containing an integer
of the response code. This is the only required key in the hashmap. The response
also accepts the :headers and :body keys. The headers key should be a hashmap of
the headers for the response, and the body key should be a string or byte-array
of the response body.

The server provides access to the resource-handler middleware which will serve
files off of the directory set at runtime. To use the resource router, require
http-server.resource-handler in the startup namespace and reference the router
function. Then enter then enter the function into the handlers vector.

Custom routes can be created by adding a route vector that looks like this 
```clojure
["GET" "/" {:status 200}] 
```
to the vector of vectors called routes. To use this routes vector you will need
to create a handler that will check each route to see if returns the body or
nil. The serve provides a handlers/check-route function that will check to see
if the method and path match, and if they match return the body. 

You can provide any handler into the handlers vector, as long as the handler returns a hashmap or nil.
To return a 404, place the handlers/not-found into the last position of the handlers vector.
## Running the Server
Once you have your server configured to your needs, you can start the server. The command line takes 2 arguments with the run server command.
```
-p --port Port number you want the server to listen on
-d --directory Location of the public directory you would like to server
```
An example command to run the server on port 5000 with the public directory looks like
```
$ lein run -p 5000 -d ./public
```
