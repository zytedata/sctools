(ns sctools.hooks
  (:require [clj-kondo.impl.utils :as api :refer [parse-string]]
            [clj-kondo.impl.rewrite-clj.parser :refer [parse-file]])
  (:refer-clojure :exclude [if-let when-let when-some if-some]))

(defmacro prog1 [expr & body]
  `(let [~'<> ~expr]
     ~@body
     ~'<>))

#_(prog1 1 (println <>))

(def nil-token (api/token-node 'nil))

(defn ->token [x]
  (api/token-node x))

(defn wrap-do [body]
  (conj body (api/token-node 'do)))

(defrecord Splice [items])

(defn ->splice [& args]
  (Splice. args))

(defn splice? [x]
  (instance? Splice x))

(defn ->node
  "Convert clojure atoms/vector/seqs to rewrite-clj symbols.

  * list is converted to list-node
  * vector is converted to vector-node
  * nil element in list/vector is removed
  * splice is expanded into parent container
  "
  [x]
  (cond
    (symbol? x)
    (api/token-node x)

    (splice? x)
    (->> (.-items x)
         (remove nil?)
         (map ->node)
         (Splice.))


    (vector? x)
    (api/vector-node (->> x
                          (remove nil?)
                          (map ->node)
                          (reduce (fn [accu cur]
                                    (if (splice? cur)
                                      (reduce into [] [accu (.-items cur)])
                                      (conj accu cur))) [])))

    (seq? x)
    (api/list-node (->> x
                        (remove nil?)
                        (map ->node)
                        (reduce (fn [accu cur]
                                  (if (splice? cur)
                                    (concat accu (.-items cur))
                                    (concat accu [cur]))) nil)))

    :else
    x))

(defn ->list [& args]
  (->node (list* args)))

#_(->node ['if 'let '('if 'let)])

(defn transform-if-let
  "A variation on if-let where all the exprs in the bindings vector must be true.
   Also supports :let.

  (if-let [:let [c 100]
           a foo
           b bar]
    x1
    x2)
  "
  [bindings then else]
   (if (seq bindings)
     (let [bindings (vec bindings) b0-node (first bindings)
           b0 (api/sexpr b0-node)]
       (if (or (= :let b0) (= 'let b0))
         (->list 'let (second bindings)
                 (transform-if-let (drop 2 bindings) then else))
         (let [[sym expr] (take 2 bindings)]
           (->list 'let [sym expr]
                   (->list 'if sym
                           (transform-if-let (subvec bindings 2) then else)
                           else)))))
     then))

(defn if-let [{:keys [node]}]
  (let [[bindings then else] (-> node :children rest)
        bindings (->> bindings :children)]
     {:node (transform-if-let bindings then else)}))

(defn if-let2 [{:keys [node]}]
  (let [[bindings then else] (-> node :children rest)]
    {:node (->list 'let bindings then else)}))

(defn transform-when-let
  "A variation on when-let where all the exprs in the bindings vector must be true.
   Also supports :let.

  (when-let [:let [c 100]
           a foo
           b bar]
    x1
    x2)
  "
  [bindings body]
  (transform-if-let bindings body nil-token))

(defn when-let [{:keys [node]}]
  (let [[bindings & body] (-> node :children rest)
        bindings (->> bindings :children)]
    {:node (transform-when-let bindings (wrap-do body))}))

(defn transform-if-some
  "A variation on if-some where all the exprs in the bindings vector must be true.
   Also supports :let.

  (if-some [:let [c 100]
           a foo
           b bar]
    x1
    x2)
  "
  [bindings then else]
  (if (seq bindings)
    (let [bindings (vec bindings) b0-node (first bindings)
          b0 (api/sexpr b0-node)]
      (if (or (= :let b0) (= 'let b0))
        (->list 'let (second bindings)
                (transform-if-some (drop 2 bindings) then else))
        (let [[sym expr] (take 2 bindings)]
          (->list 'let [sym expr]
                  (->list 'if (->list 'some? sym)
                          (transform-if-some (subvec bindings 2) then else)
                          else)))))
    then))

(defn if-some [{:keys [node]}]
  (let [[bindings then else] (-> node :children rest)
        bindings (->> bindings :children)]
    {:node (transform-if-some bindings then else)}))

(defn transform-when-some [bindings body]
  (transform-if-some bindings body nil-token))

(defn when-some [{:keys [node]}]
  (let [[bindings & body] (-> node :children rest)
        bindings (->> bindings :children)]
    {:node (transform-when-some bindings (wrap-do body))}))

(defn transform-cond*
  "Effectively expand the cond* macro for clj-kondo"
  [clauses]
  (let [[test-node expr & more-clauses] clauses
        test (api/sexpr test-node)]
    (if (next clauses)
      (cond
        (or (= :do test) (= 'do test))
        (->list 'do expr (transform-cond* more-clauses))

        (or (= :let test) (= 'let test))
        (->list 'let expr (transform-cond* more-clauses))

        (or (= :when test) (= 'when test))
        (->list 'when ~expr (transform-cond* more-clauses))

        (or (= :when-let test) (= 'when-let test))
        (transform-when-let (:children expr) (transform-cond* more-clauses))

        (or (= :when-some test) (= 'when-some test))
        (transform-when-some (:children expr) (transform-cond* more-clauses))

        :else
        (if (next more-clauses)
          (->list 'if test-node expr (transform-cond* more-clauses))
          (->list 'when test-node expr)))
      test-node)))


(defn cond* [{:keys [node]}]
  {:node (transform-cond* (-> node :children rest))})

(def auto-map {'cond*     cond*
               'if-some   if-some
               'when-some when-some
               'if-let    if-let
               'when-let  when-let})

(defn auto [{:keys [node] :as input}]
  (let [which    (-> node :children first)
        resolved (get auto-map (api/sexpr which))]
    (resolved input)))

(comment
  (parse-string "(if-let [a foo b bar] x1 x2)")
  (parse-file "dev/src/sctools/example.clj")
  (vector? (parse-string "[1 2]"))
  (list? (parse-string "(1 2)"))
  (seq? (parse-string "(1 2)"))

  (-> (if-let2 {:node (parse-string "(if-let [a foo] x1 x2)")})
      :node
      ;; api/sexpr
      )
  (-> (if-let {:node (parse-string "(if-let [a foo] x1 x2 nil)")})
      :node
      api/sexpr
      )
  (-> (if-let {:node (parse-string "(if-let [a bar] 100)")})
      :node
      api/sexpr
      )

  (-> (when-some {:node (parse-string "(when-some [a \"foo\" b bar] (+ a 100) 100)")})
      :node
      api/sexpr
      )
  (-> (when-let {:node (parse-string "(when-let [a \"foo\" b bar] (+ a 100) 100)")})
      :node
      api/sexpr)

  (def vnode1 (parse-string "(if-let [a foo b bar] x1 x2)"))
  (let [[a b c d] (-> vnode1 :children rest)]
    [a])
  (-> vnode1 :children rest first :children)

  (-> (cond* {:node (parse-string "(cond* :let [foo \"foo\"] (+ foo 2) 100)")})
      :node
      api/sexpr)

  (-> (when-let {:node (parse-string "(when-let [foo \"foo\"] (+ foo 2) 100)")})
      :node
      api/sexpr)

  (-> (when-some {:node (parse-string "(when-some [foo \"foo\"] (+ foo 2) 100)")})
      :node
      api/sexpr)

  (-> (cond* {:node (parse-string
"(cond*
    :when-let [foo \"foo\"
               bar 1]
    (> foo 1)
    100

    :when-some [bar \"foo\"]
    (> bar 3)
    200
    )")})
      :node
      api/sexpr)

  (-> (cond* {:node (parse-string
"
(cond*
    :let [store (db-session-store)]
    :do (is (nil? (store/read-session store \"s1\")))

    :let [session-id (store/write-session store nil session-data)]
    :do (is (= session-data (store/read-session store session-id)))

    :let [new-data (assoc session-data :foo :bar)]
    :do (store/write-session store session-id new-data)

    :let [new-data-back (store/read-session store session-id)]
    (do (is (= new-data-back new-data))
        (store/delete-session store session-id)
        (is (nil? (store/read-session store session-id)))))
")})
      :node
      api/sexpr)

  (-> (->node [(->splice [1 2])])
      (api/sexpr)
      )


  ())
