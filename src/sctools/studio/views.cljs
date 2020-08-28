(ns sctools.studio.views
  (:require [helix.core :as hx :refer [defnc $]]
            [sctools.app.layout :as layout]
            [helix.dom :as d]))

(defnc jobs-studio-view []
  (layout/set-title "Jobs Studio")
  "Jobs Studio")

