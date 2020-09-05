(ns sctools.studio.machine
  (:require [statecharts.core :as fsm :refer [assign]]
            [statecharts.rf :as fsm.rf]
            [re-frame.core :as rf]
            [kitchen-async.promise :as p]
            [lambdaisland.glogi :as log]
            [sctools.api :as api]
            [sctools.studio.cache :refer [get-cached-info cache-job-info]]
            [sctools.studio.utils :refer [spider-name-from-results]]))

(def studio-path [(rf/path :studio)])

(declare studio-machine)

(def results-cache 1)

(rf/reg-event-db
 :studio/fsm-start
 studio-path
 (fn [studio [_ {:keys [spider from to]}]]
   (assoc studio :state
          (fsm/initialize studio-machine
                          {:context {:spider spider :from from :to to}}))))

(rf/reg-event-db
 :studio/fsm-event
 studio-path
 (fn [studio [_ etype data]]
   (assoc studio
          :state (fsm/transition studio-machine
                                 (:state studio)
                                 {:type etype :data data}))))

(defn fetch-one
  [{:keys [spider current from results] :as context}]
    (let [current (or current from)
         job (str spider "/" current)]
      (p/let [info (get-cached-info job)]
        (if info
          (do
            (log/debug :msg (str "using cached info for job " job))
            (js/setTimeout #(rf/dispatch [:studio/fsm-event :success-fetch info])
                           0))
          (let [fx (api/job-info-request
                    {:job job
                     :on-success [:studio/fsm-event :success-fetch]
                     :on-failure [:studio/fsm-event :fail-fetch]})]
            (log/debug :msg (str "fetching job " job))
            (rf/dispatch [::fsm.rf/call-fx fx]))))
      (assoc context :current current)))

(defn all-jobs-fetched?
  [{:keys [from to current]}]
  (= current to))

(defn current-job [{:keys [spider current] :as context}]
  (str spider "/" current))

(defn on-fetched [context {:keys [data]}]
  (let [job (current-job context)
        info {:success true :info data}]
    (log/debug :msg (str "fetched " job))
    (cache-job-info job data)
    (-> context
        (assoc-in [:results job] info)
        (assoc :spider-name (get data "spider")))))

(defn on-fetch-failed [context {:keys [data]}]
  (let [job (current-job context)
        info {:success false :error data}]
    (log/debug :msg (str "failed to fetch " job) :error data)
    (assoc-in context [:results job] info)))

(defn handle-next [context]
  (update context :current inc))

(defn add-to-recent [{:keys [spider from to spider-name]}]
  (rf/dispatch [:studio/add-recent {:from (str spider "/" from)
                                    :to (str spider "/" to)
                                    :spider-name spider-name}]))

(def studio-machine
  (fsm/machine
   {:id      :fetch-job-stats
    :initial :fetching
    :context nil
    :states
    {:fetching {:entry (assign #'fetch-one)
                :on    {:success-fetch [{:guard   all-jobs-fetched?
                                         :actions (assign on-fetched)
                                         :target  :fetched}
                                        {:target  :fetching
                                         :actions [(assign on-fetched)
                                                   (assign handle-next)]}]
                        :fail-fetch    [{:guard   all-jobs-fetched?
                                         :target  :fetched
                                         :actions (assign on-fetch-failed)}
                                        {:target  :fetching
                                         :actions [(assign on-fetch-failed)
                                                   (assign handle-next)]}]}}
     :fetched  {:entry add-to-recent}}}))

(comment
  (rf/dispatch [:studio/fsm-start {:spider "1887/861" :from 136449 :to 136451}])

  ())
