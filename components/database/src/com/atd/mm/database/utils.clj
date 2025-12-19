(ns com.atd.mm.database.utils
  (:require [clojure.string :as str]
            [datomic.client.api :as d]))

(defn delete-db!
  [client db-name]
  (tap> "Deleting DB")
  (d/delete-database client {:db-name db-name}))

(defn tap-datoms-aevt!
  [conn]
  (let [db (d/db conn)]
    (tap> (d/datoms db {:index :aevt}))))

(defn tap-datoms-eavt!
  [conn]
  (let [db (d/db conn)]
    (tap> (d/datoms db {:index :eavt}))))

(defn get-schema
  [{:keys [excludes includes]} conn]
  (let [db (d/db conn)
        schema-query '{:eid 0 :selector [{:db.install/attribute [*]}]}
        results (-> (d/pull db schema-query) :db.install/attribute)

        filtered-results (if (seq includes)
                           (filter #(some (partial str/starts-with? (namespace (:db/ident %)))
                                          includes)
                                   results)
                           (remove #(some (partial str/starts-with? (namespace (:db/ident %)))
                                          excludes)
                                   results))]
    (map #(-> %
              (update :db/valueType :db/ident)
              (update :db/cardinality :db/ident))
         filtered-results)))

