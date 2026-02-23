(ns com.atd.mm.grand-central.model.pipeline
  "Legacy namespace — delegates to pipeline component.
   Prefer requiring com.atd.mm.pipeline.interface directly."
  (:require
   [com.atd.mm.grand-central.resolver :as resolver]
   [com.atd.mm.pipeline.interface :as pj]))

;; --- Deprecated wrappers (use pipeline.interface directly) ---

(defn pipeline-valid?
  [pipeline]
  (pj/job-valid? pipeline))

(defn create-pipeline
  [pipeline & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/create-job xtdb-node pipeline)))

(defn get-all-pipelines
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/get-all-jobs xtdb-node)))

(defn get-pipeline
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/get-job xtdb-node id)))

(defn delete-all-pipelines
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/delete-all-jobs xtdb-node)))

(defn delete-pipeline
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/delete-job xtdb-node id)))