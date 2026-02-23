(ns system
  (:require [clojure.pprint]
            [clojure.tools.namespace.repl :as tn-repl]

            [com.atd.mm.grand-central.core :as target-system]

            ;; Donut System requires
            [donut.system :as ds]
            [donut.system.repl :as dsr]
            [donut.system.repl.state :as dsr-state]

            [logging :as logging])
  (:import [java.io File]))

;; Only scan project source dirs — skip tmp, docs, refs, etc.
(tn-repl/set-refresh-dirs
 "development/src"
 "bases/grand-central/src"
 "components/config/src"
 "components/core-utils/src"
 "components/database/src"
 "components/http-client/src"
 "components/job-runner/src"
 "components/media-ingest/src"
 "components/media-processor/src"
 "components/pipeline/src"
 "components/user/src")

(def system-config
  {::ds/defs
   {:debugging {:system-portal (logging/system-portal-config "System viewer")}}})

(defmethod ds/named-system :donut.system/repl
  [_]
  system-config)

;; ---------- JVM shutdown hook ----------
;; donut.system.repl.state has (repl/disable-reload!), so it survives
;; refresh-all.  We stash the hook Thread there so we only ever register
;; one, even across refresh-all cycles (which remove-ns our namespace
;; and re-evaluate defonce).

(when-not (::shutdown-hook (meta #'dsr-state/system))
  (let [hook (Thread.
              (fn []
                (println "[shutdown-hook] JVM shutting down, stopping systems...")
                (try
                  ((requiring-resolve 'com.atd.mm.grand-central.core/shutdown!))
                  (catch Exception e
                    (println "[shutdown-hook] Warning stopping grand-central:" (.getMessage e))))
                (try
                  ((requiring-resolve 'donut.system.repl/stop))
                  (catch Exception e
                    (println "[shutdown-hook] Warning stopping dev system:" (.getMessage e))))))]
    (.addShutdownHook (Runtime/getRuntime) hook)
    (alter-meta! #'dsr-state/system assoc ::shutdown-hook hook)))

(defn stop
  []
  (println "Stopping dev system")
  (dsr/stop))

(defn start
  []
  (println "Starting dev system")
  (dsr/start))

(defn restart
  []
  (stop)
  (start))

(defn refresh-and-restart
  []
  (dsr/restart))

;; ===== Full project refresh =====
;; Stops both systems, unloads ALL namespaces, reloads from disk,
;; then restarts everything. Use when stale defs or removed vars
;; are causing trouble — this is the nuclear option without
;; restarting the JVM.

(defn- stop-all!
  "Gracefully stop both the target system (grand-central) and the
  dev system (Portal), swallowing errors so we always reach the
  namespace refresh phase.

  Uses requiring-resolve so we don't hold stale var references
  across a tools.namespace refresh."
  []
  (println "[refresh] Stopping grand-central system...")
  (try
    ;; Resolve at call-time — the var may have been reloaded
    ((requiring-resolve 'com.atd.mm.grand-central.core/shutdown!))
    (catch Exception e
      (println "[refresh] Warning stopping grand-central:" (.getMessage e))))
  ;; Also nil out the database atom in case it survived defonce
  (try
    (when-let [atom-var (resolve 'com.atd.mm.database.interface/node-atom)]
      (reset! @atom-var nil))
    (catch Exception _))
  (println "[refresh] Stopping dev system...")
  (try (dsr/stop) (catch Exception e
                    (println "[refresh] Warning stopping dev system:" (.getMessage e)))))

(defn- start-all!
  "Restart both the dev system and grand-central.
  Called by tools.namespace after refresh-all finishes.

  IMPORTANT: tools.namespace catches any exception thrown here and
  returns it instead of propagating — errors vanish unless we handle
  them ourselves."
  []
  (println "[refresh] Starting dev system (Portal)...")
  (try
    ((requiring-resolve 'system/start))
    (println "[refresh] Dev system started.")
    (catch Exception e
      (println "[refresh] ERROR starting dev system:" (.getMessage e))
      (.printStackTrace e)))
  (println "[refresh] Starting grand-central system...")
  (try
    ((requiring-resolve 'com.atd.mm.grand-central.core/-main))
    (println "[refresh] All systems running.")
    (catch Exception e
      (println "[refresh] ERROR starting grand-central:" (.getMessage e))
      (.printStackTrace e))))

(defn full-refresh
  "Nuclear refresh: stop everything → unload + reload every namespace → restart.
  Clears stale vars, removed defs, and renamed multimethods.
  Does NOT require a JVM restart."
  []
  (stop-all!)
  (println "[refresh] Clearing and reloading all namespaces...")
  (let [result (tn-repl/refresh-all :after 'system/start-all!)]
    (cond
      (instance? Throwable result)
      (do (println "[refresh] FAILED — namespace reload error:")
          (println (.getMessage ^Throwable result))
          (println "[refresh] Fix the error and run (full-refresh) again.")
          (println "[refresh] Stack trace: (clojure.repl/pst *e)"))

      (= result :ok)
      (println "[refresh] Refresh complete.")

      :else
      (println "[refresh] Refresh returned:" (pr-str result)))))

;; ===== XTDB data management =====

(defn- delete-dir-recursive!
  "Recursively delete a directory and all its contents."
  [^File dir]
  (when (.exists dir)
    (doseq [^File f (reverse (file-seq dir))]
      (.delete f))))

(defn nuke-xtdb!
  "Wipe the XTDB transaction log and local storage.
  The system MUST be stopped first. Restarts with a clean DB.

  Use when you see errors like:
    'Object tables/…/data/….arrow doesn't exist'"
  []
  (stop-all!)
  (let [xtdb-dir (File. "./tmp/mm")]
    (println "[nuke-xtdb] Deleting" (.getAbsolutePath xtdb-dir))
    (delete-dir-recursive! xtdb-dir)
    (println "[nuke-xtdb] XTDB data wiped. Starting fresh...")
    ;; Recreate the dir so XTDB doesn't complain
    (.mkdirs xtdb-dir))
  (start-all!))

;; ===== Entry points =====

(defn start-dev
  []
  ;; Start dev system (Portal)
  (start)
  ;; Start the target system (grand-central)
  (target-system/-main))

(defn get-system-portal
  []
  (dsr/instance [:debugging :system-portal]))

(comment
  ;; Start dev. This will init the integrant system referenced above
  (start-dev)

  (stop)

  (dsr/restart)

  (refresh-and-restart)

  ;; Nuclear refresh — unloads ALL namespaces and restarts everything
  (full-refresh)

  ;; Wipe XTDB data and start fresh (fixes corrupt log errors)
  (nuke-xtdb!)

  ;;Keep from folding
  )