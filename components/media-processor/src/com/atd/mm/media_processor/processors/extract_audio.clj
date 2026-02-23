(ns com.atd.mm.media-processor.processors.extract-audio
  (:require [com.atd.mm.media-processor.processors.core :refer [execute-process]]))

(defmethod execute-process :media/extract-audio [{:keys [opts]}]
  (tap> opts))
