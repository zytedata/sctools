(ns sctools.app
  (:require ["moment" :as moment]
            [applied-science.js-interop :as j]
            [sctools.app.fixes]
            [sctools.home :refer [bootstrap-view home-view]]
            [sctools.app.logging :refer [start-logging]]
            [sctools.app.error-boundary :refer [error-capturer inc-epoch]]
            ;; for side effects
            [day8.re-frame.http-fx]
            [day8.re-frame.async-flow-fx]
            [sctools.app.core]
            [sctools.app.layout]
            [sctools.app.auth]
            [sctools.theme :refer [theme-provider]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.dom :as dom]))

(defonce on-devcards-page?
  (= (oget js/window "location.pathname")
     "/static/devcards.html"))

(defn app-ui []
  [:> theme-provider
   (if @(rf/subscribe [:app/booted])
     [home-view]
     [bootstrap-view])])

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
  #_(set! js/sctools_build_ts 1590000000000)
  (when-let [build-ts (j/get js/window :sctools_build_ts)]
    (js/console.log "SCTools Build TS: "
                    (-> (j/call moment :utc build-ts)
                        (j/call :fromNow))
                    "[" (js/Date. build-ts) "]"))
  (start-logging)
  (when-not on-devcards-page?
    (rf/dispatch-sync [:app/boot])
    (render!)))
