(ns sctools.utils.rf-utils
  (:require [re-frame.core :as rf]
            [helix.hooks :as hooks :refer [use-state]])
  (:require-macros [sctools.utils.rf-utils :refer [defev]]))

(defn quick-sub
  ([k]
   (quick-sub k nil))
  ([k & {:keys [default]}]
   (let [parent (-> k namespace keyword)
         child (-> k name keyword)]
     (rf/reg-sub
      k
      :<- [parent]
      (fn [v]
        (child v default))))))

(defn db-sub [k & [path]]
  (let [path (or path [k])]
    (rf/reg-sub
     k
     (fn [db _] (get-in db path)))))

(defn make-subscriber
  "Helper function to save typing the subs module name every time"
  [prefix]
  (fn [k]
    @(rf/subscribe [(keyword prefix k)])))

(defn make-dispatcher-impl [prefix sync?]
  (fn [k & body]
    (let [[k body] (if (vector? k)
                      [(first k) (next k)]
                      [k body])
          newk [(keyword prefix k)]
          disp (if sync?
                        rf/dispatch-sync
                        rf/dispatch)]
      (if (nil? body)
        (disp newk)
        (disp (into newk body))))))

(defn make-dispatcher [prefix]
  (make-dispatcher-impl prefix false))

(defn make-sync-dispatcher [prefix]
  (make-dispatcher-impl prefix true))

(defn make-use-state [prefix]
  (fn [sub event]
    (let [sub (keyword prefix sub)
          event (keyword prefix event)
          [state set-state] (use-state @(rf/subscribe [sub]))
          new-set-state (fn [value]
                          (set-state value)
                          (rf/dispatch [event value]))]
      [state new-set-state])))

(defn use-atom
  "The atom is used for purely working with hot-reload. Maybe in the
  prod builds we can just subst it with plain use-state?"
  [aref]
  (let [[state set-state] (use-state @aref)
        new-set-state (fn [new-state & args]
                        (let [new-state
                              (if (fn? new-state)
                                (apply new-state state args)
                                new-state)]
                          (set-state (reset! aref new-state))))]
    [state new-set-state]))

(defn fire-on-event [event f]
  {:when :seen?
   :events event
   :dispatch-fn f
   :halt? true})

(defonce async-flow-idgen (atom 0))

(defn simple-flow [event f]
  {:id (swap! async-flow-idgen inc)
   :rules [(fire-on-event event f)]})

(rf/reg-event-fx
 ::async
 (fn [_ [_ {:keys [event f]}]]
   {:async-flow (simple-flow event f)}))

(defn add-event-hook [event f]
  (rf/dispatch [::async
                {:event event :f f}]))

(defn extract-subkey
  ":set-foo => :foo"
  [k]
  (-> k
      name
      (subs 4)
      keyword))

(defn quick-event
  [path evkey]
  (let [subkey (extract-subkey evkey)]
    (rf/reg-event-db
     evkey
     path
     (fn [module [_ id]]
       (assoc module subkey id)))))
