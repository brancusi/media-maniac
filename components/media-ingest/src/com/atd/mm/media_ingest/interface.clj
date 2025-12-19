(ns com.atd.mm.media-ingest.interface
  (:require [com.atd.mm.media-ingest.core :as core]))

;; Public API - delegates to core implementation

;; ========== PRIMARY METADATA EXTRACTION ==========
(defn get-comprehensive-file-info
  "Get comprehensive metadata for any media file automatically.
   This is the main function to use when ingesting files from any camera type.
   Automatically detects camera type and extracts all available metadata."
  [filepath]
  (core/get-comprehensive-file-info filepath))

;; ========== BASIC FUNCTIONS ==========
(defn xxh3-64-file-local
  "Returns XXH3-64 hex for a local file using xxhsum -H2.
   Requires: `brew install xxhash` on macOS."
  [path]
  (core/xxh3-64-file-local path))

(defn ffprobe-json
  "Run ffprobe and return parsed JSON (keyword keys).
   Returns {:error ...} map if ffprobe fails."
  [f]
  (core/ffprobe-json f))

(defn get-file-info
  "Get basic file information including metadata.
   For comprehensive metadata, use get-comprehensive-file-info instead."
  [f]
  (core/get-file-info f))

;; ========== DIRECTORY AND BATCH OPERATIONS ==========
(defn list-media
  "Return seq of absolute file paths in `dir` whose extension is in `exts` (a set of lowercase strings).
   Example: (list-media \"/Volumes/CARD/DCIM/100MSDCF\" #{\"mp4\" \"mov\"})"
  [dir exts]
  (core/list-media dir exts))

(defn analyze-sd-card
  "Analyze all media files on an SD card and return comprehensive metadata.
   Returns a map with :files (vector of file metadata) and :summary.
   Automatically detects camera types and formats."
  [sd-card-path]
  (core/analyze-sd-card sd-card-path))

;; ========== REMOTE OPERATIONS ==========
(defn xxh3-64-remote
  "SSH to NAS and compute XXH3-64 via Docker+xxhsum (-H2).
   Args:
     :identity -> SSH key on your Mac (e.g., \"/Users/atd/.ssh/id_ed25519_nas\")
     :user     -> NAS username (e.g., \"aramzadikian\")
     :host     -> NAS host or .local name
     :dir      -> NAS directory to mount (e.g., \"/volume1/MMM/2025/08/28/src/fx3\")
     :file     -> filename inside that directory (e.g., \"C1557.MP4\")
   Returns the hex digest string, or throws ex-info on error."
  [opts]
  (core/xxh3-64-remote opts))

;; ========== MAIN INGEST FUNCTION ==========
(defn ingest-media
  "Main function to ingest media files from a source directory"
  [target-dir]
  (core/ingest-media target-dir))

(comment

  (def sample-media "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4")
  (get-comprehensive-file-info sample-media)


  ;;Keep from folding
  )