(ns system
  (:require [clojure.pprint]

            [com.atd.mm.grand-central.core :as target-system]

            ;; Donut System requires
            [donut.system :as ds]
            [donut.system.repl :as dsr]
            [donut.system.repl.state :as dsr-state]

            [logging :as logging]))

#_(def system-config
    (-> target-system/system-config
        (assoc-in [::ds/defs :debugging :system-portal]
                  (ds/cache-component (logging/system-portal-config "System viewer")))))

(def system-config
  {::ds/defs
   {:debugging {:system-portal (logging/system-portal-config "System viewer")}}})

(defmethod ds/named-system :donut.system/repl
  [_]
  system-config)

(defn stop
  []
  (println "Stopping system")
  (dsr/stop))

(defn start
  []
  (println "Starting system")
  (dsr/start)

  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. stop)))

(defn restart
  []
  (stop)
  (start))

(defn refresh-and-restart
  []
  (dsr/restart))

(defn start-dev
  []
  ;; Start the target system
  (target-system/-main)

  ;; Start donut system
  (start))

(defn get-system-portal
  []
  (dsr/instance [:debugging :system-portal]))

(comment
  ;; Start dev. This will init the integrant system referenced above
  (start-dev)

  (stop)

  (dsr/restart)

  (refresh-and-restart)

  (tap> "hji")

  ;;Keep from folding
  )