(ns sctools.foo
  (:require [bb-utils.clojure :refer [if-let cond* when-let when-some if-some]]))

(comment
  #_(if-let [a 1
           b 2]
    (+ a b)
    1)

  (if-let [a "foo"]
    (+ a 1)
    #_(+ b 2))

  #_(when-let [a "foo"]
    (+ a 0)
    (+ b 2))

  #_(when-let [a "foo"]
    (+ a 0)
    (+ b 1))

  #_(cond*
    :let [foo "foo"]
    (+ foo 2)
    100
    )

  #_(cond*
    :when-let [foo "foo"
               bar 1]
    (> foo 1)
    100

    :when-some [bar "foo"]
    (> bar 3)
    200
    )

  #_(let [foo "a"]
    (+ foo 2))


  ())
