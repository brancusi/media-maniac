(ns com.atd.mm.media-processor.ffmpeg
  "Low-level FFmpeg/FFprobe wrappers for media processing."
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [com.atd.mm.core-utils.interface :as cu]))

(defn convert-resolution-to-file-name
  [str-resolution]
  (let [resolution (str/split str-resolution #":")
        width (first resolution)
        height (second resolution)]
    {:w (cu/force-int width)
     :h (cu/force-int height)}))

(defn generate-proxy
  [input-file & {:keys [output-file resolution]
                 :or {resolution "720:-1"}}]
  (let [scale-filter (str "scale=" resolution)
        input-path (cu/get-folder-path input-file)
        input-file-name (cu/get-file-name input-file)
        final-output-file (if output-file
                            output-file
                            (str input-path "/"
                                 input-file-name
                                 "_proxy_"
                                 (:w (convert-resolution-to-file-name resolution))
                                 ".mov"))

        _ (cu/ensure-dir! (cu/get-folder-path final-output-file))

        ffmpeg-args ["ffmpeg"
                     "-y"
                     "-i" input-file
                     "-c:v" "prores_ks"
                     "-profile:v" "1"
                     "-pix_fmt" "yuv422p10le"
                     "-vf" scale-filter
                     "-c:a" "pcm_s16le"
                     final-output-file]

        process-builder (doto (ProcessBuilder. ffmpeg-args)
                          (.redirectErrorStream true))
        process (.start process-builder)
        output (slurp (.getInputStream process))
        exit-code (.waitFor process)]

    (if (zero? exit-code)
      {:success true
       :output-file final-output-file}
      {:success false
       :exit-code exit-code
       :error output})))

(defn get-video-duration
  "Extract the duration of a video file in seconds using ffprobe."
  [video-file]
  (let [ffprobe-args ["ffprobe"
                      "-v" "error"
                      "-show_entries" "format=duration"
                      "-of" "default=noprint_wrappers=1:nokey=1"
                      video-file]
        result (apply sh/sh ffprobe-args)
        duration-str (str/trim (:out result))]
    (when (and (zero? (:exit result))
               (seq duration-str))
      (Double/parseDouble duration-str))))

(defn extract-frame-at-timestamp
  "Extract a single frame from a video at a specific timestamp."
  [video-file output-file timestamp]
  (cu/ensure-dir! (cu/get-folder-path output-file))
  (let [ffmpeg-args ["ffmpeg"
                     "-y"
                     "-ss" (str timestamp)
                     "-i" video-file
                     "-vframes" "1"
                     "-q:v" "2"
                     output-file]
        process-builder (doto (ProcessBuilder. ffmpeg-args)
                          (.redirectErrorStream true))
        process (.start process-builder)
        output (slurp (.getInputStream process))
        exit-code (.waitFor process)]
    (if (zero? exit-code)
      {:success true
       :output-file output-file
       :timestamp timestamp}
      {:success false
       :exit-code exit-code
       :error output})))

(defn calculate-frame-timestamps
  "Calculate timestamps at which to extract frames based on frequency in seconds."
  [duration frequency-seconds]
  (range 0 (int duration) frequency-seconds))

(defn generate-output-filename
  "Generate a filename for an extracted frame."
  [base-path base-name frame-number timestamp]
  (str base-path "/" base-name "_frame_"
       (format "%04d" frame-number)
       "_t" (format "%.2f" (double timestamp)) "s.jpg"))

(defn extract-frames-from-video
  "Extract still images from a video at specified frequency.

   Parameters:
   - video-file: Path to the video file
   - output-dir: Directory where frames will be saved
   - frequency-seconds: Extract one frame every N seconds
   - base-name: Base name for output files (optional)"
  [video-file & {:keys [output-dir frequency-seconds base-name]
                 :or {frequency-seconds 10
                      base-name "frame"}}]
  (let [duration (get-video-duration video-file)
        _ (when-not duration
            (throw (ex-info "Could not determine video duration"
                            {:video-file video-file})))

        output-path (or output-dir (cu/get-folder-path video-file))
        _ (cu/ensure-dir! output-path)

        timestamps (calculate-frame-timestamps duration frequency-seconds)

        results (doall
                 (map-indexed
                  (fn [idx timestamp]
                    (let [output-file (generate-output-filename
                                       output-path
                                       base-name
                                       idx
                                       timestamp)
                          result (extract-frame-at-timestamp
                                  video-file
                                  output-file
                                  timestamp)]
                      (merge result {:frame-number idx})))
                  timestamps))]

    {:success (every? :success results)
     :total-frames (count results)
     :duration duration
     :frequency-seconds frequency-seconds
     :frames results}))

(defn generate-proxy-with-frame-extraction
  "Generate a proxy video and extract still images at specified frequency.

   Parameters:
   - input-file: Source video file path
   - proxy-output-file: Output path for proxy (optional)
   - frames-output-dir: Directory for extracted frames (optional)
   - proxy-resolution: Resolution for proxy (default: 720:-1)
   - frame-frequency-seconds: Extract one frame every N seconds (default: 10)"
  [input-file & {:keys [proxy-output-file
                        frames-output-dir
                        proxy-resolution
                        frame-frequency-seconds]
                 :or {proxy-resolution "720:-1"
                      frame-frequency-seconds 10}}]
  (let [proxy-result (generate-proxy input-file
                                     :output-file proxy-output-file
                                     :resolution proxy-resolution)

        _ (when-not (:success proxy-result)
            (throw (ex-info "Failed to generate proxy" proxy-result)))

        proxy-file (:output-file proxy-result)

        base-name (cu/get-file-name input-file)
        frames-dir (or frames-output-dir
                       (str (cu/get-folder-path input-file) "/frames"))

        frames-result (extract-frames-from-video
                       proxy-file
                       :output-dir frames-dir
                       :frequency-seconds frame-frequency-seconds
                       :base-name base-name)]

    {:success (:success frames-result)
     :proxy-file proxy-file
     :proxy-result proxy-result
     :frames-result frames-result
     :frames-directory frames-dir}))
