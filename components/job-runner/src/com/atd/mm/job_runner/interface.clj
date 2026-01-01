(ns com.atd.mm.job-runner.interface
  (:require
   [com.atd.mm.job-runner.core :as core]
   [donut.system :as ds]))

(defn job-by-tx-id
  [tx-id & opts]
  (core/job-by-tx-id tx-id opts))

(defn create-producer
  [& opts]
  (apply core/create-producer opts))

(defn create-worker
  [producer & args]
  (apply core/create-worker producer args))

(defn stop-worker
  [worker]
  (core/stop-worker worker))

(defn queue-job
  [handler-function fn-args opts]
  (core/queue-job handler-function fn-args opts))

(defn clear-all-jobs
  [& opts]
  (apply core/clear-all-jobs opts))

(comment

  (clear-all-jobs)

  ;;Keep from folding
  )

(def system-config
  #::ds{:start (fn [{{:keys [job-runner]} ::ds/config}]
                 (let [redis-config (:redis job-runner)
                       producer (create-producer redis-config)]

                   (println "Starting Redis related")

                   {:producer producer
                    :workers (mapv (fn [worker-config]
                                     (create-worker producer worker-config))
                                   (:workers job-runner))}))
        :stop (fn [{::ds/keys [instance]}]
                (println "Stopping Redis related")
                (println "Stopping workers...")

                (doseq [worker (:workers instance)]
                  (stop-worker worker))

                (println "Stopped all workers"))
        :config {:job-runner (ds/ref [:config :env :job-runner])}})
