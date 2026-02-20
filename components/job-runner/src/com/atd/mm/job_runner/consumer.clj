(ns com.atd.mm.job-runner.consumer
  (:require
   [goose.brokers.redis.broker :as rds]))

(defn create-consumer
  [& {:keys [conn-opts scheduler-polling-interval-sec]}]
  (let [scheduler-polling-interval-sec (or scheduler-polling-interval-sec 15)
        conn-opts (or conn-opts rds/default-opts)]
    (rds/new-consumer conn-opts scheduler-polling-interval-sec)))