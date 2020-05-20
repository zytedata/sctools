(ns sctools.app.events
  (:require [re-frame.core :as rf]))

(def default-db
  {:layout {:drawer-open false}})

(rf/reg-event-db
 :initialize
 (fn [{:keys [db]}]
   default-db))

(rf/reg-event-db
 :layout/toggle-drawer
 (fn [{:keys [db]}]
   default-db))
