(ns com.atd.mm.grand-central.model.process
  (:require
   [com.atd.mm.grand-central.resolver :as resolver]
   [xtdb.api :as xt]))

(defn process-completed?
  [process]
  (= (:status process) :completed))

(defn all-process-deps-completed?
  [process]
  (every? process-completed? (:deps process)))

(defn update-process
  [id patch & {:keys [xtdb-node]}]
  (when-not (uuid? id)
    (throw (ex-info "You must use a uuid to update a process"
                    {:id id
                     :type (type id)})))
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (xt/submit-tx xtdb-node [(into [:patch-docs :processes] [(merge patch {:xt/id id})])])))

(defn update-processes
  [processes & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    (xt/submit-tx xtdb-node [(into [:patch-docs :processes] processes)])))

(defn get-all-processes
  [& {:keys [xtdb-node projection]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        projection (or projection '[*])]
    (xt/q xtdb-node (list 'from :processes projection))))

(defn get-process
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        query ['(fn [id]
                  (from :processes [{:xt/id id} *]))
               id]]
    (first (xt/q xtdb-node query))))

(defn get-ready-to-process
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        open-processes (xt/q xtdb-node '(-> (from :processes [{:status :open} *])
                                            (with {:deps (pull* (fn [deps]
                                                                  (->
                                                                   (unify (from :processes [{:xt/id dep-id} status])
                                                                          (unnest {dep-id deps})))))})))]
    (filter all-process-deps-completed? open-processes)))

(defn delete-all-processes
  [& {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        process-ids (map :xt/id (get-all-processes {:projection '[xt/id]}))]
    (xt/submit-tx xtdb-node [(into [:delete-docs :processes] process-ids)])))

(defn delete-process
  [id & {:keys [xtdb-node]}]
  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))
        process (get-process id)] 1
       (xt/submit-tx xtdb-node [(into [:delete-docs :processes] [(:xt/id process)])])))


(comment

  (delete-process #uuid "80521d21-693a-4dec-8629-c36f8f894912")

  (get-process #uuid "d2c91d3c-abb5-44f8-b7e9-69c0287c80d1")

  (get-all-processes)

  (get-all-processes {:projection '[xt/id]})

  (delete-all-processes)

  (get-ready-to-process)

  (update-processes [{:xt/id 5
                      :something-else "Yeah"
                      :deps [1]
                      :status :completed}])

  (update-process #uuid "fb5ae948-abff-4eaf-b2bd-3c2a05dfde35" {:status :open})

  ;;Keep from folding
  )