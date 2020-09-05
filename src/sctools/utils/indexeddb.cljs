(ns sctools.utils.indexeddb
  (:require ["idb" :refer [openDB deleteDB wrap unwrap]]
            [cognitect.transit :as t]
            [applied-science.js-interop :as j]
            [kitchen-async.promise :as p]))

(def r (t/reader :json))
(def w (t/writer :json))

(defn- read-transit [x]
  (t/read r x))

(defn- write-transit [x]
  (t/write w x))

(defn- check-key [k]
  (assert (or (string? k)
              (keyword? k))))

(defonce dbs (atom nil))

(defn open-db
  [db-name version opendb-callbacks]
  (if-let [db (get dbs db-name)]
    (p/->promise db)
    (p/let [db (openDB db-name version opendb-callbacks)]
      (swap! dbs assoc db-name db)
      db)))

(defn set-item [db-name store-name k v]
  (check-key k)
  (let [db (get @dbs db-name)
        tx (j/call db :transaction store-name "readwrite")
        store (j/call tx :objectStore store-name)]
    (p/try
      ;; value is before key!
      (j/call store :put (write-transit v) (str k))
      (j/get tx :done))))

(defn get-item
  ([db-name store-name k]
   (get-item db-name store-name k nil))
  ([db-name store-name k not-found]
   (check-key k)
   (let [db (get @dbs db-name)
         tx (j/call db :transaction store-name "readonly")
         store (j/call tx :objectStore store-name)]
     (p/try
       (p/let [v (j/call store :get (str k))]
         (if v
           (read-transit v)
           not-found))))))

(defn close-all-dbs []
  (doseq [[k db] @dbs]
    (p/do
      (j/call db :done)
      (swap! dbs dissoc k))))

(comment

  @dbs
  (close-all-dbs)

  (open-db "test1" 1 (j/lit {:upgrade
                             (fn [db]
                               (j/call db :createObjectStore "store1"))}))
  (set-item "test1" "store1" :foo :bar)
  (p/let [k :foo
          v (get-item "test1" "store1" :foo)]
    (println [k v]))
  (write-transit "true")
  ())
