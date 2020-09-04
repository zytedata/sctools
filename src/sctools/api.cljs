(ns sctools.api
  (:require [sctools.http :as http]))

(def dash-url "https://app.scrapinghub.com")
(def hs-url "https://storage.scrapinghub.com")

(defn load-api-key []
  (get-in @re-frame.db/app-db [:init :api-key]))

(defn api-key-request
  [{:keys [api-key on-success on-failure] :as opts}]
  (http/get-request
   (str dash-url "/http/users/get.json")
   {:params {:apikey api-key}
    :on-success on-success
    :on-failure on-failure}))

(defn job-info-request
  [{:keys [job on-success on-failure] :as opts}]
  (http/get-request
   (str hs-url "/jobs/" job)
   {:params {:apikey (load-api-key)
             :format :jl
             :add_summary 1}
    :on-success on-success
    :on-failure on-failure}))

(comment
  (defprotocol Proto1
    :extend-via-metadata true
    (method1 [this])
    (method2 [this]))

  (def obj1
    (with-meta
      {:a 1}
      {`method1 (fn [this] (get this :a))
       `method2 (fn [this] (inc (get this :a)))}))

  (method1 obj1)
  (method2 obj1)

  (def obj2
    (with-meta
      {:a 1}
      {`method1 (fn [this] (get this :a))
       ;; `method2 (fn [this] (get this :a))
       }))

  (method1 obj2)
  (method2 obj2)
  
  ())
