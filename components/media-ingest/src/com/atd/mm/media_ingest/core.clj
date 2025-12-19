(ns com.atd.mm.media-ingest.core
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io])
  (:import (java.lang ProcessBuilder)))

;; Constants and configuration
(def ^:private media-ext
  #"(jpe?g|tiff?|png|arw|cr3|nef|rw2|heic|heif|mp4|mov|m4v|avi|mkv|wav|mp3|flac)$")

(def docker-bin "/var/packages/ContainerManager/target/usr/bin/docker")

(def ^:private media-ext-set
  #{"jpg" "jpeg" "tif" "tiff" "png" "arw" "cr3" "nef" "rw2" "heic" "heif"
    "mp4" "mov" "m4v" "avi" "mkv" "wav" "mp3" "flac"})

(def ^:private skip-dirs
  #{"/.Spotlight-V100" "/.Trashes" "/.fseventsd" "/.Spotlight-Temp"})

(def ^:const buf-size (* 1024 1024))

;; Private utility functions
(defn- q [s]
  (str "'" (str/replace s #"'" "'\"'\"'") "'"))

(defn- mac->nas [p]
  (if (and (.startsWith p "/Volumes/") (>= (.indexOf p "/" 9) 0))
    (str "/volume1/" (subs p (count "/Volumes/")))
    p))

(defn- shell-quote
  "Safely single-quote a path for remote /bin/sh.
   (a'b) -> 'a'\"'\"'b'"
  [s]
  (str "'" (str/replace s #"'" "'\"'\"'") "'"))

;; Public API functions
(defn xxh3-64-file-local
  "Returns XXH3-64 hex for a local file using xxhsum -H2.
   Requires: `brew install xxhash` on macOS."
  [path]
  (let [{:keys [exit out err]} (sh/sh "xxhsum" "-H2" path)]
    (if (zero? exit)
      (let [hex (-> out str/trim (str/split #"\s+") first)]
        (if (str/blank? hex)
          (throw (ex-info "xxhsum returned empty output" {:stdout out :stderr err}))
          hex))
      (throw (ex-info "xxhsum failed" {:exit exit :stderr err :stdout out})))))

(defn ffprobe-json
  "Run ffprobe and return parsed JSON (keyword keys).
   Returns {:error ...} map if ffprobe fails."
  [^String f]
  (let [cmd ["ffprobe" "-v" "quiet"
             "-print_format" "json"
             "-show_format" "-show_streams"
             f]
        pb  (doto (ProcessBuilder. cmd)
              (.redirectErrorStream true))
        p   (.start pb)]
    (try
      (with-open [r (io/reader (.getInputStream p))]
        (let [out (slurp r)
              exit (.waitFor p)]
          (if (zero? exit)
            (json/parse-string out true)
            {:error :ffprobe-failed
             :exit exit
             :stderr/out out})))
      (catch Exception e
        {:error :exception
         :message (.getMessage e)}))))

(defn ffprobe-detailed-color
  "Run ffprobe with detailed color information extraction.
   This can sometimes reveal additional picture profile information."
  [^String f]
  (let [cmd ["ffprobe" "-v" "quiet"
             "-select_streams" "v:0"
             "-show_entries" "stream=color_space,color_transfer,color_primaries,color_range,field_order,chroma_location"
             "-show_entries" "stream_tags"
             "-show_entries" "format_tags"
             "-print_format" "json"
             f]
        pb  (doto (ProcessBuilder. cmd)
              (.redirectErrorStream true))
        p   (.start pb)]
    (try
      (with-open [r (io/reader (.getInputStream p))]
        (let [out (slurp r)
              exit (.waitFor p)]
          (if (zero? exit)
            (json/parse-string out true)
            {:error :ffprobe-detailed-failed
             :exit exit
             :stderr/out out})))
      (catch Exception e
        {:error :exception
         :message (.getMessage e)}))))

(defn summarize
  "Extract a few handy fields from ffprobe's JSON.
   Keeps full JSON under :raw for later if needed."
  [^String f ffj]
  (let [fmt    (:format ffj)
        streams (:streams ffj)
        vstr   (first (filter #(= "video" (:codec_type %)) streams))
        astr   (first (filter #(= "audio" (:codec_type %)) streams))]
    {:file        f
     :size-bytes  (some-> fmt :size (parse-long))
     :duration-s  (some-> fmt :duration (Double/parseDouble))
     :bit_rate    (some-> fmt :bit_rate (parse-long))
     :format-name (:format_name fmt)

     :video {:codec (:codec_name vstr)
             :width (:width vstr)
             :height (:height vstr)
             :fps (when-let [r (:r_frame_rate vstr)]
                    (try
                      (let [[n d] (str/split r #"/")]
                        (when (and n d)
                          (/ (Double/parseDouble n)
                             (Double/parseDouble d))))
                      (catch Exception _ nil)))
             :rotation (some-> vstr :tags :rotate)}
     :audio {:codec (:codec_name astr)
             :channels (:channels astr)
             :sample_rate (some-> astr :sample_rate (parse-long))}
     :raw ffj}))

(defn detect-camera-type
  "Attempt to detect camera type from metadata tags and format info"
  [ffj]
  (let [fmt (:format ffj)
        tags (:tags fmt)
        streams (:streams ffj)
        video-stream (first (filter #(= "video" (:codec_type %)) streams))
        video-tags (:tags video-stream)
        major-brand (:major_brand tags)
        compatible-brands (:compatible_brands tags)
        encoder-tag (or (:encoder tags) (:encoder video-tags))]

    (cond
      ;; Sony cameras - enhanced detection
      (or (str/includes? (str (:make tags)) "Sony")
          (str/includes? (str (:model tags)) "FX")
          (str/includes? (str (:encoder tags)) "Sony")
          (str/includes? (str (:handler_name video-stream)) "Sony")
          ;; XAVC is Sony's format
          (= major-brand "XAVC")
          (str/includes? (str compatible-brands) "XAVC")
          ;; Check for Sony-specific encoding
          (str/includes? (str encoder-tag) "HEVC Coding"))
      :sony

      ;; RED cameras
      (or (str/includes? (str (:make tags)) "RED")
          (str/includes? (str (:encoder tags)) "RED")
          (re-find #"(?i)red" (str (:comment tags)))
          (= major-brand "RED"))
      :red

      ;; DJI (Osmo, drones)
      (or (str/includes? (str (:make tags)) "DJI")
          (str/includes? (str (:model tags)) "FC")
          (str/includes? (str (:model tags)) "Osmo")
          (str/includes? (str (:encoder tags)) "DJI"))
      :dji

      ;; Canon
      (or (str/includes? (str (:make tags)) "Canon")
          (str/includes? (str (:encoder tags)) "Canon"))
      :canon

      ;; Panasonic
      (or (str/includes? (str (:make tags)) "Panasonic")
          (str/includes? (str (:encoder tags)) "Panasonic"))
      :panasonic

      ;; Blackmagic
      (or (str/includes? (str (:make tags)) "Blackmagic")
          (str/includes? (str (:encoder tags)) "Blackmagic"))
      :blackmagic

      :else :unknown)))

(defn extract-comprehensive-metadata
  "Extract comprehensive metadata from ffprobe JSON for database storage.
   Returns a map with all metadata fields defined in the schema."
  [^String filepath ffj]
  (let [fmt (:format ffj)
        streams (:streams ffj)
        video-stream (first (filter #(= "video" (:codec_type %)) streams))
        audio-stream (first (filter #(= "audio" (:codec_type %)) streams))
        fmt-tags (:tags fmt)
        video-tags (:tags video-stream)
        audio-tags (:tags audio-stream)
        camera-type (detect-camera-type ffj)]

    (merge
     ;; Basic file info
     {:file-path filepath
      :camera-type camera-type}

     ;; Container format information
     (when fmt
       {:container-format (some-> fmt :format_name)
        :format-name (some-> fmt :format_name)
        :format-long-name (some-> fmt :format_long_name)
        :size-bytes (some-> fmt :size parse-long)
        :duration (some-> fmt :duration Double/parseDouble)
        :bitrate (some-> fmt :bit_rate parse-long)})

     ;; Format tags from container
     (when fmt-tags
       {:creation-time (:creation_time fmt-tags)
        :major-brand (:major_brand fmt-tags)
        :compatible-brands (when (:compatible_brands fmt-tags)
                             [(:compatible_brands fmt-tags)])
        :encoder (:encoder fmt-tags)
        :encoding-tool (:encoding_tool fmt-tags)})

     ;; Video codec information
     (when video-stream
       {:video-codec (:codec_name video-stream)
        :video-codec-long-name (:codec_long_name video-stream)
        :video-profile (:profile video-stream)
        :video-level (:level video-stream)
        :video-bitrate (some-> video-stream :bit_rate parse-long)
        :video-framerate (:r_frame_rate video-stream)
        :width (:width video-stream)
        :height (:height video-stream)
        :pixel-format (:pix_fmt video-stream)
        :color-space (:color_space video-stream)
        :color-transfer (:color_trc video-stream)
        :color-primaries (:color_primaries video-stream)
        :color-range (:color_range video-stream)
        :handler-name (:handler_name video-stream)})

     ;; Video tags (camera-specific metadata often here)
     (when video-tags
       {:timecode (:timecode video-tags)
        :rotate (:rotate video-tags)})

     ;; Audio codec information
     (when audio-stream
       {:audio-codec (:codec_name audio-stream)
        :audio-codec-long-name (:codec_long_name audio-stream)
        :audio-bitrate (some-> audio-stream :bit_rate parse-long)
        :audio-sample-rate (some-> audio-stream :sample_rate parse-long)
        :audio-channels (:channels audio-stream)})

     ;; Camera metadata (EXIF-style from container tags)
     (when fmt-tags
       {:camera-make (:make fmt-tags)
        :camera-model (:model fmt-tags)
        :software (:software fmt-tags)})

     ;; Collect custom tags for anything we might have missed
     {:custom-tags (vec (for [[k v] (merge fmt-tags video-tags audio-tags)
                              :when (and k v (string? v) (not (str/blank? v)))]
                          (str (name k) "=" v)))}

     ;; Keep raw data for debugging/future extraction
     {:raw-ffprobe ffj})))

(defn detect-log-profile
  "Detect log/gamma curve from various camera metadata.
   Different cameras store this information in different places."
  [metadata camera-type]
  (let [custom-tags (:custom-tags metadata)
        color-transfer (:color-transfer metadata)
        software (:software metadata)
        encoding-tool (:encoding-tool metadata)
        pixel-format (:pixel-format metadata)
        major-brand (:major-brand metadata)]

    (cond
      ;; Sony S-Log detection - enhanced for XAVC
      (and (= camera-type :sony)
           (or (some #(str/includes? % "S-Log") custom-tags)
               (str/includes? (str software) "S-Log")
               (str/includes? (str encoding-tool) "S-Log")
               ;; For XAVC files, infer from format characteristics
               (and (= major-brand "XAVC")
                    (= pixel-format "yuv422p10le")  ; 10-bit indicates likely log
                    (:video-profile metadata))))      ; Has profile indicates pro format
      (cond
        (some #(str/includes? % "S-Log3") custom-tags) "S-Log3"
        (some #(str/includes? % "S-Log2") custom-tags) "S-Log2"
        ;; For XAVC 10-bit without explicit S-Log tags, likely S-Log3
        (and (= major-brand "XAVC") (= pixel-format "yuv422p10le")) "S-Log3 (inferred)"
        :else "S-Log")

      ;; Canon Log detection
      (and (= camera-type :canon)
           (or (some #(str/includes? % "Log") custom-tags)
               (str/includes? (str software) "Log")))
      "Canon Log"

      ;; RED detection
      (= camera-type :red)
      "REDLogFilm"

      ;; Panasonic V-Log
      (and (= camera-type :panasonic)
           (some #(str/includes? % "V-Log") custom-tags))
      "V-Log"

      ;; Blackmagic Film
      (and (= camera-type :blackmagic)
           (some #(str/includes? % "Film") custom-tags))
      "Blackmagic Film"

      ;; Standard transfer curves
      (= color-transfer "smpte2084") "PQ (ST.2084)"
      (= color-transfer "arib-std-b67") "HLG"
      (= color-transfer "bt709") "Rec.709"

      :else nil)))

(defn get-comprehensive-file-info
  "Get comprehensive metadata for any media file automatically.
   This is the main function to use when ingesting files from any camera type."
  [^String filepath]
  (let [ffprobe-result (ffprobe-json filepath)]
    (if (:error ffprobe-result)
      {:file filepath
       :error (:error ffprobe-result)
       :detail (dissoc ffprobe-result :error)}
      (let [metadata (extract-comprehensive-metadata filepath ffprobe-result)
            camera-type (:camera-type metadata)
            log-profile (detect-log-profile metadata camera-type)]
        (cond-> metadata
          log-profile (assoc :gamma-curve log-profile))))))

(defn get-file-info
  [^String f]
  (let [j (ffprobe-json f)]
    (if (:error j)
      {:file f :error (:error j) :detail (dissoc j :error)}
      (summarize f j))))

(defn list-media
  "Return seq of absolute file paths in `dir` whose extension is in `exts` (a set of lowercase strings).
   Example: (list-media \"/Volumes/CARD/DCIM/100MSDCF\" #{\"mp4\" \"mov\"})"
  [dir exts]
  (->> (.listFiles (io/file dir))
       (filter #(.isFile ^java.io.File %))
       (map #(.getAbsolutePath ^java.io.File %))
       (filter (fn [path]
                 (let [ext (some-> path (str/lower-case) (str/split #"\.") last)]
                   (contains? exts ext))))))

(defn analyze-sd-card
  "Analyze all media files on an SD card and return comprehensive metadata.
   Returns a map with :files (vector of file metadata) and :summary."
  [sd-card-path]
  (let [media-files (list-media sd-card-path media-ext-set)
        file-info (mapv get-comprehensive-file-info media-files)
        successful (filter #(not (:error %)) file-info)
        failed (filter :error file-info)
        camera-types (frequencies (map :camera-type successful))
        formats (frequencies (map :container-format successful))
        total-size (reduce + 0 (keep :size-bytes successful))]

    {:files file-info
     :summary {:total-files (count media-files)
               :successful (count successful)
               :failed (count failed)
               :camera-types camera-types
               :formats formats
               :total-size-bytes total-size
               :total-size-gb (/ total-size 1e9)
               :duration-hours (/ (reduce + 0 (keep :duration successful)) 3600)}}))

(defn xxh3-64-remote
  "SSH to NAS and compute XXH3-64 via Docker+xxhsum (-H2).
   Args:
     :identity -> SSH key on your Mac (e.g., \"/Users/atd/.ssh/id_ed25519_nas\")
     :user     -> NAS username (e.g., \"aramzadikian\")
     :host     -> NAS host or .local name
     :dir      -> NAS directory to mount (e.g., \"/volume1/MMM/2025/08/28/src/fx3\")
     :file     -> filename inside that directory (e.g., \"C1557.MP4\")
   Returns the hex digest string, or throws ex-info on error."
  [{:keys [identity user host dir file] :as _opts}]
  (when (some str/blank? [identity user host dir file])
    (throw (ex-info "identity, user, host, dir, and file are required"
                    {:identity identity :user user :host host :dir dir :file file})))
  (let [remote-cmd (format
                    ;; same container flow, but with -H2 for XXH3
                    (str "%s run --rm -v %s:/data alpine "
                         "sh -lc %s")
                    docker-bin
                    (pr-str dir)
                    (pr-str (format
                             "apk add --no-cache xxhash >/dev/null && xxhsum -H2 %s"
                             (pr-str (str "/data/" file)))))
        {:keys [exit out err]}
        (sh/sh "ssh" "-i" identity (str user "@" host) remote-cmd)]
    (if (zero? exit)
      (let [hex (-> out str/trim (str/split #"\s+") first)]
        (if (str/blank? hex)
          (throw (ex-info "Command succeeded but no hash parsed"
                          {:stdout out :stderr err :cmd remote-cmd}))
          hex))
      (throw (ex-info "Remote Docker xxhsum failed"
                      {:exit exit :stderr err :stdout out :cmd remote-cmd})))))

(defn ingest-media
  "Main function to ingest media files from a source directory"
  [target-dir]
  (println "Ingesting media from:" target-dir))
