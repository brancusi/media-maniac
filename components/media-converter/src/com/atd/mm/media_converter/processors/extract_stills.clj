(ns com.atd.mm.media-converter.processors.extract-stills
  (:require [com.atd.mm.media-converter.processors.core :refer [process-rule]]))

(defmethod process-rule :media/extract-stills [{:keys [opts]}]
  (tap> opts))