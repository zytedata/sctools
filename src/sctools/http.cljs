(ns sctools.http
  (:require [ajax.core :as ajax]))

(def json-formats
  {:format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? false})})

(defn wrap-vector-value
  "Wrap the value of k in m to be a vector because rf/dispatch wants
  that."
  [m k]
  (if (and (contains? m k)
           (not (vector? (get m k))))
    (update m k vector)
    m))

(defn wrap-callbacks-vector [opts]
  (-> opts
      (wrap-vector-value :on-success)
      (wrap-vector-value :on-failure)))

(defn post-request
  [url opts]
  (let [opts (-> opts
                 wrap-callbacks-vector)
        ajax-spec (merge {:method :post
                          :uri url}
                         json-formats
                         opts)]
    {:http-xhrio ajax-spec}))

(defn get-request
  [url opts]
  (let [opts (-> opts
                 wrap-callbacks-vector)
        ajax-spec (merge {:method :get
                          :uri url}
                         json-formats
                         opts)]
    {:http-xhrio ajax-spec}))

(defn delete-request
  [url opts]
  (let [opts (-> opts
                 wrap-callbacks-vector)
        ajax-spec (merge {:method :delete
                          :uri url}
                         json-formats
                         opts)]
    {:http-xhrio ajax-spec}))

