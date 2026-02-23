(ns com.atd.mm.database.interface
  (:require
   [donut.system :as ds]
   [com.atd.mm.database.core :as impl]))

;; ===== Node holder =====
;; Set at Donut System start so any component can access the
;; running XTDB node without depending on the base's resolver.

(defonce ^:private node-atom (atom nil))

(defn set-node!
  "Store the running XTDB node. Called once during system start."
  [node]
  (reset! node-atom node))

(defn get-node
  "Return the running XTDB node set by `set-node!`.
   Throws if system has not been started."
  []
  (or @node-atom
      (throw (ex-info "XTDB node not available — has the system been started?" {}))))

;; ===== Donut System lifecycle =====

(def system-config
  #::ds{:start (fn [{{:keys [node-config]} ::ds/config}]
                 (println "Starting xtdb node")
                 (let [node (impl/start-xtdb-node node-config)]
                   (set-node! node)
                   node))
        :stop (fn [{::ds/keys [instance]}]
                (println "Stopping xtdb node")
                (reset! node-atom nil)
                (impl/stop-xtdb-node instance))
        :config {:node-config (ds/ref [:config :env :xtdb-config])}})