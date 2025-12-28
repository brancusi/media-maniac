(ns com.atd.mm.media-converter.core
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [com.atd.mm.media-converter.processors.core :refer [process-rule]]
   [com.atd.mm.media-converter.processors.interface]
   [ubergraph.core :as uber]
   [debug :as debug :refer [spy spy-when spy-present stash peek-stash]]
   [com.rpl.specter :refer [transform MAP-VALS ALL filterer select setval]]
   [com.atd.mm.core-utils.interface :as cu]))


(defn update-rule-deps
  [data old-id new-id]
  (setval [(filterer #(:deps %))
           ALL
           :deps
           (filterer #(= % old-id))
           ALL]
          new-id
          data))

(defn prepare-rules
  [data]
  (let [keys (select [ALL :id] data)]
    (reduce (fn [acc old-id]
              (let [new-id (str (java.util.UUID/randomUUID))
                    updated-data (setval [(filterer #(= (:id %) old-id)) ALL :id]
                                         new-id
                                         acc)]
                (update-rule-deps updated-data old-id new-id)))
            data
            keys)))

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

        _ (io/make-parents final-output-file)

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

  (io/make-parents output-file)

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
        _ (io/make-parents (str output-path "/dummy.txt"))

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

        input-file-name (cu/get-file-name input-file)
        base-name (cu/get-file-name input-file-name)
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

(comment

  (process-rule {:type :proxy
                 :opts {:hey :son}})

  (process-rule {:type :copy
                 :opts {:hey :copy}})

  (process-rule {:type :stills
                 :opts {:hey :stills}})

  (process-rule {:type :transcribe
                 :opts {:hey :trans}})

  ;;Keep from folding
  )

(defn process-video
  [file & {:keys [rules]}]
  (let [src-hash (cu/hash-file file)]
    (doseq [rule rules]
      (process-rule rule))
    src-hash))

(def rules [{:id "proxy-720"
             :type :media/proxy
             :opts {:size 720
                    :name "_proxy_%name.%ext"}}
            {:id "extract-stills"
             :deps ["proxy-720"]
             :type :media/extract-stills
             :opts {:frequency 10
                    :size 320
                    :quality 1
                    :type "jpg"
                    :name "%name_%frame.%ext"}}
            {:id "extract-audio"
             :type :media/extract-audio
             :opts {:encoding "wav"
                    :bitrate 42000
                    :dest "/audio"
                    :name "%name_.%ext"}}
            {:id "transcribe"
             :deps ["extract-audio" "proxy-720"]
             :type :media/transcribe
             :opts {:model :whisper}}
            {:id "copy"
             :type :media/copy
             :opts {:dest "/here"}}])


(defn build-b
  [self-id deps]
  (mapv (fn [in]
          [in self-id]) deps))

(defn build-a
  [rule]
  (let [id (:id rule)
        deps (:deps rule)
        has-deps? (seq deps)]
    (if has-deps?
      (into [[id rule]] (build-b id deps))
      [[id rule]])))

(defn rules->ubergraph
  [rules]
  (let [data (vec (concat (mapcat build-a rules)))]
    (apply uber/digraph data)))

(defn root-nodes
  "Get the root nodes of the graph. No deps"
  [g]
  (let [nodes (uber/nodes g)]
    (filter (fn [node]
              (nil? (uber/predecessors g node))) nodes)))

(comment
  (prepare-rules rules)

  (uber/pprint
   (rules->ubergraph (prepare-rules rules)))

  (-> (rules->ubergraph (prepare-rules rules))
      root-nodes)

;
  )

(comment

  (process-video
   "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4"
   {:rules rules})

  ;; Example usage: Extract frames every 10 seconds
  (generate-proxy-with-frame-extraction
   "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4"
   {:proxy-output-file "/Users/atd/Documents/projects/media-maniac/refs/proxy/sample.mov"
    :frame-frequency-seconds 10})

  ;; Example usage: Extract frames every 20 seconds with custom output
  (generate-proxy-with-frame-extraction
   "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4"
   :proxy-output-file "/Users/atd/Documents/projects/media-maniac/refs/sample_proxy.mov"
   :frames-output-dir "/Users/atd/Documents/projects/media-maniac/refs/frames"
   :frame-frequency-seconds 20)

  ;; Just extract frames from an existing video (without creating proxy first)
  (extract-frames-from-video
   "/Users/atd/Documents/projects/media-maniac/refs/sample_proxy_720.mov"
   :frequency-seconds 2
   :output-dir "/Users/atd/Documents/projects/media-maniac/refs/stills")

  ;; Get video duration
  (get-video-duration "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4")

  ;;Keep from folding
  )



(comment



;
  )