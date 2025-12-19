(ns com.atd.mm.media-ingest.picture-profiles
  "Picture profile detection and classification for various camera manufacturers.
   Uses namespaced keywords for consistent, efficient representation."
  (:require [clojure.string :as str]))

;; ========== PICTURE PROFILE KEYWORDS ==========om.atd.mm.media-ingest.picture-profiles(ns com.atd.mm.media-ingest.picture-profiles)
;;  "Picture profile detection and classification for various camera manufacturers.
;;    Uses namespaced keywords for consistent, efficient representation."

;; ========== PICTURE PROFILE KEYWORDS ==========

;; Sony S-Log profiles
(def sony-profiles
  {:s-log     :sony/s-log
   :s-log2    :sony/s-log2
   :s-log3    :sony/s-log3
   :s-log3-inferred :sony/s-log3-inferred
   :s-gamut   :sony/s-gamut
   :s-gamut3  :sony/s-gamut3
   :s-gamut3-cine :sony/s-gamut3-cine
   :rec709    :sony/rec709
   :rec2020   :sony/rec2020})

;; Canon Log profiles
(def canon-profiles
  {:canon-log   :canon/log
   :canon-log2  :canon/log2
   :canon-log3  :canon/log3
   :cinema-gamut :canon/cinema-gamut
   :dci-p3      :canon/dci-p3
   :rec709      :canon/rec709
   :rec2020     :canon/rec2020})

;; RED profiles
(def red-profiles
  {:redlogfilm    :red/logfilm
   :red-log3g10   :red/log3g10
   :red-gamma     :red/gamma
   :redwidegamut  :red/widegamut
   :dragon-color  :red/dragon-color
   :rec709        :red/rec709
   :rec2020       :red/rec2020})

;; Panasonic profiles
(def panasonic-profiles
  {:v-log       :panasonic/v-log
   :v-log-l     :panasonic/v-log-l
   :v-gamut     :panasonic/v-gamut
   :rec709      :panasonic/rec709
   :rec2020     :panasonic/rec2020
   :hybrid-log-gamma :panasonic/hlg})

;; DJI profiles
(def dji-profiles
  {:d-log       :dji/d-log
   :d-cinelike  :dji/d-cinelike
   :rec709      :dji/rec709
   :normal      :dji/normal})

;; Blackmagic profiles
(def blackmagic-profiles
  {:blackmagic-film   :blackmagic/film
   :blackmagic-video  :blackmagic/video
   :rec709           :blackmagic/rec709
   :rec2020          :blackmagic/rec2020
   :extended-video   :blackmagic/extended-video})

;; Standard color transfer curves
(def standard-profiles
  {:bt709        :std/bt709
   :bt2020       :std/bt2020
   :smpte2084    :std/pq-st2084
   :hlg          :std/hlg
   :arib-std-b67 :std/hlg
   :linear       :std/linear
   :gamma22      :std/gamma22
   :gamma28      :std/gamma28})

;; All profiles map for reverse lookup
(def all-profiles
  (merge sony-profiles
         canon-profiles
         red-profiles
         panasonic-profiles
         dji-profiles
         blackmagic-profiles
         standard-profiles))

;; ========== DETECTION FUNCTIONS ==========

(defn detect-sony-profile
  "Detect Sony picture profile from metadata characteristics."
  [metadata]
  (let [custom-tags (:custom-tags metadata)
        software (:software metadata)
        encoding-tool (:encoding-tool metadata)
        pixel-format (:pixel-format metadata)
        major-brand (:major-brand metadata)
        color-transfer (:color-transfer metadata)]

    (cond
      ;; Explicit S-Log detection from tags
      (some #(str/includes? % "S-Log3") custom-tags) :sony/s-log3
      (some #(str/includes? % "S-Log2") custom-tags) :sony/s-log2
      (some #(str/includes? % "S-Log") custom-tags) :sony/s-log

      ;; Software/encoding tool detection
      (str/includes? (str software) "S-Log3") :sony/s-log3
      (str/includes? (str software) "S-Log2") :sony/s-log2
      (str/includes? (str software) "S-Log") :sony/s-log
      (str/includes? (str encoding-tool) "S-Log3") :sony/s-log3
      (str/includes? (str encoding-tool) "S-Log2") :sony/s-log2
      (str/includes? (str encoding-tool) "S-Log") :sony/s-log

      ;; XAVC format inference
      (and (= major-brand "XAVC")
           (= pixel-format "yuv422p10le")) :sony/s-log3-inferred
      (and (= major-brand "XAVC")
           (str/includes? (str pixel-format) "10le")) :sony/s-log3-inferred

      ;; Standard transfer curves
      (= color-transfer "bt709") :sony/rec709
      (= color-transfer "bt2020") :sony/rec2020

      :else nil)))

(defn detect-canon-profile
  "Detect Canon picture profile from metadata characteristics."
  [metadata]
  (let [custom-tags (:custom-tags metadata)
        software (:software metadata)
        color-transfer (:color-transfer metadata)]

    (cond
      (some #(str/includes? % "Canon Log 3") custom-tags) :canon/log3
      (some #(str/includes? % "Canon Log 2") custom-tags) :canon/log2
      (some #(str/includes? % "Canon Log") custom-tags) :canon/log
      (str/includes? (str software) "Canon Log") :canon/log
      (= color-transfer "bt709") :canon/rec709
      (= color-transfer "bt2020") :canon/rec2020
      :else nil)))

(defn detect-red-profile
  "Detect RED picture profile from metadata characteristics."
  [metadata]
  (let [custom-tags (:custom-tags metadata)
        major-brand (:major-brand metadata)
        color-transfer (:color-transfer metadata)]

    (cond
      (some #(str/includes? % "REDLogFilm") custom-tags) :red/logfilm
      (some #(str/includes? % "Log3G10") custom-tags) :red/log3g10
      (= major-brand "RED") :red/logfilm  ; Default for RED files
      (= color-transfer "bt709") :red/rec709
      (= color-transfer "bt2020") :red/rec2020
      :else nil)))

(defn detect-panasonic-profile
  "Detect Panasonic picture profile from metadata characteristics."
  [metadata]
  (let [custom-tags (:custom-tags metadata)
        color-transfer (:color-transfer metadata)]

    (cond
      (some #(str/includes? % "V-Log L") custom-tags) :panasonic/v-log-l
      (some #(str/includes? % "V-Log") custom-tags) :panasonic/v-log
      (= color-transfer "arib-std-b67") :panasonic/hlg
      (= color-transfer "bt709") :panasonic/rec709
      (= color-transfer "bt2020") :panasonic/rec2020
      :else nil)))

(defn detect-dji-profile
  "Detect DJI picture profile from metadata characteristics."
  [metadata]
  (let [custom-tags (:custom-tags metadata)
        color-transfer (:color-transfer metadata)]

    (cond
      (some #(str/includes? % "D-Log") custom-tags) :dji/d-log
      (some #(str/includes? % "D-Cinelike") custom-tags) :dji/d-cinelike
      (= color-transfer "bt709") :dji/rec709
      :else :dji/normal)))

(defn detect-blackmagic-profile
  "Detect Blackmagic picture profile from metadata characteristics."
  [metadata]
  (let [custom-tags (:custom-tags metadata)
        color-transfer (:color-transfer metadata)]

    (cond
      (some #(str/includes? % "Blackmagic Film") custom-tags) :blackmagic/film
      (some #(str/includes? % "Film") custom-tags) :blackmagic/film
      (= color-transfer "bt709") :blackmagic/rec709
      (= color-transfer "bt2020") :blackmagic/rec2020
      :else :blackmagic/video)))

(defn detect-standard-profile
  "Detect standard color transfer curves."
  [metadata]
  (let [color-transfer (:color-transfer metadata)]

    (case color-transfer
      "smpte2084" :std/pq-st2084
      "arib-std-b67" :std/hlg
      "bt709" :std/bt709
      "bt2020" :std/bt2020
      "linear" :std/linear
      "gamma22" :std/gamma22
      "gamma28" :std/gamma28
      nil)))

;; ========== MAIN DETECTION FUNCTION ==========

(defn detect-picture-profile
  "Detect picture profile from metadata based on camera type.
   Returns a namespaced keyword representing the detected profile."
  [metadata camera-type]
  (case camera-type
    :sony (or (detect-sony-profile metadata)
              (detect-standard-profile metadata))

    :canon (or (detect-canon-profile metadata)
               (detect-standard-profile metadata))

    :red (or (detect-red-profile metadata)
             (detect-standard-profile metadata))

    :panasonic (or (detect-panasonic-profile metadata)
                   (detect-standard-profile metadata))

    :dji (or (detect-dji-profile metadata)
             (detect-standard-profile metadata))

    :blackmagic (or (detect-blackmagic-profile metadata)
                    (detect-standard-profile metadata))

    ;; Unknown camera type - try standard profiles
    :unknown (detect-standard-profile metadata)

    ;; Default fallback
    nil))

;; ========== DISPLAY HELPERS ==========

(defn profile-display-name
  "Convert namespaced keyword back to human-readable display name."
  [profile-keyword]
  (case profile-keyword
    ;; Sony
    :sony/s-log "S-Log"
    :sony/s-log2 "S-Log2"
    :sony/s-log3 "S-Log3"
    :sony/s-log3-inferred "S-Log3 (inferred)"
    :sony/s-gamut "S-Gamut"
    :sony/s-gamut3 "S-Gamut3"
    :sony/s-gamut3-cine "S-Gamut3.Cine"
    :sony/rec709 "Rec.709"
    :sony/rec2020 "Rec.2020"

    ;; Canon
    :canon/log "Canon Log"
    :canon/log2 "Canon Log 2"
    :canon/log3 "Canon Log 3"
    :canon/cinema-gamut "Cinema Gamut"
    :canon/dci-p3 "DCI-P3"
    :canon/rec709 "Rec.709"
    :canon/rec2020 "Rec.2020"

    ;; RED
    :red/logfilm "REDLogFilm"
    :red/log3g10 "Log3G10"
    :red/gamma "RED Gamma"
    :red/widegamut "REDWideGamut"
    :red/dragon-color "Dragon Color"
    :red/rec709 "Rec.709"
    :red/rec2020 "Rec.2020"

    ;; Panasonic
    :panasonic/v-log "V-Log"
    :panasonic/v-log-l "V-Log L"
    :panasonic/v-gamut "V-Gamut"
    :panasonic/hlg "Hybrid Log Gamma"
    :panasonic/rec709 "Rec.709"
    :panasonic/rec2020 "Rec.2020"

    ;; DJI
    :dji/d-log "D-Log"
    :dji/d-cinelike "D-Cinelike"
    :dji/rec709 "Rec.709"
    :dji/normal "Normal"

    ;; Blackmagic
    :blackmagic/film "Blackmagic Film"
    :blackmagic/video "Blackmagic Video"
    :blackmagic/extended-video "Extended Video"
    :blackmagic/rec709 "Rec.709"
    :blackmagic/rec2020 "Rec.2020"

    ;; Standard
    :std/bt709 "Rec.709"
    :std/bt2020 "Rec.2020"
    :std/pq-st2084 "PQ (ST.2084)"
    :std/hlg "HLG"
    :std/linear "Linear"
    :std/gamma22 "Gamma 2.2"
    :std/gamma28 "Gamma 2.8"

    ;; Fallback
    (name profile-keyword)))

(comment

  ;;  Build me a sample call for this namespace


  ;;Keep from folding
  )