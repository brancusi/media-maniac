(ns com.atd.mm.config.interface
  (:require [com.atd.mm.config.core :as impl]
            [donut.system :as ds]))

(defn create-config
  [config-path]
  (impl/create-config config-path))

(def system-config
  #::ds{:start (fn [{{:keys [config-path]} ::ds/config}]
                 (create-config config-path))
        :config {:config-path (ds/local-ref [:config-path])}})

(comment

  (create-config "grand-central/config.edn")

  ;;Keep from folding
  )