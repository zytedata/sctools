(ns sctools.studio.bench
  (:require [re-frame.core :as rf]
            [linked.core :as linked]
            [meander.epsilon :as me]
            [com.rpl.specter :as sp]
            [sctools.studio.utils :refer [info-keys info-titles format-info-field]]
            [sctools.utils.rf-utils :as rfu :refer [db-sub quick-sub]]))

(defn matches-filter? [{:keys [filtering k v]} info]
  (or (not filtering)
      (not v)
      (not k)
      (= (get-in info ["spider_args" (name k)])
         v)))

;; return a map of k => [orig value, display value]
(defn format-info [info {:keys [columns stats]}]
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
       (into (linked/map))))

(defn f-meander [results check-info process-info]
  (me/search results
    {?job {:success true
           :info (me/pred check-info ?info)}}

    [?job (process-info ?info)]))

(defn f-clojure [results check-info process-info]
  (vec (keep (fn [[job {:keys [success info]}]]
               (when (and success
                          (check-info info))
                 [job (process-info info)]))
             results)))

(defn f-specter [results check-info process-info]
  (sp/select [sp/ALL
              (sp/collect-one sp/FIRST)
              sp/LAST
              (sp/pred :success)
              :info
              (sp/pred check-info)
              (sp/view process-info)]
             results))

(defn run-bench [f n]
  (let [filters @(rf/subscribe [:studio/filters])
        prefs @(rf/subscribe [:studio/prefs])
        results @(rf/subscribe [:studio/state.results])
        ;; results (merge results (sp/setval [sp/MAP-KEYS sp/END] "+1" results))
        check-info (partial matches-filter? filters)
        process-info #(format-info % prefs)]
    (simple-benchmark
     []
     (count (f results check-info process-info))
     n)))

(comment
  (sp/select sp/ALL [1 2 3])
  (sp/transform (sp/srange 0 2) sp/NONE [1 2 3])

  (me/match 1
    1
    :yes

    _
    :no)

  ;; when there are 100 jobs in results map: 
  ;; 2ms
  (run-bench f-clojure 500)
   ;; 2.1ms
  (run-bench f-specter 500)
  ;; 2.5ms
  (run-bench f-meander 500)

  ())
