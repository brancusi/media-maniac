(ns com.atd.mm.media-processor.store
  "Data access layer for media processing.

   Centralises all XTDB queries that processors need:
   fetching steps, resolving parent-job data, writing
   status transitions, and managing assets.
   Uses database/get-node so callers never have to thread
   an xtdb-node through."
  (:require
   [com.atd.mm.database.interface :as database]
   [xtdb.api :as xt]))

;; ===== Reads =====

(defn get-step
  "Fetch a pipeline step by ID.  Returns nil when not found."
  [step-id]
  (let [node (database/get-node)]
    (first (xt/q node
                 ['(fn [id]
                     (from :pipeline-steps [{:xt/id id} *]))
                  step-id]))))

(defn get-step-status
  "Return the current status keyword for a step."
  [step-id]
  (let [node (database/get-node)]
    (-> (xt/q node
              ['(fn [id]
                  (from :pipeline-steps [{:xt/id id} status]))
               step-id])
        first
        :status)))

(defn get-job-asset-id
  "Fetch the asset UUID from a parent pipeline job."
  [job-id]
  (let [node (database/get-node)]
    (-> (xt/q node
              ['(fn [id]
                  (from :pipeline-jobs [{:xt/id id} asset-id]))
               job-id])
        first
        :asset-id)))

;; ===== Writes =====

(defn mark-step-completed!
  "Set step to :completed with output data.
   result should be a map with :asset-ids vector."
  [step-id result]
  (let [node (database/get-node)]
    (xt/submit-tx node
                  [[:patch-docs :pipeline-steps
                    {:xt/id step-id :status :completed
                     :output result}]])))

(defn mark-step-failed!
  "Set step to :failed with error info."
  [step-id error]
  (let [node (database/get-node)]
    (xt/submit-tx node
                  [[:patch-docs :pipeline-steps
                    {:xt/id step-id :status :failed :error error}]])))

;; ===== Assets =====

(defn create-asset!
  "Persist a single asset document.  Returns the asset's :xt/id."
  [asset]
  (let [node (database/get-node)
        asset-id (or (:xt/id asset) (java.util.UUID/randomUUID))
        asset (assoc asset :xt/id asset-id)]
    (xt/submit-tx node [[:put-docs :assets asset]])
    asset-id))

(defn create-assets!
  "Persist multiple asset documents atomically.
   Assigns UUIDs where missing.  Returns vector of :xt/id values."
  [assets]
  (let [node (database/get-node)
        assets (mapv (fn [a]
                       (if (:xt/id a) a (assoc a :xt/id (java.util.UUID/randomUUID))))
                     assets)
        ids    (mapv :xt/id assets)]
    (xt/submit-tx node [(into [:put-docs :assets] assets)])
    ids))

(defn get-asset
  "Fetch a single asset by ID."
  [asset-id]
  (let [node (database/get-node)]
    (first (xt/q node
                 ['(fn [id]
                     (from :assets [{:xt/id id} *]))
                  asset-id]))))

(defn get-assets-by-ids
  "Fetch asset docs by a vector of UUIDs."
  [asset-ids]
  (let [node (database/get-node)]
    (xt/q node
          ['(fn [ids]
              (-> (from :assets [{:xt/id id} *])
                  (where (in id ids))))
           (vec asset-ids)])))

(defn get-assets-by-type
  "Filter a collection of assets by :asset/type keyword."
  [assets asset-type]
  (filterv #(= asset-type (:asset/type %)) assets))

(defn get-dep-asset-ids
  "Given a step, return a map of {dep-step-id [asset-uuid ...]}
   for all completed dependencies."
  [step]
  (let [node (database/get-node)]
    (->> (:deps step)
         (map (fn [dep-id]
                (let [output (-> (xt/q node
                                       ['(fn [id]
                                           (from :pipeline-steps [{:xt/id id} output]))
                                        dep-id])
                                 first :output)]
                  [dep-id (:asset-ids output)])))
         (into {}))))

(defn get-dep-assets
  "Given a step, fetch all artifact documents from its dependencies.
   Returns a flat vector of asset maps."
  [step]
  (let [dep-map     (get-dep-asset-ids step)
        all-ids     (vec (mapcat val dep-map))]
    (when (seq all-ids)
      (get-assets-by-ids all-ids))))

(defn find-assets-by-hash
  "Find assets with a matching imoHash.  Useful for dedup checks."
  [imohash]
  (let [node (database/get-node)]
    (xt/q node
          ['(fn [hash]
              (from :assets [{:asset/imohash hash} *]))
           imohash])))
