(ns sctools.theme
  (:require ["@material-ui/core/styles" :refer [createMuiTheme]]))

(def theme
  (createMuiTheme
   (clj->js {:palette {:primary {:main "rgba(0, 0, 0, 0.7)"}}})))
