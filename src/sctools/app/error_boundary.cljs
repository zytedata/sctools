(ns sctools.app.error-boundary
  (:require [reagent.core :as r]
            [oops.core :refer [oget]]))

(defonce epoch (atom 0))

(defn inc-epoch
  "The epoch is increased every time after a reload, to avoid the error
  boundary ui being shown even after the error is fixed."
  []
  (swap! epoch inc))

(def error-capturer
  (let [methods
        {:constructor
         (fn [this]
           ;; (js/console.log "react error boundary: constructor called")
           (set! (.-state this) #js {:hasError false}))

         :getDerivedStateFromError
         (fn [error]
           #js {:hasError true
                :epoch @epoch
                :error error})

         :reagent-render
         (fn []
           (this-as this
             (let [children (r/children this)
                   state (.-state ^js this)]
               ;; #p children
               (when (not= 1 (count children))
                 (js/console.warn "Component error-boundary requires a single child component. Additional children are ignored."))
               (if (and (oget state :hasError)
                        (= (oget state :epoch) @epoch))
                 (do
                   (js/console.warn "An error occurred downstream (see errors above). The element subtree will not be rendered.")

                   [:div.react-error-info
                    [:h2 "Error happened"]
                    [:pre (oget state :error.stack)]])
                 (first children)))))}
        react-comp (r/create-class methods)]
    (r/adapt-react-class react-comp)))
