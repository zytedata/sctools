(ns sctools.cypress
  (:require [applied-science.js-interop :as j]
            [bb-utils.clojure :refer [prog1]]))

(defn parent-val [k]
  (j/get-in js/window [:parent k]))

(def vrun (parent-val :vrun))
(def cy (parent-val :cy))

(defn reload []
  (.reload cy)
  (vrun))

(defn click-by-text
  ([text]
   (click-by-text text (j/lit {})))
  ([text opts]
   (prog1 (-> cy
              (.contains text opts)
              (.click))
     (vrun))))

(defn type-text [text]
  (prog1 (-> cy
             (.get "body")
             (.type text))
    (vrun)))

(defn visit [url]
  (prog1 (.visit cy url)
    (vrun)))

;; (describe
;;  "The Home page"
;;  (fn []
;;    (it
;;     "successfully loads"
;;     (fn []
;;       (.visit cy  "/index-dev.html")
;;       (-> cy
;;           (.get "body")
;;           (.type "{ctrl}h"))
;;       (-> cy
;;           (.get "input[name=\"api-key\"]")
;;           (.type "ffffffffffffffffffffffffffffffff"))
;;       (-> cy
;;           (.contains "Test")
;;           (.click))
;;       (-> cy
;;           (.contains "Go")
;;           (.click))
;;       ))))

(comment
  (click-by-text "security")
  (click-by-text "go to" (j/lit {:matchCase false}))
  (click-by-text "go" (j/lit {:matchCase false}))
  (click-by-text "go to")
  (type-text "{ctrl}h")

  (reload)
  (visit "/index-dev.html")
  (visit "/index-dev.html#/settings")
  (visit "/index-dev.html#/debug")

  ())
