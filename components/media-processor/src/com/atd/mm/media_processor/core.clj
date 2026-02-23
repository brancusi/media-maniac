(ns com.atd.mm.media-processor.core
  "Entry point for media processing dispatch.
   FFmpeg operations live in ffmpeg.clj; multimethod processors live
   in processors/*.clj."
  (:require
   [com.atd.mm.media-processor.ffmpeg :as ffmpeg]
   [com.atd.mm.media-processor.processors.core :refer [execute-process]]
   [com.atd.mm.media-processor.processors.interface]
   [com.atd.mm.media-processor.store :as store]))

;; Re-export FFmpeg functions so existing callers via core still work
(def generate-proxy ffmpeg/generate-proxy)
(def get-video-duration ffmpeg/get-video-duration)
(def extract-frames-from-video ffmpeg/extract-frames-from-video)
(def generate-proxy-with-frame-extraction ffmpeg/generate-proxy-with-frame-extraction)

(defn process-media
  "Goose entry point.  Receives a single step-id (UUID), fetches the
   step from XTDB, then dispatches to the appropriate processor
   multimethod.  Each processor is responsible for resolving any
   additional data it needs (e.g. parent job :src) via the store."
  [step-id]
  (let [step (store/get-step step-id)]
    (when-not step
      (throw (ex-info "Step not found" {:step-id step-id})))
    (execute-process step)))
