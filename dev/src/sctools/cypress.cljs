(ns sctools.cypress
  (:require [applied-science.js-interop :as j]
            [bb.clojure :refer [prog1]]
            ["./cy_utils"
             :as cy
             :refer [clickByText fillInput clearInput wait
                     visit reload forward back]]))

(defn parent-val [k]
  (j/get-in js/window [:parent k]))

(defn park-page []
  (visit "sctools/static/dev/styles/tailwind-custom.css"))

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
  (clickByText "button" "Go")
  (clickByText "span" "go to the jobs studio")
  (clickByText "security")
  (clickByText "go")
  (clickByText "go to")
  (clickByText "filter")
  (clickByText "go!")

  (fillInput "from-job" "1/1/1")
  (fillInput "to-job" "1/1/2")
  (clearInput "to-job")

  (wait "@jobs/3")
  (wait 100)
  (cy/get "table[data-cy=infos-table]")
  (cy/get "tr[data-cy=infos-row]")


  (reload)
  (visit "/")
  (visit "/#/settings")
  (visit "/#/debug")
  (cy/stubJobsInfoResponse)

  (back)
  (park-page)

  ())
