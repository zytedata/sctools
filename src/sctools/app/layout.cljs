(ns sctools.app.layout
  (:require [sctools.utils.rf-utils :as rfu :refer [db-sub quick-sub defev]]
            [reagent.core :as r]
            [helix.hooks :as hooks :refer [use-effect]]
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

(rf/reg-event-db
 :app/set-title
 (fn [db [_ title]]
   (assoc-in db [:page :title] title)))

(rf/reg-sub
 :app/title
 (fn [{:keys [page]}]
   (:title page)))

(defn set-title [title]
  (use-effect :once
    (js/setTimeout
     (fn []
       (when (not= @(rf/subscribe [:app/title]) title)
         (rf/dispatch [:app/set-title title])))
     0)
    #(rf/dispatch [:app/set-title nil])))
