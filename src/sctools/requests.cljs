(ns sctools.requests
  (:require [sctools.api :as api]))

(def dash-url "https://app.scrapinghub.com")

(defn api-key-request
  [{:keys [api-key on-success on-failure] :as opts}]
  (api/get-request
   (str dash-url "/api/users/get.json")
   {:params {:apikey api-key}
    :on-success on-success
    :on-failure on-failure}))
