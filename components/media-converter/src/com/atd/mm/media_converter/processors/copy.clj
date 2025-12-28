(ns com.atd.mm.media-converter.processors.copy
  (:require [com.atd.mm.media-converter.processors.core :refer [process-rule]]))

(defmethod process-rule :media/copy [{:keys [opts]}]
  (tap> opts))