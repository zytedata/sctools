(ns sctools.cypress.basic-spec
  (:require [applied-science.js-interop :as j]))

(def cy js/cy)
(def it js/it)
(def describe js/describe)

;; describe('The Home Page', () => {
;;   console.log('it', it)
;;   it('successfully loads', () => {
;;     cy.visit('/index-dev.html')
;;     cy.get('body').type('{ctrl}h')
;;     // cy.window().then((win) => {
;;     //   win.localStorage.setItem("day8.re-frame-10x.show-panel", "false")
;;     // });
;;     cy.get('input[name="api-key"]').type('ffffffffffffffffffffffffffffffff')
;;     cy.contains('Test').click()
;;     cy.contains('Go').click()
;;   })
;; })

(describe
 "The Home page"
 (fn []
   (it
    "successfully loads"
    (fn []
      (.visit cy  "/index-dev.html")
      (-> cy
          (.get "body")
          (.type "{ctrl}h"))
      ;; (-> cy
      ;;     (.get "input[name=\"api-key\"]")
      ;;     (.type "ffffffffffffffffffffffffffffffff"))
      ;; (-> cy
      ;;     (.contains "Test")
      ;;     (.click))
      ;; (-> cy
      ;;     (.contains "Go")
      ;;     (.click))
      ))))

