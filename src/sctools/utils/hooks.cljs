(ns sctools.utils.hooks
  (:require [helix.core :as hx :refer [defnc $]]
            [applied-science.js-interop :as j]
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

