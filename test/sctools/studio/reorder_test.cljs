(ns sctools.studio.reorder-test
  (:require [clojure.test :refer [deftest is are]]
            [sctools.studio.reorder :refer [reorder-columns]]))


(deftest reorder-columns-test
  (are [args]
      (let [[columns source target _ new] args]
        (is (= new (reorder-columns columns source target))))
    [[:a :b :c] :b :a :=> [:b :a :c]]
    [[:a :b :c] :b :c :=> [:a :b :c]]

    [[:a :b :c] :c :a :=> [:c :a :b]]
    [[:a :b :c] :c :b :=> [:a :c :b]]

    [[:a :b :c] :a :c :=> [:b :a :c]]
    [[:a :b :c] :a :b :=> [:a :b :c]]))
