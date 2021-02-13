(ns sctools.utils.uri
  (:require [goog.Uri]
            [goog.Uri.QueryData]))

(defn to-string [s]
  (cond
    (keyword? s)
    (name s)

    :else
    (str s)))

(defn map->querydata [m]
  (let [qdata (goog.Uri.QueryData.)]
    (reduce-kv (fn [q k v] (.add q (to-string k) (to-string v)))
               qdata m)
    (str qdata)))

(defn map->query [m]
  (-> m
      map->querydata
      str))

(defn add-query [path query]
  (-> (goog.Uri. path)
      (.setQueryData (map->querydata query))
      (.toString)))
