(ns sctools.utils.transit-utils
  (:require [cognitect.transit :as t]))

(def r (t/reader :json))
(def w (t/writer :json))

(defn read-transit [x]
  (t/read r x))

(defn write-transit [x]
  (t/write w x))
