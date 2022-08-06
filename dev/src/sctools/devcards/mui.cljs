(ns sctools.devcards.mui
  (:require [reagent.core :as r]
            #_[sctools.mui-reagent]
            [reagent.dom :as rdom]
            ;; Scoped names require Cljs 1.10.439
            ["@mui/material/Button" :default Button]
            ["@mui/material/styles" :refer [createTheme ThemeProvider]]
            ["@mui/lab/Timeline" :default Timeline]
            ["@mui/lab/TimelineItem" :default TimelineItem]
            ["@mui/lab/TimelineSeparator" :default TimelineSeparator]
            ["@mui/lab/TimelineConnector" :default TimelineConnector] ["@mui/lab/TimelineContent" :default TimelineContent]
            ["@mui/lab/TimelineDot" :default TimelineDot]
            ["@mui/lab/TimelineOppositeContent" :default TimelineOppositeContent]
            ["@mui/material/Typography" :default Typography]
            [helix.core :as hx :refer [defnc $]]
            [devcards.core :as cards :refer [defcard defcard-rg]]
            [sctools.theme :refer [theme]]
            [helix.dom :as d]
            [helix.hooks :as hooks]))

(defcard css-test
  (let [foo 'bg-green-500
        bar 'text-white]
    ($ ThemeProvider {:theme theme}
    ($ Button {:variant "contained"}
       "Hello World"))))
