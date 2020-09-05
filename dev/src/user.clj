(ns user
  (:require [shadow.cljs.devtools.api :as shadow]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [sctools.server :as server]
            [hashp.core]
            [debux.dbg]
            [debux.cs.core]
            [debux.cs.clog]
            [debux.cs.clogn]
            [debux.cs.util]))

(defn watch! []
  (shadow/watch :app)
  (shadow/nrepl-select :app))

(defn watch-uitest! []
  (shadow/watch :uitest)
  #_(shadow/nrepl-select :uitest))

(defn watch-test! []
  (shadow/watch :test))

(defn stop-watch-uitest! []
  (shadow/stop-worker :uitest))

(defmacro load-test-key []
  (when-some [keyfile (io/resource "sctools_api_key.edn")]
    (read-string (slurp keyfile))))

;; This ns is set as the :init-ns of shadow-cljs.edn, this way we'll
;; start the dev server immediately when shadow-cljs is started.
(defonce setup-on-init (server/start!))


(comment
  (load-test-key)
  (defprotocol Proto1
    :extend-via-metadata true
    (method1 [this])
    (method2 [this]))

  (def obj1
    (with-meta
      {:a 1}
      {`method1 (fn [this] (get this :a))
       `method2 (fn [this] (get this :a))}))

  (deftype Type1 []
    Proto1
    (method1 [this]
      :type1-method1))

  (method1 (Type1.))
  (method2 (Type1.))

  (method1 obj1)
  (method2 obj1)

  (def obj2
    (with-meta
      {:a 1}
      {`method1 (fn [this] (get this :a))
       ;; `method2 (fn [this] (get this :a))
       }))

  (method1 obj2)
  (method2 obj2)

  ())
