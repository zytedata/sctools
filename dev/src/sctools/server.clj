(ns sctools.server
  "Fake server to generate data for integration tests."
  (:require [cheshire.core :as json]
            [hiccup.core :as h]
            [clojure.string :as str]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor.helpers :as interceptor :refer [defbefore]]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [ring.middleware.resource :refer [resource-request]]
            [io.pedestal.http.route :as http.route]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def log-request
  "Same as io.pedestal.http.log-request except it also shows the query
  params of the request."
  (interceptor/on-request
    ::http/log-request
    (fn [request]
      (log/info :msg (format "%s %s"
                             (clojure.string/upper-case (name (:request-method request)))
                             (str (:uri request)
                                  (when-let [query (:query-string request)]
                                    (str "?" query)))))
      (log/meter ::request)
      request)))

(def ok             (partial response 200))
(def not-authorized (partial response 401))
(def forbidden      (partial response 403))
(def internal-error (partial response 500))
(def not-found      (partial response 404))

(defn echo [request]
  (def vr request)
  (-> request
      (select-keys [:async-supported?
                    :context-path
                    :headers
                    :path-info
                    :path-params
                    :protocol
                    :query-params
                    :query-string
                    :remote-addr
                    :request-method
                    :scheme
                    :server-name
                    :server-port
                    :uri])
      ok))

(defn app-html []
  [:html
   [:head
    [:meta {:name :viewport
            :content "width=device-width, user-scalable=no"}]
    [:link {:rel "stylesheet" :href "/static/dev/styles/tailwind-main.css"}]
    [:link
     {:rel "stylesheet"
      :href
      "https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap"}]
    [:link {:rel "stylesheet"
            :href "https://fonts.googleapis.com/icon?family=Material+Icons"}]]
   [:body
    [:div#app]
    [:script {:src "/static/dev/js/main.js"}]]])

(defn app-view [request]
  (let [content (h/html (app-html))]
    (ok content "Content-Type" "text/html")))

(defn my-resource [request]
  (resource-request request "/app"))

(def server-routes
  #{
    ["/" :get `app-view :route-name :app]
    ["/dev/cards.html" :get `app-view :route-name :devcards]
    ["/static/*path" :get [`my-resource] :route-name :static]
    ["/echo/*path"
     :get [http/json-body `echo]
     :route-name :echo]})

(defn- create-routes []
  (fn []
    (http.route/expand-routes (var-get #'server-routes))))

(defn- create-server! [routes]
  (let [host "127.0.0.1"
        port 3345]
    (http/create-server
     {::http/routes     routes
      ::http/type       :jetty
      ::http/host       host
      ::http/port       port
      ::http/join?      false
      ::http/request-logger #'log-request
      ::http/secure-headers
      {:content-security-policy-settings {:object-src "'none'"}}
      ::http/container-options {:daemon? true}})))

(defonce server-ref (atom nil))

(defn start!
  "Start the server in fthe "
  []
  (reset! server-ref
        (-> (create-routes)
            (create-server!)
            http/start))
  nil)

(defn stop!
  "Start the server in fthe "
  []
  (when-some [server @server-ref]
    (http/stop server))
  nil)

(comment

  (do
    (stop!)
    (start!)
    ())

  ())
