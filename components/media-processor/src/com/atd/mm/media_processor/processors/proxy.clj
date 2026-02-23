(ns com.atd.mm.media-processor.processors.proxy
  "Proxy video processor — generates a lower-resolution ProRes proxy
   from a source video file.

   Resolves the parent job's :asset-id via the store at execution time.
   Validates the resolved input against ProxyInput before processing."
  (:require
   [clojure.string :as str]
   [com.atd.mm.core-utils.interface :as cu]
   [com.atd.mm.media-processor.processors.core :refer [execute-process]]
   [com.atd.mm.media-processor.ffmpeg :as ffmpeg]
   [com.atd.mm.media-processor.store :as store]
   [malli.core :as m]
   [malli.error :as me]))

;; ===== Specs =====

(def ProxyOpts
  "Options the proxy step expects in its :opts map."
  [:map
   [:destination :string]
   [:size {:optional true} :int]
   [:name {:optional true} :string]])

(def ProxyInput
  "Resolved input for the proxy processor — step data + :asset-id from parent job."
  [:map
   [:xt/id :uuid]
   [:pipeline-job-id :uuid]
   [:processor [:= :media/proxy]]
   [:asset-id :uuid]
   [:opts ProxyOpts]])

;; ===== Data resolution =====

(defn- resolve-input
  "Build a validated proxy input map from the step + parent job."
  [step]
  (let [asset-id (store/get-job-asset-id (:pipeline-job-id step))
        input    (assoc step :asset-id asset-id)]
    (when-not (m/validate ProxyInput input)
      (throw (ex-info "Invalid proxy input"
                      {:step-id (:xt/id step)
                       :errors  (me/humanize (m/explain ProxyInput input))})))
    input))

;; ===== Helpers =====

(defn- resolve-output-path
  "Build the proxy output file path from step opts and source file."
  [src {:keys [name destination]}]
  (let [src-name (cu/get-file-name src)
        output-name (-> (or name "_proxy_%name.mov")
                        (str/replace "%name" src-name)
                        (str/replace "%ext" "mov"))]
    (str destination "/" output-name)))

;; ===== Processor =====

(defmethod execute-process :media/proxy [step]
  (let [step-id (:xt/id step)]
    (try
      (let [{:keys [asset-id opts] :as input} (resolve-input step)
            asset       (store/get-asset asset-id)
            src         (:asset/path asset)
            size        (or (:size opts) 720)
            resolution  (str size ":-1")
            _           (cu/ensure-dir! (:destination opts))
            output-file (resolve-output-path src opts)]

        (tap> {:event :proxy-start :step-id step-id :asset-id asset-id :src src})

        (let [result (ffmpeg/generate-proxy src
                                            :output-file output-file
                                            :resolution resolution)]
          (if (:success result)
            (do
              (store/mark-step-completed! step-id
                                          {:output-file (:output-file result)})
              (tap> {:event :proxy-completed :step-id step-id :output (:output-file result)})
              result)
            (let [err-msg "Proxy generation failed"]
              (store/mark-step-failed! step-id
                                       (str err-msg ": " (:error result)))
              (throw (ex-info err-msg
                              {:step-id step-id
                               :src     src
                               :result  result}))))))
      (catch Exception e
        ;; Only mark failed if still :processing (avoid double-write
        ;; when the throw above already marked it)
        (when (= :processing (store/get-step-status step-id))
          (store/mark-step-failed! step-id (str e)))
        (throw e)))))
