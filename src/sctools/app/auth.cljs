(ns sctools.app.auth
  (:require [re-frame.core :as rf]
            [sctools.utils.rf-utils
             :as rfu
             :refer [db-sub quick-sub]]))

(db-sub :auth)

(rf/reg-sub
 :auth/authed
 :<- [:auth]
 (fn [auth]
   (:api-key auth)))
