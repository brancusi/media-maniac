(ns com.atd.mm.grand-central.core
  (:require [com.atd.mm.grand-central.system :as main-system])
  (:gen-class))

(defn init!
  []
  (main-system/create-system))

(defn shutdown!
  []
  (main-system/shutdown-system))

(defn -main
  "Initialize the Grand Central system.
  When run as an uberjar entry point, registers a shutdown hook.
  When called from dev (REPL), the dev system owns the shutdown hook,
  so pass :dev? true or no args from the REPL to skip hook registration."
  ([]
   (-main :dev))
  ([mode]
   (init!)
   (println "Grand Central system initialized.")
   (when (= mode :prod)
     (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. shutdown!)))))

(comment

  (-main)




  ;;Keep from folding
  )