(ns sctools.theme
  (:require ["@material-ui/core/styles" :refer [createMuiTheme ThemeProvider]]
            [helix.core :as hx :refer [defnc $]]))

;; tailwind breakpoints
(def breakpoints {:values {:xs 0
                           :sm 640
                           :md 768
                           :lg 1024
                           :xl 1280}})

(def theme
  (createMuiTheme
   #_(clj->js {})
   (clj->js {:palette {:primary {:main "rgba(63, 81, 181, 0.8)"
                                 :contrastText "white"}}
             :breakpoints breakpoints})))

(defnc theme-provider [{:keys [children]}]
  ($ ThemeProvider {:theme theme}
    children))
