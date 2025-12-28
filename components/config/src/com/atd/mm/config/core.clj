(ns com.atd.mm.config.core
  (:require [aero.core :refer [read-config read-config-into-tagged-literal
                               reader resource-resolver]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))



(defn create-config [config-path]
  (let [resource (io/resource config-path)
        processed (read-config resource {:resolver resource-resolver})]
    (assoc processed
           :config-path config-path
           :config-resource resource)))

(comment

  (-> (create-config "grand-central/config.edn")
      :open-ai
      :api-key)



  ;;Keep from folding
  )