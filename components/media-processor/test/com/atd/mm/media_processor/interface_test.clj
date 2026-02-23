(ns com.atd.mm.media-processor.interface-test
  (:require [clojure.test :as test :refer :all]
            [com.atd.mm.media-processor.interface :as mp]
            [com.atd.mm.media-processor.ffmpeg :as ffmpeg]
            [com.atd.mm.media-processor.processors.core :as proc-core]))

;; =================================================================
;; Interface accessibility
;; =================================================================

(deftest interface-functions-accessible
  (testing "Process entry point is a function"
    (is (fn? mp/process-media)))

  (testing "FFmpeg wrappers are exposed"
    (is (fn? mp/generate-proxy))
    (is (fn? mp/extract-frames-from-video))
    (is (fn? mp/generate-proxy-with-frame-extraction))
    (is (fn? mp/get-video-duration))))

;; =================================================================
;; Multimethod registration
;; =================================================================

(deftest all-processor-types-registered
  (let [methods (methods proc-core/execute-process)
        expected #{:media/proxy
                   :media/extract-stills
                   :media/extract-audio
                   :media/transcribe
                   :media/copy}]
    (testing "All five processor types are registered"
      (is (= expected (set (keys methods)))))

    (testing "Each registered method is a function"
      (doseq [[k v] methods]
        (is (fn? v) (str k " should be a function"))))))

;; =================================================================
;; Pure utility functions (no FFmpeg needed)
;; =================================================================

(deftest calculate-frame-timestamps--returns-correct-seq
  (testing "10s video at 5s frequency → [0 5]"
    (is (= [0 5] (vec (ffmpeg/calculate-frame-timestamps 10 5)))))

  (testing "30s video at 10s frequency → [0 10 20]"
    (is (= [0 10 20] (vec (ffmpeg/calculate-frame-timestamps 30 10)))))

  (testing "5s video at 10s frequency → [0] (one frame at start)"
    (is (= [0] (vec (ffmpeg/calculate-frame-timestamps 5 10)))))

  (testing "0 duration → empty sequence"
    (is (= [] (vec (ffmpeg/calculate-frame-timestamps 0 10))))))

(deftest generate-output-filename--formats-correctly
  (testing "Standard frame filename"
    (is (= "/out/clip_frame_0000_t0.00s.jpg"
           (ffmpeg/generate-output-filename "/out" "clip" 0 0.0))))

  (testing "Frame at 25.5s with index 5"
    (is (= "/out/clip_frame_0005_t25.50s.jpg"
           (ffmpeg/generate-output-filename "/out" "clip" 5 25.5))))

  (testing "High frame number"
    (is (= "/media/frames/video_frame_0123_t120.75s.jpg"
           (ffmpeg/generate-output-filename "/media/frames" "video" 123 120.75)))))

(deftest convert-resolution-to-file-name--parses-resolution
  (testing "Standard 1920:1080"
    (is (= {:w 1920 :h 1080}
           (ffmpeg/convert-resolution-to-file-name "1920:1080"))))

  (testing "720p shorthand 1280:720"
    (is (= {:w 1280 :h 720}
           (ffmpeg/convert-resolution-to-file-name "1280:720"))))

  (testing "Handles string numbers"
    (is (= {:w 3840 :h 2160}
           (ffmpeg/convert-resolution-to-file-name "3840:2160")))))
