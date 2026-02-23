(ns com.atd.mm.pipeline.step
  "Pipeline step CRUD and dependency resolution against XTDB."
  (:require
   [xtdb.api :as xt]))

(defn step-completed?
  [step]
  (= (:status step) :completed))

(defn all-step-deps-completed?
  "Check whether all dependencies of a step have :completed status."
  [step]
  (every? step-completed? (:deps step)))

(defn update-step
  "Patch a single pipeline step by ID."
  [xtdb-node id patch]
  (when-not (uuid? id)
    (throw (ex-info "You must use a uuid to update a step"
                    {:id id
                     :type (type id)})))
  (xt/submit-tx xtdb-node [(into [:patch-docs :pipeline-steps]
                                 [(merge patch {:xt/id id})])]))

(defn update-steps
  "Patch multiple pipeline steps."
  [xtdb-node steps]
  (xt/submit-tx xtdb-node [(into [:patch-docs :pipeline-steps] steps)]))

(defn get-all-steps
  [xtdb-node & {:keys [projection]}]
  (let [projection (or projection '[*])]
    (xt/q xtdb-node (list 'from :pipeline-steps projection))))

(defn get-step
  [xtdb-node id]
  (first (xt/q xtdb-node
               ['(fn [id]
                   (from :pipeline-steps [{:xt/id id} *]))
                id])))

(defn get-ready-steps
  "Return all :open steps whose dependencies are all :completed.
   Resolves dep UUIDs to their current status via XTQL pull*."
  [xtdb-node]
  (let [open-steps (xt/q xtdb-node
                         '(-> (from :pipeline-steps [{:status :open} *])
                              (with {:deps (pull* (fn [deps]
                                                    (->
                                                     (unify (from :pipeline-steps [{:xt/id dep-id} status])
                                                            (unnest {dep-id deps})))))})))]
    (filter all-step-deps-completed? open-steps)))

(defn delete-all-steps
  [xtdb-node]
  (let [step-ids (map :xt/id (get-all-steps xtdb-node :projection '[xt/id]))]
    (xt/submit-tx xtdb-node [(into [:delete-docs :pipeline-steps] step-ids)])))

;; ===== Status lifecycle helpers =====

(defn mark-step-processing!
  "Set a step's status to :processing."
  [xtdb-node id]
  (update-step xtdb-node id {:status :processing}))

(defn mark-step-completed!
  "Set a step's status to :completed and optionally store output data."
  [xtdb-node id & {:keys [output]}]
  (update-step xtdb-node id (cond-> {:status :completed}
                              output (assoc :output output))))

(defn mark-step-failed!
  "Set a step's status to :failed with error information."
  [xtdb-node id & {:keys [error]}]
  (update-step xtdb-node id (cond-> {:status :failed}
                              error (assoc :error error))))

;; ===== Delete =====

(defn delete-step
  [xtdb-node id]
  (let [step (get-step xtdb-node id)]
    (xt/submit-tx xtdb-node [(into [:delete-docs :pipeline-steps] [(:xt/id step)])])))
