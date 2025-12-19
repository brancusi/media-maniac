(ns com.atd.mm.core-utils.impl.file
  (:require [clojure.java.io :as io]
            [clojure.string :refer [split join]]))

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

(defn ensure-dir-exists!
  "Ensures that the directory portion of the file path exists.
   Create all missing dirs along the path"
  [file-path]
  (let [{:keys [dir]} (get-path-parts file-path)
        segments (split dir #"/")]
    (reduce (fn [acc cur]
              (let [prev-in-resource (io/file acc)
                    prev-resource-exists? (boolean prev-in-resource)
                    next-file-attempt (io/file prev-in-resource cur)
                    next-exists? (when next-file-attempt
                                   (.exists next-file-attempt))]
                (when (not next-exists?)
                  (when (and prev-resource-exists?
                             next-file-attempt)
                    (.mkdirs next-file-attempt)))
                (join "/" [acc cur])))
            segments)))

(defn path-exists?
  [file-path]
  (println (str "Checking if path exists: " file-path " " (io/resource file-path)))
  (boolean (io/resource file-path)))

(defn ensure-file-exists!
  "Ensures that the file exists. Create all missing dirs along the path and the file."
  [file-path]
  (let [{:keys [dir file-name]} (get-path-parts file-path)
        _ (ensure-dir-exists! file-path)
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

(defn spit-with-dirs [file-path content]
  (ensure-file-exists! file-path)
  (spit file-path content))