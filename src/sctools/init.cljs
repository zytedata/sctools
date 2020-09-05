(ns sctools.init
  (:require ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/CircularProgress" :default CircularProgress]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Tooltip" :default Tooltip]
            ["@material-ui/lab/Alert" :default Alert]
            ["react" :as react]
            ["react-dom" :as ReactDOM]
            ["react-router-dom"
             :refer [HashRouter Switch Route
                     Redirect Link NavLink
                     useHistory useLocation]]
            #_["@material-ui/core/Accordion" :default Accordion]
            #_["@material-ui/core/AccordionSummary" :default AccordionSummary]
            #_["@material-ui/core/AccordionDetails" :default AccordionDetails]
            #_["@material-ui/icons/ExpandMore" :default ExpandMoreIcon]
            [applied-science.js-interop :as j]
            [clojure.string :as str]
            [helix.core :as hx :refer [defnc $]]
            [helix.dom :as d]
            [helix.hooks :as hooks :refer [use-effect use-memo]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [sctools.api :as api]
            [sctools.app.layout :as layout]
            [sctools.theme :refer [theme]]
            [sctools.utils.local-storage :as local-storage]
            [sctools.utils.rf-utils
             :as rfu
             :refer [db-sub quick-sub]]))

(def api-key-name :sctools/api-key)

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

(rf/reg-event-db
 :init/load-api-key
 init-path
 (fn [init]
   (rf/dispatch [:init/loaded])
   (if-let [api-key (local-storage/get-item api-key-name)]
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
   (local-storage/set-item api-key-name (:api-key init))
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
   {:class "text-base p-2 leading-normal"}
"The API key is only stored in your local browser.
It would never be sent to any third-party service."))

(def leftbar-mask-id "leftbar-mask-container")

(defn leftbar-mask-impl []
  (d/div {:id "leftbar-mask"}))

(defnc leftbar-mask []
  (let [el (use-memo :once
             (if-let [mask-div (js/document.getElementById leftbar-mask-id)]
               mask-div
               (let [mask-div (js/document.createElement "div")]
                 (j/assoc! mask-div :id leftbar-mask-id)
                 (j/call js/document.body :append mask-div)
                 mask-div)))]
    (ReactDOM/createPortal ($ leftbar-mask-impl) el)))

(defnc init-view-impl [{:keys [api-key checking error]}]
  (layout/set-title "Setup")
  (d/div
    ($ leftbar-mask)
    (d/form {:class '[h-full w-full mx-auto mt-32 w-full
                      flex flex-col items-start justify-start space-y-3]
             :style {:width "400px"}}
            (d/div {:class '[text-xl w-full]}
                   "Please set your Scrapy Cloud API Key:")
            (d/div {:class '[pb-2]}
             ($ Tooltip {:title security-tooltip}
                (d/div
                 {:class '[text-sm cursor-pointer px-1 rounded text-gray-700
                           border-b hover:shadow]}
                 (d/i {:class '[fa fa-info-circle pr-1 text-gray-600]})
                 "security tip")))
            (d/div
             {:class '[flex flex-row space-x-2 w-full]}
             ($ TextField {:className "flex-grow"
                           :variant "outlined"
                           :autoComplete "off"
                           :type "password"
                           :value (or api-key "")
                           :disabled checking
                           :onChange (fn [event]
                                       (rf/dispatch-sync [:init/set-api-key
                                                          (j/get-in event [:target :value])])
                                       (r/flush))
                           :label "API Key"})
             ($ Button {:className "flex-none"
                        :color "primary"
                        :onClick #(rf/dispatch [:init/submit])
                        :variant "contained"
                        :disabled (str/blank? api-key)}
                (d/div {:class '[normal-case text-lg]}
                       (if checking
                         ($ CircularProgress {:size "1.5em"
                                              :color "inherit"})
                         "Test"))))
            (when error
              ($ Alert {:severity "warning"}
                 error))
            #_($ Accordion {:className "w-full"}
               ($ AccordionSummary
                  {:expandIcon (d/i {:class '[fas fa-caret-down]})}
                  (d/div {:class '[text-sm]}
                         "Security Tips"))
               ($ AccordionDetails security-tooltip)))))

(defn init-view []
  (let [{:keys [api-key checking error]} @(rf/subscribe [:init])]
    ($ init-view-impl {:api-key api-key
                       :error error
                       :checking checking})))

(comment

  (local-storage/get-item api-key-name)
  (local-storage/delete-item api-key-name)
  (local-storage/set-item api-key-name (test-key))

  ())
