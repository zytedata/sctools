(ns sctools.studio.chart
  (:require
    ["react-vega" :refer [VegaLite]]
    [applied-science.js-interop :as j]
    [helix.core :as hx :refer [$ defnc]]
    [helix.dom :as d]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [sctools.studio.utils :refer [get-descriptive-name]]
    [sctools.utils.hooks :refer [use-query]]
    [sctools.utils.transit-utils :as transit]
    sctools.studio.events
    sctools.studio.machine
    sctools.studio.subs))

(defn create-vega-spec [data-name]
  (j/lit
   {:data {:name :jobdata}
    :mark :line
    :encoding {:x {:field :job
                   :type :ordinal
                   :title "Job"
                   :axis {:labelAngle 45
                          :titleFontSize 14
                          :titlePadding 20}}}

    :layer [{:encoding {:y {:field :data
                            :title data-name
                            :scale {:zero false}
                            :axis {:format "~s"
                                   :titleFontSize 14
                                   ;; :titleAngle 90
                                   :titlePadding 30}
                            :type :quantitative}}
             :layer
             [{:mark :line}
              ;; Use transform+filter would result in endless console errors
              ;; like here: https://github.com/vega/vega-lite/issues/6266
              ;;
              ;;   {:transform [{:filter {:selection :hover}}]
              ;;    :mark :point}
              {:mark :point
               :encoding {:opacity {:condition {:value 1 :selection :hover}
                                    :value 0}}
               }]}

            {:mark :rule
             :encoding {:opacity {:condition {:value 0.5 :selection :hover}
                                  :value 0}
                        :color {:value "rgb(116,144,181)"}
                        :tooltip
                        [{:field :job :type :ordinal  :title "Job"}
                         {:field :ts  :type :temporal :title "Date" :format "%Y/%m/%d %H:%M:%S"}
                         {:field :data :type :quantitative :title data-name}]}

             :selection {:hover {:type :single
                                 :fields [:job]
                                 :nearest true
                                 :on :mouseover
                                 :empty :none
                                 :clear :mouseout}}}]
    }))

(defn get-chart-query []
  (-> (use-query)
      :q
      (js/decodeURIComponent)
      (js/JSON.parse)
      (transit/read-transit)))

(defnc job-chart-view-impl [{:keys [chart-data id stat?]}]
  (def vdata chart-data)
  (d/div {:class '[flex flex-col w-full h-full overflow-scroll]}
    (d/div {:class '[mt-8 flex flex-col items-center justify-center]}
      (let [spec (create-vega-spec (get-descriptive-name id))
            data (j/lit {:jobdata (clj->js chart-data)})]
        (js/console.log "data =" data)
        (js/console.log "spec =" spec)
        ($ VegaLite {:spec spec
                     :data data
                     :renderer "svg"
                     :width "container"
                     :height 700})))))

(defn job-chart-view-data-wrapper [id stat?]
  (let [chart-data @(rf/subscribe [:studio/chart.col-data {:id id :stat? stat?}])]
    ($ job-chart-view-impl {:chart-data chart-data
                            :id id
                            :stat? stat?})))

(defnc job-chart-view []
  ;; we have to use a native class component since we want to use the use-query
  ;; hook here.
  (let [{:keys [id stat?]} (get-chart-query)]
    (r/as-element [job-chart-view-data-wrapper id stat?])))
