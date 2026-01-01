(ns com.atd.mm.job-runner.core
  (:require
   [goose.brokers.redis.broker :as rds]
   [goose.api.enqueued-jobs :as enqueued-jobs]
   [goose.api.scheduled-jobs :as scheduled-jobs]
   [goose.client :as c]
   [goose.worker :as w]))

(defn my-fn
  [args]
  (tap> args)
  #_(println "my-fn called with 5 seconds" arg1 arg2))

(defn create-producer
  [& opts]
  (let [opts (or opts rds/default-opts)]
    (rds/new-producer opts)))

(defn create-worker
  [producer & {:keys [threads queue]}]
  (let [client-opts (assoc w/default-opts
                           :broker producer
                           :threads (or threads 1)
                           :queue (or queue "default"))]
    (w/start client-opts)))

(defn stop-worker
  [worker]
  (w/stop worker))

(defn queue-job
  [handler-function fn-args {:keys [producer queue]}]
  (let [producer (or producer (create-producer))
        queue (or queue "default")
        client-opts (assoc c/default-opts
                           :broker producer
                           :queue queue)]

    (c/perform-async client-opts handler-function fn-args)))

(defn clear-all-jobs
  [& {:keys [producer
             queue]}]
  (let [producer (or producer (create-producer))]
    (if queue
      (enqueued-jobs/purge producer queue)
      (doseq [q (enqueued-jobs/list-all-queues producer)]
        (tap> q)
        (enqueued-jobs/purge producer q)))))

(defn job-by-tx-id
  [tx-id & {:keys [producer queue]}]
  (let [producer (or producer (create-producer))
        queue (or queue "default")
        match-fn (fn [job]
                   (tap> job)
                   (= (-> job :args first :id) tx-id))
        results (enqueued-jobs/find-by-pattern producer queue match-fn)]
    (first (seq results))))

(comment
  (queue-job 'my-fn
             {:queue "hey-son"}
             {:args {:id "hi"}})

  (queue-job 'my-fn
             {:queue "heavy-process"}
             {:id "asdfkjhsdf"})

  (job-by-tx-id "asdfkjhsdf" {:queue "heavy-process"})

  (clear-all-jobs)
  (clear-all-jobs {:queue "heavy-process"})
  (clear-all-jobs {:queue "hey-son"})

  ;;Keep from folding
  )

(def testind-uuid (java.util.UUID/randomUUID))
(def redis-producer (create-producer))

(enqueued-jobs/list-all-queues redis-producer)

(enqueued-jobs/size redis-producer "heavy-process")

(job-by-tx-id (java.util.UUID/randomUUID))


;; Get scheduled jobs
(scheduled-jobs/find-by-pattern redis-producer 10)


(let [#_#_rds-producer (rds/new-producer rds/default-opts)
      client-opts (assoc c/default-opts
                         :broker redis-producer
                         :queue :heavy-process)]

  (c/perform-async client-opts `my-fn {:id testind-uuid
                                       :something-else "yo"})

  #_(c/perform-in-sec client-opts 3 `my-fn "create-proxy" :with-args)
  #_(rds/close rds-producer))
;;=> ("heavy-process")

(comment

  w/default-opts
  ;;=> {:threads 5, :queue "default", :graceful-shutdown-sec 30}
  ;;Keep from folding
  )


#_(let [rmq-consumer (rmq/new-consumer rmq/default-opts)
        ;; Along with RabbitMQ, Goose supports Redis as well.
        worker-opts (assoc w/default-opts
                           :broker rmq-consumer
                           :threads 1
                           :queue :heavy-process)
        worker (w/start worker-opts)]
    ;; When shutting down worker...
    #_(w/stop worker) ; Performs a graceful shutsdown.
    #_(rmq/close rmq-consumer))