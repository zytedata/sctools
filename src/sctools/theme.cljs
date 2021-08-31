(ns sctools.theme
  (:require ["@material-ui/core/styles"
             :refer [createTheme ThemeProvider StylesProvider]]
            [applied-science.js-interop :as j]
            [clojure.string :as str]
            [helix.core :as hx :refer [defnc $]]))

;; tailwind breakpoints
(def breakpoints {:values {:xs 0
                           :sm 640
                           :md 768
                           :lg 1024
                           :xl 1280}})

(def fonts
  (str/join "," ["system-ui"
    "-apple-system"
    "Segoe UI"
    "Roboto"
    "Ubuntu"
    "Cantarell"
    "Noto Sans"
    "sans-serif"
    "BlinkMacSystemFont"
    "\"Segoe UI\""
    "Roboto"
    "\"Helvetica Neue\""
    "Arial"
    "\"Noto Sans\""
    "sans-serif"
    "\"Apple Color Emoji\""
    "\"Segoe UI Emoji\""
    "\"Segoe UI Symbol\""
    "\"Noto Color Emoji\""]))

(def theme
  (createTheme
   (j/lit {:palette {:primary {:main "rgba(63, 81, 181, 0.8)"
                               :contrastText "white"}}
           :breakpoints breakpoints
           :typography {:fontFamily fonts}})))

(defnc theme-provider [{:keys [children]}]
  ;; Make sure tailwindcss rules comes later than mui's runtime styles
  ;; so it could have higher specificity.
  ($ StylesProvider {:injectFirst true}
    ($ ThemeProvider {:theme theme}
       children)))
