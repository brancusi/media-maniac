(ns com.atd.mm.media-processor.processors.extract-stills
  (:require
   [com.atd.mm.core-utils.interface :as cu]
   [com.atd.mm.media-processor.processors.core :refer [execute-process]]
   [com.atd.mm.media-processor.ffmpeg :as ffmpeg]
   [com.atd.mm.media-processor.store :as store]))

(defmethod execute-process :media/extract-stills [{:keys [opts] :as step}]
  (let [asset-id  (store/get-job-asset-id (:pipeline-job-id step))
        asset     (store/get-asset asset-id)
        src       (:asset/path asset)]
    (tap> {:event :extract-stills-start :step-id (:xt/id step) :asset-id asset-id :src src})
    (let [frequency   (or (:frequency opts) 10)
          destination (:destination opts)
          _           (when destination (cu/ensure-dir! destination))
          base-name   (cu/get-file-name src)
          result      (ffmpeg/extract-frames-from-video src
                                                        :output-dir destination
                                                        :frequency-seconds frequency
                                                        :base-name base-name)]
      (when-not (:success result)
        (throw (ex-info "Frame extraction failed"
                        {:step-id (:xt/id step)
                         :src src
                         :result result})))
      (tap> {:event :extract-stills-done
             :step-id (:xt/id step)
             :total-frames (:total-frames result)})
      result)))
