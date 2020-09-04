(ns sctools.app.logging
  (:require [lambdaisland.glogi :as log]
            [lambdaisland.glogi.console :as glogi-console]))

(defn start-logging []
  (glogi-console/install!)
  (if ^boolean goog.DEBUG
    (log/set-levels {:glogi/root :info})
    (log/set-levels {:glogi/root :info})))


(comment
  (log/debug :msg "hi")
  
  ())
