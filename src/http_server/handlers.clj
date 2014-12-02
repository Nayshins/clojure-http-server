(ns http-server.handlers)

(defn not-found [request]
  {:status 404})

(defmulti evaluate-route (fn [fun request]
                           (class fun)))

(defmethod evaluate-route clojure.lang.APersistentMap [fun request]
  fun)

(defmethod evaluate-route clojure.lang.IFn [fun request]
  (fun request))

(defn check-route [request route]
  (let [[method path fun] route]
  (if (and (= method (request :action))
           (= path (request :location)))
    (evaluate-route fun request))))

(defn try-handlers [handlers request]
  (let [fun (first handlers)]
    (if-let [response (fun request)]
      response
      (recur (rest handlers) request))))
