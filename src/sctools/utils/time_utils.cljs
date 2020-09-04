(ns sctools.utils.time-utils
  (:require [clojure.string :as str]))


(def time-units
  (array-map :year 31536000
             :month 2592000
             :day 86400
             :hour 3600
             :minute 60
             :second 1))

(defn parse-time-delta [delta]
  (second (reduce
           (fn [[remaining accu] [unit unit-secs]]
             [(rem remaining unit-secs)
              (assoc accu unit (quot remaining unit-secs))])
           [delta]
           time-units)))

(defn readable-time-delta [delta]
  (let [delta (parse-time-delta delta)]
    (->> (for [unit (keys time-units)]
           (let [value (get delta unit)]
             (when (pos? value)
               (str value " " (name unit) (when (> value 1) "s")))))
         (remove nil?)
         (str/join ", ")
         )))
