(ns system
  (:require [clojure.pprint]

            [com.atd.mm.database.interface :as db]

            ;; Donut System requires
            [donut.system :as ds]
            [donut.system.repl :as dsr]
            [donut.system.repl.state :as dsr-state]

            [logging :as logging]
            [datomic.client.api :as d]


            [com.atd.mm.database.utils :as db-utils]))

(def system-config
  {::ds/defs
   {:persistance {:datomic-client db/client-config
                  :datomic-connection db/connection-config}
    :debugging {:system-portal (ds/cache-component (logging/system-portal-config "System viewer"))
                #_#_:log-portal (logging/portal-config "Logging viewer")}}})

(defmethod ds/named-system :donut.system/repl
  [_]
  system-config)

(defn stop
  []
  (println "Stopping system")
  (dsr/stop))

(defn start
  []
  (println "Starting system")
  (dsr/start)

  (.addShutdownHook (Runtime/getRuntime) (Thread. stop)))

(defn restart
  []
  (stop)
  (start))

(defn refresh-and-restart
  []
  (dsr/restart))

(defn start-dev
  []
  ;; Start donut system
  (start))

(defn get-system-portal
  []
  (dsr/instance [:debugging :system-portal]))

(defn get-db-connection
  []
  (dsr/instance [:persistance :datomic-connection]))

(defn get-db-client
  []
  (dsr/instance [:persistance :datomic-client]))

(defn reset-db!
  []
  (db-utils/delete-db! (get-db-client) "media-maniac-db")
  (dsr/restart))




(comment
  ;; Start dev. This will init the integrant system referenced above
  (start-dev)

  (stop)

  (get-db-connection)

  (db-utils/get-schema {} (get-db-connection))

  (get-db-client)

  (dsr/restart)
  ;;=> :donut.system/start
  ;;=> :donut.system/start

  (db-utils/delete-db! (get-db-client) "media-maniac-db")
  ;;=> true

  (refresh-and-restart)

  ;;   

  (d/transact (get-db-connection) {:tx-data [{:db/ident       :media/hash
                                              :db/valueType   :db.type/string
                                              :db/cardinality :db.cardinality/one
                                              :db/unique      :db.unique/identity
                                              :db/doc         "Media Hash"}

                                             {:db/ident       :media/copy
                                              :db/valueType   :db.type/string
                                              :db/cardinality :db.cardinality/many
                                              :db/doc         "A copy of this media file"}]})

  (d/transact (get-db-connection) {:tx-data [{:media/hash "abc123"}]})

  (d/q '[:find ?e ?hash
         :where
         [?e :media/hash ?hash]]
       (d/db (get-db-connection)))


  (reset-db!)

  ;;Keep from folding
  )