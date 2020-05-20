(ns sctools.app
  (:require [sctools.app.fixes]
            [sctools.home :refer [home-view]]
            [hashp.core]
            [sctools.app.error-boundary :refer [error-capturer inc-epoch]]
            [clojure.string :as str]
            ;; for side effects
            [day8.re-frame.http-fx]
            [sctools.app.events]
            [sctools.app.subs]
            [sctools.app.layout]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.dom :as dom]))

(defonce on-devcards-page?
  (= (oget js/window "location.pathname")
     "/static/devcards.html"))

(defn app-ui []
  [home-view])

(defn protected-app-ui []
  [error-capturer [app-ui]])

(defn render! []
  (dom/render
   protected-app-ui
   (.querySelector js/document "#app")))

(defn before-reload [])

(defn after-reload []
  (when-not on-devcards-page?
    (rf/clear-subscription-cache!)
    (inc-epoch)
    (render!)))

(defn main []
  (when-not on-devcards-page?
    (rf/dispatch-sync [:initialize])
    (render!)))
