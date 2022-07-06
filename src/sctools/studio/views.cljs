(ns sctools.studio.views
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
            ["react-router-dom"
             :refer
             [Route Switch useParams useRouteMatch]]
            [applied-science.js-interop :as j]
            [bb.clojure :refer [cond*]]
            [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [medley.core :as m]
            [helix.dom :as d]
            [helix.hooks :as hooks :refer [use-effect use-memo use-state]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [sctools.app.layout :as layout]
            sctools.studio.events
            sctools.studio.machine
            sctools.studio.subs
            [sctools.studio.chart :refer [job-chart-view]]
            [sctools.studio.utils
             :refer
             [get-job-number get-spider info-keys info-titles is-job-valid?]]
            [sctools.utils.rf-utils :as rfu :refer [use-atom]]
            [sctools.studio.reorder :as drag]
            [sctools.utils.string-utils :refer [truncate-left]]
            [sctools.widgets.common :refer [error-msg tooltip popover]]
            [statecharts.core :as fsm]))

(defnc job-input [{:keys [job cb-event input-name label]}]
  ($ TextField {:name input-name
                :variant "outlined"
                :autoComplete "off"
                :type "text"
                :value (or job "")
                :onChange
                (fn [event]
                  (rf/dispatch-sync [cb-event
                                     (j/get-in event [:target :value])])
                  (r/flush))
                :label label}))

(defn get-valdiation-error [from to]
  (cond*
    (not (is-job-valid? from))
    "from job is not valid"

    (not (is-job-valid? to))
    "To Job is not valid"

    (= from to)
    "from job shall not be the same as To Job"

    (not (= (get-spider from)
            (get-spider to)))
    "from / to belongs to different spiders"

    :let [from-id (get-job-number from)
          to-id (get-job-number to)]

    (>= from-id to-id)
    "from job ID must be less than to job ID"

    (> (- to-id from-id) 5000)
    "At most 5000 jobs to show."

    :else
    nil))

(defnc job-range-input-view-impl [{:keys [jobs recents]}]
  (layout/set-title "Jobs Studio")
  (let [{:keys [from to]} jobs
        [error set-error] (use-state nil)]
    (d/div {:class '[h-full max-w-sm lg:max-w-lg
                     mx-auto space-x-8
                     pt-20 flex flex-row justify-start items-start]}
      (d/div {:onChange #(set-error nil)
              :class '[w-full h-full
                       flex flex-col justify-start items-start space-y-5]}

        (d/div {:class '[text-2xl]} "From Job:")
        ($ job-input {:job from
                      :input-name "from-job"
                      :cb-event :studio/update-from
                      :label "From Job"})

        (d/div {:class '[text-2xl]} "To Job:")
        ($ job-input {:job to
                      :input-name "to-job"
                      :cb-event :studio/update-to
                      :label "To Job"})
        (d/div {:class '[w-full flex flex-row space-x-4 items-start]}
          ($ Button {:className "flex-none"
                     :color "primary"
                     :onClick
                     (fn []
                       (if-some [error (get-valdiation-error from to)]
                         (set-error error)
                         (rf/dispatch [:studio/submit])))
                     :variant "contained"}
             "Go!")
          (when error
            ($ Alert {:severity "warning"}
               error))))
      (when-let [recents (seq recents)]
        (d/div {:class '[flex flex-col justify-start items-start space-x-4]}
          (d/div {:class '[text-2xl]} "Recently Used")
          (d/ul {:class '[list-disc]}
            (for [{:keys [from to spider-name] :as k} recents]
              (d/li
                  {:key k}
                ($ Button
                  {:color "secondary"
                   :onClick (fn []
                              (rf/dispatch [:studio/update-jobs [from to]]))}
                  (d/span {:class '[normal-case]}
                    (str spider-name " " from " ~ " to))))))
          )))))

(defn job-input-view []
  (let [jobs @(rf/subscribe [:studio/jobs])
        recents @(rf/subscribe [:studio/recents])]
    ($ job-range-input-view-impl {:jobs jobs :recents recents})))

(defnc loading-view [{:keys [spider from to current spider-name]}]
  (let [total (inc (- to from))
        finished (- current from)
        current-job (str spider "/" current)]
    (d/div {:class '[h-full w-full pt-16 px-4
                     flex flex-col items-center justify-start space-y-8]}
      (when spider-name
        (d/div spider-name))
      (d/div {:class '[w-64 mx-auto
                       flex flex-col items-center justify-center space-y-2]}
        (d/div
            (str "Loading " finished " / " total))
        ($ LinearProgress {:variant "determinate"
                           :value (* 100 (/ finished total))
                           :className " w-full px-4"
                           :color "primary"}))
      (d/div
          (str "Fetching " current-job)))))

(defnc error-view []
  (r/as-element [error-msg {:msg "Error happed" :center? true}]))


(defn job-url [job]
  (str "https://app.scrapinghub.com/p/" job))

(defn toggle-prefs-dialog []
  (rf/dispatch [:studio/prefs.toggle-dlg]))

(defnc job-row [{:keys [job info drag]}]
    ;; #p info
  ($ TableRow {:data-cy "infos-row"}
     ($ TableCell {:align "left"}
        ($ Link {:href (job-url job)
                 :key "job"
                 :data-cy "job"
                 :rel "noreferrer"
                 :target "_blank"} (truncate-left job 15)))
     (for [[k [_ v]] info
           :let [drag-key k]]
       ($ TableCell {:key k
                     :align "left"
                     :onDragEnter #(drag/on-drag-enter % drag-key)
                     :onDragLeave #(drag/on-drag-leave % drag-key)
                     :onDragOver (fn [ev]
                                   (j/call ev :preventDefault))
                     :onDrop #(drag/on-drop % drag-key)
                     :className (when (= k :spider_args)
                                  "whitespace-pre-line")
                     & (when (keyword? k)
                         {:data-cy (str "job-" (name k))})}
          (str v)))))

(defnc header-cell [{:keys [title id stat? sorting cell-attrs drag]}]
    ;; #p drag
  (let [on-hide (if stat?
                  #(rf/dispatch [:studio/prefs.remove-stat id])
                  #(rf/dispatch [:studio/prefs.toggle-column id]))
        drag-source (:source drag)
        drag-target (:target drag)
        drag-key id
        is-drag-source? (= drag-key drag-source)
        is-drop-target? (= drag-key drag-target)
        on-sort #(rf/dispatch [:studio/sorts.enable {:id id :stat? stat?}])
        on-reverse-sort #(rf/dispatch [:studio/sorts.reverse])
        on-clear-sort #(rf/dispatch [:studio/sorts.clear])
        [anchor-el set-anchor-el] (use-state nil)
        close-popover #(set-anchor-el nil)
        goto-chart #(rf/dispatch [:studio/chart.show {:id id :stat? stat?}])]
    ($ TableCell {:align "left"
                  :draggable true
                  :onDragStart #(drag/on-drag-start % drag-key)
                  :onDragEnd #(drag/on-drag-end % drag-key)
                  :onDragEnter #(drag/on-drag-enter % drag-key)
                  :onDragLeave #(drag/on-drag-leave % drag-key)
                  :onDragOver (fn [ev]
                                (j/call ev :preventDefault))
                  :onDrop #(drag/on-drop % drag-key)
                  & (merge cell-attrs
                           (when is-drop-target?
                             {:className "border-l-4 border-purple-500 x-child-pointer-events-none"}))}
       (d/div {:class (cond-> '[group
                                flex flex-row justify-between items-center]
                        is-drag-source?
                        (concat #_'[border-2 border-red-50]))
               :onMouseEnter (when-not drag-source
                               #(set-anchor-el (j/get % :currentTarget)))
               :onMouseLeave close-popover}
         (d/div {:class '[flex flex-row justify-between items-center space-x-1]}
           title
           (when sorting
             (hx/<>
              (d/i {:data-cy "sort-indicator"
                    :class (conj '[fas cursor-pointer p-1 border-2
                                   text-xs
                                   border-gray-700 shadow]
                                 (if (= sorting :descending)
                                   'fa-sort-amount-down
                                   'fa-sort-amount-up))})))
           (when anchor-el
             ($ popover {:anchorEl anchor-el
                         :data-cy "delete-doc-dialog"
                         :draggable true
                         :onDragStart #(drag/on-drag-start % drag-key)
                         :onDragEnd #(drag/on-drag-end % drag-key)
                         :onClose close-popover
                         :anchorOrigin (j/lit {:horizontal :left
                                               :vertical   :top})

                         & (when is-drag-source?
                             is-drag-source?
                             {:className "pointer-events-none"})
                         }
                (d/div {:on-mouse-leave close-popover
                        :class '[p-4 space-x-2
                                 flex flex-row justify-start items-center]}
                  title

                  (if sorting
                    (hx/<>
                     ($ tooltip {:title "Click to reverse sort"}
                        (d/i {:data-cy "sorted-col"
                              :class (conj '[fas cursor-pointer p-1 border-2
                                             text-xs
                                             border-gray-700 shadow]
                                           (if (= sorting :descending)
                                             'fa-sort-amount-down
                                             'fa-sort-amount-up))
                              :on-click on-reverse-sort}))
                     ($ tooltip {:title "Clear sorting"}
                        (d/i {:data-cy "clear-sorting"
                              :class '[fal fa-times-circle
                                       hover:text-blue-300
                                       cursor-pointer]
                              :on-click on-clear-sort})))
                    ($ tooltip {:title "Sort with this column"}
                       (d/i {:data-cy "sort-col"
                             :class '[fas fa-sort-amount-up
                                      hover:text-blue-300
                                      cursor-pointer]
                             :on-click on-sort})))
                  ($ tooltip {:title "Visualize"}
                     (d/i {:data-cy "visualize-col"
                           :class '[fas fa-chart-bar hover:text-blue-500
                                    cursor-pointer]
                           :on-click goto-chart}))))))))))

(defnc column-header-title [{:keys [title]}]
  ($ Typography {:variant "subtitle1"}
     title))

(defnc stat-header-title [{:keys [title]}]
  ($ tooltip
    {:title title}
    ($ Typography {:variant "subtitle1"}
       (truncate-left title 30))))

(defnc jobs-table-header [{:keys [headers job-count drag]}]
  ($ TableHead
    ($ TableRow
      ($ TableCell {:align "left"
                    :key "job"}
         (d/div {:class '[flex flex-row items-center
                          space-x-4]}
           (d/span (str "Job (" job-count " rows)"))
           ($ tooltip {:title "Click to configure which columns to display"}
              (d/i
                  {:data-cy "show-studio-preference"
                   :class '[fas fa-cog cursor-pointer]
                   :on-click toggle-prefs-dialog}))))
      (for [{:keys [sorting id title stat?]} headers
            :let [title (if stat?
                          ($ stat-header-title {:title title})
                          ($ column-header-title {:title title}))]]
        ($ header-cell
          {:id id
           :key id
           :stat? stat?
           :sorting sorting
           :title title
           :drag drag
           :cell-attrs {:data-cy (if (keyword? id)
                                   (str "col-" (name id))
                                   "col-stat")}})))))

(defn get-el-available-height [el]
  (let [screen-height (j/get-in js/document [:documentElement :clientHeight])
        el-top (-> el
                   (j/call :getBoundingClientRect)
                   (j/get :top))]
    (max (- screen-height el-top) 100)))

(defn adjust-el-height [el]
  (j/assoc-in! el [:style :height] (str (get-el-available-height el) "px")))

(defnc job-infos-table-impl [{:keys [sorts headers infos drag]}]
  (d/div {:class '[overflow-y-scroll]}
    ($ Table {:stickyHeader true
              :data-cy "infos-table"
              :size "small"}
       ($ jobs-table-header {:headers headers
                             :sorts sorts
                             :drag drag
                             :job-count (count infos)})
       ($ TableBody
         (for [[job info] infos]
           ($ job-row {:job job
                       :key job
                       :drag drag
                       :info info}))))))

(defn job-infos-table []
  (let [headers @(rf/subscribe [:studio/table.headers])
        sorts @(rf/subscribe [:studio/sorts])
        infos @(rf/subscribe [:studio/table.rows])
        drag @(rf/subscribe [:studio/drag])]
    ;; #p infos
    ($ job-infos-table-impl {:headers headers
                             :sorts sorts
                             :infos infos
                             :drag drag})))

(defn render-input-func [label & [auto-focus]]
  (fn [params]
    ($ TextField
      {:label label
       :autoFocus auto-focus
       :variant "outlined" & params})))

(def render-k-input
  (render-input-func "arg"))

(def render-v-input
  (render-input-func "value"))

(def render-stats-input
  (render-input-func "stats" true))

(defnc filters-edit-view [{:keys [args k v]}]
  (d/div {:class '[w-full px-2 flex
                   flex-col md:flex-row
                   justify-start
                   items-center
                   space-x-0 space-y-2
                   md:space-x-4 md:space-y-0]}
    ($ Autocomplete {:id "job-args-k"
                     :options (into-array (keys args))
                     :className "w-full flex-grow"
                     :value (or k "")
                     :onChange
                     (fn [_ value]
                       (rf/dispatch [:studio/filters.update-key value]))
                     :renderInput render-k-input})
    (when k
      (d/i {:class '[far fa-equals]}))
    (when k
      (let [vcounts (get args k)]
        ($ Autocomplete {:id "job-args-v"
                         :options (into-array (keys vcounts))
                         :getOptionLabel (fn [v]
                                           (if (str/blank? v)
                                             ""
                                             (str v " (" (get vcounts v) " jobs)")))
                         :className "w-full flex-grow"
                         :value (or v "")
                         :onChange
                         (fn [_ value]
                           (rf/dispatch [:studio/filters.update-value value]))
                         :renderInput render-v-input})))
    (d/i {:class '[text-xl cursor-pointer
                   hover:shadow
                   flex-none
                   fal fa-times-circle]
          :on-click #(rf/dispatch [:studio/filters.clear])})))


(defnc filter-header-view [{:keys [args filters]}]
  (let [{:keys [filtering]} filters]
    (d/div {:class '[w-full pl-1]}
      (cond
        filtering
        ($ filters-edit-view {:args args & (select-keys filters [:k :v])})

        :else
        ($ Button {:color "primary"
                   :onClick #(rf/dispatch [:studio/filters.begin-filter])
                   :variant "contained"}
           (d/div {:class '[space-x-2]}
             (d/i {:class '[fal fa-filter]})
             (d/span "Filter jobs by argument")))))))

(defn collect-args [results]
  (->> results
       vals
       (filter :success)
       (map :info)
       (mapcat #(-> % (get "spider_args")))
       ;; map of (k, v) => count
       (frequencies)
       ;; Map[k, Map[v, count]]
       (reduce (fn [accu [[k v] n]]
                 (update accu k (fnil assoc (sorted-map)) v n))
               {})))

(defn collect-stats [results]
  (->> results
       vals
       (filter :success)
       (mapcat #(get-in % [:info "scrapystats"]))
       (map first)
       (into #{})))

(defnc reset-columns-button []
  ($ Button {:className "flex-none"
             :variant "contained"
             :color "primary"
             :onClick #(rf/dispatch [:studio/prefs.reset])}
     "Reset to defaults"))

(defnc column-checkboxes [{:keys [columns]}]
  ($ FormGroup {:row false}
     (for [[k label] (map vector info-keys info-titles)
           :let [visible (get columns k)]]
       (let [checkbox
             ($ Checkbox
               {:checked (js/Boolean visible)
                :onChange #(rf/dispatch [:studio/prefs.toggle-column k])})]
         ($ FormControlLabel {:control checkbox
                              :key label
                              :label label})))))

(defnc stats-display-tab [{:keys [selected-stats results]}]
  (let [stats (use-memo [results]
                (collect-stats results))]
    (d/div {:class '[pt-2 w-full h-full
                     flex flex-col justify-start
                     space-y-4]}
      ($ Autocomplete {:id "stats"
                       :options (-> stats
                                    #_(clojure.set/difference selected-stats)
                                    sort
                                    into-array)
                       :className "w-full flex-grow"
                       :onChange
                       (fn [_ value]
                         (when value
                           (rf/dispatch [:studio/prefs.add-stat value])))
                       :renderInput render-stats-input})
      (when selected-stats
        (d/div {:class '[w-full h-full flex flex-col justify-start space-y-2]}
          (for [k selected-stats
                :let [_ (type selected-stats)]]
            (d/div {:key k
                    :data-cy "hide-stat"
                    :class '[w-full space-x-2
                             flex flex-row justify-start items-center]}
              (d/i {:data-cy "hide-stat-button"
                    :class '[far fa-times cursor-pointer]
                    :on-click #(rf/dispatch [:studio/prefs.remove-stat k])})
              ($ Typography {:variant "subtitle1"}
                 k))))))))

(defonce current-tab (atom 0))

(defnc prefs-dialog-content [{:keys [prefs results]}]
  (let [[tab set-tab] (use-atom current-tab)]
    (d/div {:class '[p-8 w-full h-full
                     flex flex-col items-start justify-start
                     space-y-4]
            :style {:height "500px"}}
      (d/div {:class '[w-full flex flex-row justify-between items-center]}
        ($ Typography {:variant "h5"
                       :color "primary"}
           "Jobs Studio Preferences")
        (d/i {:data-cy "close-preference-dialog"
              :class '[text-2xl fas fa-times-circle cursor-pointer]
              :on-click toggle-prefs-dialog}))
      (d/div {:class '[w-full h-full
                       overflow-hidden
                       flex flex-col]}
        ($ Tabs {:orientation "horizontal"
                 :value tab
                 :className "flex-none"
                 :onChange (fn [_ index]
                             (set-tab index))}
           ($ Tab {:label "Columns to display"})
           ($ Tab {:label "Stats to display"}))
        (d/div {:class '[w-full h-full px-4 flex-grow overflow-y-auto]}
          (cond
            (= tab 0)
            (hx/<>
             ($ column-checkboxes {:columns (:columns prefs)})
             ($ reset-columns-button))

            :else
            ($ stats-display-tab {:selected-stats (:stats prefs)
                                  :results results})
            ))))))

(defnc prefs-dialog [props]
  ($ Dialog {:onClose toggle-prefs-dialog
             :data-cy "studio-preference-dialog"
             :fullWidth true
             :maxWidth "sm"
             :open true}
     ($ prefs-dialog-content {& props})))

(defnc job-infos-view [{:keys [state filters prefs]}]
  (def vresults (:results state))
  (d/div {:class "w-full h-[calc(100%-56px)] pt-4 flex flex-col justify-start space-y-2"}
    (let [args (use-memo [state]
                 (collect-args (:results state)))]
      ($ filter-header-view {:args args :filters filters}))
    (r/as-element [job-infos-table])
    (when (:showing prefs)
      ($ prefs-dialog {:prefs prefs & state}))))

(defn has-cache? [from_ to_ spider_]
  (when-let [{:keys [_state from to spider]} @(rf/subscribe [:studio/state])]
    (and _state
         (= from from_)
         (= to to_)
         (= spider spider_))))

(defnc job-detail-view-impl [{:keys [state filters prefs]}]
  (j/let [^:js {:keys [project spider from_id to_id]} (useParams)
          from (str project "/" spider "/" from_id)
          to (str project "/" spider "/" to_id)]
    (assert (and (is-job-valid? from)
                 (is-job-valid? to)))
    (use-effect :once
      (rf/dispatch [:app/set-title (str "Job " from " to " to)])

      ;; the job ids could be empty in db when user sets the url
      ;; directly.
      (when (empty? @(rf/subscribe [:studio/jobs]))
        (rf/dispatch [:studio/update-jobs [from to]]))

      (let [spider (get-spider from)
            from (get-job-number from)
            to (get-job-number to)]
        (when-not (has-cache? from to spider)
          (rf/dispatch [:studio/fsm-start {:spider spider
                                           :from from
                                           :to to}]))))

    (cond
      (fsm/matches state :fetching)
      ($ loading-view {& state})

      (fsm/matches state :fetched)
      ($ job-infos-view {:state state
                         :filters filters
                         :prefs prefs})

      (fsm/matches state :fetch-failed)
      ($ error-view)
      )))

(defn job-detail-view [{:keys [done-view]}]
  (let [state @(rf/subscribe [:studio/state])
        filters @(rf/subscribe [:studio/filters])
        prefs @(rf/subscribe [:studio/prefs])]
    ;; #p (:_state state)
    ;; #p drag
    (if (and done-view
             (fsm/matches state :fetched))
      done-view
      ($ job-detail-view-impl {:state state
                               :filters filters
                               :prefs prefs}))))

(defn fetch-or-chart-view []
  [job-detail-view {:done-view [job-chart-view]}])

(defnc jobs-studio-view []
  (use-effect :once
    (rf/dispatch-sync [:studio/init]))
  (j/let [^:js {:keys [path]} (useRouteMatch)]
    ($ Switch
      ($ Route
        {:path
         (str path
              "/job/:project/:spider/:from_id/_/:to_id")}
        (r/as-element [job-detail-view]))
      ($ Route
        {:path
         (str path
              "/chart/:project/:spider/:from_id/_/:to_id")}
        (r/as-element [fetch-or-chart-view]))
      ($ Route {:path path}
         (r/as-element [job-input-view])))))


(comment
  (def vresults {})
  (keys vresults)
  (tap> @(rf/subscribe [:studio]))
  (-> @(rf/subscribe [:studio])
      keys)
  @(rf/subscribe [:studio/prefs])
  (-> @(rf/subscribe [:studio]) :recents)
  (-> @(rf/subscribe [:studio]) :state keys)
  (-> @(rf/subscribe [:studio]) :state (fsm/matches :fetched))
  (-> @(rf/subscribe [:studio/chart.width]))
  (collect-args vresults)

  ())
