(ns com.atd.mm.pipeline.prepare
  "Transforms a raw pipeline definition (with string IDs) into
   XTDB-ready data with UUIDs and execution metadata."
  (:require
   [com.atd.mm.pipeline.template :as template]
   [com.rpl.specter :refer [ALL filterer select setval]]))

(defn- update-step-deps
  "Replace all occurrences of old-id with new-id in :deps vectors."
  [steps old-id new-id]
  (setval [(filterer #(:deps %))
           ALL
           :deps
           (filterer #(= % old-id))
           ALL]
          new-id
          steps))

(defn prepare-job
  "Prepare a raw pipeline definition for persistence:
   1. Generate a pipeline-job UUID
   2. Replace string step IDs with UUIDs (including in :deps)
   3. Stamp each step with :status :open and :pipeline-job-id

   Input:  {:asset-id <uuid> :steps [{:xt/id \" proxy-720 \" :processor :media/proxy ...}]}
   Output: {:asset-id <uuid> :xt/id <uuid> :steps [{:xt/id <uuid> :pipeline-job-id <uuid> :status :open ...}]}"
  [data]
  (let [job-id (java.util.UUID/randomUUID)
        steps (:steps data)
        string-ids (select [ALL :xt/id] steps)

        ;; Replace string IDs with UUIDs, updating dep references
        steps-with-uuids (reduce (fn [acc old-id]
                                   (let [new-id (java.util.UUID/randomUUID)
                                         updated (setval [(filterer #(= (:xt/id %) old-id))
                                                          ALL :xt/id]
                                                         new-id
                                                         acc)]
                                     (update-step-deps updated old-id new-id)))
                                 steps
                                 string-ids)

        ;; Augment each step with execution metadata
        steps-ready (->> steps-with-uuids
                         (mapv #(assoc % :status :open))
                         (mapv #(assoc % :pipeline-job-id job-id)))]

    (assoc data
           :steps steps-ready
           :xt/id job-id)))

(defn- template-step->raw-step
  "Convert a PipelineTemplate StepDefinition (:id key) to the format
   expected by prepare-job (:xt/id key)."
  [step-def]
  (-> step-def
      (assoc :xt/id (:id step-def))
      (dissoc :id)))

(defn create-job-from-template
  "Create a pipeline job from a stored template.
   Fetches the template from XTDB, converts its steps, and runs prepare-job.

   Returns a prepared PipelineJob ready for `create-job`.
   Throws if template-id is not found."
  [xtdb-node template-id asset-id]
  (let [template (template/get-template xtdb-node template-id)
        _ (when-not template
            (throw (ex-info "Pipeline template not found"
                            {:template-id template-id})))
        raw-steps (mapv template-step->raw-step (:steps template))]
    (prepare-job {:asset-id asset-id
                  :template-id (:xt/id template)
                  :steps raw-steps})))
