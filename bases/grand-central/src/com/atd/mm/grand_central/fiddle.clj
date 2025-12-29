(ns com.atd.mm.grand-central.fiddle
  (:require [com.atd.mm.media-converter.interface :as mc]
            [com.atd.mm.grand-central.model.api :as api]))


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

(def basic-pipeline {:rules [{:xt/id "proxy-720"
                              :type :media/proxy
                              :opts {:size 720
                                     :name "_proxy_%name.%ext"
                                     :destination "./tmp/media"}}]})

(defn build-basic-pipeline
  [src]
  (-> basic-pipeline
      (assoc :src src)
      mc/prepare-pipeline))

(comment

  (let [prepped-pipeline (build-basic-pipeline "./tmp/sample.MP4")
        valid? (mc/pipeline-valid? prepped-pipeline)]

    (if valid?
      (api/create-pipeline prepped-pipeline)
      (tap> false)))
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   expected :xt/id or "_id" in doc
  ;;   
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   expected :xt/id or "_id" in doc
  ;;   
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   expected :xt/id or "_id" in doc
  ;;   


  ;;Keep from folding
  )


