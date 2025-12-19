(ns com.atd.mm.media-converter.core
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.java.shell :as sh]
            [com.atd.mm.core-utils.interface :as cu]
            [clojure.java.io :as io])
  (:import (java.lang ProcessBuilder)))

(defn convert-resolution-to-file-name
  [str-resolution]
  (let [resolution (str/split str-resolution #":")
        width (first resolution)
        height (second resolution)]
    {:w (cu/force-int width)
     :h (cu/force-int height)}))

(comment

  (convert-resolution-to-file-name "1280:-1")
  ;;=> {:w 1280, :h -1}

  ;;Keep from folding
  )

(defn generate-proxy
  [input-file & {:keys [output-file resolution]
                 :or {resolution "720:-1"}}]
  (let [scale-filter (str "scale=" resolution)
        input-path (cu/get-folder-path input-file)
        output-path (cu/get-folder-path output-file)
        input-file-name (cu/get-file-name input-file)
        final-output-file (if output-file
                            output-file
                            (str input-path "/"
                                 input-file-name
                                 "_proxy_"
                                 (:w (convert-resolution-to-file-name resolution))
                                 ".mov"))

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
       :output-file output-file}
      {:success false
       :exit-code exit-code
       :error output})))

(comment
  (generate-proxy "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4")

  (cu/get-folder-path "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4")
  (cu/get-path-parts "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4")

  ;;Keep from folding
  )