(ns diagram.all
  "Top-level namespace that regenerates every diagram in one pass.
   Evaluate this namespace (or call `render-all!`) to rebuild all
   PNG + SVG outputs into `docs/diagrams/`."
  (:require [diagram.fiddle :as fiddle]
            [diagram.pipeline-concept :as pipeline-concept]))

(defn render-all!
  "Regenerate all diagrams across all diagram namespaces."
  []
  (println "Rendering diagrams →  docs/diagrams/")
  (println)
  (println "  diagram.fiddle:")
  (fiddle/render-all!)
  (println)
  (println "  diagram.pipeline-concept:")
  (pipeline-concept/render-all!)
  (println)
  (println "Done. All diagrams written to docs/diagrams/"))

(comment
  (render-all!)
  ;;
  )
