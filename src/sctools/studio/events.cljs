(ns sctools.studio.events
  (:require [bb.clojure :refer [prog1]]
            [clojure.string :as str]
            [linked.core :as linked]
            [sctools.utils.transit-utils :as transit]
            [re-frame.core :as rf]
            [sctools.utils.uri :refer [add-query]]
            [sctools.studio.utils :refer [info-displays]]
            [sctools.utils.local-storage :as local-storage]))

(def studio-path [(rf/path :studio)])
(def filters-path [(rf/path :studio :filters)])
(def prefs-path [(rf/path :studio :prefs)])
(def sorts-path [(rf/path :studio :sorts)])

(def prefs-storage-key :sctools.studio/prefs)
(def recents-storage-key :sctools.studio/recents)

(defn load-prefs []
  (-> (or (local-storage/get-transit prefs-storage-key)
          {:columns info-displays})
      (update :stats #(into (sorted-set) %))))

(rf/reg-event-db
 :studio/init
 studio-path
 (fn [studio]
   (cond-> studio
     (not (contains? studio :recents))
     (assoc :recents (into (linked/set)
                           (local-storage/get-transit recents-storage-key)))

     (not (contains? studio :prefs))
     (assoc :prefs (load-prefs)))))

(defn sync-job-ids [{:keys [from to] :as studio} old-from]
  (if (or (str/blank? to)
          (and old-from (str/starts-with? old-from to)))
    (assoc studio :to from)
    studio))

(defn get-spider [job]
  (->> (str/split job #"/")
       (take 2)
       (str/join "/")))

(defn get-job-number [job]
  (-> (str/split job #"/")
      last
      js/Number))


(rf/reg-event-db
 :studio/update-jobs
 studio-path
 (fn [{:keys [from] :as studio}  [_ [from to]]]
   (-> studio
       (assoc :from from :to to))))

(rf/reg-event-db
 :studio/update-from
 studio-path
 (fn [{:keys [from] :as studio}  [_ job]]
   (-> studio
       (assoc :from job)
       (sync-job-ids from))))

(rf/reg-event-db
 :studio/update-to
 studio-path
 (fn [studio [_ job]]
   (assoc studio :to job)))

(defn add-recent [recents record]
  (let [recents (->> recents
                     (remove #(= % record))
                     (#(conj % record)))]
    (if (> (count recents) 5)
      (->> recents
           (take 5))
      recents)))

(rf/reg-event-db
 :studio/add-recent
 studio-path
 (fn [studio [_ record]]
   (prog1 (update studio :recents add-recent record)
     (local-storage/set-transit recents-storage-key
                                (vec (:recents <>))))))

(defn is-job-valid? [job]
  (and (not (str/blank? job))
       (re-matches #"[1-9][0-9]*/[1-9][0-9]*/[1-9][0-9]*" job)))

(defn nav-to-job [from to]
  (let [spider (get-spider from)
        to (get-job-number to)]
    (rf/dispatch [:app/push-state (str "/studio/job/" from "/_/" to)])))

(defn nav-to-chart [from to params]
  (let [spider (get-spider from)
        to (get-job-number to)]
    (rf/dispatch
     [:app/push-state
      (add-query (str "/studio/chart/" from "/_/" to)
                 {:q (-> params
                         transit/write-transit
                         js/JSON.stringify)})])))

(rf/reg-event-db
 :studio/submit
 studio-path
 (fn [{:keys [from to] :as studio}]
   (assert (and (is-job-valid? from)
                (is-job-valid? to)))
   (nav-to-job from to)
   (update studio :state dissoc :value)))

(rf/reg-event-db
 :studio/filters.begin-filter
 filters-path
 (fn [filters]
   (assoc filters :filtering true)))

(rf/reg-event-db
 :studio/filters.clear
 filters-path
 (fn [filters]
   (dissoc filters :filtering :v)))

(rf/reg-event-db
 :studio/filters.update-key
 filters-path
 (fn [{:keys [k] :as filters} [_ new-k]]
   (cond-> (assoc filters :k new-k)
     (and k
          (not= k new-k))
     (dissoc :v))))

(rf/reg-event-db
 :studio/filters.update-value
 filters-path
 (fn [filters [_ value]]
   (assoc filters :v value)))

(rf/reg-event-db
 :studio/prefs.toggle-dlg
 prefs-path
 (fn [prefs]
   (update prefs :showing not)))

(defn save-prefs [prefs]
  (local-storage/set-transit prefs-storage-key (dissoc prefs :showing)))

(rf/reg-event-db
 :studio/prefs.toggle-column
 prefs-path
 (fn [prefs [_ col]]
   (prog1 (update-in prefs [:columns col] not)
     (save-prefs <>))))

(rf/reg-event-db
 :studio/prefs.add-stat
 prefs-path
 (fn [{:keys [stats] :as prefs}  [_ k]]
   (prog1 (assoc prefs :stats (into (sorted-set)
                                    (conj stats k)))
     (save-prefs <>))))

(rf/reg-event-db
 :studio/prefs.remove-stat
 prefs-path
 (fn [{:keys [stats] :as prefs}  [_ k]]
   (prog1 (assoc prefs :stats (disj stats k))
     (save-prefs <>))))

(rf/reg-event-db
 :studio/prefs.reset
 prefs-path
 (fn [prefs]
   (assoc prefs :columns info-displays)))

(rf/reg-event-db
 :studio/sorts.enable
 sorts-path
 (fn [sorts [_ {:keys [id stat?]}]]
   (assoc sorts :col {:id id
                      :descending? false
                      :stat? stat?})))

(rf/reg-event-db
 :studio/sorts.clear
 sorts-path
 (fn [sorts]
   (dissoc sorts :col)))

(rf/reg-event-db
 :studio/sorts.reverse
 sorts-path
 (fn [sorts]
   (update-in sorts [:col :descending?] not)))

(rf/reg-event-db
 :studio/chart.show
 studio-path
 (fn [{:keys [from to] :as studio} [_ {:keys [id stat?]}]]
   (assert (and (is-job-valid? from)
                (is-job-valid? to)))
   (nav-to-chart from to {:id id :stat? stat?})
   studio))
