(ns sctools.settings
  (:require
   ["@material-ui/core/Button" :default Button]
   ["@material-ui/core/CircularProgress" :default CircularProgress]
   ["@material-ui/lab/Alert" :default Alert]
   [helix.hooks :as hooks :refer [use-effect]]
   [applied-science.js-interop :as j]
   [helix.core :as hx :refer [$ defnc]]
   [helix.dom :as d]
   [kitchen-async.promise :as p]
   [re-frame.core :as rf]
   ["react-router-dom"
     :refer
     [Route Switch useParams]]
   [sctools.app.layout :as layout]
   [sctools.studio.cache :refer [clear-cache]]
   [sctools.utils.async-utils :refer [asleep]]
   [sctools.utils.rf-utils :as rfu :refer [db-sub quick-sub]]))


(defnc settings-child []
  (let [id (j/get (useParams) :id)]
    (d/div "Hello Nested Route " id)))

(def settings-path [(rf/path :settings)])
(def cache-settings-path [(rf/path :settings :cache)])

(rf/reg-event-db
 :settings/cache.init
 cache-settings-path
 (fn [cache]
   (assoc cache :clearing false :cleared false)))

(rf/reg-event-db
 :settings/cache.clear
 cache-settings-path
 (fn [cache]
   (p/do
     (clear-cache)
     ;; (asleep 200)
     (rf/dispatch [:settings/cache.cleared]))
   (assoc cache :clearing true :cleared false)))

(rf/reg-event-db
 :settings/cache.cleared
 cache-settings-path
 (fn [cache]
   (assoc cache :clearing false :cleared true)))

(db-sub :settings)
(quick-sub :settings/cache)

(defnc settings-view-impl
  [{:keys [cache]}]
  (layout/set-title "Settings")
  (use-effect :once
    (rf/dispatch-sync [:settings/cache.init]))
  (d/div
    ($ Switch
       ($ Route {:path "/settings/:id"}
          ($ settings-child)))
    (d/div {:class '[design-paper py-4 px-8 h-full w-full]}
      (d/div {:class '[design-paper-inner py-4 px-8 flex flex-col justify-start w-full border rounded space-y-3]}
        (d/div {:class '[design-title text-2xl font-semibold text-left #_border w-full]}
          "Clear Cache")
        (d/hr)
        ($ Button {:color "primary"
                   :variant "contained"
                   :data-cy "cache-clear-btn"
                   :onClick (fn []
                              (rf/dispatch [:settings/cache.clear]))
                   :size "medium"
                   :className "w-60"
                   & (when (:clearing cache)
                       {:disabled true})}
          (d/div {:class '[flex flex-row space-x-4 items-center justify-between]}
            (d/span "Clear cache")
            (when (:clearing cache)
              ($ CircularProgress {:size "1em"
                                   :color "primary"
                                   ;; :className "text-green-100"
                                   }))))
        (d/div {:class "text-gray-700"}
          "click this button to clear cached job stats in the browser")
        (when (:cleared cache)
          ($ Alert {:severity "success"
                    :data-cy "cache-clear-success"}
            "Cache is cleared"))
        ))))

(defn settings-view []
  (let [cache @(rf/subscribe [:settings/cache])]
    ($ settings-view-impl {:cache cache})))

(comment
  @(rf/subscribe [:settings/cache])
  ())
