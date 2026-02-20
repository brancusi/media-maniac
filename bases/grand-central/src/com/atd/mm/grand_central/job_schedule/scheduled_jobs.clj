(ns com.atd.mm.grand-central.job-schedule.scheduled-jobs
  (:require [donut.system :as ds]))

(def system-config
  #::ds{:start (fn [{{:keys [node-config]} ::ds/config}]
                 (println "Starting xtdb node")
                 #_(impl/start-xtdb-node node-config))
        :stop (fn [{::ds/keys [instance]}]
                (println "Stopping xtdb node")
                #_(impl/stop-xtdb-node instance))
        :config {:node-config (ds/ref [:config :env :xtdb-config])}})