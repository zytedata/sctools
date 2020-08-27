(ns sctools.datascript
  (:require [datascript.core :as db]
            [cljs.pprint]
            [clojure.string :as str]))

#_(def movie-schema [{:db/ident :movie/title
                    :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one
                    :db/doc "The title of the movie"}

                   {:db/ident :movie/genre
                    :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one
                    :db/doc "The genre of the movie"}

                   {:db/ident :movie/release-year
                    :db/valueType :db.type/long
                    :db/cardinality :db.cardinality/one
                    :db/doc "The year the movie was released in theaters"}])
#_(def schema {:aka {:db/cardinality :db.cardinality/many}})
#_(def conn (db/create-conn schema))

(def db-schema
  {:repo/owner {:db/valueType :db.type/ref}
   :repo/lang {:db/cardinality :db.cardinality/many}
   :book/color {:db/valueType :db.type/ref}
   :settings/key {:db/unique :db.unique/identity}
   })
(def conn (db/create-conn db-schema))
#_(def conn (db/create-conn {}))

(def github-users [{:db/id 11
                    :user/name "richhikey"
                    :user/email "rich@e.com"}
                   {:db/id 22
                    :user/name "tonsky"
                    :user/email "tonsky@f.com"}
                   {:db/id 33
                    :user/name "pitchyless"
                    :user/email "pitchyless@g.com"}])

(db/transact! conn github-users)

;; inspect the EAVT index
(->> (db/datoms @conn :eavt)
     (take 10)
     #_(take-last 10)
     (map #(zipmap [:e :v :a :t] %))
     cljs.pprint/print-table)

;; select email from user where name = "richhikey"
(db/q '[:find [?uid ?email]
        :where
        [?uid :user/name "richhikey"]
        [?uid :user/email ?email]]
      @conn)

;; use the input, scalar binding
(db/q '[:find [?uid ?email]
        :in $ ?user
        :where
        [?uid :user/name ?user]
        [?uid :user/email ?email]]
      @conn "richhikey")

;; use the input, tuple binding
(db/q '[:find [?uid ?email]
        :in $ [?user]
        :where
        [?uid :user/name ?user]
        [?uid :user/email ?email]]
      @conn ["richhikey"])

;; use the input, collection binding
(db/q '[:find ?u ?email
        :in $ [?user ...]
        :where
        [?u :user/name ?user]
        [?u :user/email ?email]]
      @conn ["tonsky" "richhikey"])

;; select count(*)
(db/q '[:find (count ?e)
        :where
        [?e _ _]]
      @conn)

;; filter with clojure code
(db/q '[:find ?user
        :where
        [_ :user/name ?user]
        [(clojure.string/starts-with? ?user "rich")]]
      @conn)

(def github-orgs [{:db/id 44
                   :org/name "clojure"}])

(db/transact! conn github-orgs)

(db/q '[:find ?org
        :where
        [_ :org/name ?org]]
      @conn)

(def github-repos [{:db/id 54
                    :repo/slug "clojure/data.json"
                    :repo/owner 44}
                   {:db/id 55
                    :repo/slug "clojure/clojure"
                    :repo/owner 44}
                   {:db/id 66
                    :repo/slug "tonsky/datascript"
                    :repo/owner 22}
                   {:db/id 166
                    :repo/slug "tonsky/blog"
                    :repo/owner 22}])

(db/transact! conn github-repos)

(db/q '[:find ?repo
        :where
        [?u :user/name "tonsky"]
        [?r :repo/owner ?u]
        [?r :repo/slug ?repo]]
      @conn)

(db/q '[:find ?name (count ?repo)
        :where
        (or [?p :user/name ?name]
            [?p :org/name ?name])
        [?r :repo/owner ?p]
        [?r :repo/slug ?repo]]
      @conn)

(db/q '[:find (pull ?u [:org/name :user/name]) (count ?r)
        :where
        [?r :repo/owner ?u]]
      @conn)

(db/q '[:find (sum ?u)
        :where
        [?r :repo/owner ?u]]
      @conn)

(def repo-langs [{:db/id 55
                  :repo/lang :java}
                 {:db/id 55
                  :repo/lang :clojure}
                 {:db/id 66
                  :repo/lang :clojure}
                 {:db/id 66
                  :repo/lang :clojurescript}])

(db/transact! conn repo-langs)

(db/q '[:find ?repo ?lang
        :where
        #_[?r :repo/lang [:java :clojure]]
        [?r :repo/lang ?lang]
        [?r :repo/owner 22]
        [?r :repo/slug ?repo]]
      @conn)

#_(defn repo-owner [?p ?name]
  (or [?p :user/name ?name]
      [?p :org/name ?name]))

#_(db/q '[:find ?p (count ?repo)
        :where
        [(sctools.datascript/repo-owner ?p ?name)]
        [?r :repo/owner ?p]
        [?r :repo/slug ?repo]]
      @conn)

(db/pull @conn '[:user/email] 11)
(-> (db/entity @conn 11) :user/email)
(db/touch (db/entity @conn 11))
(db/touch (db/entity @conn 55))

(db/q '[:find [?lang ...]
        :where
        [_ :repo/lang ?lang]]
      @conn)

;; pull query
(def pull-query
  '[:repo/slug
   {:repo/owner
    [:user/name
     :org/name
     {:repo/_owner
      [:repo/slug
       (:repo/lang :limit 1)]}
     ]}])

(db/q `[~:find (~'pull ~'?e ~pull-query)
        ~:where
        ~'[?e :repo/slug "tonsky/datascript"]]
      @conn)

(db/q '[:find (pull ?e pattern)
       :in $ pattern
       :where
       [?e :repo/slug "tonsky/datascript"]]
      @conn pull-query)

;; use pull api directly
(db/pull @conn [:repo/slug {:repo/owner [:org/name]}] 55)
(db/pull-many @conn [:repo/slug
                     {:repo/owner
                      [:user/name
                       :org/name]}] [55 66])

;; reverse lookup
(db/pull @conn '[:repo/slug
                 {:repo/_owner [:user/name :org/name]}] 55)

(db/pull @conn pull-query 66)

#_(db/transact!
 conn
 {:foo {:v1 :a1
        :v2 :a2}})

(db/transact!
 conn
 [{:settings/key :theme
   :settings/value :dark
   :settings/value2 :dark2}])

(db/transact!
 conn
 [{:settings/key :theme
   :settings/value :light}])

(def settings-id (db/q '[:find ?id .
                         :where
                         [?id :settings/key :theme]]
                       @conn))

(defn load-settings [k]
  (db/q '[:find ?v .
          :in $ ?k
          :where
          [?s :settings/key ?]
          [?s :settings/value ?v]]
        @conn k))

(load-settings :theme)

(db/transact!
 conn
 [[:db/add "settings/theme" :settings/key :theme]
  [:db/add "settings/theme" :settings/value :light]])

#_(db/transact!
 conn
 [[:db/retract settings-id :settings/value]])


#_(db/transact!
 conn
 [[:db/retractEntity settings-id]])

(load-settings :theme)

(->> (db/datoms @conn :eavt)
     #_(take 10)
     (take-last 8)
     (map #(zipmap [:e :v :a :t] %))
     cljs.pprint/print-table)


(db/transact!
 conn
 [[:db/add -1 :db/ident :yellow]
  [:db/add -2 :db/ident :green]])

(db/transact!
 conn
 [[:db/add "book1" :book/color :yellow]
  [:db/add "book1" :book/name "China is not Happy!"]])


(db/touch (db/entity @conn :yellow))
