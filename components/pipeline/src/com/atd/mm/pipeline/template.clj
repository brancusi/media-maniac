(ns com.atd.mm.pipeline.template
  "Pipeline template CRUD against XTDB."
  (:require
   [com.atd.mm.pipeline.specs :as specs]
   [malli.core :as m]
   [malli.error :as me]
   [xtdb.api :as xt]))

(defn template-valid?
  [template]
  (m/validate specs/PipelineTemplate template))

(defn explain-invalid-template
  [template]
  (-> (m/explain specs/PipelineTemplate template)
      me/humanize))

(defn create-template
  "Store a pipeline template in XTDB. Assigns a UUID if :xt/id is missing."
  [xtdb-node template]
  (when-not (template-valid? template)
    (throw (ex-info "Not a valid pipeline template"
                    {:template template
                     :explanation (explain-invalid-template template)})))
  (let [template (if (:xt/id template)
                   template
                   (assoc template :xt/id (java.util.UUID/randomUUID)))]
    (xt/submit-tx xtdb-node [[:put-docs :pipeline-templates template]])))

(defn get-template
  [xtdb-node id]
  (first (xt/q xtdb-node
               ['(fn [id]
                   (from :pipeline-templates [{:xt/id id} *]))
                id])))

(defn get-all-templates
  [xtdb-node]
  (xt/q xtdb-node '(from :pipeline-templates [*])))

(defn delete-template
  [xtdb-node id]
  (xt/submit-tx xtdb-node [[:delete-docs :pipeline-templates id]]))

(defn delete-all-templates
  [xtdb-node]
  (let [ids (map :xt/id (get-all-templates xtdb-node))]
    (xt/submit-tx xtdb-node [(into [:delete-docs :pipeline-templates] ids)])))
