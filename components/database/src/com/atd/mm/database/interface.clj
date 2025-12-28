(ns com.atd.mm.database.interface
  (:require
   [donut.system :as ds]
   [com.atd.mm.database.core :as impl]))

(def system-config
  #::ds{:start (fn [{{:keys [node-config]} ::ds/config}]
                 (println "Starting xtdb node")
                 (impl/start-xtdb-node node-config))
        :stop (fn [{::ds/keys [instance]}]
                (println "Stopping xtdb node")
                (impl/stop-xtdb-node instance))
        :config {:node-config (ds/ref [:config :env :xtdb-config])}})