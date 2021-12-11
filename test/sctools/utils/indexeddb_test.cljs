(ns sctools.utils.indexeddb-test
  (:require [sctools.utils.indexeddb :as idb]
            [applied-science.js-interop :as j]
            [kitchen-async.promise :as p]
            [cljs.test :refer [deftest is async use-fixtures]]))

(def test-db-name "testdb1")
(def test-store-name "teststore1")

(deftest ^:focus idb-test
  (async
    done
    (p/let [db (idb/open-db test-db-name 1
                            (j/lit {:upgrade
                                    (fn [db]
                                      (j/call db :createObjectStore test-store-name))}))
            get-item (partial idb/get-item test-db-name test-store-name)
            set-item (partial idb/set-item test-db-name test-store-name)
            delete-item (partial idb/delete-item test-db-name test-store-name)

            _ (set-item :foo :bar)
            _ (set-item :foo1 :bar1)
            v (get-item :foo)
            _ (is (= :bar v))

            _ (delete-item :foo)
            v (get-item :foo)
            _ (is (= nil v))

            _ (set-item :foo :baz)
            v2 (get-item :foo)
            _ (is (= :baz v2))

            _ (idb/clear-store test-db-name test-store-name)
            v (get-item :foo)
            _ (is (= nil v))
            v (get-item :foo1)
            _ (is (= nil v))]
      (done))))
