(ns http-server.handlers)

(defn not-found [request]
  {:status 404})

(defmulti evaluate-route (fn [fun request]
                           (class fun)))

(defmethod evaluate-route clojure.lang.APersistentMap [fun request]
  fun)

(defmethod evaluate-route clojure.lang.IFn [fun request]
  (fun request))

(defmulti check-path (fn [route-path request-path]
                       (class route-path)))

(defmethod check-path java.util.regex.Pattern [route-path request-path]
  (re-matches route-path request-path))

(defmethod check-path java.lang.String [route-path request-path]
  (= route-path request-path))

(defn check-route [request route]
  (let [[method path fun] route]
  (if (and (= method (request :action))
           (check-path path (:location request)))
    (evaluate-route fun request))))

(defn try-handlers [handlers request]
  (let [fun (first handlers)]
    (if-let [response (fun request)]
      response
      (recur (rest handlers) request))))
