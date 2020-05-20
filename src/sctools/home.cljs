(ns sctools.home
  (:require ["react" :as react]
            ["react-router-dom" :refer [HashRouter Switch Route Link]]
            ["@material-ui/core"
             :refer [AppBar Toolbar Typography IconButton Button
                     List ListItem ListItemIcon ListItemText
                     Divider Icon]]
            ["@material-ui/core/styles" :refer [createMuiTheme ThemeProvider]]
            [sctools.theme :refer [theme]]
            [re-frame.core :as rf]
            [helix.core :as hx :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]))

(defnc index-view []
  (d/div
    {:class '[h-full w-full flex flex-col items-center justify-start]}
    "Hello SCTools"))

(defnc settings-view []
  (d/div
    {:class '[h-full w-full flex flex-col items-center justify-start]}
    "Hello Settings"))

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
                      :className "flex-grow"
                      }
          "News")
       ($ Button {:color "inherit"}
          "Login"))))

(defnc list-item-link [{:keys [icon primary to] :as props}]
  (let [memorized-comp (fn [item-props ref]
                         ($ Link {:to to
                                  :ref ref
                                  & item-props}))
        render-link (hooks/use-memo [to]
                      (react/forwardRef memorized-comp))]
    (d/li
     ($ ListItem {:button true
                  :component render-link}
        (when icon
          ($ ListItemIcon
             ($ Icon {:className (str "fa " icon)})))
        ($ ListItemText {:primary primary})))))

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
                          :icon "fa-cog"}))))

(defnc main-area [{:keys [drawer-open children] :as props}]
  (d/div {:className (str "x-main-area md:x-ml-240px "
                          (when drawer-open "x-drawer-open"))}
    ($ top-bar)
    children))

(defnc home-view-impl [{:keys [drawer-open]}]
  ($ ThemeProvider {:theme theme}
    ($ HashRouter
       (d/div {:class '[flex-grow]}
              ($ main-area {:drawer-open drawer-open}
                 ($ Switch
                    ($ Route {:path "/settings"}
                       ($ settings-view))
                    ($ Route {:path "/"}
                       ($ index-view))))
              ($ side-bar {:drawer-open drawer-open})))))

(defn home-view []
  (let [drawer-open @(rf/subscribe [:layout/drawer-open])]
    ($ home-view-impl {:drawer-open drawer-open})))
