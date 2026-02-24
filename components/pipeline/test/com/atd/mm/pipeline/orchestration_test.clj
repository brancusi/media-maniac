(ns com.atd.mm.pipeline.orchestration-test
  "Contract tests for pipeline → Goose orchestration.
   Pure tests — no XTDB or Redis required.
   Validates the shapes and contracts at the boundary between
   pipeline orchestration and the job-runner."
  (:require
   [clojure.test :refer :all]
   [com.atd.mm.pipeline.interface :as pipeline]))

;; =================================================================
;; Symbol resolution — catches namespace typos / missing requires
;; =================================================================

(deftest process-media-symbol-resolves
  (testing "The handler symbol passed to Goose actually resolves to a var"
    (let [sym 'com.atd.mm.media-processor.interface/process-media]
      (require (symbol (namespace sym)))
      (is (some? (resolve sym))
          "process-media symbol must resolve — if this fails, Goose jobs will silently drop"))))

;; =================================================================
;; process-open-steps! contract
;; =================================================================

(deftest process-open-steps!--queues-correct-shape
  (testing "Each ready step is marked :processing and enqueued with a UUID arg"
    (let [step-id-1 (java.util.UUID/randomUUID)
          step-id-2 (java.util.UUID/randomUUID)
          fake-steps [{:xt/id step-id-1 :status :open :processor :media/proxy}
                      {:xt/id step-id-2 :status :open :processor :media/extract-stills}]
          marked-ids (atom [])
          queued-calls (atom [])]

      ;; Stub step resolution and job-runner
      (with-redefs [com.atd.mm.pipeline.step/get-ready-steps
                    (fn [_xtdb-node] fake-steps)

                    com.atd.mm.pipeline.step/mark-step-processing!
                    (fn [_node id] (swap! marked-ids conj id))

                    com.atd.mm.job-runner.interface/queue-job
                    (fn [handler-fn fn-args opts]
                      (swap! queued-calls conj {:handler handler-fn
                                                :args fn-args
                                                :opts opts}))]

        (let [result (pipeline/process-open-steps! :fake-node :fake-producer)]
          (testing "returns count and IDs of queued steps"
            (is (= 2 (:queued result)))
            (is (= [step-id-1 step-id-2] (:step-ids result))))

          (testing "marks each step :processing before enqueueing"
            (is (= [step-id-1 step-id-2] @marked-ids)))

          (testing "enqueues one job per step"
            (is (= 2 (count @queued-calls))))

          (testing "handler is the fully-qualified process-media symbol"
            (is (every? #(= 'com.atd.mm.media-processor.interface/process-media
                            (:handler %))
                        @queued-calls)))

          (testing "each job arg is the step UUID (not a map, not a vector)"
            (is (= step-id-1 (:args (first @queued-calls))))
            (is (= step-id-2 (:args (second @queued-calls)))))

          (testing "queue defaults to heavy-process"
            (is (every? #(= "heavy-process" (get-in % [:opts :queue]))
                        @queued-calls))))))))

(deftest process-open-steps!--no-ready-steps
  (testing "Does nothing when no steps are ready"
    (with-redefs [com.atd.mm.pipeline.step/get-ready-steps
                  (fn [_] [])

                  com.atd.mm.job-runner.interface/queue-job
                  (fn [& _] (throw (ex-info "Should not be called" {})))]

      (let [result (pipeline/process-open-steps! :fake-node :fake-producer)]
        (is (= 0 (:queued result)))
        (is (empty? (:step-ids result)))))))

(deftest process-open-steps!--custom-queue
  (testing "Respects :queue override"
    (with-redefs [com.atd.mm.pipeline.step/get-ready-steps
                  (fn [_] [{:xt/id (java.util.UUID/randomUUID)
                            :status :open
                            :processor :media/copy}])

                  com.atd.mm.pipeline.step/mark-step-processing!
                  (fn [_ _] nil)

                  com.atd.mm.job-runner.interface/queue-job
                  (fn [_ _ opts]
                    (is (= "light-process" (:queue opts))))]

      (pipeline/process-open-steps! :fake-node :fake-producer
                                    :queue "light-process"))))

;; =================================================================
;; Goose lifecycle handlers
;; =================================================================

(deftest on-step-death--marks-step-failed
  (testing "Death handler marks step :failed in XTDB"
    (let [step-id (java.util.UUID/randomUUID)
          failed-calls (atom [])
          fake-job {:args [step-id]}
          fake-ex (Exception. "boom")]

      (with-redefs [com.atd.mm.pipeline.step/mark-step-failed!
                    (fn [_node id & {:keys [error]}]
                      (swap! failed-calls conj {:id id :error error}))]

        (pipeline/on-step-death {:xtdb-node :fake-node} fake-job fake-ex)

        (is (= 1 (count @failed-calls)))
        (is (= step-id (:id (first @failed-calls))))
        (is (string? (:error (first @failed-calls))))))))

(deftest on-step-death--handles-missing-context
  (testing "Death handler does not throw when xtdb-node is nil"
    (pipeline/on-step-death {} {:args [nil]} (Exception. "boom"))
    (is true "Handler completed without throwing"))

  (testing "Death handler does not throw when step-id is nil"
    (pipeline/on-step-death {:xtdb-node :fake} {:args []} (Exception. "boom"))
    (is true "Handler completed without throwing")))

(deftest on-step-error--does-not-throw
  (testing "Error handler (per-retry callback) runs without error"
    (let [step-id (java.util.UUID/randomUUID)]
      (is (some? (pipeline/on-step-error {} {:args [step-id] :retry-count 2}
                                         (Exception. "transient")))))))

;; =================================================================
;; Step status predicates (pure)
;; =================================================================

(deftest step-completed?--checks-status
  (is (true? (pipeline/step-completed? {:status :completed})))
  (is (false? (pipeline/step-completed? {:status :open})))
  (is (false? (pipeline/step-completed? {:status :processing})))
  (is (false? (pipeline/step-completed? {:status :failed}))))

(deftest all-step-deps-completed?--checks-all-deps
  (testing "All deps completed → true"
    (is (true? (pipeline/all-step-deps-completed?
                {:deps [{:status :completed}
                        {:status :completed}]}))))

  (testing "One dep not completed → false"
    (is (false? (pipeline/all-step-deps-completed?
                 {:deps [{:status :completed}
                         {:status :open}]}))))

  (testing "No deps → true (vacuously)"
    (is (true? (pipeline/all-step-deps-completed? {:deps []})))))
