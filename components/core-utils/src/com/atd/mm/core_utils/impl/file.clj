(ns com.atd.mm.core-utils.impl.file
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :refer [split trim]])
  (:import
   [com.dynatrace.hash4j.file FileHashing]))

(def type-map {"image/jpg" "jpg"
               "image/jpeg" "jpg"
               "image/png" "png"
               "image/gif" "gif"})

(defn get-extention-by-type
  [type]
  (get type-map type))

(defn get-folder-path
  "Takes a file path and returns the folder portion, stripping the file name.
   If the file name pattern (a forward slash followed by any characters except a forward slash, a dot, and then any characters except a forward slash) is not found, it returns the original path.

   Example:
   (get-folder-path \"/path/to/your/file.txt\")
   => \"/path/to/your\"

   (get-folder-path \"/path/to/your/directory\")
   => \"/path/to/your/directory\""
  [path]
  (when path
    (if-let [matched (re-matches (re-pattern #"^(.*?)(\/[^\/]*\.[^\/]*)$") path)]
      (second matched)
      path)))

(defn get-path-parts
  "Parses a file path to return the directory and filename separately.
  If there is no file in the path, it returns the path as the directory.

  Examples:
  (get-path-parts \"/Users/atd/file.txt\")
  ;; => {:dir \"/Users/atd/\", :file-name \"file.txt\"}

  (get-path-parts \"/Users/atd/Documents/\")
  ;; => {:dir \"/Users/atd/Documents/\", :file-name nil}"

  [path]
  (let [pattern #"^(.*?\/)([^\/]*\.[^\/]*)?$"
        matched (re-matches pattern path)]
    (if matched
      {:dir (second matched) :file-name (nth matched 2)}
      {:dir path :file-name nil})))


(defn get-file-name
  "Extracts the file name without extension from a file path."
  [file-path]
  (let [{:keys [file-name]} (get-path-parts file-path)]
    (if file-name
      (first (split file-name #"\."))
      nil)))

(defn get-file-name-with-ext
  "Extracts the file name with extension from a file path.
   Returns nil if the path has no file component.

   Example:
   (get-file-name-with-ext \"/path/to/sample.MP4\")
   => \"sample.MP4\""
  [file-path]
  (:file-name (get-path-parts file-path)))

(defn path-exists?
  [file-path]
  (println (str "Checking if path exists: " file-path " " (io/resource file-path)))
  (boolean (io/resource file-path)))

(defn ensure-file-exists!
  "Ensures that the file exists. Create all missing dirs along the path and the file."
  [file-path]
  (let [{:keys [dir file-name]} (get-path-parts file-path)
        _ (io/make-parents file-path)
        file (io/file (io/resource file-path))]
    (if file
      file
      (spit (str (io/resource dir) "/" file-name) ""))))

(defn resource-exsists?
  [resource-path]
  (boolean (io/file (io/resource resource-path))))

(defn file-exsists?
  [file-path]
  (.exists (io/file file-path)))

(defn ensure-dir!
  "Ensures that a directory and all parent directories exist.
   Uses io/make-parents on a sentinel file to create the full path.
   Returns the directory path as a string."
  [dir-path]
  (let [dir (io/file dir-path)]
    (io/make-parents (io/file dir "."))
    (.mkdirs dir)
    (.getPath dir)))

(defn spit-with-dirs [file-path content]
  (ensure-file-exists! file-path)
  (spit file-path content))

(defn hash-file
  "Generates an imoHash (128-bit, sampling-based) for a file.
   Very fast — O(1) read regardless of file size.
   Good for dedup identity, not for integrity verification."
  [file]
  (let [hv (.hashFileTo128Bits
            (FileHashing/imohash1_0_2)
            (io/file file))]
    (format "%016x%016x"
            (.getMostSignificantBits hv)
            (.getLeastSignificantBits hv))))

(defn xxh3-128-file
  "Computes XXH3-128 hash for a local file using xxhsum CLI.
   Full-file hash — linear in file size but very fast (~2-4 GB/s).
   128-bit digest practically eliminates collision risk at scale.
   Good for integrity verification.
   Requires: `brew install xxhash` on macOS."
  [path]
  (let [{:keys [exit out err]} (sh/sh "xxhsum" "-H128" (str path))]
    (if (zero? exit)
      (let [hex (-> out trim (split #"\s+") first)]
        (if (clojure.string/blank? hex)
          (throw (ex-info "xxhsum returned empty output" {:stdout out :stderr err}))
          hex))
      (throw (ex-info "xxhsum failed" {:exit exit :stderr err :stdout out})))))

(comment

  (hash-file "/Users/atd/Desktop/tokyo.mp4")

  ;;Keep from folding
  )

(comment
  (require '[clojure.repl.deps :as deps])
  (deps/add-lib 'com.dynatrace.hash4j/hash4j)

;
  )