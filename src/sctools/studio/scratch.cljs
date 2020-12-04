(ns sctools.studio.scratch
  (:require [re-frame.core :as rf]
            [meander.epsilon :as me]
            [medley.core :as m]
            [linked.core :as linked]
            [com.rpl.specter :as sp]
            [sctools.studio.utils :refer [info-keys info-titles format-info-field]]
            [sctools.utils.rf-utils :as rfu :refer [db-sub quick-sub]]))


(sp/select sp/ALL [1 2 3])
(sp/transform (sp/srange 0 2) sp/NONE [1 2 3])

(def results (-> @(re-frame.core/subscribe [:studio/state.results])))
(count results)

(sp/select [sp/ALL] results)
(sp/select [sp/MAP-VALS] {:a 1})

(sp/select [sp/ALL sp/MAP-VALS odd?] [{:a 1}])

(sp/setval [sp/MAP-VALS :success (complement not)] results)

(def d1 {:a {:success true :info 1}
         :b {:success false :info 2}})

(sp/setval [sp/MAP-VALS #(not (:success %))]
          sp/NONE
          d1)

(->> d1
     (m/filter-vals :success)
     )

(def infos @(rf/subscribe [:studio/state.results]))
(-> infos first)
(reduce + (me/search infos
   (me/scan [_ {:success true
                :info {"items" ?items}}])
   ?items))
