(ns sctools.studio.utils
  (:require ["moment" :as moment]
            [medley.core :as m]
            [clojure.string :as str]
            [sctools.utils.time-utils :refer [readable-time-delta]]
            [linked.core :as linked]
            [sctools.utils.common :refer [keyword->str]]))

;; TODO: move to utils?
(defn format-time
  [ts]
  (if (nil? ts)
    "N/A"
    (let [date (.utc ^js moment ts)]
      (if (.isValid date) (.format date "YYYY-MM-DD HH:mm:ss UTC") ts))))

(def sensitive-args
  #{"password"
    "passwd"
    "aws_secret_access_key"
    "api-key"
    "apikey"
    "shub_api_key"
    "shub_apikey"
    "sh_apikey"
    "sh_api_key"})

(defn hide-secrets [k v]
  (if (sensitive-args (str/lower-case k))
    "******"
    v))

(defn format-job-args [args]
  (->> args
       (map (fn [[k v]] (str k "=" (hide-secrets k v))))
       (str/join "\n")))

(defn format-runtime [running_time finished_time]
  (let [delta (-> (if finished_time
                    (moment finished_time)
                    (moment))
                  (.diff (moment running_time)))]
    [delta (readable-time-delta (/ delta 1000))]))

(def counter-trans
  (fnil str 0))

(defn get-runtime [jobinfo]
  (if-let [running_time (jobinfo "running_time")]
    (format-runtime running_time (jobinfo "finished_time"))
    [0 "N/A"]))

(def info-attrs
  ;; Here we use array map to keep the display order.
  (array-map :state {:title "State" :trans identity}
             :close_reason {:title "Outcome" :trans (fnil str "N/A")}
             :spider_args {:title "Args" :trans format-job-args}
             :items {:title "Items" :trans counter-trans}
             :pages {:title "Requests" :trans counter-trans}
             :logs {:title "Logs" :trans counter-trans :display false}
             :errors {:title "Errors" :trans counter-trans}
             :pending_time {:title "Schedule time" :trans format-time :display false}
             :running_time {:title "Start time" :trans format-time}
             :finished_time {:title "Finish time" :trans format-time}
             :version {:title "Version" :trans identity :display false}
             :units {:title "Units" :trans identity :display false}

             ;; derived fields
             :runtime {:title "Runtime" :derived? true :trans get-runtime}))

(def info-keys (keys info-attrs))

(def info-titles (map (fn [[_ {:keys [title]}]]
                        title)
                      info-attrs))

(defn spider-name-from-results [results]
  (->> results
       (some (fn [[job result]]
               (when (:success result)
                 (-> result
                     :info
                     (get "spider")))))))

(def info-displays
  (->> info-keys
       (map (fn [k]
              [k (if-some [display (:display (k info-attrs))]
                   display
                   true)]))
       (into {})))

(defn is-job-valid? [job]
  (and (not (str/blank? job))
       (re-matches #"[1-9][0-9]*/[1-9][0-9]*/[1-9][0-9]*" job)))

(defn get-spider [job]
  (->> (str/split job #"/")
       (take 2)
       (str/join "/")))

(defn get-job-number [job]
  (-> (str/split job #"/")
      last
      js/Number))

(defn format-info-field [info k]
  (let [{:keys [trans derived?]
         :or {trans identity}} (get info-attrs k)]
    (if derived?
      (trans info)
      (as-> (get info (name k)) v
        [v (trans v)]))))
