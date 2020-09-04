(ns sctools.utils.common
  (:require [cuerdas.core :as cstr]))

(defn keyword->str [kw]
  (-> (str kw)
      (cstr/slice 1)))
