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



(comment

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
           (join (from :processes [{:xt/id p} {:status :completed}])))
          (return xt/id)))
  ;;=> [#:xt{:id 2} #:xt{:id 2} #:xt{:id 4}]
  ;;=> [{:p 6} {:p 1} {:p 1}]
  ;;=> []
  ;;=> [{:p 6} {:p 1} {:p 3} {:p 1}]
  ;;=> [{:p 1} {:p 3} {:p 6} {:p 1}]
  ;;=> [#:xt{:id 2} #:xt{:id 2} #:xt{:id 2} #:xt{:id 4}]
  ;;=> []
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   Attribute in var spec must be symbol
  ;;   
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   Unnest takes only a single binding
  ;;   
  ;;=> []
  ;;=> []
  ;;=> []
  ;;=> [#:xt{:id 5} #:xt{:id 5} #:xt{:id 2} #:xt{:id 2} #:xt{:id 4} #:xt{:id 4} #:xt{:id 3} #:xt{:id 3}]
  ;;=> [{:xt/id 5, :p 6}
  ;;    {:xt/id 5, :p 1}
  ;;    {:deps [1 3 6], :xt/id 2, :p 6}
  ;;    {:deps [1 3 6], :xt/id 2, :p 1}
  ;;    {:deps [1], :xt/id 4, :p 6}
  ;;    {:deps [1], :xt/id 4, :p 1}
  ;;    {:xt/id 3, :p 6}
  ;;    {:xt/id 3, :p 1}]


  ;;=> [{:p 6, :deps [1 3 6], :xt/id 2} {:p 1, :deps [1 3 6], :xt/id 2} {:p 1, :deps [1], :xt/id 4}]
  ;;=> [#:xt{:id 2} #:xt{:id 2} #:xt{:id 4}]
  ;;=> [#:xt{:id 2} #:xt{:id 4}]
  ;;=> [{:p 1} {:p 1}]
  ;;=> [{:p 3}]
  ;;=> [{:p 3}]
  ;;=> [{:p 1} {:p 1}]
  ;;=> [{:p 1, :deps [1 3], :xt/id 2} {:p 1, :deps [1], :xt/id 4}]
  ;;=> [{:p 1, :deps [1 3], :xt/id 2} {:p 1, :deps [1], :xt/id 4}]
  ;;=> [{:p 1, :deps [1 3], :xt/id 2} {:p 1, :deps [1], :xt/id 4}]




  ;;Keep from folding
  )