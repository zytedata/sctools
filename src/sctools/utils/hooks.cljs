(ns sctools.utils.hooks
  (:require [helix.core :as hx :refer [defnc $]]
            [applied-science.js-interop :as j]
            [medley.core :as m]
            ["react-router-dom" :refer [useLocation]]
            [goog.events]
            [helix.dom :as d]
            [helix.hooks :as hooks]))

(defn use-window-height
  "Triggers re-render on window height updates (for instance after user
  resizes the window)."
  []
  (let [[height set-height] (hooks/use-state nil)]
    (hooks/use-effect :once
      (let [listener-key
            (goog.events/listen js/window "resize"
                                #(set-height (j/get js/window :innerHeight)))]
        #(goog.events/unlistenByKey listener-key)))))

(defn add-window-height-watcher
  "Register a callback to run when the window height changes. Returns a
  function that unregisters the event listener, suitable to be used in
  a use-effect hook."
  [f]
  (let [listener-key
        (goog.events/listen js/window "resize" (fn []
                                                 (f)
                                                 nil
                                                 ))]
    #(goog.events/unlistenByKey listener-key)))

(defn use-query []
  (->> (js/URLSearchParams. (j/get (useLocation) :search))
       (map vec)
       (into {})
       (m/map-keys keyword)))
