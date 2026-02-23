(ns com.atd.mm.media-processor.processors.copy
  (:require [com.atd.mm.media-processor.processors.core :refer [execute-process]]))

(defmethod execute-process :media/copy [{:keys [opts]}]
  (tap> opts))
