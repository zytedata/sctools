(ns sctools.studio.subs
  (:require [re-frame.core :as rf]
            [linked.core :as linked]
            [sctools.studio.utils :refer [info-keys info-titles format-info-field]]
            [sctools.utils.rf-utils :as rfu :refer [db-sub quick-sub]]))

(db-sub :studio)
(quick-sub :studio/state)
(quick-sub :studio/recents)
(quick-sub :studio/filters)
(quick-sub :studio/prefs)
(quick-sub :studio/sorts)

(rf/reg-sub
 :studio/jobs
 :<- [:studio]
 (fn [studio]
   (select-keys studio [:from :to])))

(rf/reg-sub
 :studio/state.results
 :<- [:studio/state]
 (fn [state]
   (get-in state [:context :results])))

(defn matches-filter? [{:keys [filtering k v]} info]
  (or (not filtering)
      (not v)
      (not k)
      (= (get-in info ["spider_args" (name k)])
         v)))

;; return a map of k => [orig value, display value]
(defn format-info [info {:keys [columns stats]}]
  (->> (concat (for [k     info-keys
                     :when (get columns k)
                     :let  [[v display-v] (format-info-field info k)]]
                 [k [v display-v]])
               (for [k    stats
                     :let [v (get-in info ["scrapystats" k])]]
                 [k [v (or v "N/A")]]))
       (into (linked/map))))

(defn safe-compare [x y]
  (try
    (compare x y)
    (catch js/Error _
      (compare (str x) (str y)))))

(defn sort-infos [sorts infos]
  (if-let [{:keys [id descending?]} (:col sorts)]
    (let [cmp (fn [[_ info1] [_ info2]]
                ;; info is a tuple of [job info-map]
                (let [orig-value1 (-> info1 (get id) first)
                      orig-value2 (-> info2 (get id) first)]
                  (cond-> (safe-compare orig-value1 orig-value2)
                    descending?
                    (* -1))))]
      (sort cmp infos))
    infos))

(rf/reg-sub
 :studio/table.rows
 :<- [:studio/state.results]
 :<- [:studio/filters]
 :<- [:studio/prefs]
 :<- [:studio/sorts]
 (fn [[results filters prefs sorts]]
   (->> (keep (fn [[job {:keys [success info]}]]
            (when (and success
                       (matches-filter? filters info))
              [job (format-info info prefs)]))
              results)
        (sort-infos sorts))))

;; Return [:enum [:ascending :descending nil]]
(defn get-sorting [{:keys [col]} current-id stat?]
  (when-some [{:keys [id descending?]} col]
    (when (and (= current-id id)
             (= (boolean stat?) (boolean (:stat? col))))
      (if descending?
        :descending
        :ascending))))

(rf/reg-sub
 :studio/table.headers
 :<- [:studio/prefs]
 :<- [:studio/sorts]
 (fn [[{:keys [columns stats]} sorts]]
   (concat (for [[k title] (map vector info-keys info-titles)
                 :when (get columns k)]
             {:id k
              :sorting (get-sorting sorts k false)
              :title title})
           (for [k stats]
             {:id k
              :sorting (get-sorting sorts k true)
              :title k
              :stat? true}))))
