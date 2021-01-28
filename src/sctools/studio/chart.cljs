(ns sctools.studio.chart
  (:require ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/Checkbox" :default Checkbox]
            ["@material-ui/core/Dialog" :default Dialog]
            ["@material-ui/core/FormControlLabel" :default FormControlLabel]
            ["@material-ui/core/FormGroup" :default FormGroup]
            ["@material-ui/core/LinearProgress" :default LinearProgress]
            ["@material-ui/core/Link" :default Link]
            ["@material-ui/core/Paper" :default Paper]
            ["@material-ui/core/Tab" :default Tab]
            ["@material-ui/core/Table" :default Table]
            ["@material-ui/core/TableBody" :default TableBody]
            ["@material-ui/core/TableCell" :default TableCell]
            ["@material-ui/core/TableHead" :default TableHead]
            ["@material-ui/core/TableRow" :default TableRow]
            ["@material-ui/core/Tabs" :default Tabs]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Typography" :default Typography]
            ["@material-ui/lab/Alert" :default Alert]
            ["@material-ui/lab/Autocomplete" :default Autocomplete]
            ["react-vega" :refer [VegaLite]]
            ["react-router-dom"
             :refer
             [Route Switch useParams useRouteMatch]]
            [applied-science.js-interop :as j]
            [bb.clojure :refer [cond*]]
            [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.dom :as d]
            [helix.hooks :as hooks :refer [use-ref use-effect use-memo use-state]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [sctools.app.layout :as layout]
            sctools.studio.events
            sctools.studio.machine
            sctools.studio.subs
            [sctools.utils.rf-utils :as rfu :refer [use-atom]]
            [sctools.widgets.common :refer [error-msg tooltip popover]]
            [statecharts.core :as fsm]))

(defn create-vega-spec []
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
                            :title "Items"
                            :scale {:zero false}
                            :axis {:format "~s"
                                   :titleFontSize 14
                                   :titleAngle 0
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
                        [{:field :job :type :ordinal :title "Job"}
                         {:field :data :type :quantitative :title "Items"}]
                        }

             :selection {:hover {:type :single
                                 :fields [:job]
                                 :nearest true
                                 :on :mouseover
                                 :empty :none
                                 :clear :mouseout}}}]
    }))

(defnc job-chart-view-impl [{:keys [chart-data]}]
  (def vdata chart-data)
  (d/div {:class '[flex flex-col w-full h-full overflow-scroll]}
    (d/div {:class '[mt-8 flex flex-col items-center justify-center]}
      (let [spec (create-vega-spec)
            data (j/lit {:jobdata (clj->js chart-data)})]
        ($ VegaLite {:spec spec
                     :data data
                     :renderer "svg"
                     :width "container"
                     :height 700})))))

(defn job-chart-view []
  (let [chart-data @(rf/subscribe [:studio/chart.col-data])]
    ($ job-chart-view-impl {:chart-data chart-data})))
