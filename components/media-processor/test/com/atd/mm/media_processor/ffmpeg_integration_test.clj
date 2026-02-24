(ns ^:integration com.atd.mm.media-processor.ffmpeg-integration-test
  "Integration tests that exercise real FFmpeg/FFprobe against a
   synthetic test video.  Tagged ^:integration so they are skipped by
   default (see tests.edn :kaocha.filter/skip-meta).

   Run explicitly:
     clojure -M:dev:test:kaocha --focus-meta :integration"
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.test :refer :all]
   [com.atd.mm.media-processor.ffmpeg :as ffmpeg]))

;; =================================================================
;; Helpers
;; =================================================================

(defn- ffmpeg-available?
  "Returns true when ffmpeg and ffprobe are both on PATH."
  []
  (try
    (and (zero? (:exit (sh/sh "ffmpeg" "-version")))
         (zero? (:exit (sh/sh "ffprobe" "-version"))))
    (catch Exception _ false)))

(defn- probe-dimensions
  "Use ffprobe to read width×height of a video file. Returns [w h]."
  [path]
  (let [result (sh/sh "ffprobe"
                      "-v" "error"
                      "-select_streams" "v:0"
                      "-show_entries" "stream=width,height"
                      "-of" "csv=s=x:p=0"
                      path)
        parts  (str/split (str/trim (:out result)) #"x")]
    (when (= 2 (count parts))
      [(Integer/parseInt (first parts))
       (Integer/parseInt (second parts))])))

(defn- probe-has-audio?
  "Returns true when the file contains at least one audio stream."
  [path]
  (let [result (sh/sh "ffprobe"
                      "-v" "error"
                      "-select_streams" "a"
                      "-show_entries" "stream=codec_type"
                      "-of" "csv=p=0"
                      path)]
    (and (zero? (:exit result))
         (str/includes? (:out result) "audio"))))

(defn- file-exists-and-nonzero? [path]
  (let [f (io/file path)]
    (and (.exists f) (pos? (.length f)))))

;; =================================================================
;; Fixture — generate a 2-second synthetic test video once per ns
;; =================================================================

(def ^:private test-dir     (str (System/getProperty "java.io.tmpdir") "/mm-ffmpeg-integration"))
(def ^:private test-video   (str test-dir "/synthetic_test.mp4"))

(defn- generate-synthetic-video!
  "Create a 2-second 320×240 colour-bars video with a silent audio
   track.  Fast to encode, sufficient for proxy + frame extraction."
  []
  (let [dir (io/file test-dir)]
    (.mkdirs dir)
    (let [result (sh/sh "ffmpeg" "-y"
                        "-f" "lavfi" "-i" "color=c=blue:s=320x240:d=2:r=25"
                        "-f" "lavfi" "-i" "anullsrc=r=48000:cl=stereo"
                        "-shortest"
                        "-c:v" "libx264" "-preset" "ultrafast"
                        "-c:a" "aac" "-b:a" "64k"
                        test-video)]
      (when-not (zero? (:exit result))
        (throw (ex-info "Failed to generate synthetic test video"
                        {:stderr (:err result)
                         :exit   (:exit result)}))))))

(defn- cleanup-test-dir! []
  (let [dir (io/file test-dir)]
    (when (.exists dir)
      (doseq [f (reverse (file-seq dir))]
        (.delete f)))))

(defn ffmpeg-fixture
  "Once-fixture: generate synthetic video, run all tests, clean up."
  [run-tests]
  (if (ffmpeg-available?)
    (try
      (generate-synthetic-video!)
      (run-tests)
      (finally
        (cleanup-test-dir!)))
    (do
      (println "\n⚠  FFmpeg not found on PATH — skipping integration tests\n")
      ;; still call run-tests so Kaocha sees the test vars; each test
      ;; will effectively be a no-op because test-video won't exist
      ;; and the guard `when` in tests will skip assertions.
      )))

(use-fixtures :once ffmpeg-fixture)

;; =================================================================
;; Guard macro — skip body when fixture didn't run
;; =================================================================

(defmacro ^:private when-video-exists [& body]
  `(if (file-exists-and-nonzero? test-video)
     (do ~@body)
     (is true "SKIPPED — synthetic video not available (FFmpeg missing?)")))

;; =================================================================
;; Tests — generate-proxy
;; =================================================================

(deftest generate-proxy--produces-valid-output
  (when-video-exists
   (let [proxy-out (str test-dir "/proxy_out.mov")
         result    (ffmpeg/generate-proxy test-video
                                          :output-file proxy-out
                                          :resolution "720:-1")]
     (testing "returns success"
       (is (:success result)))

     (testing "output file exists and is non-zero"
       (is (file-exists-and-nonzero? proxy-out)))

     (testing "output has expected width (height scales automatically)"
       (let [[w _h] (probe-dimensions proxy-out)]
         (is (= 720 w))))

     (testing "output retains audio track"
       (is (probe-has-audio? proxy-out))))))

(deftest generate-proxy--custom-resolution
  (when-video-exists
   (let [proxy-out (str test-dir "/proxy_480.mov")
         result    (ffmpeg/generate-proxy test-video
                                          :output-file proxy-out
                                          :resolution "480:-1")]
     (testing "success at 480-wide"
       (is (:success result)))

     (testing "width matches requested resolution"
       (let [[w _h] (probe-dimensions proxy-out)]
         (is (= 480 w)))))))

(deftest generate-proxy--default-output-path
  (when-video-exists
   (testing "when no output-file given, proxy lands next to source"
     (let [result (ffmpeg/generate-proxy test-video :resolution "720:-1")]
       (is (:success result))
       (is (file-exists-and-nonzero? (:output-file result)))
       ;; clean up the default-named file
       (io/delete-file (:output-file result) true)))))

;; =================================================================
;; Tests — extract-frames-from-video
;; =================================================================

(deftest extract-frames--correct-count-and-files
  (when-video-exists
   (let [frames-dir (str test-dir "/frames")
         result     (ffmpeg/extract-frames-from-video
                     test-video
                     :output-dir  frames-dir
                     :frequency-seconds 1
                     :base-name "syn")]

     (testing "returns overall success"
       (is (:success result)))

     (testing "frame count matches expected timestamps (0s, 1s for 2s video)"
       (is (= 2 (:total-frames result))))

     (testing "each frame file exists and is non-zero"
       (doseq [frame (:frames result)]
         (is (:success frame)
             (str "frame " (:frame-number frame) " should succeed"))
         (is (file-exists-and-nonzero? (:output-file frame))
             (str "frame file should exist: " (:output-file frame)))))

     (testing "duration was detected"
       (is (number? (:duration result)))
       (is (< 1.5 (:duration result) 3.0)
           "synthetic video should be ~2 seconds")))))

(deftest extract-frames--higher-frequency-than-duration
  (when-video-exists
   (let [frames-dir (str test-dir "/frames-single")
         result     (ffmpeg/extract-frames-from-video
                     test-video
                     :output-dir  frames-dir
                     :frequency-seconds 10
                     :base-name "single")]

     (testing "extracts exactly one frame at t=0 when freq > duration"
       (is (:success result))
       (is (= 1 (:total-frames result)))))))

;; =================================================================
;; Tests — generate-proxy-with-frame-extraction (combined)
;; =================================================================

(deftest proxy-with-frame-extraction--end-to-end
  (when-video-exists
   (let [proxy-out  (str test-dir "/combo_proxy.mov")
         frames-dir (str test-dir "/combo_frames")
         result     (ffmpeg/generate-proxy-with-frame-extraction
                     test-video
                     :proxy-output-file proxy-out
                     :frames-output-dir frames-dir
                     :proxy-resolution  "720:-1"
                     :frame-frequency-seconds 1)]

     (testing "overall success"
       (is (:success result)))

     (testing "proxy file created"
       (is (file-exists-and-nonzero? (:proxy-file result))))

     (testing "frames extracted from the proxy"
       (is (= 2 (get-in result [:frames-result :total-frames])))
       (doseq [frame (get-in result [:frames-result :frames])]
         (is (file-exists-and-nonzero? (:output-file frame))))))))

;; =================================================================
;; Tests — get-video-duration
;; =================================================================

(deftest get-video-duration--returns-reasonable-value
  (when-video-exists
   (let [dur (ffmpeg/get-video-duration test-video)]
     (testing "returns a number"
       (is (number? dur)))
     (testing "close to 2 seconds"
       (is (< 1.5 dur 3.0))))))
