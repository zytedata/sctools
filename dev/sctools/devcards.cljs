(ns sctools.devcards
  (:require [devcards.core :as cards :refer [defcard defcard-rg]]
            [oops.core :refer [oget]]
            ["react" :as react]))

(defonce on-devcards-page?
  (= (oget js/window "location.pathname")
     "/static/devcards.html"))

(defn setup-devcards []
  ;; required by sablono which is required by devcards
  (set! (.-React js/window) react)
  (cards/start-devcard-ui!))

(defonce _
  (when on-devcards-page?
    (setup-devcards)))

(defonce observed-atom
  (let [a (atom 0)]
    (js/setTimeout (fn [] (swap! observed-atom inc)) 3000)
   a))

(defcard atom-observing-card observed-atom)
