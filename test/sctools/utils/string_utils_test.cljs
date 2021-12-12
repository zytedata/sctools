(ns sctools.utils.string-utils-test
  (:require [sctools.utils.string-utils :refer [truncate-left]]
            [cljs.test :as t :include-macros true]
            [cljs.test :refer [deftest is are]]))

(deftest test-truncate-left
  (are [args]
      (let [[before n after] args]
        (is (= (truncate-left before n) after) args))
    ["abcdef" 3 "...def"]
    ["abcdef" 5 "...bcdef"]
    ["abcdef" 6 "abcdef"]

    ["abc" 10 "abc"]
    ))
