(ns sctools.routes
  (:require ["@material-ui/core/Icon" :default Icon]
            ["@material-ui/core/ListItem" :default ListItem]
            ["@material-ui/core/ListItemIcon" :default ListItemIcon]
            ["@material-ui/core/ListItemText" :default ListItemText]
            ["react" :as react]
            [sc.api]
            [postmortem.core :as pm]
            [postmortem.instrument :as pi]
            [postmortem.xforms :as xf]
            ["react-router-dom" :refer [NavLink Redirect Route Switch]]
            [helix.core :as hx :refer [$ defnc]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [re-frame.core :as rf]))

(defnc list-item-link [{:keys [icon primary to] :as props}]
  (let [memorized-comp (fn [item-props ref]
                         ($ NavLink {:to to
                                     :ref ref
                                     :activeClassName "x-link-selected"
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

(defn is-auth-done []
  @(rf/subscribe [:init/authed]))

(defnc PrivateRoute [{:keys [children] :as props}]
  (let [render (fn []
                 (if (is-auth-done)
                   ($ Switch
                      children)
                   ($ Redirect {:to "/init"})))]
    ($ Route {:render render & (dissoc props :children)})))

(defnc AuthRoute [props]
  (if-not (is-auth-done)
    ($ Route {& props})
    ($ Redirect {:to "/"})))

#_(defn sc-foo [a b]
  (sc.api/spy
   (+ a b)))

(defn foo [a b]
  (pm/dump :foo)
  (+ a b))

(comment

  (pi/instrument `foo {:xform (xf/take-last 2)})
  (pi/unstrument `foo)

  (foo 1 2)

  ())
