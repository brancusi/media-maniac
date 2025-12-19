(ns com.atd.mm.database.schema
  "Database schema definitions for the media management system")

(def media-schema
  "Complete schema for media management system including media files, hashes, locations, transfers, and destinations"
  [;; ========== MEDIA FILE ENTITY ==========
   ;; Core media file information
   {:db/ident       :media/id
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Unique identifier for a media file"}

   {:db/ident       :media/original-filename
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Original filename as found on SD card"}

   {:db/ident       :media/file-type
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc         "Type of media file: :image, :video, :audio, :raw"}

   {:db/ident       :media/size-bytes
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "File size in bytes"}

   {:db/ident       :media/created-date
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "Date the media was originally created (from EXIF/metadata)"}

   {:db/ident       :media/processed-date
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "Date the media was processed by this system"}

   ;; ========== HASH ENTITY ==========
   ;; Separate entity for hash information
   {:db/ident       :hash/algorithm
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc         "Hash algorithm used: :xxh3-64, :sha256, :md5"}

   {:db/ident       :hash/value
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Hex string of the hash value - unique across system"}

   {:db/ident       :hash/computed-date
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "When this hash was computed"}

   ;; Relationship: media -> hash
   {:db/ident       :media/hash
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Reference to the hash entity for this media"}

   ;; ========== LOCATION ENTITY ==========
   ;; Tracks where files are stored
   {:db/ident       :location/type
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc         "Type of location: :sd-card, :local-disk, :nas, :cloud"}

   {:db/ident       :location/path
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Full path to the file"}

   {:db/ident       :location/verified-date
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "Last date this location was verified"}

   {:db/ident       :location/is-valid
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc         "Whether this location is currently valid/accessible"}

   {:db/ident       :location/copy-date
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "When this copy was created"}

   {:db/ident       :location/verification-hash
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Hash computed during verification (for integrity checking)"}

   ;; Relationship: media -> locations (many copies)
   {:db/ident       :media/locations
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc         "All locations where this media file exists"}

   ;; ========== TRANSFER JOB ENTITY ==========
   ;; Tracks transfer operations
   {:db/ident       :transfer/id
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Unique identifier for a transfer job"}

   {:db/ident       :transfer/source-path
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Source path (usually SD card)"}

   {:db/ident       :transfer/started-date
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "When the transfer job started"}

   {:db/ident       :transfer/completed-date
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "When the transfer job completed"}

   {:db/ident       :transfer/status
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc         "Transfer status: :pending, :in-progress, :completed, :failed"}

   {:db/ident       :transfer/files-processed
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Number of files processed in this transfer"}

   {:db/ident       :transfer/bytes-transferred
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Total bytes transferred in this job"}

   ;; Relationship: transfer -> media files
   {:db/ident       :transfer/media-files
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc         "Media files processed in this transfer"}

   ;; ========== DESTINATION CONFIG ==========
   ;; Configuration for where files should be copied
   {:db/ident       :destination/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Name of the destination (e.g., 'main-nas', 'backup-drive')"}

   {:db/ident       :destination/base-path
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Base path for this destination"}

   {:db/ident       :destination/enabled
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc         "Whether this destination is currently enabled"}

   {:db/ident       :destination/priority
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Priority order for copying (1 = highest)"}

   {:db/ident       :destination/pattern
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Path pattern for organizing files (e.g., 'YYYY/MM/DD')"}

   ;; ========== METADATA ENTITY ==========
   ;; For storing EXIF and other metadata
   {:db/ident       :metadata/camera-make
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Camera manufacturer"}

   {:db/ident       :metadata/camera-model
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Camera model"}

   {:db/ident       :metadata/iso
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "ISO setting"}

   {:db/ident       :metadata/aperture
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one
    :db/doc         "Aperture f-stop value"}

   {:db/ident       :metadata/shutter-speed
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Shutter speed (e.g., '1/60')"}

   {:db/ident       :metadata/focal-length
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one
    :db/doc         "Focal length in mm"}

   {:db/ident       :metadata/gps-latitude
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one
    :db/doc         "GPS latitude"}

   {:db/ident       :metadata/gps-longitude
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one
    :db/doc         "GPS longitude"}

   {:db/ident       :metadata/width
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Image/video width in pixels"}

   {:db/ident       :metadata/height
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Image/video height in pixels"}

   {:db/ident       :metadata/duration
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one
    :db/doc         "Video duration in seconds"}

   ;; ========== FORMAT & CODEC METADATA ==========
   ;; Container format information
   {:db/ident       :metadata/container-format
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Container format (MP4, MOV, AVI, MKV, etc.)"}

   {:db/ident       :metadata/format-name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Full format name from ffprobe"}

   {:db/ident       :metadata/format-long-name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Long format description"}

   ;; Video codec information
   {:db/ident       :metadata/video-codec
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Video codec (h264, hevc, prores, etc.)"}

   {:db/ident       :metadata/video-codec-long-name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Full video codec description"}

   {:db/ident       :metadata/video-profile
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Video codec profile (High, Main, Baseline, etc.)"}

   {:db/ident       :metadata/video-level
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Video codec level"}

   {:db/ident       :metadata/video-bitrate
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Video bitrate in bits per second"}

   {:db/ident       :metadata/video-framerate
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Video frame rate (e.g., '23.976', '29.97', '60')"}

   ;; Audio codec information
   {:db/ident       :metadata/audio-codec
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Audio codec (aac, pcm_s16le, etc.)"}

   {:db/ident       :metadata/audio-codec-long-name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Full audio codec description"}

   {:db/ident       :metadata/audio-bitrate
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Audio bitrate in bits per second"}

   {:db/ident       :metadata/audio-sample-rate
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Audio sample rate in Hz"}

   {:db/ident       :metadata/audio-channels
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Number of audio channels"}

   ;; Color space and profile information
   {:db/ident       :metadata/color-space
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Color space (bt709, bt2020, smpte170m, etc.)"}

   {:db/ident       :metadata/color-transfer
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Color transfer function (bt709, smpte2084, arib-std-b67, etc.)"}

   {:db/ident       :metadata/color-primaries
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Color primaries (bt709, bt2020, etc.)"}

   {:db/ident       :metadata/color-range
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Color range (tv, pc, limited, full)"}

   {:db/ident       :metadata/picture-type
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Picture type for video frames"}

   ;; Log and cinema profiles
   {:db/ident       :metadata/gamma-curve
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Gamma curve or log profile (S-Log2, S-Log3, Log-C, etc.)"}

   {:db/ident       :metadata/cinema-profile
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Cinema profile or picture style"}

   ;; Pixel format and bit depth
   {:db/ident       :metadata/pixel-format
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Pixel format (yuv420p, yuv422p10le, etc.)"}

   {:db/ident       :metadata/bit-depth
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Bit depth per channel"}

   ;; Advanced metadata for professional formats
   {:db/ident       :metadata/timecode
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "SMPTE timecode if present"}

   {:db/ident       :metadata/creation-time
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Creation time from container metadata"}

   {:db/ident       :metadata/major-brand
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Major brand from MP4/MOV metadata"}

   {:db/ident       :metadata/compatible-brands
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc         "Compatible brands from MP4/MOV metadata"}

   ;; RAW format specifics
   {:db/ident       :metadata/raw-format
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "RAW format type (CR2, NEF, ARW, etc.)"}

   {:db/ident       :metadata/white-balance
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "White balance setting"}

   {:db/ident       :metadata/exposure-mode
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Exposure mode (manual, auto, etc.)"}

   {:db/ident       :metadata/metering-mode
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Metering mode"}

   ;; Additional technical metadata
   {:db/ident       :metadata/encoder
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Encoder used to create the file"}

   {:db/ident       :metadata/encoding-tool
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Tool used for encoding"}

   {:db/ident       :metadata/handler-name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Handler name from container"}

   ;; File format extensions for custom metadata
   {:db/ident       :metadata/custom-tags
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc         "Custom tags in key=value format"}

   ;; Relationship: media -> metadata
   {:db/ident       :media/metadata
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Metadata extracted from the media file"}])

(def sample-destinations
  "Sample destination configurations"
  [{:destination/name "main-nas"
    :destination/base-path "/volume1/Photos"
    :destination/enabled true
    :destination/priority 1
    :destination/pattern "YYYY/MM/DD"}

   {:destination/name "backup-drive"
    :destination/base-path "/Volumes/Backup/Media"
    :destination/enabled true
    :destination/priority 2
    :destination/pattern "YYYY/MM"}

   {:destination/name "local-archive"
    :destination/base-path "/Users/atd/Media/Archive"
    :destination/enabled false
    :destination/priority 3
    :destination/pattern "YYYY/MM/DD"}])

(defn get-schema
  "Returns the complete schema for transacting"
  []
  media-schema)

(defn get-sample-destinations
  "Returns sample destination configurations"
  []
  sample-destinations)
