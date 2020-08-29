(ns sctools.studio.views
  (:require [helix.core :as hx :refer [defnc $]]
            [statecharts.core :as fsm]
            [clojure.string :as str]
            [applied-science.js-interop :as j]
            [reagent.core :as r]
            [sctools.studio.machine]
            [sctools.utils.rf-utils :as rfu :refer [db-sub quick-sub]]
            [re-frame.core :as rf]
            [sctools.app.layout :as layout]
            ["react-router-dom"
             :refer
             [HashRouter NavLink Redirect Route Switch useParams useRouteMatch]]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Button" :default Button]
            [helix.dom :as d]))

(def studio-path [(rf/path :studio)])

(db-sub :studio)
(quick-sub :studio/job)
(quick-sub :studio/state)

(rf/reg-event-db
 :studio/update-job
 studio-path
 (fn [studio [_ job]]
   (assoc studio :job job)))

(defn is-job-valid? [job]
  (re-matches #"[1-9][0-9]*/[1-9][0-9]*/[1-9][0-9]*" job))


(defn nav-to-job [job]
  (rf/dispatch [:app/push-state (str "/studio/job/" job)]))

(rf/reg-event-db
 :studio/submit
 studio-path
 (fn [{:keys [job] :as studio}]
   (assert (is-job-valid? job))
   (nav-to-job job)
   studio))

(defnc job-input-view-impl
  [{:keys [job]}]
  (layout/set-title "Jobs Studio")
  (let [job-valid? (and (not (str/blank? job))
                        (is-job-valid? job))]
    (d/div {:id "studio-main"
            :class '[w-full h-full
                     mx-auto pt-20 x-max-w-400px
                     flex flex-col justify-start items-start]}
           (d/div {:class '[text-2xl]} "Job ID:")
           (d/div {:class '[flex flex-row pt-4 space-x-2 w-full]}
                  ($ TextField {:className "flex-grow"
                                :name "job-id"
                                :variant "outlined"
                                :autoComplete "on"
                                :type "text"
                                :value (or job "")
                                :onChange
                                (fn [event]
                                  (rf/dispatch-sync [:studio/update-job
                                                     (j/get-in event [:target :value])])
                                  (r/flush))
                                :label "Job ID"})
                  ($ Button {:className "flex-none"
                             :color "primary"
                             :disabled (not job-valid?)
                             :onClick #(rf/dispatch [:studio/submit])
                             :variant "contained"}
                     "Go!"))
           )))

(defn job-input-view []
  (let [job @(rf/subscribe [:studio/job])]
    ($ job-input-view-impl {:job job})))

(defnc job-detail-view-impl [{:keys [state]}]
  #p 'job-detail-view---render
  (j/let [^:js {:keys [project spider job]} (useParams)
          job (str project "/" spider "/" job)]
    (rf/dispatch [:app/set-title (str "Job Studio " job)])
    (hx/<>
     (d/div "Job is" job)
     (d/div "value is"
            (cond
              (fsm/matches state :loading)
              "loading"

              :else
              (:value state)
              )))))

(defn job-detail-view []
  (let [state (rf/subscribe [:studio/state])]
    ($ job-detail-view-impl {:state state})))

(defnc jobs-studio-view []
  (j/let [^:js {:keys [path]} (useRouteMatch)]
    ($ Switch
       ($ Route {:path (str path "/job/:project/:spider/:job")}
          ($ job-detail-view))
       ($ Route {:path path}
          (r/as-element [job-input-view])))))


(comment
  (nav-to-job "1887/861/136449")

  ())
