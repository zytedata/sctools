(ns sctools.init
  (:require ["react" :as react]
            [clojure.string :as str]
            [oops.core :refer [oget]]
            ["react-router-dom"
             :refer [HashRouter Switch Route
                     Redirect Link NavLink
                     useHistory useLocation]]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/Tooltip" :default Tooltip]
            ["@material-ui/core/CircularProgress" :default CircularProgress]
            ["@material-ui/lab/Alert" :default Alert]
            [sctools.theme :refer [theme]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [sctools.requests :as api]
            [sctools.utils.rf-utils
             :as rfu
             :refer [db-sub quick-sub]]
            [helix.core :as hx :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]))

(def api-key-name "sctools.api-key")

;; TODO: we can simply use namespaced keywords, e.g. subscribe ::submit?
;; - No, other modules may still need to refer to init/foo
(def dispatch (rfu/make-dispatcher :init))
(def dispatch-sync (rfu/make-sync-dispatcher :init))
(def subscribe (rfu/make-subscriber :init))
(def init-path [(rf/path :init)])

(db-sub :init)
(quick-sub :init/authed)
(quick-sub :init/api-key)
(quick-sub :init/checking)
(quick-sub :init/error)

(defn set-checking [init v]
  (assoc init :checking v))

(defn set-error [init v]
  (assoc init :error v))

(rf/reg-event-db
 :init/set-api-key
 init-path
 (fn [init [_ api-key]]
   (assoc init :api-key api-key)))

(defn local-storage-get [k]
  (.getItem js/window.localStorage k))

(defn local-storage-set [k v]
  (.setItem js/window.localStorage k v))

(rf/reg-event-db
 :init/load-api-key
 init-path
 (fn [init]
   (dispatch :loaded)
   (if-let [api-key (local-storage-get api-key-name)]
     (do
       (-> init
           (assoc :api-key api-key)
           (assoc :authed true))))))

(rf/reg-event-fx
 :init/submit
 init-path
 (fn [{init :db}]
   (let [init (-> init
                  (set-checking true)
                  (set-error nil))]
     (merge
      {:db init}
      (api/api-key-request
       {:api-key (:api-key init)
        :on-success :init/success-check
        :on-failure :init/fail-check})))))

(rf/reg-event-db
 :init/loaded
 (fn [db]
   db))

(rf/reg-event-db
 :init/success-check
 init-path
 (fn [init [_ doc]]
   ;; #p doc
   (local-storage-set api-key-name (:api-key init))
   (-> init
       (set-checking false)
       (assoc :user-info doc)
       (assoc :authed true))))

(defn error-from-response [response]
  (or (get-in response [:response "message"])
      "API Key check failed"))

(rf/reg-event-db
 :init/fail-check
 init-path
 (fn [init [_ {status :status :as response}]]
   ;; (def vresp1 response)
   ;; #p response
   (-> init
       (set-checking false)
       (set-error (error-from-response response)))))

(def security-tooltip
  (d/div
   {:class "text-sm"}
"The api key is only stored in your local browser.
It's never sent to any third-party service"))

(defnc init-view-impl [{:keys [api-key checking error]}]
  (d/form {:class '[h-full w-full mx-auto mt-32 w-full
                   flex flex-col items-start justify-start space-y-4]
           :style {:width "400px"}}
    (d/div {:class '[text-xl w-full]}
           "Please set your Scrapy Cloud API Key:")
    ($ Tooltip {:title security-tooltip}
       (d/div
        {:class '[text-sm underline cursor-pointer pb-4]}
        #_(d/i {:class '[fa fa-question pr-1 text-gray-600]})
        (d/i "security tip")))
    (d/div
     {:class '[flex flex-row space-x-2 w-full]}
     ($ TextField {:className "flex-grow"
                   :variant "outlined"
                   :autoComplete "off"
                   :type "password"
                   :value (or api-key "")
                   :disabled checking
                   :onChange (fn [event]
                               (dispatch-sync :set-api-key
                                              (oget event "target.value"))
                               (r/flush))
                   :label "API Key"})
     ($ Button {:className "flex-none"
                :color "primary"
                :onClick #(dispatch :submit)
                :variant "contained"
                :disabled (str/blank? api-key)}
        (d/div {:class '[normal-case text-lg]}
               (if checking
                 ($ CircularProgress {:size "1.5em"
                                      :color "inherit"})
                 "Test"))))
    (when error
      ($ Alert {:severity "warning"}
         error))))

(defn init-view []
  (let [{:keys [api-key checking error]} @(rf/subscribe [:init])]
    ($ init-view-impl {:api-key api-key
                       :error error
                       :checking checking})))
