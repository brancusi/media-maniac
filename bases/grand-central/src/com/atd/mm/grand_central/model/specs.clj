(ns com.atd.mm.grand-central.model.specs
  "Re-exports schemas from pipeline component for backward compatibility.
   Prefer requiring com.atd.mm.pipeline.interface directly."
  (:require
   [com.atd.mm.pipeline.interface :as pipeline]))

;; --- Legacy aliases (prefer direct component requires) ---

(def StepDefinition pipeline/StepDefinition)
(def PipelineTemplate pipeline/PipelineTemplate)
(def PipelineStep pipeline/PipelineStep)
(def PipelineJob pipeline/PipelineJob)