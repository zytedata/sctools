(ns sctools.studio.machine
  (:require [statecharts.core :as fsm]
            [re-frame.core :as rf]
            [statecharts.rf :as fsm.rf]
            [sctools.api :as api]))

(def studio-path [(rf/path :studio)])

(declare studio-machine)

(rf/reg-event-db
 :studio/fsm-start
 studio-path
 (fn [{:keys [job] :as studio}]
   (assoc studio :state (fsm/initialize studio-machine {:context {:job job}}))))

(rf/reg-event-db
 :studio/fsm-event
 studio-path
 (fn [studio [_ etype data]]
   (assoc studio
          :state (fsm/transition studio-machine
                                 (:state studio)
                                 {:type etype :data data}))))

(defn start-fetch [context event]
  (let [fx (api/job-info-request
            {:job (:job context)
             :on-success [:studio/fsm-event :success-fetch]
             :on-failure [:studio/fsm-event :fail-fetch]})]
    (rf/dispatch [::fsm.rf/call-fx fx])))

(defn on-fetched [context event]
  #p 'on-fetched
  #p context
  #p event)

(defn on-fetch-failed [context event]
  #p 'on-fetch-failed
  #p context
  #p event)

(def studio-machine
  (fsm/machine
   {:id :fetch-job-stats
    :initial :fetching
    :context nil
    :states
    {:fetching {:entry start-fetch
                :on {:success-fetch {:target :fetched
                                     :actions on-fetched}
                     :fail-fetch {:target :fetch-failed
                                  :actions on-fetch-failed}}}
     :fetched {}
     :fetch-failed {:on {:retry :fetching}}}}))

(comment

  ())
