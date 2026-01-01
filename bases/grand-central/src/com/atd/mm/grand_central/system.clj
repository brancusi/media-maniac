(ns com.atd.mm.grand-central.system
  (:require
   [com.atd.mm.grand-central.resolver :as resolver]
   [com.atd.mm.config.interface :as config]
   [com.atd.mm.http-client.interface :as http-client]
   [com.atd.mm.job-runner.interface :as job-runner]
   [com.atd.mm.database.interface :as database]
   [donut.system :as ds]))

(def system-config
  {::ds/defs
   {:config {:env config/system-config
             :config-path "grand-central/config.edn"}
    :http-client {:client http-client/system-config}
    :job-runner {:job-runner job-runner/system-config}
    :database {:node database/system-config}}})

(defn register-system!
  [system]
  (alter-var-root #'resolver/rs (fn [& _]
                                  system)))

(defn unregister-system!
  []
  (alter-var-root #'resolver/rs (fn [& _] nil)))

(defn create-system
  []
  (register-system! (ds/signal system-config ::ds/start)))

(defn shutdown-system
  []
  (when resolver/rs
    (ds/signal resolver/rs ::ds/stop)
    (unregister-system!)))

(comment
  resolver/rs
  (create-system)
  (shutdown-system)
  ;;Keep from folding
  )