(ns com.atd.mm.media-processor.processors.transcribe
  (:require [com.atd.mm.media-processor.processors.core :refer [execute-process]]))

(defmethod execute-process :media/transcribe [{:keys [opts]}]
  (tap> opts))
