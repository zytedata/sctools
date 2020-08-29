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
             :add_summary 1}
    :on-success on-success
    :on-failure on-failure}))
