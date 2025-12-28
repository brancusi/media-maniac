(ns com.atd.mm.grand-central.core
  (:require [com.atd.mm.grand-central.system :as main-system])
  (:gen-class))

(defn init!
  []
  (main-system/create-system))

(defn shutdown!
  []
  (main-system/shutdown-system))

(defn -main []
  (init!)
  (println "Grand Central system initialized.")
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. shutdown!)))

(comment

  (-main)




  ;;Keep from folding
  )