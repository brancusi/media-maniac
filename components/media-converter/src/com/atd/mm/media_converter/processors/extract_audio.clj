(ns com.atd.mm.media-converter.processors.extract-audio
  (:require [com.atd.mm.media-converter.processors.core :refer [process-rule]]))

(defmethod process-rule :media/extract-audio [{:keys [opts]}]
  (tap> opts))