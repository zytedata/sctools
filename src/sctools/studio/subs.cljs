(ns sctools.studio.subs
  (:require [re-frame.core :as rf]
            [linked.core :as linked]
            [meander.epsilon :as me]
            [com.rpl.specter :as sp]
            [sctools.studio.utils :refer [info-keys info-titles format-info-field]]
            [sctools.utils.rf-utils :as rfu :refer [db-sub quick-sub]]
            [sctools.studio.reorder :refer [apply-ordering]]
            [medley.core :as m]))

(db-sub :studio)
(quick-sub :studio/state)
(quick-sub :studio/recents)
(quick-sub :studio/filters)
(quick-sub :studio/prefs)
(quick-sub :studio/sorts)
(quick-sub :studio/chart)

(rf/reg-sub
 :studio/jobs
 :<- [:studio]
 (fn [studio]
   (select-keys studio [:from :to])))

(rf/reg-sub
 :studio/state.results
 :<- [:studio/state]
 (fn [state]
   (:results state)))

(defn matches-filter? [{:keys [filtering k v]} info]
  (or (not filtering)
      (not v)
      (not k)
      (= (get-in info ["spider_args" (name k)])
         v)))

;; return a linked map of k => [orig value, display value].
(defn format-info
  [info {:keys [columns stats ordering]}]
  ;; (def info columns)
  ;; (def columns columns)
  ;; (def stats columns)
  (->> (concat (for [k     info-keys
                     :when (get columns k)
                     :let  [[v display-v] (format-info-field info k)]]
                 [k [v display-v]])
               (for [k    stats
                     :let [v (get-in info ["scrapystats" k])]]
                 [k [v (or v "N/A")]]))
       ;; TODO: this is O(N) where N = number of rows (because it sorts within
       ;; each row). We shall maket this O(1) by sorting in one go globally.
       (apply-ordering ordering first)
       (into (linked/map))))


#_(s/transform [s/ALL (s/pred vcolumns)]
               #(vector % (format-info-field vinfo %)) info-keys)

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
   (let [filterfn (partial matches-filter? filters)]
     (->> (me/search results
            {?job {:success true
                   :info (me/pred filterfn ?info)}}

            [?job (format-info ?info prefs)])
          (sort-infos sorts)))))

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
 (fn [[{:keys [columns stats ordering]} sorts]]
   (->> (concat (for [[k title] (map vector info-keys info-titles)
                  :when (get columns k)]
              {:id k
               :sorting (get-sorting sorts k false)
               :title title})
            (for [k stats]
              {:id k
               :sorting (get-sorting sorts k true)
               :title k
               :stat? true}))
        (apply-ordering ordering :id))))

(rf/reg-sub
 :studio/chart.col-data
 :<- [:studio/state.results]
 :<- [:studio/filters]
 :<- [:studio/prefs]
 (fn [[results filters prefs] [_ {:keys [id stat?]}]]
   ;; #p (first results)
   (let [filterfn (partial matches-filter? filters)
         datafn (if stat?
                  #(get-in % ["scrapystats" id])
                  (fn [info]
                    (let [v (format-info-field info id)
                          vparts (count v)]
                      (case vparts
                        3 (nth v 2)
                        2 (nth v 1)
                        (nth v 0)))))]
     (->> (me/search results
            {?job {:success true
                   :info (me/pred filterfn ?info)}}

            {:job ?job
             :ts  (get ?info "pending_time")
             :data (datafn ?info)})))))

(rf/reg-sub
  :studio/chart.width
  :<- [:studio/chart]
  (fn [chart]
    (:width chart)))

(comment
  (->> @(rf/subscribe [:studio/state.results])
      vals
      (map :info)
      (map #(format-info-field % :runtime)))

  @(rf/subscribe [:studio/chart.col-data {:id :runtime}])

  :end-comment)
