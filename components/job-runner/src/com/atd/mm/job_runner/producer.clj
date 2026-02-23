(ns com.atd.mm.job-runner.producer
  (:require
   [goose.brokers.redis.broker :as rds]))

(defn create-producer
  [& {:keys [url pool-opts] :as _conn-opts}]
  (let [url (or url (:url rds/default-opts))
        pool-opts (or pool-opts (:pool-opts rds/default-opts))
        conn-opts {:url url :pool-opts pool-opts}]
    (rds/new-producer conn-opts)))

(comment



  ;;Keep from folding
  )