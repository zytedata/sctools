(ns sctools.studio.machine
  (:require [statecharts.core :as fsm :refer [assign]]
            [applied-science.js-interop :as j]
            [statecharts.rf :as fsm.rf]
            [re-frame.core :as rf]
            [kitchen-async.promise :as p]
            [lambdaisland.glogi :as log]
            [sctools.api :as api]
            [linked.core :as linked]
            [sctools.studio.cache :refer [get-cached-info cache-job-info]]
            [sctools.studio.utils :refer [spider-name-from-results get-job-number]]))

(def studio-path [(rf/path :studio)])

(declare studio-machine)

(def results-cache 1)

(defonce id (volatile! 0))

(def concurrent-fetches 5)

(rf/reg-event-db
 :studio/fsm-start
 studio-path
 (fn [studio [_ {:keys [spider from to]}]]
   (assert (and (pos? from)
                (pos? to)
                (> to from))
     (str "FROM and TO must be positive numbers, and FROM must be greater than TO"))
   (assoc studio :state
          (fsm/initialize studio-machine
                          {:context {:spider spider
                                     ;; Increase the counter so if
                                     ;; there is already a maching
                                     ;; running, the events for that
                                     ;; old machine would not be
                                     ;; triggered anymore.
                                     :epoch (vswap! id inc)
                                     :from from
                                     :to to
                                     :results {}}}))))

(defn current-epoch [studio]
  (get-in studio [:state :context :epoch]))

(rf/reg-event-db
 :studio/fsm-event
 studio-path
 (fn [studio [_ etype meta data]]
   (if (= (current-epoch studio) (:epoch meta))
     (assoc studio
            :state (fsm/transition studio-machine
                                   (:state studio)
                                   {:type etype
                                    :meta (dissoc meta :epoch)
                                    :data data}))
     (do
       (log/info :msg "event for an stale machine" :etype etype)
       studio))))

(defn handle-next
  [current from]
  (cond
    (nil? current)
    from

    :else
    (inc current)))

(defn fetch-one-impl
  [{:keys [spider current from results epoch] :as context}]
  (let [current (handle-next current from)
        job (str spider "/" current)
        meta {:epoch epoch :job job}]
    (p/let [info (get-cached-info job)]
      (if info
        (do
          (log/debug :msg (str "using cached info for job " job))
          (js/setTimeout
           #(rf/dispatch [:studio/fsm-event :success-fetch meta info])
           0))
        (let [fx (api/job-info-request
                  {:job job
                   :on-success [:studio/fsm-event :success-fetch meta]
                   :on-failure [:studio/fsm-event :fail-fetch meta]})]
          (log/debug :msg (str "fetching job " job))
          (rf/dispatch [::fsm.rf/call-fx fx]))))
    (assoc context :current current)))

(defn fetch-one
  [{:keys [current from to] :as context}]
  (cond
    (nil? current)
    ;; On the first call, kick start multiple fetching threads
    (let [concurrency (min concurrent-fetches
                           (inc (- to from)))]
      (reduce fetch-one-impl context (range concurrency)))

    :else
    (fetch-one-impl context)))

(defn all-jobs-fetched?
  [{:keys [from to results]}]
  (= (count results)
     (- to from)))

(defn all-jobs-requested?
  [{:keys [current from to]}]
  (= current to))

(defn current-job [{:keys [spider current] :as context}]
  (str spider "/" current))

(defn on-fetched [context {:keys [data meta]}]
  (let [job (:job meta)
        info {:success true :info data}]
    (log/debug :msg (str "fetched " job))
    (when (= (get data "state") "finished")
      (cache-job-info job data))
    (-> context
        (assoc-in [:results job] info)
        (assoc :spider-name (get data "spider")))))

(defn on-fetch-failed [context {:keys [data meta]}]
  (let [job (:job meta)
        info {:success false :error data}]
    (log/debug :msg (str "failed to fetch " job) :error data)
    (assoc-in context [:results job] info)))

(defn add-to-recent [{:keys [spider from to spider-name]}]
  (rf/dispatch [:studio/add-recent {:from (str spider "/" from)
                                    :to (str spider "/" to)
                                    :spider-name spider-name}]))

(defn compare-job-id [j1 j2]
  (compare (get-job-number j1) (get-job-number j2)))

(defn sort-results [{:keys [results] :as context}]
  (assoc context :results
         (->> results
              (sort (fn [[j1 _] [j2 _]]
                      (compare-job-id j1 j2)))
              (into (linked/map)))))

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

                                        ;; When this request is not
                                        ;; the last one but there is
                                        ;; no more request to send, do
                                        ;; an internal
                                        ;; self-transition.
                                        {:guard   all-jobs-requested?
                                         :actions (assign on-fetched)}

                                        {:target  :fetching
                                         :actions (assign on-fetched)}]

                        :fail-fetch    [{:guard   all-jobs-fetched?
                                         :target  :fetched
                                         :actions (assign on-fetch-failed)}

                                        {:guard   all-jobs-requested?
                                         :actions (assign on-fetched)}

                                        {:target  :fetching
                                         :actions (assign on-fetch-failed)}]}}
     :fetched  {:entry [add-to-recent
                        (assign #'sort-results)]}}}))

(comment
  (rf/dispatch [:studio/fsm-start {:spider "1887/861" :from 136449 :to 136451}])

  ())
