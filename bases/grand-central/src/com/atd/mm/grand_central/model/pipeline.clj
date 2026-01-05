(ns com.atd.mm.grand-central.model.pipeline
  (:require
   [com.atd.mm.grand-central.resolver :as resolver]
   [com.atd.mm.grand-central.model.specs :as model-specs]
   [com.atd.mm.media-converter.interface :as media-converter]
   [com.rpl.specter :refer [ALL select]]
   [malli.core :as m]
   [malli.error :as me]
   [xtdb.api :as xt]))

(defn pipeline-valid?
  [pipeline]
  (m/validate model-specs/Pipeline pipeline))

(defn explain-invalid-pipeline
  [pipeline]
  (-> (m/explain model-specs/Pipeline pipeline)
      me/humanize))

(defn create-pipeline
  [pipeline & {:keys [xtdb-node]}]

  (when-not (pipeline-valid? pipeline)
    (throw (ex-info "Not a valid pipeline"
                    {:pipeline pipeline
                     :explanation (explain-invalid-pipeline pipeline)})))

  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        processes-tx-data (:processes pipeline)
        pipeline-tx-data {:xt/id (:xt/id pipeline)
                          :src (:src pipeline)}]
    (xt/submit-tx xtdb-node [(into [:put-docs :processes] processes-tx-data)
                             [:put-docs :pipelines pipeline-tx-data]])))

(defn get-all-pipelines
  [& {:keys [xtdb-node return]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))

        query (seq (cond-> ['->
                            '(from :pipelines [{:xt/id pipeline-id} *])
                            '(with {:processes (pull* (fn [pipeline-id]
                                                        (-> (from :processes [{:pipeline-id pipeline-id} *]))))})]
                     return (conj return)))]
    (xt/q xtdb-node query)))

(defn get-pipeline
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        query ['(fn [id]
                  (->
                   (from :pipelines [{:xt/id id} *])
                   (with {:processes (pull* [(fn [id]
                                               (-> (from :processes [{:pipeline-id id} *])))
                                             id])})))
               id]]
    (first (xt/q xtdb-node query))))

(defn delete-all-pipelines
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        pipelines (get-all-pipelines)
        pipeline-ids (map :xt/id pipelines)
        process-ids (select [ALL :processes ALL :xt/id] pipelines)]
    (xt/submit-tx xtdb-node [(into [:delete-docs :pipelines] pipeline-ids)
                             (into [:delete-docs :processes] process-ids)])))

(defn delete-pipeline
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        pipeline (get-pipeline id)
        process-ids (select [:processes ALL :xt/id] pipeline)]
    (xt/submit-tx xtdb-node [(into [:delete-docs :pipelines] [(:xt/id pipeline)])
                             (into [:delete-docs :processes] process-ids)])))

(comment

  (map :xt/id (get-all-pipelines))

  (get-pipeline #uuid "008d6589-add6-4be0-9188-242b444eeaf6")

  (delete-pipeline #uuid "03e1545e-f6d0-4b02-a1f2-b466f87a9394")

  (get-pipeline #uuid "03e1545e-f6d0-4b02-a1f2-b466f87a9394")

  (get-all-pipelines)

  (get-all-pipelines {:return '(return xt/id)})

  (delete-all-pipelines)

  (try
    (create-pipeline (media-converter/prepare-pipeline {:xt/id "basic-pipeline"
                                                        :src "hi"
                                                        :processes [{:xt/id "proxy-720"
                                                                     :status :open
                                                                     :type :media/proxy}
                                                                    {:xt/id "proxy-320"
                                                                     :status :open
                                                                     :type :media/proxy}
                                                                    {:xt/id "transcribe"
                                                                     :status :open
                                                                     :type :media/transcribe
                                                                     :deps ["proxy-320"]}]}))
    (catch clojure.lang.ExceptionInfo e
      (tap> {:message (ex-message e)
             :data (ex-data e)
             :exception e})))

  (explain-invalid-pipeline
   {:xt/id (java.util.UUID/randomUUID)
    :src "hi"
    :processes [{:xt/id (java.util.UUID/randomUUID)
                 :status :open
                 :type :media/proxy}]})

  (pipeline-valid?
   {:xt/id (java.util.UUID/randomUUID)
    :src "hi"
    :processes [{:hey 1}]})

  (media-converter/prepare-pipeline {:xt/id "basic-pipeline"
                                     :src "hi"
                                     :processes [{:xt/id "proxy-720"
                                                  :status :open
                                                  :type :media/proxy}
                                                 {:xt/id "proxy-320"
                                                  :status :open
                                                  :type :media/proxy}
                                                 {:xt/id "transcribe"
                                                  :status :open
                                                  :type :media/transcribe
                                                  :deps ["proxy-320"]}]})
  ;;=> {:xt/id #uuid "659685d0-b912-436c-ab5b-b4938524382b",
  ;;    :src "hi",
  ;;    :processes
  ;;    [{:xt/id #uuid "d242f45b-5c93-433f-97f8-c46facf9316b",
  ;;      :status :open,
  ;;      :type :media/proxy,
  ;;      :pipeline-id #uuid "659685d0-b912-436c-ab5b-b4938524382b"}
  ;;     {:xt/id #uuid "ee531a9d-b56f-4576-8763-6a33a41b14ab",
  ;;      :status :open,
  ;;      :type :media/proxy,
  ;;      :pipeline-id #uuid "659685d0-b912-436c-ab5b-b4938524382b"}
  ;;     {:xt/id #uuid "9795a638-2388-45d4-b5d5-5a2fcbbdabd5",
  ;;      :status :open,
  ;;      :type :media/transcribe,
  ;;      :deps [#uuid "ee531a9d-b56f-4576-8763-6a33a41b14ab"],
  ;;      :pipeline-id #uuid "659685d0-b912-436c-ab5b-b4938524382b"}]}
  ;;=> {:xt/id #uuid "0ab0f1c9-fd97-42f4-9ce2-207f7d217cb0",
  ;;    :src "hi",
  ;;    :processes
  ;;    [{:xt/id #uuid "460e4a38-e99f-4c2f-a163-7c7af1765e51",
  ;;      :status :open,
  ;;      :type :media/proxy,
  ;;      :pipeline-id #uuid "0ab0f1c9-fd97-42f4-9ce2-207f7d217cb0"}
  ;;     {:xt/id #uuid "8ee1a3cb-c6b2-4002-b807-402cb7edb99c",
  ;;      :status :open,
  ;;      :type :media/proxy,
  ;;      :pipeline-id #uuid "0ab0f1c9-fd97-42f4-9ce2-207f7d217cb0"}
  ;;     {:xt/id #uuid "64a136fd-1a7d-4694-802e-1aea1d6deafe",
  ;;      :status :open,
  ;;      :type :media/transcribe,
  ;;      :deps [#uuid "8ee1a3cb-c6b2-4002-b807-402cb7edb99c"],
  ;;      :pipeline-id #uuid "0ab0f1c9-fd97-42f4-9ce2-207f7d217cb0"}]}


  ;;Keep from folding
  )