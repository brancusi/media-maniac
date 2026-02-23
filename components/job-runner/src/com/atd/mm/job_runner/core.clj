(ns com.atd.mm.job-runner.core
  (:require
   [com.atd.mm.job-runner.producer :refer [create-producer]]
   [goose.api.enqueued-jobs :as enqueued-jobs]
   [goose.api.scheduled-jobs :as scheduled-jobs]
   [goose.client :as c]
   [goose.worker :as w]))

(defn queue-job
  [handler-function fn-args {:keys [producer queue retry-opts]}]
  (let [producer (or producer (create-producer))
        queue (or queue "default")
        client-opts (cond-> (assoc c/default-opts
                                   :broker producer
                                   :queue queue)
                      retry-opts (assoc :retry-opts retry-opts))]

    (c/perform-async client-opts handler-function fn-args)))

(defn clear-all-jobs
  [& {:keys [producer
             queue]}]
  (let [producer (or producer (create-producer))]
    (if queue
      (enqueued-jobs/purge producer queue)
      (doseq [q (enqueued-jobs/list-all-queues producer)]
        (enqueued-jobs/purge producer q)))))

(defn get-all-jobs
  [& {:keys [producer]}]
  (let [producer (or producer (create-producer))
        queues (enqueued-jobs/list-all-queues producer)]
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
                   (= (-> job :args first :id) tx-id))
        queued-results (enqueued-jobs/find-by-pattern producer queue match-fn)
        scheduled-results (scheduled-jobs/find-by-pattern producer match-fn)
        match (or (first (seq scheduled-results))
                  (first (seq queued-results)))]
    match))

(defn hey-son
  [& args]
  (tap> {:args args}))

(comment

  (enqueued-jobs/list-all-queues (create-producer))
  ;;=> ()

  (get-all-jobs)
  ;;=> ()


  (queue-job `hey-son
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