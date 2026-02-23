(ns fiddle.fiddle-1
  (:require [com.atd.mm.pipeline.interface :as pipeline]
            [com.atd.mm.grand-central.resolver :as resolver]
            [com.atd.mm.job-runner.interface :as job-runner]))

(def rules [{:id "proxy-720"
             :processor :media/proxy
             :opts {:size 720
                    :name "_proxy_%name.%ext"}}
            {:id "extract-stills"
             :deps ["proxy-720"]
             :processor :media/extract-stills
             :opts {:frequency 10
                    :size 320
                    :quality 1
                    :type "jpg"
                    :name "%name_%frame.%ext"}}
            {:id "extract-audio"
             :processor :media/extract-audio
             :opts {:encoding "wav"
                    :bitrate 42000
                    :dest "/audio"
                    :name "%name_.%ext"}}
            {:id "transcribe"
             :deps ["extract-audio" "proxy-720"]
             :processor :media/transcribe
             :opts {:model :whisper}}
            {:id "copy"
             :processor :media/copy
             :opts {:dest "/here"}}])

(def basic-pipeline {:steps [{:xt/id "proxy-720"
                              :processor :media/proxy
                              :opts {:size 720
                                     :name "_proxy_%name.%ext"
                                     :destination "./tmp/media"}}]})

(defn build-basic-pipeline
  [asset-id]
  (-> basic-pipeline
      (assoc :asset-id asset-id)
      pipeline/prepare-job))

(comment

  (build-basic-pipeline "./tmp/sample.MP4")

  (let [prepped (build-basic-pipeline "./tmp/sample.MP4")
        valid? (pipeline/job-valid? prepped)]

    (if valid?
      (pipeline/create-job (resolver/get-xtdb-node) prepped)
      (tap> false)))

  ;;Keep from folding
  )

(comment
  (require '[clojure.repl.deps :as deps])
  (deps/add-lib 'com.phronemophobic/clj-graphviz)

;
  )


