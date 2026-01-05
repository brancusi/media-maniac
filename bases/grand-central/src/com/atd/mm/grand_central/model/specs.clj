(ns com.atd.mm.grand-central.model.specs)

(def ProcessDefinition
  [:map
   [:xt/id :uuid]
   [:status [:enum :open :processing :completed]]
   [:opts {:optional true} :map]
   [:type [:enum :media/proxy
           :media/extract-audio
           :media/extract-stills
           :media/transcribe
           :media/copy]]
   [:deps {:optional true} [:vector :uuid]]])

(def Pipeline
  [:map
   [:xt/id :uuid]
   [:src :string]
   [:processes [:vector #'ProcessDefinition]]])