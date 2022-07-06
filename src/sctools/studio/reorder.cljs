(ns sctools.studio.reorder
  (:require [applied-science.js-interop :as j]
            [re-frame.core :as rf]
            [dom-top.core :refer [loopr letr]]
            [com.rpl.specter :as sp]
            [sctools.studio.utils :refer [info-keys]]
            [bb.clojure :as bb]))

(def drag-path [(rf/path :studio :drag)])

(rf/reg-sub
 :studio/drag
 :<- [:studio]
 (fn [studio]
   (:drag studio)))

(rf/reg-sub
 :studio/drag.source
 :<- [:studio/drag]
 (fn [drag]
   (:source drag)))

(rf/reg-sub
 :studio/drag.target
 :<- [:studio/drag]
 (fn [drag]
   (:target drag)))

(rf/reg-event-db
 :studio/drag.start
 drag-path
 (fn [drag [_ k]]
   (assoc drag :source k)))

(rf/reg-event-db
 :studio/drag.end
 drag-path
 (fn [drag]
   (dissoc drag :source)))

(rf/reg-event-db
 :studio/drag.enter
 drag-path
 (fn [{:keys [source] :as drag} [_ k]]
   (if (not= k source)
     (assoc drag :target k)
     drag)))

(rf/reg-event-db
 :studio/drag.leave
 drag-path
 (fn [drag [_ k]]
   (if (= k (:target drag))
     (dissoc drag :target)
     drag)))

(defn get-ordering []
  (letr [{:keys [columns stats ordering]} @(rf/subscribe [:studio/prefs])
         _ (when ordering
             (return ordering))
         cols (->> info-keys
                   (keep (fn [k]
                           (when (get columns k)
                             k))))]
    (concat cols stats)))

(defn find-index [coll x]
  (let [max-i (dec (count coll))]
    (loopr [i 0]
           [v coll]
           (if (= v x)
             i
             (if (= i max-i)
               nil
               (recur (inc i)))))))

(defn reorder-columns
  "Make sure `source` is in front of `target`"
  [columns source target]
  ;; #p [columns source target]
  (letr [source-index (find-index columns source)
         _ (when-not source-index
             (return columns))
         columns_ (sp/setval [(sp/nthpath source-index)] sp/NONE columns)
         ;; target index must be located within the new coll after removing
         ;; `source`
         target-index (find-index columns_ target)
         _ (when-not target-index
             (return columns))]
    (sp/setval [(sp/before-index target-index)] source columns_)))

(rf/reg-event-db
 :studio/drag.drop
 (rf/path [:studio])
 (fn [{:keys [drag] :as studio}  [_ k]]
   (let [{:keys [source target]} drag]
     (-> studio
         (update :prefs assoc :ordering (reorder-columns (get-ordering) source target))
         (dissoc :drag)))))

(defn on-drag-start
  [ev k]
  ;; (println ":onDragStart" k)
  (j/call ev :stopPropagation)
  (j/assoc-in! ev [:dataTransfer :effectAllowed] "move")
  (rf/dispatch [:studio/drag.start k]))

(defn on-drag-end
  [ev k]
  ;; (println ":onDragEnd" k)
  (j/call ev :stopPropagation)
  (rf/dispatch [:studio/drag.end k]))

(defn on-drag-enter
  [ev k]
  ;; (println ":onDragEnter" k)
  (rf/dispatch [:studio/drag.enter k]))

(defn on-drag-leave
  [ev k]
  ;; (println ":onDragLeave" k)
  (rf/dispatch [:studio/drag.leave k]))

(defn on-drop
  [ev k]
  ;; (println ":onDrop" k)
  (rf/dispatch [:studio/drag.drop k]))

(defn apply-ordering
  "Apply the ordering to the coll. The order of each of coll's element `x` is
  decided by applying `(keyfn x)`"
  [ordering keyfn coll]
  (if-not (seq ordering)
    coll
    (sort-by
      #(find-index ordering (keyfn %))
      coll)))
