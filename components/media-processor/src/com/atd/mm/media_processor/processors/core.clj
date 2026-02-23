(ns com.atd.mm.media-processor.processors.core)

(defmulti execute-process
  "Dispatch media processing by step :processor.
   Receives a step map.  Processors resolve any additional data
   they need (e.g. parent job :src) via the store namespace."
  :processor)
