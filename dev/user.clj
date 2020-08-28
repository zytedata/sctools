(ns user
  (:require [shadow.cljs.devtools.api :as shadow]
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

;; This ns is set as the :init-ns of shadow-cljs.edn, this way we'll
;; start the dev server immediately when shadow-cljs is started.
(defonce setup-on-init (server/start!))
