(ns com.atd.mm.job-runner.interface
  (:require
   [com.atd.mm.job-runner.core :as core]))


(defn job-by-tx-id
  [tx-id]
  (core/job-by-tx-id tx-id))


