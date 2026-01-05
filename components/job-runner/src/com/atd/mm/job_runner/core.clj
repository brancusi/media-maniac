(ns com.atd.mm.job-runner.core
  (:require
   [goose.brokers.redis.broker :as rds]
   [goose.api.enqueued-jobs :as enqueued-jobs]
   [goose.api.scheduled-jobs :as scheduled-jobs]
   [goose.client :as c]
   [goose.worker :as w]))

(defn my-fn
  [args]
  (Thread/sleep 10000)
  (tap> {:fn "my-fn"
         :args args})
  #_(println "my-fn called with 5 seconds" arg1 arg2))

(defn create-producer
  [& opts]
  (let [opts (or (first opts) rds/default-opts)]
    (tap> {:creating-producer opts})
    (rds/new-producer opts)))

(defn create-consumer
  [& opts]

  (let [opts (or (first opts) rds/default-opts)]
    (tap> {:creating-consumer rds/default-opts})
    (rds/new-consumer rds/default-opts)))

(defn create-worker
  [& {:as opts
      :keys [threads queue]}]
  (let [redis-consumer (create-consumer)

        worker-opts (assoc w/default-opts
                           :broker redis-consumer
                           :threads (or threads 1)
                           :queue (or queue "default"))]
    (w/start worker-opts)))

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

    (tap> {:queuing-job handler-function
           :with-args fn-args
           :to-queue queue})

    (c/perform-async client-opts handler-function fn-args)))

(defn create-cron-job
  [func & {:keys [func-args producer cron-name cron-schedule timezone]}]
  (let [producer (or producer (create-producer))
        client-opts (assoc c/default-opts
                           :broker producer)
        cron-name (or cron-name (str (java.util.UUID/randomUUID)))
        timezone (or timezone "US/Pacific")
        cron-schedule (or cron-schedule "*/1 * * * *")
        cron-opts {:cron-name     cron-name
                   :cron-schedule cron-schedule
                   :timezone      timezone}]

    (c/perform-every client-opts cron-opts func func-args)))

(defn my-cron-func
  [args]
  (tap> {:func "my-cron-func"
         :args args}))

(comment


  (create-cron-job 'com.atd.mm.job-runner.core/my-cron-func {:func-args {:hi "son"}})
  ;;=> {:cron-name "257525ba-83de-4eed-b3cf-5d8eedd96a75", :cron-schedule "*/1 * * * *", :timezone "US/Pacific"}


  ;;Keep from folding
  )

(defn clear-all-jobs
  [& {:keys [producer
             queue]}]
  (let [producer (or producer (create-producer))]
    (if queue
      (enqueued-jobs/purge producer queue)
      (doseq [q (enqueued-jobs/list-all-queues producer)]
        (tap> q)
        (enqueued-jobs/purge producer q)))))

(defn get-all-jobs
  [& {:keys [producer]}]
  (let [producer (or producer (create-producer))
        queues (enqueued-jobs/list-all-queues producer)]
    (tap> "Getting all jobs in all queues:")
    (mapcat
     (fn [q]
       (tap> q)
       (enqueued-jobs/find-by-pattern producer q (fn [_] true)))
     queues)))

(defn job-by-tx-id
  [tx-id & {:keys [producer queue]}]
  (let [producer (or producer (create-producer))
        queue (or queue "default")
        match-fn (fn [job]
                   (tap> job)
                   (= (-> job :args first :id) tx-id))
        queued-results (enqueued-jobs/find-by-pattern producer queue match-fn)
        scheduled-results (scheduled-jobs/find-by-pattern producer match-fn)
        match (or (first (seq scheduled-results))
                  (first (seq queued-results)))]
    match))

(comment

  (enqueued-jobs/list-all-queues (create-producer))

  (get-all-jobs)

  (queue-job 'my-fn
             {:args {:id "hi"}}
             {:queue "default"})

  (let [uuid "id-234567"
        queue "heavy-process1"
        already-queued? (job-by-tx-id uuid {:queue queue})]
    (if already-queued?
      (tap> {:job-exists uuid})
      (queue-job 'com.atd.mm.job-runner.core/my-fn
                 {:id uuid}
                 {:queue queue})))

  (queue-job 'com.atd.mm.job-runner.core/my-fn
             {:id "id-23456"}
             {:queue "heavy-process"})

  (job-by-tx-id "id-23456" {:queue "heavy-process"})

  (clear-all-jobs)
  (clear-all-jobs {:queue "heavy-process"})
  (clear-all-jobs {:queue "hey-son"})



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

  ;;Keep from folding
  )


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