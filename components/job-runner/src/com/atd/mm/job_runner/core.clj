(ns com.atd.mm.job-runner.core
  (:require
   [goose.brokers.redis.broker :as rds]
   [goose.api.enqueued-jobs :as enqueued-jobs]
   [goose.api.scheduled-jobs :as scheduled-jobs]
   [goose.client :as c]
   [goose.worker :as w]))

(defn job-by-tx-id
  [tx-id redis-producer]
  (let [results (enqueued-jobs/find-by-pattern redis-producer
                                               :heavy-process (fn [job]
                                                                (tap> job)
                                                                (= (-> job :args first :id) tx-id)))]
    (first (seq results))))

(defn my-fn
  [args]
  (tap> args)
  #_(println "my-fn called with 5 seconds" arg1 arg2))

(defn create-producer
  [& opts]
  (let [opts (or opts rds/default-opts)]
    (rds/new-producer opts)))

(defn queue-job
  [handler-function & {:keys [args producer queue]}]
  (let [producer (or producer (create-producer))
        queue (or queue "default")
        client-opts (assoc c/default-opts
                           :broker producer
                           :queue queue)]

    (tap> {:client-opts client-opts
           :prodecer producer
           :handler-function handler-function
           :args args})

    (c/perform-async client-opts handler-function args)))


(comment

  (queue-job 'my-fn {:args {:id "hi"}})
  ;;=> {:id "94267259-54d7-4aa8-bfba-5d149b005c1f"}
  ;;=> {:id "52fdc881-b068-44e8-b6e4-25c4f5a7b9f5"}
  ;;=> {:id "7ea6145e-795f-4195-8f22-930bc797f4f9"}
  ;;=> {:id "c9b4d5ba-ff9b-440d-8af7-fc38e1475170"}
  ;;=> {:id "6584ff60-b490-4fb6-8ca3-4aa8b2d38d6b"}
  ;;=> {:id "557e30c7-659d-494e-b95e-cdf16c34af55"}
  ;;=> {:id "8dd2ad4d-a1b7-44ef-9786-d15a7a9f594f"}
  ;;=> {:id "0aecef0f-926c-4579-bb75-3a4c6af8eccd"}
  ;;=> {:id "bc877068-8a7a-411e-98ec-6e2439f71585"}
  ;;=> {:id "9ec50c9c-a881-4ab9-9859-9160d18c4ba0"}

  ;;Keep from folding
  )


(def testind-uuid (java.util.UUID/randomUUID))

(def redis-producer (rds/new-producer rds/default-opts))

(job-by-tx-id testind-uuid redis-producer)
;;=> nil


(enqueued-jobs/list-all-queues redis-producer)

(enqueued-jobs/size redis-producer :heavy-process)



(defn job-by-tx-id
  [tx-id]
  (let [results (enqueued-jobs/find-by-pattern redis-producer
                                               :heavy-process #(= (-> % :args first :id) tx-id))]
    (first (seq results))))

(job-by-tx-id (java.util.UUID/randomUUID))

(enqueued-jobs/purge redis-producer :heavy-process)
;;=> Execution error (ArityException) at com.atd.mm.job-runner.interface/eval66181 (interface.clj:74).
;;   Wrong number of args (2) passed to: goose.api.enqueued-jobs/find-by-pattern
;;   

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