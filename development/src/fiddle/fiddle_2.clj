(ns fiddle.fiddle-2
  (:require [com.atd.mm.core-utils.interface :as cu]
            [com.atd.mm.grand-central.resolver :as resolver]
            [com.atd.mm.job-runner.interface :as job-runner]
            [com.atd.mm.pipeline.interface :as pipeline]
            [xtdb.api :as xt]))

;; ---------- helpers ----------

(defn timestamp-str
  "Compact timestamp for directory names, e.g. \"20260222-143052\"."
  []
  (.format (java.time.LocalDateTime/now)
           (java.time.format.DateTimeFormatter/ofPattern "yyyyMMdd-HHmmss")))

;; ---------- retry opts (shared across workflows) ----------

(def retry-opts
  {:max-retries          3
   :death-handler-fn-sym 'com.atd.mm.pipeline.handlers/on-step-death
   :error-handler-fn-sym 'com.atd.mm.pipeline.handlers/on-step-error})

;; ===================================================================
;; Create a source video asset — run this first to register the MP4
;; ===================================================================

(comment

  (def node (resolver/get-xtdb-node))

  ;; ---- Create a video asset for the sample MP4 ----

  (def sample-path "/Users/atd/Documents/projects/media-maniac/refs/sample.MP4")

  (def source-asset
    {:xt/id                  (java.util.UUID/randomUUID)
     :asset/type             :asset/video
     :asset/path             sample-path
     :asset/created          (java.time.Instant/now)})


  ;; Validate against the spec
  (pipeline/asset-valid? source-asset)

  ;; Persist to XTDB
  (xt/submit-tx node [[:put-docs :assets source-asset]])

  ;; Grab the asset-id for use in pipelines below
  (def sample-asset-id (:xt/id source-asset))
  (tap> {:source-asset source-asset})

  ;;
  )

;; ===================================================================
;; Inline pipeline — define steps directly, prepare, persist, process
;; ===================================================================

(comment

  ;; ---- 1. Output dirs ----

  (def ts (timestamp-str))
  (def proxy-dir  (cu/ensure-dir! (str "./tmp/pipeline-run/" ts "/proxy")))
  (def frames-dir (cu/ensure-dir! (str "./tmp/pipeline-run/" ts "/frames")))

  ;; ---- 2. Prepare & validate ----
  ;; Uses sample-asset-id from the asset-creation block above

  (def raw-pipeline
    {:asset-id sample-asset-id
     :steps [{:xt/id  "proxy-720"
              :status :open
              :processor :media/proxy
              :opts      {:size 720
                          :name "_proxy_%name.%ext"
                          :destination proxy-dir}}

             #_{:xt/id  "extract-stills"
                :status :open
                :processor :media/extract-stills
                :opts   {:frequency 10
                         :destination frames-dir}
                :deps   ["proxy-720"]}]})


  (def prepared (pipeline/prepare-job raw-pipeline))

  (pipeline/job-valid? prepared)

  ;; ---- 4. Persist to XTDB ----

  (def node (resolver/get-xtdb-node))

  (pipeline/create-job node prepared)


  ;; ---- 5. Verify ----

  (tap> (pipeline/get-job node (:xt/id prepared)))
  (tap> {:ready-steps (pipeline/get-ready-steps node)})
  (tap> {:get-all-steps (pipeline/get-all-steps node)})

  ;; ---- 6. Process open steps ----
  ;; Marks ready steps :processing, queues each step UUID to Goose.
  ;; Workers resolve step + job data from XTDB at execution time.

  (pipeline/process-open-steps!
   node (resolver/get-producer) :retry-opts retry-opts)

  ;; ---- 7. Inspect ----

  (tap> (job-runner/get-all-jobs))
  (tap> (pipeline/get-all-jobs node))

  ;; ---- cleanup ----

  (pipeline/delete-job node (:xt/id prepared))
  (pipeline/delete-all-jobs node)
  (job-runner/clear-all-jobs)

  ;; ---- 5. Verify ----


  ;;
  )

;; ===================================================================
;; Template-based — store a template once, create jobs from it
;; ===================================================================

(comment

  (def node (resolver/get-xtdb-node))

  ;; ---- 1. Store template ----

  (pipeline/create-template node
                            {:name        "proxy-and-stills"
                             :description "720p ProRes proxy + frame extraction every 10s"
                             :steps [{:id   "proxy-720"
                                      :processor :media/proxy
                                      :opts {:size 720
                                             :name "_proxy_%name.%ext"}}

                                     {:id   "extract-stills"
                                      :processor :media/extract-stills
                                      :opts {:frequency 10}
                                      :deps ["proxy-720"]}]})

  ;; ---- 2. Get template ID ----

  (def template-id (:xt/id (first (pipeline/get-all-templates node))))

  ;; ---- 3. Create job from template ----

  ;; Uses sample-asset-id from the asset-creation block above
  (def prepared
    (pipeline/create-job-from-template
     node template-id
     sample-asset-id))

  ;; ---- 4. Validate & persist ----

  (pipeline/job-valid? prepared)
  (pipeline/create-job node prepared)

  ;; ---- 5. Process open steps ----

  (tap> {:ready-steps (pipeline/get-ready-steps node)})

  (pipeline/process-open-steps!
   node (resolver/get-producer) :retry-opts retry-opts)

  ;; ---- cleanup ----

  (pipeline/delete-job node (:xt/id prepared))
  (pipeline/delete-all-templates node)

  ;;
  )
