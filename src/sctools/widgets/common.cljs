(ns sctools.widgets.common
  (:require ["@material-ui/core/Tooltip" :default Tooltip]
            ["@material-ui/core/Typography" :default Typography]
            [helix.core :as hx :refer [defnc $]]
            [helix.dom :as d]))

(defn error-icon []
  [:svg {:class "h-5 w-5 text-red-500"
         :fill  "currentColor"
         :viewBox "0 0 20 20"}
   [:path
    {:clip-rule "evenodd"
     :d         "M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
     :fill-rule "evenodd"}]])

(defn error-msg [{:keys [center? icon? child msg] :or {center? false icon? true}}]
  (let [class (cond->
                  '[w-full h-full text-red-500
                    flex flex-row items-center]
                center?
                (conj 'justify-center))
        child (or child msg)]
    (if icon?
      [:div.error.flex.flex-row
       {:class class}
       [:div.mr-1
        [error-icon]]
       child]
      [:div.error
       {:class class}
       child])))

(defnc tooltip [{:keys [title children]}]
  (let [content ($ Typography
                   {:variant "subtitle1"
                    :className "p-1"}
                   title)]
    ($ Tooltip {:title content}
       children)))
