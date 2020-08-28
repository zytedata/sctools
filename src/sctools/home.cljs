(ns sctools.home
  (:require ["@material-ui/core/AppBar" :default AppBar]
            ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/Divider" :default Divider]
            ["@material-ui/core/Icon" :default Icon]
            ["@material-ui/core/IconButton" :default IconButton]
            ["@material-ui/core/List" :default List]
            ["@material-ui/core/ListItem" :default ListItem]
            ["@material-ui/core/ListItemIcon" :default ListItemIcon]
            ["@material-ui/core/ListItemText" :default ListItemText]
            ["@material-ui/core/Toolbar" :default Toolbar]
            ["@material-ui/core/Typography" :default Typography]
            ["@material-ui/core/CircularProgress" :default CircularProgress]
            ["react" :as react]
            ["react-router-dom" :refer [HashRouter NavLink Redirect Route Switch]]
            [helix.core :as hx :refer [$ defnc <>]]
            [emotion.core :refer [defstyled]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [sctools.routes :refer [AuthRoute PrivateRoute
                                    list-item-link is-auth-done]]
            [sctools.init :refer [init-view]]
            [sctools.theme :refer [theme]]
            [sctools.studio.views :refer [jobs-studio-view]]))

(defstyled Div :div
  {:background-color :inherit})

(defnc index-view []
  ($ Div
    {:className "h-full w-full flex flex-col items-center justify-start"}
    "Hello SCTools"))

(defnc settings-view []
  (d/div
    ($ Switch
       ($ Route {:path "/settings/1"}
        (d/div "Hello Nested Route 2")))
    (d/div
     {:class '[h-full w-full flex flex-col items-center justify-start]}
     "Hello Settings")))

(defnc top-bar [{:keys [title]}]
  ($ AppBar {:position "static"}
    ($ Toolbar
       (d/div {:className "md:hidden"}
        ($ IconButton {:edge "start"
                       :color "inherit"
                       :aria-label "menu"
                       :onClick #(rf/dispatch [:layout/toggle-drawer])}
           ($ Icon {:className "fa fa-bars mr-2"})))
       ($ Typography {:variant "h6"
                      :className "flex-grow"}
          (d/div {:id "title-anchor"
                  :class '[whitespace-pre-wrap space-x-4]}
                 (d/span "SC Tools")
                 (when title
                   (<>
                    (d/span {:class '[align-text-bottom text-xl]}
                            (d/i {:class '[fas fa-caret-right]}))
                    (d/span title)))))
       ($ Button {:color "inherit"}
          (d/a {:href "https://github.com/lucywang000/sctools"
                :target "_blank"}
               "About")))))

(defnc drawer [{:keys [open children] :as props}]
  (d/div {:class (cond-> '[x-drawer]
                   open
                   (conj 'x-drawer-open))}
    (d/div {:class '[x-h-48px md:x-h-64px flex flex-row justify-end items-center]}
           (d/div {:class "md:hidden"}
                  ($ IconButton {:onClick #(rf/dispatch [:layout/toggle-drawer])}
                     (d/i {:class '[fa fa-chevron-left]}))))
    ($ Divider)
    children))

(defnc side-bar [{:keys [drawer-open]}]
  ($ drawer {:open drawer-open}
    ($ List
       ($ list-item-link {:to "/studio"
                          :primary "Jobs Studio"
                          :icon "fa-home-alt"})
       ($ list-item-link {:to "/settings"
                          :primary "Settings"
                          :icon "fa-cog"}))))

(defnc main-area [{:keys [title drawer-open children] :as props}]
  (d/div {:class (cond-> '[x-main-area md:x-ml-240px]
                   drawer-open
                   (conj 'x-drawer-open))}
    ($ top-bar {:title title})
    children))

(defnc home-view-impl [{:keys [drawer-open auth-done title]}]
  ($ HashRouter
    (d/div {:class '[flex-grow]}
           ($ main-area {:drawer-open drawer-open :title title}
              ($ Switch
                 ($ AuthRoute {:path "/init"}
                    (r/as-element [init-view]))
                 ($ PrivateRoute {:path "/"}
                    ($ Route {:path "/settings"}
                       ($ settings-view))
                    ($ Route {:path "/studio"}
                       ($ jobs-studio-view))
                    ($ Route {:path "/"}
                       ($ index-view)))))
           ($ side-bar {:drawer-open drawer-open}))))

(defn home-view []
  (let [drawer-open @(rf/subscribe [:layout/drawer-open])
        title @(rf/subscribe [:app/title])
        auth-done (is-auth-done)]
    ($ home-view-impl {:drawer-open drawer-open
                       :title title
                       :auth-done auth-done})))

(defn bootstrap-view []
  [:div {:class '[w-full h-full
                  flex flex-row items-center justify-center]}
    ($ CircularProgress {:size "10em"})])
