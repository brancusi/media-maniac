(ns com.atd.mm.grand-central.resolver
  (:require [xtdb.api :as xt]))

(defonce rs nil)

(defn get-instances
  []
  (-> rs :donut.system/instances))

(defn get-http-client
  []
  (-> (get-instances) :http-client))

(defn get-config
  []
  (-> (get-instances) :system :config))

(defn get-xtdb-node
  []
  (-> (get-instances) :database :node))

(def q (partial xt/q (get-xtdb-node)))

(defn process-completed?
  [process]
  (= (:status process) :completed))

(defn all-process-deps-completed?
  [process]
  (every? process-completed? (:deps process)))

(defn get-ready-to-process
  []
  (let [open-processes (q '(-> (from :processes [{:status :open} *])

                               (with {:deps (pull* (fn [deps]
                                                     (->
                                                      (unify (from :processes [{:xt/id dep-id} status])
                                                             (unnest {dep-id deps})))))})))]
    (filter all-process-deps-completed? open-processes)))


(comment

  (every? process-completed? [{:status :completed}])

  (get-ready-to-process)
  ;;=> ({:xt/id 5, :status :open} {:deps [{:dep-id 1, :status :completed}], :xt/id 4, :status :open} {:xt/id 3, :status :open})
  ;;=> ({:xt/id 5, :status :open} {:deps [{:dep-id 1, :status :completed}], :xt/id 4, :status :open} {:xt/id 3, :status :open})
  ;;=> Syntax error compiling at (bases/grand-central/src/com/atd/mm/grand_central/resolver.clj:41:3).
  ;;   Unable to resolve symbol: get-ready-to-process in this context
  ;;   



  (get-http-client)
  (get-config)
  (def q (partial xt/q (get-xtdb-node)))

  (q '(from :processes [*]))

  (xt/submit-tx (get-xtdb-node) [[:put-docs :processes
                                  {:xt/id 1
                                   :status :completed}
                                  {:xt/id 2
                                   :deps [1 3 6]
                                   :status :open}
                                  {:xt/id 3
                                   :status :open}
                                  {:xt/id 4
                                   :deps [1]
                                   :status :open}
                                  {:xt/id 5
                                   :status :open}
                                  {:xt/id 6
                                   :status :completed}]])


  (q '(-> (from :processes [xt/id deps])
          (where (nil? deps))))


  (q '(-> (unify
           (from :processes [{:status :open} xt/id deps])
           (unnest {p deps})
           (from :processes [{:xt/id p} {:status :completed}]))
          #_(return xt/id)))


  (q '(-> (unify
           (from :processes [{:status :open} xt/id deps])
           (unnest {dep-id deps})
           (from :processes [{:xt/id dep-id} {:status :completed}]))

          #_(return xt/id)))


  (q '(-> (from :processes [{:status :open} *])

          (with {:deps (pull* (fn [deps]
                                (->
                                 (unify (from :processes [{:xt/id dep-id} status])
                                        (unnest {dep-id deps})))))})))
  ;;=> [{:xt/id 5, :status :open}
  ;;    {:deps [{:dep-id 6, :status :completed} {:dep-id 1, :status :completed} {:dep-id 3, :status :open}],
  ;;     :xt/id 2,
  ;;     :status :open}
  ;;    {:deps [{:dep-id 1, :status :completed}], :xt/id 4, :status :open}
  ;;    {:xt/id 3, :status :open}]
  ;;=> [{:xt/id 5, :status :open}
  ;;    {:deps [{:dep-id 6, :status :completed} {:dep-id 1, :status :completed} {:dep-id 3, :status :open}],
  ;;     :xt/id 2,
  ;;     :status :open}
  ;;    {:deps [{:dep-id 1, :status :completed}], :xt/id 4, :status :open}
  ;;    {:xt/id 3, :status :open}]
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   * is not a valid in from when inside a unify context
  ;;   
  ;;=> [{:xt/id 5, :status :open}
  ;;    {:deps [{:dep-id 6} {:dep-id 1} {:dep-id 3}], :xt/id 2, :status :open}
  ;;    {:deps [{:dep-id 1}], :xt/id 4, :status :open}
  ;;    {:xt/id 3, :status :open}]
  ;;=> [#:xt{:id 5} {:deps [{:dep-id 6} {:dep-id 1} {:dep-id 3}], :xt/id 2} {:deps [{:dep-id 1}], :xt/id 4} #:xt{:id 3}]
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   Not all variables in expression are in scope
  ;;   
  ;;=> [#:xt{:id 5} #:xt{:id 2} #:xt{:id 4} #:xt{:id 3}]
  ;;=> [#:xt{:id 5} {:deps [{:dep-id 6} {:dep-id 1} {:dep-id 3}], :xt/id 2} {:deps [{:dep-id 1}], :xt/id 4} #:xt{:id 3}]
  ;;=> [{:deps [{:dep-id 5} {:dep-id 6} {:dep-id 2} {:dep-id 1} {:dep-id 4} {:dep-id 3}], :xt/id 5}
  ;;    {:deps [{:dep-id 5} {:dep-id 6} {:dep-id 2} {:dep-id 1} {:dep-id 4} {:dep-id 3}], :xt/id 2}
  ;;    {:deps [{:dep-id 5} {:dep-id 6} {:dep-id 2} {:dep-id 1} {:dep-id 4} {:dep-id 3}], :xt/id 4}
  ;;    {:deps [{:dep-id 5} {:dep-id 6} {:dep-id 2} {:dep-id 1} {:dep-id 4} {:dep-id 3}], :xt/id 3}]
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   Attribute in col spec must be keyword
  ;;   
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   Illegal argument: 'xtql/unknown-query-op'
  ;;   
  ;;=> [{:deps [1 3 6], :xt/id 2, :dep-id 6} {:deps [1 3 6], :xt/id 2, :dep-id 1} {:deps [1], :xt/id 4, :dep-id 1}]
  ;;=> [#:xt{:id 2} #:xt{:id 2} #:xt{:id 4}]
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   Illegal argument: 'xtql/unknown-query-tail'
  ;;   
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   Illegal argument: 'xtql/unknown-query-tail'
  ;;   
  ;;=> [{:deps [1 3 6], :xt/id 2, :dep-id 1}
  ;;    {:deps [1 3 6], :xt/id 2, :dep-id 3}
  ;;    {:deps [1 3 6], :xt/id 2, :dep-id 6}
  ;;    {:deps [1], :xt/id 4, :dep-id 1}]
  ;;=> [#:xt{:id 5}
  ;;    #:xt{:id 5}
  ;;    {:deps [1 3 6], :xt/id 2}
  ;;    {:deps [1 3 6], :xt/id 2}
  ;;    {:deps [1], :xt/id 4}
  ;;    {:deps [1], :xt/id 4}
  ;;    #:xt{:id 3}
  ;;    #:xt{:id 3}]



  ;;Keep from folding
  )