(ns sctools.studio.cache
  (:require [cljs.cache :as cache]
            [applied-science.js-interop :as j]
            [sctools.utils.indexeddb :as idb]
            [sctools.utils.local-storage :as local-storage]
            [kitchen-async.promise :as p]))

(defonce infos-cache
  (atom (cache/lru-cache-factory {} :threshold 200)))

(def icache-db "jobs")
(def icache-store "jobinfo")
(def icache-version 1)

(def icache-disable-key :sctools/disable-icache)

(defn -icache-disabled? []
  (local-storage/get-item icache-disable-key))

(def icache-disable? (delay
                       (-icache-disabled?)))

(def icache-opencallbacks
  (j/lit
   {:upgrade (fn [db]
               (j/call db :createObjectStore icache-store))}))

(defn open-icache []
  (idb/open-db icache-db icache-version icache-opencallbacks))

(defn get-cached-info [job]
  (let [cache @infos-cache]
    (if (cache/has? cache job)
      (do (swap! infos-cache cache/hit cache job)
          (cache/lookup cache job))
      ;; This would return a promise!
      (when-not @icache-disable?
        (p/do
          ;; open-icache is safe to call more than once
          (open-icache)
          (p/->
            (idb/get-item icache-db icache-store job)
            :info))))))

(defn cache-job-info [job info]
  (swap! infos-cache cache/miss job info)
  (when-not @icache-disable?
    (idb/set-item icache-db icache-store job {:info info})))

(comment

  (def C (cache/lru-cache-factory {:a 1, :b 2} :threshold 2))
  (cache/lookup C :a)
  (cache/lookup C :b)
  (cache/lookup C :c)
  (cache/miss C :c 2)

  (open-icache)
  (p/->>
    (get-cached-info  "1887/5547/2400")
    (def v1))
  (p/->>
    (idb/get-item icache-db icache-store "1887/5547/2400")
    :info
    (def v1))

  ())
