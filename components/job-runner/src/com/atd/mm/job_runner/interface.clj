(ns com.atd.mm.job-runner.interface
  (:require
   [goose.brokers.rmq.broker :as rmq]
   [goose.client :as c]
   [goose.worker :as w]))


(defn my-fn
  [arg1 arg2]
  (Thread/sleep 200)
  (tap> {:called-with [arg1 arg2]})
  #_(println "my-fn called with 5 seconds" arg1 arg2))

(let [rmq-producer (rmq/new-producer rmq/default-opts)

      client-opts (assoc c/default-opts :broker rmq-producer)]

  (c/perform-async client-opts `my-fn "create-proxy" :with-args)
  #_(c/perform-in-sec client-opts 900 `my-fn "create-proxy" :with-args)
  (rmq/close rmq-producer))

(let [rmq-consumer (rmq/new-consumer rmq/default-opts)
      ;; Along with RabbitMQ, Goose supports Redis as well.
      worker-opts (assoc w/default-opts :broker rmq-consumer)
      worker (w/start worker-opts)]
  ;; When shutting down worker...
  #_(w/stop worker) ; Performs a graceful shutsdown.
  #_(rmq/close rmq-consumer))