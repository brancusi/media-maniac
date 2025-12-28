(ns com.atd.mm.media-converter.processors.proxy
  (:require [com.atd.mm.media-converter.processors.core :refer [process-rule]]))

(defmethod process-rule :media/proxy [{:keys [opts]}]
  (tap> opts))