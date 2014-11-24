(ns http_server.handlers)

(defn not-found [request]
  {:status 404})

(defn check-route [request route]
  (let [[method path fun] route]
  (if (and (= method (request :action))
           (= path (request :location)))
    fun)))

(defn try-handlers [handlers request]
  (let [fun (peek handlers)]
    (if-let [response (fun request)]
      response
      (recur (pop handlers) request))))
