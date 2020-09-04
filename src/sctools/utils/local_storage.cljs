(ns sctools.utils.local-storage
  (:require [cognitect.transit :as t]))

(def r (t/reader :json))
(def w (t/writer :json))

(defn- read-transit [x]
  (t/read r x))

(defn- write-transit [x]
  (t/write w x))

(defn- check-key [k]
  (assert (or (string? k)
              (keyword? k))))

(defn get-item [k]
  (check-key k)
  (.getItem js/localStorage (str k)))

(defn set-item [k v]
  (check-key k)
  (.setItem js/localStorage (str k) v))

(defn get-transit [k]
  (check-key k)
  (some-> (get-item k)
          read-transit))

(defn set-transit [k v]
  (check-key k)
  (set-item k (write-transit v)))

(defn delete [k]
  (.removeItem js/localStorage (str k)))

(comment
  (write-transit :a)
  (set-item :a "100")
  (get-item :a)
  (set-transit :b ["a" "b"])
  (get-transit :b)
  (delete :a)
  (delete :b)
  (get-transit :sctools.studio/recents)

  ())
