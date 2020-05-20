(ns sctools.devcards.mui
  (:require [reagent.core :as r]
            #_[sctools.mui-reagent]
            [reagent.dom :as rdom]
            ;; Scoped names require Cljs 1.10.439
            ["@material-ui/core" :refer [Button]]
            ["@material-ui/core/styles" :refer [createMuiTheme ThemeProvider]]
            [helix.core :as hx :refer [defnc $]]
            [devcards.core :as cards :refer [defcard defcard-rg]]
            [sctools.theme :refer [theme]]
            [helix.dom :as d]
            [helix.hooks :as hooks]))

(defcard css-test
  (let [foo 'bg-green-500
        bar 'text-white]
    ($ ThemeProvider {:theme theme}
    ($ Button {:variant "contained"
               :color "primary"}
       "Hello World"))))
