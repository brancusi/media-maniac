(ns com.atd.mm.database.interface
  (:require [datomic.client.api :as d]
            [donut.system :as ds]))

(def connection-config
  #::ds{:start (fn [{{:keys [client]} ::ds/config}]
                 (let [db-name "media-maniac-db"
                       _ (d/create-database client {:db-name db-name})
                       conn (d/connect client {:db-name db-name})
                       _ (tap> "Created conn")]

                   ;;    Return the connection
                   conn))

        :config {:client (ds/local-ref [:datomic-client])}})

(def client-config
  #::ds{:start (fn [_]
                 (d/client {:server-type :datomic-local
                            :storage-dir "/tmp/yoson"
                            :system "dev"}))})

(comment

  (def client (d/client {:server-type :ion
                         :region "us-west-2"
                         :system "jj-dev"
                         :endpoint "https://hxpsu55070.execute-api.us-west-2.amazonaws.com/"}))
  ;; => #'com.atd.mm.datomic-client.interface/client


  (def conn (d/connect client {:db-name "jj-dev"}))


  (d/create-database client {:db-name "jj-dev"})
  ;; => true



  ;; => #'com.atd.mm.datomic-client.interface/conn

  ;; => Execution error (ExceptionInfo) at datomic.client.api.async/ares (async.clj:58).
  ;;    Unable to find keyfile at s3://jj-dev-s3datomic-e2mfshnakmyk/jj-dev/datomic/access/dbs/db/jj-dev/read/.keys. Make sure that your endpoint and db-name are correct.

  ;; => Execution error (ExceptionInfo) at datomic.client.impl.shared.Client/connect (shared.clj:421).
  ;;    Expected a map




  ;;Keep from folding
  )