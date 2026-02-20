(ns com.atd.mm.job-runner.worker
  (:require
   [com.atd.mm.job-runner.consumer :refer [create-consumer]]
   [goose.worker :as w]))

(defn create-worker
  [& {:as opts
      :keys [scheduler-polling-interval-sec
             conn-opts
             worker-opts]}]

  (let [redis-consumer (create-consumer {:scheduler-polling-interval-sec scheduler-polling-interval-sec
                                         :conn-opts conn-opts})

        worker-opts (assoc w/default-opts
                           :broker redis-consumer
                           :queue (:queue worker-opts)
                           :threads (:threads worker-opts)
                           :graceful-shutdown-sec (:graceful-shutdown-sec worker-opts))]
    (w/start worker-opts)))

(defn stop-worker
  [worker]
  (w/stop worker))