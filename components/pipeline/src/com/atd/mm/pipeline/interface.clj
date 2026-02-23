(ns com.atd.mm.pipeline.interface
  (:require
   [com.atd.mm.job-runner.interface :as job-runner]
   [com.atd.mm.pipeline.template :as template]
   [com.atd.mm.media-processor.interface :refer [process-media]]
   [com.atd.mm.pipeline.prepare :as prepare]
   [com.atd.mm.pipeline.job :as job]
   [com.atd.mm.pipeline.step :as step]
   [com.atd.mm.pipeline.handlers :as handlers]
   [com.atd.mm.pipeline.specs :as specs]
   [com.atd.mm.pipeline.asset :as asset]))

;; ===== Schemas =====

(def StepDefinition specs/StepDefinition)
(def PipelineTemplate specs/PipelineTemplate)
(def PipelineStep specs/PipelineStep)
(def PipelineJob specs/PipelineJob)
(def StepOutput specs/StepOutput)

;; ===== Template CRUD =====

(defn template-valid? [template] (template/template-valid? template))

(defn create-template
  "Store a pipeline template in XTDB."
  [xtdb-node template]
  (template/create-template xtdb-node template))

(defn get-template [xtdb-node id] (template/get-template xtdb-node id))
(defn get-all-templates [xtdb-node] (template/get-all-templates xtdb-node))
(defn delete-template [xtdb-node id] (template/delete-template xtdb-node id))
(defn delete-all-templates [xtdb-node] (template/delete-all-templates xtdb-node))

;; ===== Job preparation =====

(defn prepare-job
  "Transform a raw pipeline definition (string IDs) into an XTDB-ready
   PipelineJob with UUIDs and execution metadata."
  [pipeline-def]
  (prepare/prepare-job pipeline-def))

(defn create-job-from-template
  "Create a prepared PipelineJob from a stored template ID and asset UUID.
   Fetches the template from XTDB, converts its steps, and returns a
   job ready for `create-job`. Throws if template not found."
  [xtdb-node template-id asset-id]
  (prepare/create-job-from-template xtdb-node template-id asset-id))

;; ===== Job CRUD =====

(defn job-valid? [job] (job/job-valid? job))

(defn create-job
  "Persist a prepared pipeline job to XTDB."
  [xtdb-node job]
  (job/create-job xtdb-node job))

(defn get-job [xtdb-node id] (job/get-job xtdb-node id))
(defn get-all-jobs [xtdb-node] (job/get-all-jobs xtdb-node))
(defn delete-job [xtdb-node id] (job/delete-job xtdb-node id))
(defn delete-all-jobs [xtdb-node] (job/delete-all-jobs xtdb-node))

;; ===== Step CRUD =====

(defn get-step [xtdb-node id] (step/get-step xtdb-node id))
(defn get-all-steps [xtdb-node] (step/get-all-steps xtdb-node))

(defn update-step
  "Patch a single step by ID."
  [xtdb-node id patch]
  (step/update-step xtdb-node id patch))

(defn update-steps
  "Patch multiple steps."
  [xtdb-node steps]
  (step/update-steps xtdb-node steps))

(defn delete-step [xtdb-node id] (step/delete-step xtdb-node id))
(defn delete-all-steps [xtdb-node] (step/delete-all-steps xtdb-node))

;; ===== Dependency resolution =====

(defn get-ready-steps
  "Return all :open steps whose dependencies are all :completed."
  [xtdb-node]
  (step/get-ready-steps xtdb-node))

(defn step-completed? [step-record] (step/step-completed? step-record))
(defn all-step-deps-completed? [step-record] (step/all-step-deps-completed? step-record))

;; ===== Status lifecycle =====

(defn mark-step-processing!
  "Set a step's status to :processing."
  [xtdb-node id]
  (step/mark-step-processing! xtdb-node id))

(defn mark-step-completed!
  "Set a step's status to :completed and optionally store output data."
  [xtdb-node id & {:keys [output]}]
  (step/mark-step-completed! xtdb-node id :output output))

(defn mark-step-failed!
  "Set a step's status to :failed with error information."
  [xtdb-node id & {:keys [error]}]
  (step/mark-step-failed! xtdb-node id :error error))

;; ===== Orchestration =====

(defn process-open-steps!
  "Find all ready pipeline steps, mark them :processing, and queue
   each one to Goose for execution.  Only the step UUID is sent
   through Goose — the worker resolves all data from XTDB.

   Requires:
   - xtdb-node  — XTDB node for step queries and status updates
   - producer   — Goose Redis producer
   - opts       — optional map with :queue (default \"heavy-process\"),
                  :retry-opts (Goose retry config)"
  [xtdb-node producer & {:keys [queue retry-opts]}]
  (let [ready-steps (step/get-ready-steps xtdb-node)
        queue       (or queue "heavy-process")]
    (doseq [step ready-steps]
      (let [step-id (:xt/id step)
            opts (cond-> {:queue queue :producer producer}
                   retry-opts (assoc :retry-opts retry-opts))]

        (step/mark-step-processing! xtdb-node step-id)
        (job-runner/queue-job `process-media step-id opts)))

    {:queued (count ready-steps)
     :step-ids (mapv :xt/id ready-steps)}))

;; ===== Goose lifecycle handlers =====

(def on-step-death handlers/on-step-death)
(def on-step-error handlers/on-step-error)

;; ===== Asset specs & validation =====

(def VideoAsset asset/VideoAsset)
(def ImageAsset asset/ImageAsset)
(def AudioAsset asset/AudioAsset)
(def MetadataAsset asset/MetadataAsset)
(def TranscriptionAsset asset/TranscriptionAsset)
(def asset-specs asset/asset-specs)

(defn validate-asset
  "Validate an asset map against its :asset/type spec.
   Returns the asset if valid, throws on invalid or unknown type."
  [asset-map]
  (asset/validate-asset asset-map))

(defn asset-valid?
  "Returns true if the asset conforms to its type spec."
  [asset-map]
  (asset/asset-valid? asset-map))
