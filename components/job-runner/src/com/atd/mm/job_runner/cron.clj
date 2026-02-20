(ns com.atd.mm.job-runner.cron
  (:require
   [com.atd.mm.job-runner.producer :refer [create-producer]]
   [goose.api.cron-jobs :as cron-jobs]
   [goose.client :as c]))

(defn minutes->cron-schedule
  [minutes]
  (str "*/" minutes " * * * *"))

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

(defn get-cron-job
  [cron-name & {:keys [producer]}]
  (let [producer (or producer (create-producer))]
    (cron-jobs/find-by-name producer cron-name)))

(defn delete-cron-job
  [cron-name & {:keys [producer]}]
  (let [producer (or producer (create-producer))]
    (cron-jobs/delete producer cron-name)))

(defn my-cron-func
  [args]
  (tap> args))

(comment
  (get-cron-job "master")
  (delete-cron-job "master")
  (create-cron-job `my-cron-func {:cron-schedule (minutes->cron-schedule 1)
                                  :cron-name "master"
                                  :func-args {:hi "son"}})


  ;;Keep from folding
  )