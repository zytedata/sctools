(ns sctools.app.fixes
  (:require [re-frame.loggers]))

;;;;;;;;;;;;;;;;;;;;;;;;
;; various fixes
;;;;;;;;;;;;;;;;;;;;;;;;

(defonce fix-symbol-print
  ;; Fix js/Symbol printting. Otherwise print a react element (e.g. when
  ;; REPL tries to show the result) would result in exceptions.
  ;; https://github.com/binaryage/cljs-devtools/issues/25#issuecomment-266711869
  (when (exists? js/Symbol)
    (extend-protocol IPrintWithWriter
      js/Symbol
      (-pr-writer [sym writer _]
        (-write writer (str "\"" (.toString sym) "\""))))))

(def warn (js/console.log.bind js/console))
(re-frame.loggers/set-loggers!
 {:warn (fn [& args]
          (cond
            (= "re-frame: overwriting" (first args)) nil
            :else (apply warn args)))})
