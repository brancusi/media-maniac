(ns com.atd.mm.pipeline.lifecycle-test
  "Integration tests for the pipeline step dependency lifecycle.
   Uses a real (ephemeral, in-memory) XTDB node — no Redis required.
   Tests the full arc: create job → resolve ready steps →
   mark processing → mark completed → dependents become ready."
  (:require
   [clojure.test :refer :all]
   [com.atd.mm.database.test-helpers :as th]
   [com.atd.mm.pipeline.interface :as pipeline]
   [xtdb.api :as xt]))

;; -----------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------

(defn- make-linear-job
  "Create a simple 2-step job: step-a → step-b (b depends on a).
   Returns the prepared job map (not yet persisted)."
  []
  (pipeline/prepare-job
   {:asset-id (java.util.UUID/randomUUID)
    :steps [{:xt/id "step-a"
             :processor :media/proxy
             :opts {:destination "/tmp/test"}}
            {:xt/id "step-b"
             :processor :media/extract-stills
             :deps ["step-a"]
             :opts {:destination "/tmp/test"}}]}))

(defn- make-diamond-job
  "Create a diamond: root → [branch-a, branch-b] → join.
   Returns the prepared job map."
  []
  (pipeline/prepare-job
   {:asset-id (java.util.UUID/randomUUID)
    :steps [{:xt/id "root"
             :processor :media/copy}
            {:xt/id "branch-a"
             :processor :media/proxy
             :deps ["root"]
             :opts {:destination "/tmp/test"}}
            {:xt/id "branch-b"
             :processor :media/extract-stills
             :deps ["root"]
             :opts {:destination "/tmp/test"}}
            {:xt/id "join"
             :processor :media/transcribe
             :deps ["branch-a" "branch-b"]}]}))

(defn- step-by-processor
  "Find the first step in a job with the given :processor."
  [job processor]
  (first (filter #(= processor (:processor %)) (:steps job))))

;; -----------------------------------------------------------------
;; Tests
;; -----------------------------------------------------------------

(deftest linear-dependency-lifecycle
  (testing "Two-step linear pipeline: only step-a is ready initially,
            step-b becomes ready after step-a completes."
    (th/with-xtdb
      (fn [node]
        (let [job (make-linear-job)
              step-a (step-by-processor job :media/proxy)
              step-b (step-by-processor job :media/extract-stills)]

          ;; Persist the job
          (pipeline/create-job node job)

          ;; Initially only the root step (no deps) should be ready
          (let [ready (pipeline/get-ready-steps node)]
            (is (= 1 (count ready))
                "Only step-a (no deps) should be ready")
            (is (= (:xt/id step-a) (:xt/id (first ready)))))

          ;; Mark step-a as processing — it should no longer appear as ready
          (pipeline/mark-step-processing! node (:xt/id step-a))
          (is (empty? (pipeline/get-ready-steps node))
              "No steps ready while step-a is :processing")

          ;; Complete step-a — step-b should now be ready
          (pipeline/mark-step-completed! node (:xt/id step-a)
                                         :output {:asset-ids [(java.util.UUID/randomUUID)]})
          (let [ready (pipeline/get-ready-steps node)]
            (is (= 1 (count ready))
                "step-b should be ready after step-a completes")
            (is (= (:xt/id step-b) (:xt/id (first ready))))))))))

(deftest diamond-dependency-lifecycle
  (testing "Diamond graph: join step only becomes ready when both branches complete."
    (th/with-xtdb
      (fn [node]
        (let [job (make-diamond-job)
              root     (step-by-processor job :media/copy)
              branch-a (step-by-processor job :media/proxy)
              branch-b (step-by-processor job :media/extract-stills)
              join     (step-by-processor job :media/transcribe)]

          (pipeline/create-job node job)

          ;; Only root is ready
          (let [ready (pipeline/get-ready-steps node)]
            (is (= 1 (count ready)))
            (is (= (:xt/id root) (:xt/id (first ready)))))

          ;; Complete root → both branches become ready
          (pipeline/mark-step-completed! node (:xt/id root))
          (let [ready-ids (set (map :xt/id (pipeline/get-ready-steps node)))]
            (is (= 2 (count ready-ids)))
            (is (contains? ready-ids (:xt/id branch-a)))
            (is (contains? ready-ids (:xt/id branch-b))))

          ;; Complete only branch-a — join is NOT ready yet
          (pipeline/mark-step-completed! node (:xt/id branch-a))
          (let [ready-ids (set (map :xt/id (pipeline/get-ready-steps node)))]
            (is (contains? ready-ids (:xt/id branch-b))
                "branch-b still ready")
            (is (not (contains? ready-ids (:xt/id join)))
                "join NOT ready — branch-b still open"))

          ;; Complete branch-b → join becomes ready
          (pipeline/mark-step-completed! node (:xt/id branch-b))
          (let [ready (pipeline/get-ready-steps node)]
            (is (= 1 (count ready)))
            (is (= (:xt/id join) (:xt/id (first ready))))))))))

(deftest process-open-steps!--integration
  (testing "Full orchestration with real XTDB, stubbed Goose.
            Verifies steps get marked :processing and the right UUIDs
            are passed to queue-job."
    (th/with-xtdb
      (fn [node]
        (let [job (make-linear-job)
              step-a (step-by-processor job :media/proxy)
              queued (atom [])]

          (pipeline/create-job node job)

          ;; Stub only the Goose boundary
          (with-redefs [com.atd.mm.job-runner.interface/queue-job
                        (fn [handler args opts]
                          (swap! queued conj {:handler handler
                                              :args args
                                              :opts opts}))]

            (let [result (pipeline/process-open-steps! node :fake-producer)]
              (testing "one step queued (only step-a is ready)"
                (is (= 1 (:queued result))))

              (testing "step-a is now :processing in XTDB"
                (let [step (pipeline/get-step node (:xt/id step-a))]
                  (is (= :processing (:status step)))))

              (testing "queue-job received the step UUID"
                (is (= (:xt/id step-a) (:args (first @queued))))))))))))

(deftest failed-step-does-not-unblock-dependents
  (testing "If a step fails, its dependents never become ready."
    (th/with-xtdb
      (fn [node]
        (let [job (make-linear-job)
              step-a (step-by-processor job :media/proxy)]

          (pipeline/create-job node job)

          ;; Fail step-a
          (pipeline/mark-step-failed! node (:xt/id step-a)
                                      :error "ffmpeg crashed")

          ;; step-b should NOT be ready (dep is :failed, not :completed)
          (is (empty? (pipeline/get-ready-steps node))
              "No steps ready when dependency has failed"))))))
