(ns com.atd.mm.pipeline.interface-test
  (:require [clojure.test :as test :refer :all]
            [com.atd.mm.pipeline.interface :as pipeline]))

;; =================================================================
;; Schema accessibility
;; =================================================================

(deftest schemas-are-accessible
  (testing "All pipeline schemas are exposed via interface"
    (is (some? pipeline/PipelineTemplate))
    (is (some? pipeline/PipelineJob))
    (is (some? pipeline/PipelineStep))
    (is (some? pipeline/StepDefinition))
    (is (some? pipeline/StepOutput))))

(deftest asset-schemas-are-accessible
  (testing "All asset schemas are exposed via interface"
    (is (some? pipeline/VideoAsset))
    (is (some? pipeline/ImageAsset))
    (is (some? pipeline/AudioAsset))
    (is (some? pipeline/MetadataAsset))
    (is (some? pipeline/TranscriptionAsset))
    (is (= 5 (count pipeline/asset-specs)))))

;; =================================================================
;; Asset validation — valid assets
;; =================================================================

(def ^:private valid-video
  {:xt/id          (java.util.UUID/randomUUID)
   :asset/type     :asset/video
   :asset/path     "/media/raw/clip001.mp4"
   :asset/created  (java.time.Instant/now)})

(def ^:private valid-image
  {:xt/id          (java.util.UUID/randomUUID)
   :asset/type     :asset/image
   :asset/path     "/media/frames/frame_001.jpg"
   :asset/created  (java.time.Instant/now)})

(def ^:private valid-audio
  {:xt/id          (java.util.UUID/randomUUID)
   :asset/type     :asset/audio
   :asset/path     "/media/audio/track.wav"
   :asset/created  (java.time.Instant/now)})

(def ^:private valid-metadata
  {:xt/id          (java.util.UUID/randomUUID)
   :asset/type     :asset/metadata
   :asset/path     "/media/sidecar/clip001.xml"
   :asset/created  (java.time.Instant/now)})

(def ^:private valid-transcription
  {:xt/id          (java.util.UUID/randomUUID)
   :asset/type     :asset/transcription
   :asset/path     "/media/transcripts/clip001.srt"
   :asset/created  (java.time.Instant/now)})

(deftest asset-valid?--accepts-minimal-valid-assets
  (testing "Video asset with only required keys"
    (is (true? (pipeline/asset-valid? valid-video))))

  (testing "Image asset with only required keys"
    (is (true? (pipeline/asset-valid? valid-image))))

  (testing "Audio asset with only required keys"
    (is (true? (pipeline/asset-valid? valid-audio))))

  (testing "Metadata asset with only required keys"
    (is (true? (pipeline/asset-valid? valid-metadata))))

  (testing "Transcription asset with only required keys"
    (is (true? (pipeline/asset-valid? valid-transcription)))))

(deftest asset-valid?--accepts-assets-with-optional-keys
  (testing "Video with dimensions, duration, codec"
    (is (true? (pipeline/asset-valid?
                (assoc valid-video
                       :asset/width 3840
                       :asset/height 2160
                       :asset/duration 120.5
                       :asset/codec "h264"
                       :asset/frame-rate "23.976")))))

  (testing "Image with dimensions and frame info"
    (is (true? (pipeline/asset-valid?
                (assoc valid-image
                       :asset/width 1920
                       :asset/height 1080
                       :asset/frame-number 42
                       :asset/timestamp 5.5)))))

  (testing "Audio with codec and channels"
    (is (true? (pipeline/asset-valid?
                (assoc valid-audio
                       :asset/duration 300.0
                       :asset/codec "pcm_s16le"
                       :asset/sample-rate 48000
                       :asset/channels 2)))))

  (testing "Metadata with format"
    (is (true? (pipeline/asset-valid?
                (assoc valid-metadata :asset/format "braw-sidecar")))))

  (testing "Transcription with format and language"
    (is (true? (pipeline/asset-valid?
                (assoc valid-transcription
                       :asset/format "srt"
                       :asset/language "en"))))))

(deftest asset-valid?--accepts-hash-fields
  (testing "Assets accept imohash and xxh3-128"
    (is (true? (pipeline/asset-valid?
                (assoc valid-video
                       :asset/imohash "abc123def456"
                       :asset/xxh3-128 "fedcba987654"
                       :asset/size 1048576))))))

(deftest asset-valid?--accepts-provenance-fields
  (testing "Assets accept step-id and job-id"
    (is (true? (pipeline/asset-valid?
                (assoc valid-video
                       :asset/step-id (java.util.UUID/randomUUID)
                       :asset/job-id (java.util.UUID/randomUUID)))))))

;; =================================================================
;; Asset validation — invalid assets
;; =================================================================

(deftest asset-valid?--rejects-missing-required-keys
  (testing "Missing :xt/id"
    (is (false? (pipeline/asset-valid? (dissoc valid-video :xt/id)))))

  (testing "Missing :asset/type"
    (is (false? (pipeline/asset-valid? (dissoc valid-video :asset/type)))))

  (testing "Missing :asset/path"
    (is (false? (pipeline/asset-valid? (dissoc valid-video :asset/path)))))

  (testing "Missing :asset/created"
    (is (false? (pipeline/asset-valid? (dissoc valid-video :asset/created))))))

(deftest asset-valid?--rejects-wrong-types
  (testing "String instead of UUID for :xt/id"
    (is (false? (pipeline/asset-valid? (assoc valid-video :xt/id "not-a-uuid")))))

  (testing "Number instead of string for :asset/path"
    (is (false? (pipeline/asset-valid? (assoc valid-video :asset/path 42))))))

(deftest asset-valid?--rejects-unknown-asset-type
  (testing "Unknown :asset/type returns false"
    (is (false? (pipeline/asset-valid?
                 {:xt/id (java.util.UUID/randomUUID)
                  :asset/type :asset/unknown
                  :asset/path "/some/file"
                  :asset/created (java.time.Instant/now)})))))

(deftest validate-asset--throws-on-invalid
  (testing "Throws ex-info on missing required field"
    (is (thrown? clojure.lang.ExceptionInfo
                 (pipeline/validate-asset (dissoc valid-video :asset/path)))))

  (testing "Throws ex-info on unknown asset type"
    (is (thrown? clojure.lang.ExceptionInfo
                 (pipeline/validate-asset
                  {:xt/id (java.util.UUID/randomUUID)
                   :asset/type :asset/bogus
                   :asset/path "/nope"
                   :asset/created (java.time.Instant/now)})))))

(deftest validate-asset--returns-asset-when-valid
  (testing "Returns the asset unchanged on success"
    (let [result (pipeline/validate-asset valid-video)]
      (is (= valid-video result)))))

;; =================================================================
;; Pipeline preparation (pure, no XTDB)
;; =================================================================

(deftest prepare-job--generates-uuids-and-stamps-status
  (let [asset-id (java.util.UUID/randomUUID)
        raw {:asset-id asset-id
             :steps [{:xt/id "proxy-720"
                      :processor :media/proxy
                      :opts {:resolution "720p"}}
                     {:xt/id "extract-frames"
                      :processor :media/extract-stills
                      :deps ["proxy-720"]}]}
        result (pipeline/prepare-job raw)]

    (testing "Job gets a UUID"
      (is (uuid? (:xt/id result))))

    (testing "Asset-id is preserved"
      (is (= asset-id (:asset-id result))))

    (testing "Steps receive UUIDs replacing string IDs"
      (is (every? uuid? (map :xt/id (:steps result)))))

    (testing "All steps are stamped :open"
      (is (every? #(= :open (:status %)) (:steps result))))

    (testing "All steps have pipeline-job-id"
      (is (every? #(= (:xt/id result) (:pipeline-job-id %))
                  (:steps result))))

    (testing "Dependency references are rewritten to UUIDs"
      (let [proxy-step   (first (:steps result))
            extract-step (second (:steps result))]
        (is (empty? (or (:deps proxy-step) [])))
        (is (= [(:xt/id proxy-step)] (:deps extract-step)))))))

(deftest prepare-job--handles-no-deps
  (let [raw {:asset-id (java.util.UUID/randomUUID)
             :steps [{:xt/id "solo-step"
                      :processor :media/copy}]}
        result (pipeline/prepare-job raw)]

    (testing "Single step with no deps works"
      (is (= 1 (count (:steps result))))
      (is (uuid? (:xt/id (first (:steps result))))))))

(deftest prepare-job--handles-diamond-dependencies
  (let [raw {:asset-id (java.util.UUID/randomUUID)
             :steps [{:xt/id "ingest"
                      :processor :media/copy}
                     {:xt/id "proxy"
                      :processor :media/proxy
                      :deps ["ingest"]}
                     {:xt/id "frames"
                      :processor :media/extract-stills
                      :deps ["ingest"]}
                     {:xt/id "transcode"
                      :processor :media/proxy
                      :deps ["proxy" "frames"]}]}
        result (pipeline/prepare-job raw)]

    (testing "Four steps produced"
      (is (= 4 (count (:steps result)))))

    (testing "Diamond dep step has two UUID deps"
      (let [last-step (last (:steps result))]
        (is (= 2 (count (:deps last-step))))
        (is (every? uuid? (:deps last-step)))))))

;; =================================================================
;; Schema validation
;; =================================================================

(deftest template-valid?--validates-structure
  (testing "Valid template accepted"
    (is (true? (pipeline/template-valid?
                {:name "Standard proxy + stills"
                 :steps [{:id "proxy" :processor :media/proxy}
                         {:id "stills" :processor :media/extract-stills :deps ["proxy"]}]}))))

  (testing "Missing :name rejected"
    (is (false? (pipeline/template-valid?
                 {:steps [{:id "proxy" :processor :media/proxy}]}))))

  (testing "Invalid processor keyword rejected"
    (is (false? (pipeline/template-valid?
                 {:name "Bad template"
                  :steps [{:id "x" :processor :media/nonexistent}]})))))

(deftest job-valid?--validates-prepared-jobs
  (let [job (pipeline/prepare-job
             {:asset-id (java.util.UUID/randomUUID)
              :steps [{:xt/id "proxy-720"
                       :processor :media/proxy}]})]
    (testing "Prepared job passes validation"
      (is (true? (pipeline/job-valid? job))))))
