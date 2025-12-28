(ns com.atd.mm.media-converter.interface
  (:require
   [com.atd.mm.media-converter.core :as impl]))


(defn process-video
  [file & opts]
  (impl/process-video file opts))