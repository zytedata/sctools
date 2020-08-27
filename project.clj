(defproject sctools "0.1.0"
  :description "sctools"
  :source-paths
  ["dev"
   "src"
   "test"
   "resources"
   "assets"
   "checkouts/helix/src"
   "checkouts/devcards/src"]

  :aliases {"test" ["version"]}

  :dependencies [[thheller/shadow-cljs "2.10.18"]
                 [metosin/malli "0.0.1-SNAPSHOT"]

                 [cljs-bean "1.6.0"]
                 [lilactown/helix "0.0.13" :exclusions [cljs-bean]]
                 [emotion-cljs "0.1.2"]
                 [reagent "0.10.0" :exclusions [[cljsjs/react]
                                                [cljsjs/react-dom]
                                                [cljsjs/create-react-class]]]
                 [re-frame "1.1.1" :exclusions [[cljsjs/react]
                                                 [cljsjs/react-dom]
                                                 [cljsjs/create-react-class]]]
                 [day8.re-frame/async-flow-fx "0.1.0"]
                 [day8.re-frame/http-fx "0.2.1"]
                 [day8.re-frame/test "0.1.5" :exclusions [[re-frame]]]

                 [lambdaisland/kaocha "1.0.672"]
                 [binaryage/devtools "1.0.2"]
                 [re-frisk-remote    "1.3.4"]
                 [bdoc/devcards "0.2.7-SNAPSHOT"
                  :exclusions [[cljsjs/react]
                               [org.clojure/core.async]
                               [cljsjs/react-dom]
                               [cljsjs/create-react-class]]]
                 ;; [day8.re-frame/tracing "0.5.3"]
                 [bdoc.re-frame/tracing "1.0.0-SNAPSHOT"]
                 [day8.re-frame/re-frame-10x "0.7.0"
                  :exclusions [[zprint]
                               [binaryage/devtools]]]

                ;;;;;;;;;;;;;;;;;;;;;;;;;;;
                 ;; util libs
                ;;;;;;;;;;;;;;;;;;;;;;;;;;;
                 [applied-science/js-interop "0.2.7"]
                 [binaryage/oops     "0.7.0"]
                 [zprint "1.0.0"]
                 [metosin/reitit "0.5.5"]
                 [hashp "0.2.0"]
                 [philoskim/debux "0.7.5"]
                 [lambdaisland/uri "1.4.54"]
                 [net.cgrand/macrovich "0.2.1"]
                 [postmortem "0.4.0"
                  :exclusions [net.cgrand/macrovich]]
                 [vvvvalvalval/scope-capture "0.3.2"
                  :exclusions [org.clojure/clojure]]
                 [datascript "1.0.0"]

                ;;;;;;;;;;;;;;;;;;;;;;;;;;;
                 ;; Logging
                ;;;;;;;;;;;;;;;;;;;;;;;;;;;
                 [org.clojure/tools.logging "1.1.0"]
                 [org.slf4j/slf4j-api "1.7.30"]
                 [org.apache.logging.log4j/log4j-api "2.13.3"]
                 [org.apache.logging.log4j/log4j-core "2.13.3"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.13.3"]
                 ;; cljs logging
                 [lambdaisland/glogi "1.0.74"]

                 [cljc.java-time "0.1.11"]
                 [tick "0.4.26-alpha"]
                 [thheller/shadow-cljsjs "0.0.21"]

                 ;; http server
                 [io.pedestal/pedestal.service  "0.5.8"]
                 [io.pedestal/pedestal.route    "0.5.8"]
                 [io.pedestal/pedestal.jetty    "0.5.8"]
                 [cheshire                      "5.10.0"]

                ;;;;;;;;;;;;;;;;;;;;;;;;;;;
                ;;; Misc
                ;;;;;;;;;;;;;;;;;;;;;;;;;;;
                 [me.raynes/fs                  "1.4.6"]
                 [garden                        "1.3.10"]
                 [com.rpl/specter    "1.1.3"]
                 [bb-utils "0.1.0-SNAPSHOT"]
                 [com.maitria/packthread "0.1.10"]
                 [funcool/cuerdas    "2020.03.26-3"]
                 [medley             "1.3.0"]
                 [redux              "0.1.4"]
                 [hiccup             "1.0.5"]

                 [refactor-nrepl "2.5.0"]
                 [cider/cider-nrepl "0.25.3"]
                 [org.tcrawley/dynapath "1.1.0"]
                 [com.cemerick/pomegranate "1.1.0"]])
