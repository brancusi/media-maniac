(ns com.atd.mm.job-runner.interface
  (:require
   [com.atd.mm.job-runner.core :as core]
   [com.atd.mm.job-runner.cron :as cron]
   [com.atd.mm.job-runner.worker :as worker]
   [com.atd.mm.job-runner.producer :as producer]
   [com.atd.mm.job-runner.consumer :as consumer]
   [donut.system :as ds]))

(defn job-by-tx-id
  [tx-id & {:as opts}]
  (core/job-by-tx-id tx-id opts))

(defn create-producer
  [& {:keys [url pool-opts]
      :as conn-opts}]
  (producer/create-producer conn-opts))

(defn create-consumer
  [& {:keys [conn-opts scheduler-polling-interval-sec]
      :as opts}]
  (consumer/create-consumer opts))

(defn create-worker
  [& {:as opts
      :keys [scheduler-polling-interval-sec
             conn-opts
             worker-opts]}]
  (worker/create-worker opts))

(defn stop-worker
  [worker]
  (worker/stop-worker worker))

(defn queue-job
  [handler-function fn-args opts]
  (core/queue-job handler-function fn-args opts))

(defn create-cron-job
  [func & {:as opts}]
  (cron/create-cron-job func opts))

(defn clear-all-jobs
  [& {:as opts}]
  (core/clear-all-jobs opts))

(defn get-all-jobs
  [& {:as opts}]
  (core/get-all-jobs opts))

(def system-config
  #::ds{:start (fn [{{:keys [job-runner xtdb-node]} ::ds/config}]
                 (let [redis-config (:redis job-runner)
                       producer (create-producer redis-config)
                       error-svc (when xtdb-node {:xtdb-node xtdb-node})]
                   (println "Starting Redis related")
                   {:producer producer
                    :workers (mapv (fn [{:keys [scheduler-polling-interval-sec
                                                worker-opts
                                                conn-opts]}]
                                     (create-worker {:conn-opts redis-config
                                                     :scheduler-polling-interval-sec scheduler-polling-interval-sec
                                                     :worker-opts worker-opts
                                                     :error-service-config error-svc}))
                                   (:workers job-runner))}))
        :stop (fn [{::ds/keys [instance]}]
                (println "Stopping Redis related")
                (println "Stopping workers...")

                (doseq [worker (:workers instance)]
                  (stop-worker worker))

                (println "Stopped all workers"))
        :config {:job-runner (ds/ref [:config :env :job-runner])
                 :xtdb-node  (ds/ref [:database :node])}})
