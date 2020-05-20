(ns sctools.app.layout
  (:require [sctools.utils.rf-utils :as rfu :refer [db-sub quick-sub defev]]
            [reagent.core :as r]
            [helix.hooks :as hooks]
            [helix.core :as hx :refer [defnc $]]
            [helix.dom :as d]
            [re-frame.core :as rf]))

(def dispatch (rfu/make-dispatcher :layout))
(def subscribe (rfu/make-subscriber :layout))
(def layout-path [(rf/path :layout)])

(db-sub :layout)
(quick-sub :layout/drawer-open)

(rf/reg-event-db
 :layout/toggle-drawer
 layout-path
 (fn [layout [_ content]]
   (update layout :drawer-open not)))
