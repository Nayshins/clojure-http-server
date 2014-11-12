# Simple Http Server Written in Clojure
[![Build Status](https://travis-ci.org/Nayshins/clojure-http-server.svg?branch=master)](https://travis-ci.org/Nayshins/clojure-http-server)
## Requirments 
Leiningen 2.5 is required in order to run the server. Installation instructions can be found [here](http://leiningen.org/)

## Initial Setup
1. Clone repository into directory of your chosing
2. cd into the clojure-http-server directory
3. To install dependencies, run:
```
$ lein deps 
```
## Configure Server
The server can handle special routes by editing the config file. The accepted headings are :authenticate and :credentials, :accept-parameters, :protected, :redirect, :directory. 

The format of the file is heading: /file

An example config file to have the root path return a directory and the logs be protected by basic auth would look like:

```
directory: /

authenticate: /logs
credentials: admin:password
```

## Running the Server
Once you have your server configured to your needs, you can start the server. The command line takes 2 arguments with the run server command.
```
-p --port Port number you want the server to listen on
-d --directory Location of the public directory you would like to server
```
An example to run the server on port 5000 and server a public directory looks like

```
$ lein run -p 5000 -d /public
```
