(ns com.atd.mm.grand-central.model.process
  "Legacy namespace — delegates to pipeline component.
   Prefer requiring com.atd.mm.pipeline.interface directly."
  (:require
   [com.atd.mm.grand-central.resolver :as resolver]
   [com.atd.mm.pipeline.interface :as pj]))

;; --- Deprecated wrappers (use pipeline.interface directly) ---

(defn process-completed? [process] (pj/step-completed? process))
(defn all-process-deps-completed? [process] (pj/all-step-deps-completed? process))

(defn update-process
  [id patch & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/update-step xtdb-node id patch)))

(defn update-processes
  [processes & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/update-steps xtdb-node processes)))

(defn get-all-processes
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/get-all-steps xtdb-node)))

(defn get-process
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/get-step xtdb-node id)))

(defn get-ready-to-process
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/get-ready-steps xtdb-node)))

(defn delete-all-processes
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/delete-all-steps xtdb-node)))

(defn delete-process
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (pj/delete-step xtdb-node id)))