(ns sctools.utils.string-utils
  (:require [applied-science.js-interop :as j]))

(defn truncate-left
  [s n]
  (let [len (count s)]
    (if (<= len n)
      s
      (str "..." (j/call s :slice (- len n))))))
