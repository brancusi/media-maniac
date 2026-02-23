(ns com.atd.mm.pipeline.job
  "Pipeline job CRUD against XTDB.
   A pipeline job is stored across two tables:
   - :pipeline-jobs   — the job record (id, asset-id)
   - :pipeline-steps  — individual steps with deps and status"
  (:require
   [com.atd.mm.pipeline.specs :as specs]
   [com.rpl.specter :refer [ALL select]]
   [malli.core :as m]
   [malli.error :as me]
   [xtdb.api :as xt]))

(defn job-valid?
  [job]
  (m/validate specs/PipelineJob job))

(defn explain-invalid-job
  [job]
  (-> (m/explain specs/PipelineJob job)
      me/humanize))

(defn create-job
  "Persist a prepared pipeline job to XTDB.
   Splits the job into :pipeline-jobs and :pipeline-steps tables atomically."
  [xtdb-node job]
  (when-not (job-valid? job)
    (throw (ex-info "Not a valid pipeline job"
                    {:job job
                     :explanation (explain-invalid-job job)})))
  (let [steps-tx-data (:steps job)
        job-tx-data {:xt/id (:xt/id job)
                     :asset-id (:asset-id job)}]
    (xt/submit-tx xtdb-node [(into [:put-docs :pipeline-steps] steps-tx-data)
                             [:put-docs :pipeline-jobs job-tx-data]])))

(defn get-all-jobs
  [xtdb-node & {:keys [return]}]
  (let [query (seq (cond-> ['->
                            '(from :pipeline-jobs [{:xt/id job-id} *])
                            '(with {:steps (pull* (fn [job-id]
                                                    (-> (from :pipeline-steps [{:pipeline-job-id job-id} *]))))})]
                     return (conj return)))]
    (xt/q xtdb-node query)))

(defn get-job
  [xtdb-node id]
  (first (xt/q xtdb-node
               ['(fn [id]
                   (->
                    (from :pipeline-jobs [{:xt/id id} *])
                    (with {:steps (pull* [(fn [id]
                                            (-> (from :pipeline-steps [{:pipeline-job-id id} *])))
                                          id])})))
                id])))

(defn delete-all-jobs
  [xtdb-node]
  (let [jobs (get-all-jobs xtdb-node)
        job-ids (map :xt/id jobs)
        step-ids (select [ALL :steps ALL :xt/id] jobs)]
    (xt/submit-tx xtdb-node [(into [:delete-docs :pipeline-jobs] job-ids)
                             (into [:delete-docs :pipeline-steps] step-ids)])))

(defn delete-job
  [xtdb-node id]
  (let [job (get-job xtdb-node id)
        step-ids (select [:steps ALL :xt/id] job)]
    (xt/submit-tx xtdb-node [(into [:delete-docs :pipeline-jobs] [(:xt/id job)])
                             (into [:delete-docs :pipeline-steps] step-ids)])))
