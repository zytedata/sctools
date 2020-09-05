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
            [bb-utils.clojure :refer [cond*]]
            [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.dom :as d]
            [helix.hooks :as hooks :refer [use-effect use-memo use-state]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [sctools.app.layout :as layout]
            sctools.studio.events
            sctools.studio.machine
            sctools.studio.subs
            [sctools.studio.utils
             :refer
             [get-job-number get-spider info-keys info-titles is-job-valid?]]
            [sctools.utils.rf-utils :as rfu :refer [use-atom]]
            [sctools.widgets.common :refer [error-msg tooltip]]
            [statecharts.core :as fsm]))

(defnc job-input [{:keys [job cb-event label]}]
  ($ TextField {:name "job-id"
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

    (> (- to-id from-id) 500)
    "At most 500 jobs to show."

    :else
    nil))

(defnc job-range-input-view-impl [{:keys [jobs recents]}]
  (layout/set-title "Jobs Studio")
  (let [{:keys [from to]} jobs
        [error set-error] (use-state nil)
        [anim set-anim] (use-state false)]
    (d/div
     {:class '[h-full max-w-sm lg:max-w-lg
               mx-auto space-x-8
               pt-20 flex flex-row justify-start items-start]}
     (d/div {:onChange #(set-error nil)
             :class '[w-full h-full
                      flex flex-col justify-start items-start space-y-5]}

            (d/div {:class '[text-2xl]} "From Job:")
            ($ job-input {:job from
                          :cb-event :studio/update-from
                          :label "From Job"})

            (d/div {:class '[text-2xl]} "To Job:")
            ($ job-input {:job to
                          :cb-event :studio/update-to
                          :label "To Job"})
            (d/div {:class '[w-full flex flex-row space-x-4 items-start]}
                   ($ Button {:className "flex-none"
                              :color "primary"
                              :onClick
                              (fn [event]
                                (if-some [error (get-valdiation-error from to)]
                                  (set-error error)
                                  (rf/dispatch [:studio/submit])))
                              :variant "contained"}
                      "Go!")
                   (when error
                     ($ Alert {:severity "warning"}
                        error))))
     (when-let [recents (seq recents)]
       (d/div
        {:class '[flex flex-col justify-start items-start space-x-4]}
        (d/div {:class '[text-2xl]} "Recently Used")
        (d/ul
         {:class '[list-disc]}
         (for [{:keys [from to spider-name] :as k} recents]
           (d/li
            {:key k}
            ($ Button
               {:color "secondary"
                :onClick (fn []
                           (set-anim true)
                           (rf/dispatch [:studio/update-jobs [from to]]))}
               (d/span
                {:class '[normal-case]}
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
    (d/div
     {:class '[h-full w-full pt-16 px-4
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

(defn truncate-left [s n]
  (let [len (count s)]
    (if (<= len n)
      s
      (str "..." (j/call s :slice (- len n 3))))))

(defnc job-row [{:keys [job info]}]
  ($ TableRow
    ($ TableCell {:align "left"}
       ($ Link {:href (job-url job)
                :key "job"
                :rel "noreferrer"
                :target "_blank"} (truncate-left job 15)))
    (for [[k [_ v]] info]
      ($ TableCell {:key k
                    :align "left"
                    :className (when (= k :spider_args)
                                 "whitespace-pre-line")}
         (str v)))))

(defnc header-cell [{:keys [title id stat? sorting]}]
  (let [on-hide (if stat?
                    #(rf/dispatch [:studio/prefs.remove-stat id])
                    #(rf/dispatch [:studio/prefs.toggle-column id]))
        on-sort #(rf/dispatch [:studio/sorts.enable {:id id :stat? stat?}])
        on-reverse-sort #(rf/dispatch [:studio/sorts.reverse])
        on-clear-sort #(rf/dispatch [:studio/sorts.clear])]
    ($ TableCell {:align "left"}
       (d/div {:class '[group
                        flex flex-row justify-between items-center]}
              (d/div {:class '[flex flex-row justify-start items-center
                               space-x-1]}
                     title
                     (if sorting
                       (hx/<>
                        ($ tooltip {:title "Click to reverse sort"}
                           (d/i {:class (conj '[fas cursor-pointer p-1 border-2
                                                text-xs
                                                border-gray-700 shadow]
                                            (if (= sorting :descending)
                                              'fa-sort-amount-down
                                              'fa-sort-amount-up))
                               :on-click on-reverse-sort}))
                        ($ tooltip {:title "Clear sorting"}
                           (d/i {:class '[invisible group-hover:visible
                                          pl-2
                                          fal fa-times-circle
                                          cursor-pointer]
                                 :on-click on-clear-sort})))
                       ($ tooltip {:title "Sort with this column"}
                          (d/i {:class '[invisible group-hover:visible
                                         fas fa-sort-amount-up
                                         cursor-pointer]
                                :on-click on-sort})))
                     #_($ tooltip {:title "Hide this column"}
                        (d/i {:class '[invisible group-hover:visible
                                       fal fa-times-circle
                                       cursor-pointer]
                              :on-click on-hide})))))))

(defnc column-header-title [{:keys [title]}]
  ($ Typography {:variant "subtitle1"}
    title))

(defnc stat-header-title [{:keys [title]}]
  ($ tooltip
    {:title title}
    ($ Typography {:variant "subtitle1"}
       (truncate-left title 10))))

(defnc jobs-table-header [{:keys [headers sorts job-count]}]
  ($ TableHead
    ($ TableRow
       ($ TableCell {:align "left"
                         :key "job"}
          (d/div {:class '[flex flex-row items-center
                           space-x-4]}
                 (d/span (str "Job (" job-count " rows)"))
                 ($ tooltip {:title "Click to configure which columns to display"}
                    (d/i
                     {:class '[fas fa-cog cursor-pointer]
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
             :title title})))))

(defnc job-infos-table-impl [{:keys [sorts headers infos]}]
  (d/div
    {:class '[w-full h-full overflow-y-auto]}
    ($ Paper
       ($ Table {:stickyHeader true
                 :size "small"}
          ($ jobs-table-header {:headers headers
                                :sorts sorts
                                :job-count (count infos)})
          ($ TableBody
             (for [[job info] infos]
               ($ job-row {:job job
                           :key job
                           :info info})))))))

(defn job-infos-table []
  (let [headers @(rf/subscribe [:studio/table.headers])
        sorts @(rf/subscribe [:studio/sorts])
        infos @(rf/subscribe [:studio/table.rows])]
    ($ job-infos-table-impl {:headers headers
                             :sorts sorts
                             :infos infos})))

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
  (d/div
    {:class '[w-full px-2 flex
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
                                             (str v " (" (get vcounts v) ")")))
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
    (d/div
     {:class '[pt-2 w-full h-full
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
       (d/div
        {:class '[w-full h-full flex flex-col justify-start space-y-2]}
        (for [k selected-stats
              :let [_ (type selected-stats)]]
          (d/div {:key k
                  :class '[w-full space-x-2
                           flex flex-row justify-start items-center]}
                 (d/i {:class '[far fa-times cursor-pointer]
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
                  (d/i {:class '[text-2xl fas fa-times-circle cursor-pointer]
                        :on-click toggle-prefs-dialog}))
           (d/div
            {:class '[w-full h-full overflow-hidden
                      flex flex-col]}
            ($ Tabs {:orientation "horizontal"
                     :value tab
                     :className "flex-none"
                     :onChange (fn [_ index]
                                 (set-tab index))}
               ($ Tab {:label "Columns to display"})
               ($ Tab {:label "Stats to display"}))
            (d/div
             {:class '[w-full h-full px-4 flex-grow overflow-y-auto]}
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
             :fullWidth true
             :maxWidth "sm"
             :open true}
    ($ prefs-dialog-content {& props})))

(defnc job-infos-view [{:keys [context filters prefs]}]
  ;; (def vctx context)
  (def vresults (:results context))
  (d/div {:class '[w-full h-full pt-4
                   flex flex-col items-start justify-start space-y-2]}
    (let [args (use-memo [context]
                 (collect-args (:results context)))]
      ($ filter-header-view {:args args :filters filters}))
    (r/as-element [job-infos-table])
    (when (:showing prefs)
      ($ prefs-dialog {:prefs prefs & context}))))

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

      (when-not (some-> @(rf/subscribe [:studio/state]) :value)
        (rf/dispatch [:studio/fsm-start {:spider (get-spider from)
                                         :from (get-job-number from)
                                         :to (get-job-number to)}])))

    (cond
      (fsm/matches state :fetching)
      ($ loading-view {& (:context state)})

      (fsm/matches state :fetched)
      ($ job-infos-view {:context (:context state)
                         :filters filters
                         :prefs prefs})

      (fsm/matches state :fetch-failed)
      ($ error-view)
      )))

(defn job-detail-view []
  (let [state @(rf/subscribe [:studio/state])
        filters @(rf/subscribe [:studio/filters])
        prefs @(rf/subscribe [:studio/prefs])]
    ($ job-detail-view-impl {:state state :filters filters :prefs prefs})))

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
       ($ Route {:path path}
          (r/as-element [job-input-view])))))


(comment
  (keys vresults)
  (tap> @(rf/subscribe [:studio]))
  (-> @(rf/subscribe [:studio]) :recents)
  (collect-args vresults)

  ())
