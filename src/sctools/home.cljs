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
            ["react-router-dom"
             :refer
             [BrowserRouter HashRouter NavLink Redirect
              Route Switch useParams useHistory]]
            [applied-science.js-interop :as j]
            [helix.core :as hx :refer [$ defnc <>]]
            [emotion.core :refer [defstyled]]
            [helix.dom :as d]
            [helix.hooks :refer [use-effect]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [sctools.routes :refer [AuthRoute PrivateRoute
                                    list-item-link is-auth-done]]
            [sctools.init :refer [init-view]]
            [sctools.dev :refer [dev-view]]
            [sctools.theme :refer [theme]]
            [sctools.studio.views :refer [jobs-studio-view]]))

(defstyled HomeDiv :div
  {:background-color :inherit})


(defnc index-view []
  ($ HomeDiv
    {:className "h-full w-full pt-12 flex flex-col items-center justify-start"}
    ($ Button {:color "primary" :variant "contained"}
       ($ NavLink {:to "/studio"}
          "Go to the Jobs Studio"))))

(defnc settings-child []
  (let [id (j/get (useParams) :id)]
    (d/div "Hello Nested Route " id)))

(defnc settings-view []
  (d/div
    ($ Switch
       ($ Route {:path "/settings/:id"}
          ($ settings-child)))
    (d/div
     {:class '[h-full w-full flex flex-col items-center justify-start]}
     "Hello Settings")))

(defnc top-bar-impl [{:keys [title]}]
  ($ AppBar {:position "static"}
    ($ Toolbar
       (d/div {:className "md:hidden"}
              ($ IconButton {:edge "start"
                             :color "inherit"
                             :aria-label "menu"
                             :onClick #(rf/dispatch [:layout/toggle-drawer])}
                 ($ Icon {:className "fa fa-bars mr-2"})))
       ($ Typography {:variant "h6"
                      :className "flex flex-row flex-grow items-center"}
          (d/div {:id "title-anchor"
                  :class '[whitespace-pre-wrap space-x-4]}
                 (d/span "SC Tools")
                 (when title
                   (<>
                    (d/span {:class '[text-xl]}
                            (d/i {:class '[fas fa-caret-right]}))
                    (d/span title)))))
       ($ Button {:color "inherit"}
          (d/a {:href "https://github.com/lucywang000/sctools"
                :target "_blank"}
               "About")))))

(defn top-bar []
  (let [title @(rf/subscribe [:app/title])]
    ($ top-bar-impl {:title title})))

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

(defnc main-area [{:keys [drawer-open children] :as props}]
  (let [history (useHistory)]
    (use-effect :once
     ;; Capture the history object into the db so we can call
     ;; push/replace state later.
      (rf/dispatch [:app/set-history history])))
  (d/div {:class (cond-> '[x-main-area md:x-ml-240px relative]
                   drawer-open
                   (conj 'x-drawer-open))}
    (r/as-element [top-bar])
    children))

(defnc foo-view [props]
  #p props
  #p (useParams)
  "Hello foo")

(defnc debug-view [props]
  #p props
  #p (useParams)
  "Hello debug")

(defnc home-view-impl [{:keys [drawer-open auth-done]}]
  ($ HashRouter
    (d/div {:class '[flex-grow]}
      ($ main-area {:drawer-open drawer-open}
         ($ Switch
           ($ Route {:path "/debug"}
              ($ debug-view))
           ($ AuthRoute {:path "/init"
                         :component init-view})
           ($ PrivateRoute {:path "/"}
              ($ Route {:path "/settings"}
                 ($ settings-view))
              ($ Route {:path "/studio"}
                 ($ jobs-studio-view))
              (when ^boolean goog.DEBUG
                ($ Route {:path "/init2"}
                   ($ init-view)))
              (when ^boolean goog.DEBUG
                ($ Route {:path "/:foo+"}
                   ($ foo-view)))
              ($ Route {:path "/"}
                 ($ index-view)))))
      ($ side-bar {:drawer-open drawer-open}))))

(defn bootstrap-view []
  [:div {:class '[w-full h-full
                  flex flex-row items-center justify-center]}
    ($ CircularProgress {:size "10em"})])

(defn home-view []
  (if-not @(rf/subscribe [:app/booted])
    [bootstrap-view]
    (let [drawer-open @(rf/subscribe [:layout/drawer-open])
          auth-done (is-auth-done)]
      ($ home-view-impl {:drawer-open drawer-open
                         :auth-done auth-done}))))

(defn app-view []
  (if ^boolean goog.DEBUG
    ($ BrowserRouter
      ($ Switch
        ($ Route {:path "/dev.html"}
           ($ dev-view))
        ($ Route {:path "/"}
           (r/as-element [home-view]))))
    [home-view]))
