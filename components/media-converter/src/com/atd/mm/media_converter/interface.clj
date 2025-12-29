(ns com.atd.mm.media-converter.interface
  (:require
   [com.atd.mm.media-converter.core :as core]
   [com.atd.mm.media-converter.pipeline :as pipeline]))


(defn process-video
  [file & opts]
  (core/process-video file opts))

(defn prepare-pipeline
  [pipeline]
  (pipeline/prepare-pipeline pipeline))

(defn pipeline-valid?
  [pipeline]
  (pipeline/pipeline-valid? pipeline))