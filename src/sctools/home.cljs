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
            [helix.core :as hx :refer [$ defnc]]
            [emotion.core :refer [defstyled]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [sctools.routes :refer [AuthRoute PrivateRoute
                                    list-item-link is-auth-done]]
            [sctools.init :refer [init-view]]
            [sctools.theme :refer [theme]]))

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

(defnc top-bar []
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
          "SC Tools")
       ($ Button {:color "inherit"}
          "About"))))

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
       ($ list-item-link {:to "/"
                          :primary "Home"
                          :icon "fa-home-alt"})
       ($ list-item-link {:to "/settings"
                          :primary "Settings"
                          :icon "fa-cog"})
       ($ list-item-link {:to "/settings/1"
                          :primary "Settings 1"
                          :icon "fa-cog"})
       ($ list-item-link {:to "/init"
                          :primary "API Key"
                          :icon "fa-key"}))))

(defnc main-area [{:keys [drawer-open children] :as props}]
  (d/div {:class (cond-> '[x-main-area md:x-ml-240px]
                   drawer-open
                   (conj 'x-drawer-open))}
    ($ top-bar)
    children))

(defnc home-view-impl [{:keys [drawer-open auth-done]}]
  ($ HashRouter
    (d/div {:class '[flex-grow]}
           ($ main-area {:drawer-open drawer-open}
              ($ Switch
                 ($ AuthRoute {:path "/init"}
                    (r/as-element [init-view]))
                 ($ PrivateRoute {:path "/"}
                    ($ Route {:path "/settings"}
                       ($ settings-view))
                    ($ Route {:path "/"}
                       ($ index-view)))))
           ($ side-bar {:drawer-open drawer-open}))))

(defn home-view []
  (let [drawer-open @(rf/subscribe [:layout/drawer-open])
        auth-done (is-auth-done)]
    ($ home-view-impl {:drawer-open drawer-open
                       :auth-done auth-done})))

(defn bootstrap-view []
  [:div {:class '[w-full h-full
                  flex flex-row items-center justify-center]}
    ($ CircularProgress {:size "10em"})])
