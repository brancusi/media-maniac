(ns com.atd.mm.job-runner.worker
  (:require
   [com.atd.mm.job-runner.consumer :refer [create-consumer]]
   [goose.worker :as w]))

(defn create-worker
  [& {:as opts
      :keys [scheduler-polling-interval-sec
             conn-opts
             worker-opts
             error-service-config]}]

  (let [redis-consumer (create-consumer {:scheduler-polling-interval-sec scheduler-polling-interval-sec
                                         :conn-opts conn-opts})

        worker-opts (cond-> (assoc w/default-opts
                                   :broker redis-consumer
                                   :queue (:queue worker-opts)
                                   :threads (:threads worker-opts)
                                   :graceful-shutdown-sec (:graceful-shutdown-sec worker-opts))
                      error-service-config (assoc :error-service-config error-service-config))]
    (w/start worker-opts)))

(defn stop-worker
  [worker]
  (w/stop worker))