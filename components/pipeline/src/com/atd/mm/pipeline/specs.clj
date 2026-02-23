(ns com.atd.mm.pipeline.specs)

;; ---- Step output schema ----

(def StepOutput
  "Output stored on a completed step — just asset references."
  [:map
   [:asset-ids [:vector :uuid]]])

;; ---- Template schemas (blueprint definitions) ----

(def StepDefinition
  "Schema for a step within a pipeline template.
   Uses string IDs — UUIDs are generated when instantiating a PipelineJob."
  [:map
   [:id :string]
   [:processor [:enum :media/proxy
                :media/extract-audio
                :media/extract-stills
                :media/transcribe
                :media/copy]]
   [:opts {:optional true} :map]
   [:deps {:optional true} [:vector :string]]])

(def PipelineTemplate
  "A reusable pipeline blueprint. Defines a set of steps with their
   types, options, and dependency graph using string IDs."
  [:map
   [:xt/id {:optional true} :uuid]
   [:name :string]
   [:description {:optional true} :string]
   [:steps [:vector StepDefinition]]])

;; ---- Job schemas (concrete execution instances) ----

(def PipelineStep
  "A concrete step within a pipeline job execution.
   Has a UUID :xt/id (generated during preparation) and tracks execution status."
  [:map
   [:xt/id :uuid]
   [:pipeline-job-id :uuid]
   [:status [:enum :open :processing :completed :failed]]
   [:processor [:enum :media/proxy
                :media/extract-audio
                :media/extract-stills
                :media/transcribe
                :media/copy]]
   [:opts {:optional true} :map]
   [:deps {:optional true} [:vector :uuid]]
   [:output {:optional true} StepOutput]])

(def PipelineJob
  "A concrete pipeline execution instance — created from a template
   bound to a specific asset."
  [:map
   [:xt/id :uuid]
   [:asset-id :uuid]
   [:template-id {:optional true} :uuid]
   [:steps {:optional true} [:vector #'PipelineStep]]])
