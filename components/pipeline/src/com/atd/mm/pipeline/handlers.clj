(ns com.atd.mm.pipeline.handlers
  "Goose job lifecycle handlers for pipeline steps.
   These are fully-qualified symbols passed as :death-handler-fn-sym
   and :error-handler-fn-sym in Goose retry-opts."
  (:require [com.atd.mm.pipeline.step :as step]))

(defn on-step-death
  "Called by Goose when a pipeline step job exhausts all retries.
   Marks the step as :failed in XTDB.

   error-service-config — {:xtdb-node <node>} injected via worker opts
   job — the Goose job map (contains :args with [step-id])
   ex — the final exception"
  [error-service-config job ex]
  (try
    (let [xtdb-node (:xtdb-node error-service-config)
          step-id   (first (:args job))]
      (if (and xtdb-node step-id)
        (do
          (step/mark-step-failed! xtdb-node step-id :error (str ex))
          (tap> {:event :step-death
                 :step-id step-id
                 :error (str ex)}))
        (tap> {:event :step-death-missing-context
               :has-node? (some? xtdb-node)
               :has-step-id? (some? step-id)
               :error (str ex)})))
    (catch Exception e
      (tap> {:event :step-death-handler-error
             :error (str e)}))))

(defn on-step-error
  "Called by Goose on each failed attempt (before retry).
   Logs the error for observability but does not change step status —
   the step stays :processing until retries are exhausted."
  [error-service-config job ex]
  (let [step-id (first (:args job))]
    (tap> {:event :step-error
           :step-id step-id
           :retry-count (:retry-count job)
           :error (str ex)})))
