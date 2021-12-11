(ns sctools.utils.async-utils)

(defn asleep [ms]
  (js/Promise.
    (fn [resolve]
      (js/setTimeout resolve ms))))
