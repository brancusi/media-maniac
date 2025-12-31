(ns com.atd.mm.grand-central.model.api
  (:require
   [com.atd.mm.grand-central.resolver :as resolver]
   [debug :as debug :refer [spy spy-when spy-present stash peek-stash]]
   [xtdb.api :as xt]))

(defn process-completed?
  [process]
  (= (:status process) :completed))

(defn all-process-deps-completed?
  [process]
  (every? process-completed? (:deps process)))

(defn update-processes
  [processes & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (xt/submit-tx xtdb-node [(into [:patch-docs :processes] processes)])))

(defn create-pipeline
  [pipeline & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        processes-tx-data (:rules pipeline)
        process-ids (mapv :xt/id processes-tx-data)
        pipeline-tx-data {:xt/id (:xt/id pipeline)
                          :src (:src pipeline)
                          :processes process-ids}]
    (xt/submit-tx xtdb-node [(into [:put-docs :processes] processes-tx-data)
                             [:put-docs :pipelines pipeline-tx-data]])))


(defn get-all-pipelines
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (xt/q xtdb-node '(from :pipelines [*]))))

(defn get-all-processes
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (xt/q xtdb-node '(from :processes [*]))))


(defn get-process-by-id
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (xt/q xtdb-node ['(fn [id]
                        (from :processes [{:xt/id id} *]))
                     id])))

(defn get-ready-to-process
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    xtdb-node)
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        open-processes (xt/q xtdb-node '(-> (from :processes [{:status :open} *])

                                            (with {:deps (pull* (fn [deps]
                                                                  (->
                                                                   (unify (from :processes [{:xt/id dep-id} status])
                                                                          (unnest {dep-id deps})))))})))]
    (filter all-process-deps-completed? open-processes)))


(comment

  (get-all-pipelines)

  (get-all-processes)

  (get-ready-to-process)

  (update-processes [{:xt/id 5
                      :something-else "Yeah"
                      :deps [1]
                      :status :completed}])

  (get-process-by-id 5)

  (create-pipeline [{:xt/id (java.util.UUID/randomUUID)
                     :status :open}])

  (into [:hey :other] [{:id 1}])

  ;;Keep from folding
  )