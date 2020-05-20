(ns sctools.utils.rf-utils)

(defmacro defev
  "Define a regular function that is intended to be used in
  reg-event-db, but wrap this function so it return the db if the
  return value is nil."
  [bindings & body]
  `(let [f# (fn ~bindings
             ~@body)]
     (fn [~'db ~'spec]
       (let [new-db# (f# ~'db ~'spec)]
         (if (nil? new-db#)
           ~'db
           new-db#)))))
