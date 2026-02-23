(ns com.atd.mm.pipeline.asset
  "Asset specs and validation.

   An asset is anything the system produces or tracks — files on disk
   (proxies, stills, audio extracts) and informational data (metadata,
   transcripts).  Each asset type has its own Malli spec; validation
   dispatches on :asset/type via a simple registry lookup."
  (:require
   [malli.core :as m]
   [malli.error :as me]))

;; ===== Common keys (present on every asset) =====

(def asset-common-keys
  "Keys shared by all asset types."
  #{:xt/id :asset/type :asset/path
    :asset/size :asset/created :asset/imohash :asset/xxh3-128
    :asset/step-id :asset/job-id})

;; ===== Per-type specs =====

(def VideoAsset
  "Any video file — original camera footage, proxies, transcoded exports, subclips."
  [:map
   [:xt/id :uuid]
   [:asset/type [:= :asset/video]]
   [:asset/path :string]
   [:asset/created :any]
   [:asset/size {:optional true} :int]
   [:asset/imohash {:optional true} [:maybe :string]]
   [:asset/xxh3-128 {:optional true} [:maybe :string]]
   [:asset/step-id {:optional true} :uuid]
   [:asset/job-id {:optional true} :uuid]
   [:asset/width {:optional true} :int]
   [:asset/height {:optional true} :int]
   [:asset/duration {:optional true} :double]
   [:asset/codec {:optional true} :string]
   [:asset/frame-rate {:optional true} :string]])

(def ImageAsset
  "Any image — extracted stills, thumbnails, posters, camera JPEGs."
  [:map
   [:xt/id :uuid]
   [:asset/type [:= :asset/image]]
   [:asset/path :string]
   [:asset/created :any]
   [:asset/size {:optional true} :int]
   [:asset/imohash {:optional true} [:maybe :string]]
   [:asset/xxh3-128 {:optional true} [:maybe :string]]
   [:asset/step-id {:optional true} :uuid]
   [:asset/job-id {:optional true} :uuid]
   [:asset/width {:optional true} :int]
   [:asset/height {:optional true} :int]
   [:asset/frame-number {:optional true} :int]
   [:asset/timestamp {:optional true} :double]])

(def AudioAsset
  "Any audio file — extracted audio tracks, WAV exports, mixed audio."
  [:map
   [:xt/id :uuid]
   [:asset/type [:= :asset/audio]]
   [:asset/path :string]
   [:asset/created :any]
   [:asset/size {:optional true} :int]
   [:asset/imohash {:optional true} [:maybe :string]]
   [:asset/xxh3-128 {:optional true} [:maybe :string]]
   [:asset/step-id {:optional true} :uuid]
   [:asset/job-id {:optional true} :uuid]
   [:asset/duration {:optional true} :double]
   [:asset/codec {:optional true} :string]
   [:asset/sample-rate {:optional true} :int]
   [:asset/channels {:optional true} :int]])

(def MetadataAsset
  "Sidecar files — camera XML, BRAW metadata, EXIF dumps, EDL/AAF/XML timelines."
  [:map
   [:xt/id :uuid]
   [:asset/type [:= :asset/metadata]]
   [:asset/path :string]
   [:asset/created :any]
   [:asset/size {:optional true} :int]
   [:asset/imohash {:optional true} [:maybe :string]]
   [:asset/xxh3-128 {:optional true} [:maybe :string]]
   [:asset/step-id {:optional true} :uuid]
   [:asset/job-id {:optional true} :uuid]
   [:asset/format {:optional true} :string]])

(def TranscriptionAsset
  "Transcription outputs — SRT, VTT, plain text, Whisper JSON."
  [:map
   [:xt/id :uuid]
   [:asset/type [:= :asset/transcription]]
   [:asset/path :string]
   [:asset/created :any]
   [:asset/size {:optional true} :int]
   [:asset/imohash {:optional true} [:maybe :string]]
   [:asset/xxh3-128 {:optional true} [:maybe :string]]
   [:asset/step-id {:optional true} :uuid]
   [:asset/job-id {:optional true} :uuid]
   [:asset/format {:optional true} :string]
   [:asset/language {:optional true} :string]])

;; ===== Type → spec registry =====

(def asset-specs
  "Map of asset type keyword to its Malli spec."
  {:asset/video         VideoAsset
   :asset/image         ImageAsset
   :asset/audio         AudioAsset
   :asset/metadata      MetadataAsset
   :asset/transcription TranscriptionAsset})

;; ===== Validation =====

(defn validate-asset
  "Validate an asset map against the spec for its :asset/type.
   Returns the asset unchanged if valid.
   Throws ex-info with humanized errors if invalid or type unknown."
  [asset]
  (let [asset-type (:asset/type asset)
        spec       (get asset-specs asset-type)]
    (when-not spec
      (throw (ex-info "Unknown asset type"
                      {:asset/type asset-type
                       :known-types (keys asset-specs)})))
    (when-not (m/validate spec asset)
      (throw (ex-info "Invalid asset"
                      {:asset/type asset-type
                       :errors     (me/humanize (m/explain spec asset))})))
    asset))

(defn asset-valid?
  "Returns true if the asset conforms to its type spec, false otherwise."
  [asset]
  (let [spec (get asset-specs (:asset/type asset))]
    (and (some? spec) (m/validate spec asset))))

(defn type-specific-keys
  "Return only the type-specific keys from an asset, stripping common keys."
  [asset]
  (apply dissoc asset asset-common-keys))
