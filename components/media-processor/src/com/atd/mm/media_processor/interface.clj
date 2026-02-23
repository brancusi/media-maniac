(ns com.atd.mm.media-processor.interface
  (:require
   [com.atd.mm.media-processor.core :as core]
   [com.atd.mm.media-processor.ffmpeg :as ffmpeg]))

;; --- Single-step process execution ---

(defn process-media
  "Goose entry point.  Receives a step-id (UUID), resolves the step
   and its parent job from XTDB, then dispatches to the processor."
  [step-id]
  (core/process-media step-id))

;; --- Direct FFmpeg operations ---

(defn generate-proxy
  [input-file & opts]
  (apply ffmpeg/generate-proxy input-file opts))

(defn extract-frames-from-video
  [video-file & opts]
  (apply ffmpeg/extract-frames-from-video video-file opts))

(defn generate-proxy-with-frame-extraction
  [input-file & opts]
  (apply ffmpeg/generate-proxy-with-frame-extraction input-file opts))

(defn get-video-duration
  [video-file]
  (ffmpeg/get-video-duration video-file))
