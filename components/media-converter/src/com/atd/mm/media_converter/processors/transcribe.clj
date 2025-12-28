(ns com.atd.mm.media-converter.processors.transcribe
  (:require [com.atd.mm.media-converter.processors.core :refer [process-rule]]))

(defmethod process-rule :media/transcribe [{:keys [opts]}]
  (tap> opts))