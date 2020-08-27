(ns sctools.api
  (:require [ajax.core :as ajax]))

(def json-formats
  {:format (ajax/transit-request-format)
   :response-format (ajax/transit-response-format)})

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

(defonce epochs (atom nil))
(def increment (fnil inc 0))
(defn next-epoch [kind]
  (-> epochs
      (swap! update kind increment)
      kind))

(defn current-epoch [kind]
  (kind @epochs))

(defn add-epoch-vector [x epoch]
  (let [x (if (vector? x) x [x])]
    (into [(first x) epoch] (rest x))))

(defn wrap-epoch [name params]
  (let [epoch (next-epoch name)]
      (-> params
       (update :on-success add-epoch-vector epoch)
       (update :on-failure add-epoch-vector epoch))))

(defn same-epoch [name v]
  (= (current-epoch name) v))
