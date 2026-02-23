(ns diagram.pipeline-concept
  (:require [com.phronemophobic.clj-graphviz :refer [render-graph]]))

;; ═══════════════════════════════════════════════════════════════════
;; PIPELINE CONCEPT — Template → Job → Step Execution
;; ═══════════════════════════════════════════════════════════════════
;;
;; Shows the full lifecycle:
;;   1. A PipelineTemplate defines reusable step definitions
;;   2. create-job-from-template instantiates a PipelineJob
;;   3. Steps are created with UUIDs, :open status, resolved deps
;;   4. Scheduler picks ready steps (deps satisfied) in waves
;;   5. Processors execute, mark :completed, unlock next wave
;;
;; Example template:
;;   source.MP4
;;     ├─ proxy-720        (no deps)
;;     ├─ extract-audio    (no deps)
;;     ├─ copy             (no deps)
;;     ├─ extract-stills   (deps: proxy-720)
;;     └─ transcribe       (deps: proxy-720, extract-audio)

(def pipeline-concept-graph
  {:flags #{:directed}

   :default-attributes
   {:graph {:fontname  "Helvetica Neue"
            :fontsize  "12"
            :bgcolor   "#fdfdfd"
            :rankdir   "TB"
            :label     ""
            :labelloc  "t"
            :style     "filled"
            :color     "white"
            :fillcolor "white"
            :rank      ""
            :cluster   ""
            :pad       "0.5"
            :nodesep   "0.6"
            :ranksep   "0.8"}
    :node  {:fontname  "Helvetica Neue"
            :fontsize  "10"
            :shape     "box"
            :style     "rounded,filled"
            :fillcolor "#f0f4f8"
            :color     "#4a6fa5"
            :penwidth  "1.5"
            :label     ""
            :fontcolor "#2d3436"
            :width     "0"
            :height    "0"}
    :edge  {:fontname  "Helvetica Neue"
            :fontsize  "8"
            :color     "#78909c"
            :arrowsize "0.7"
            :label     ""
            :style     "solid"
            :penwidth  "1.0"
            :arrowhead "normal"}}

   ;; ── Nodes ──────────────────────────────────────────────────────

   :nodes [;; ── Template (reusable blueprint) ─────────────────────
           {:id "template"
            :label "PipelineTemplate\n═════════════════\n:name \"standard-ingest\"\n:steps [...step definitions...]"
            :shape "tab"
            :fillcolor "#e8eaf6"
            :color "#3949ab"
            :fontcolor "#1a237e"
            :penwidth "2.0"
            :fontsize "10"}

           {:id "tpl-s1"
            :label "StepDefinition\n──────────\n:id \"proxy-720\"\n:type :media/proxy\n:opts {:size 720}\n:deps []"
            :fillcolor "#e8eaf6"
            :color "#3949ab"
            :fontcolor "#283593"
            :fontsize "9"}

           {:id "tpl-s2"
            :label "StepDefinition\n──────────\n:id \"extract-audio\"\n:type :media/extract-audio\n:opts {:encoding \"wav\"}\n:deps []"
            :fillcolor "#e8eaf6"
            :color "#3949ab"
            :fontcolor "#283593"
            :fontsize "9"}

           {:id "tpl-s3"
            :label "StepDefinition\n──────────\n:id \"copy\"\n:type :media/copy\n:opts {:dest \"/archive\"}\n:deps []"
            :fillcolor "#e8eaf6"
            :color "#3949ab"
            :fontcolor "#283593"
            :fontsize "9"}

           {:id "tpl-s4"
            :label "StepDefinition\n──────────\n:id \"extract-stills\"\n:type :media/extract-stills\n:opts {:frequency 10}\n:deps [\"proxy-720\"]"
            :fillcolor "#e8eaf6"
            :color "#5c6bc0"
            :fontcolor "#283593"
            :fontsize "9"}

           {:id "tpl-s5"
            :label "StepDefinition\n──────────\n:id \"transcribe\"\n:type :media/transcribe\n:opts {:model :whisper}\n:deps [\"proxy-720\"\n       \"extract-audio\"]"
            :fillcolor "#e8eaf6"
            :color "#5c6bc0"
            :fontcolor "#283593"
            :fontsize "9"}

           ;; ── Transformation arrow ──────────────────────────────
           {:id "transform"
            :label "create-job-from-template\n─────────────────────\n1. Fetch template from XTDB\n2. string IDs → UUIDs\n3. Resolve dep references\n4. Stamp :open status\n5. Attach :pipeline-job-id"
            :shape "cds"
            :fillcolor "#fff3e0"
            :color "#e65100"
            :fontcolor "#bf360c"
            :penwidth "2.0"
            :fontsize "10"
            :width "3.0"}

           ;; ── Source file ────────────────────────────────────────
           {:id "source"
            :label "source.MP4\n(raw footage)"
            :shape "note"
            :fillcolor "#ffeaa7"
            :color "#d4a017"
            :fontsize "11"
            :penwidth "2.0"}

           ;; ── Job (instantiated from template) ──────────────────
           {:id "job"
            :label "PipelineJob\n═════════════════\n:xt/id #uuid \"03e1...9394\"\n:src \"source.MP4\"\n:template-id #uuid \"a1b2...\"\n:steps [5 PipelineSteps]"
            :fillcolor "#e8f5e9"
            :color "#2e7d32"
            :fontcolor "#1b5e20"
            :penwidth "2.0"
            :fontsize "10"}

           ;; ── Wave 1: no dependencies (run immediately) ─────────
           {:id "ps-proxy"
            :label "PipelineStep\n──────────\n:xt/id #uuid \"aa11...\"\n:type :media/proxy\n:opts {:size 720}\n:deps []\n:status :open → :completed"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :fontcolor "#1b5e20"
            :penwidth "2.0"
            :fontsize "9"}

           {:id "ps-audio"
            :label "PipelineStep\n──────────\n:xt/id #uuid \"bb22...\"\n:type :media/extract-audio\n:opts {:encoding \"wav\"}\n:deps []\n:status :open → :completed"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :fontcolor "#1b5e20"
            :penwidth "2.0"
            :fontsize "9"}

           {:id "ps-copy"
            :label "PipelineStep\n──────────\n:xt/id #uuid \"cc33...\"\n:type :media/copy\n:opts {:dest \"/archive\"}\n:deps []\n:status :open → :completed"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :fontcolor "#1b5e20"
            :penwidth "2.0"
            :fontsize "9"}

           ;; ── Wave 2: has dependencies ──────────────────────────
           {:id "ps-stills"
            :label "PipelineStep\n──────────\n:xt/id #uuid \"dd44...\"\n:type :media/extract-stills\n:opts {:frequency 10}\n:deps [#uuid \"aa11...\"]\n:status :open  (blocked)"
            :fillcolor "#ffcdd2"
            :color "#c62828"
            :fontcolor "#b71c1c"
            :penwidth "2.0"
            :style "rounded,filled,dashed"
            :fontsize "9"}

           {:id "ps-transcribe"
            :label "PipelineStep\n──────────\n:xt/id #uuid \"ee55...\"\n:type :media/transcribe\n:opts {:model :whisper}\n:deps [#uuid \"aa11...\"\n       #uuid \"bb22...\"]\n:status :open  (blocked)"
            :fillcolor "#ffcdd2"
            :color "#c62828"
            :fontcolor "#b71c1c"
            :penwidth "2.0"
            :style "rounded,filled,dashed"
            :fontsize "9"}

           ;; ── XTDB storage ──────────────────────────────────────
           {:id "xtdb"
            :label "XTDB v2\n─────────────\n:pipeline-templates\n:pipeline-jobs\n:pipeline-steps"
            :shape "cylinder"
            :fillcolor "#5c6bc0"
            :color "#283593"
            :fontcolor "white"
            :penwidth "1.5"
            :fontsize "10"}

           ;; ── Outputs ───────────────────────────────────────────
           {:id "out-proxy"
            :label "proxy_720.mov\n(ProRes 720p)"
            :shape "note"
            :fillcolor "#a5d6a7"
            :color "#388e3c"
            :fontsize "9"}

           {:id "out-audio"
            :label "audio.wav"
            :shape "note"
            :fillcolor "#a5d6a7"
            :color "#388e3c"
            :fontsize "9"}

           {:id "out-copy"
            :label "/archive/source.MP4"
            :shape "note"
            :fillcolor "#a5d6a7"
            :color "#388e3c"
            :fontsize "9"}

           {:id "out-stills"
            :label "frame_0001.jpg\nframe_0002.jpg ..."
            :shape "note"
            :fillcolor "#b39ddb"
            :color "#512da8"
            :fontsize "9"}

           {:id "out-transcript"
            :label "transcript.json\n(Whisper)"
            :shape "note"
            :fillcolor "#b39ddb"
            :color "#512da8"
            :fontsize "9"}

           ;; ── Legend ────────────────────────────────────────────
           {:id "leg-tpl"
            :label "Template\n(reusable blueprint)"
            :fillcolor "#e8eaf6"
            :color "#3949ab"
            :shape "box"
            :fontsize "9"
            :penwidth "1.5"}

           {:id "leg-ready"
            :label "Step: Ready\n(deps met)"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :shape "box"
            :fontsize "9"
            :penwidth "1.5"}

           {:id "leg-blocked"
            :label "Step: Blocked\n(waiting)"
            :fillcolor "#ffcdd2"
            :color "#c62828"
            :shape "box"
            :fontsize "9"
            :style "rounded,filled,dashed"
            :penwidth "1.5"}

           {:id "leg-output"
            :label "Output artifact"
            :fillcolor "#a5d6a7"
            :color "#388e3c"
            :shape "note"
            :fontsize "9"}]

   ;; ── Edges ──────────────────────────────────────────────────────

   :edges [;; Template contains step definitions
           {:from "template" :to "tpl-s1" :color "#3949ab" :penwidth "1.5"}
           {:from "template" :to "tpl-s2" :color "#3949ab" :penwidth "1.5"}
           {:from "template" :to "tpl-s3" :color "#3949ab" :penwidth "1.5"}
           {:from "template" :to "tpl-s4" :color "#3949ab" :penwidth "1.5"}
           {:from "template" :to "tpl-s5" :color "#3949ab" :penwidth "1.5"}

           ;; Dep references within template (string-based)
           {:from "tpl-s1" :to "tpl-s4" :label "\"proxy-720\"" :style "dashed" :color "#5c6bc0" :arrowhead "diamond"}
           {:from "tpl-s1" :to "tpl-s5" :label "\"proxy-720\"" :style "dashed" :color "#5c6bc0" :arrowhead "diamond"}
           {:from "tpl-s2" :to "tpl-s5" :label "\"extract-audio\"" :style "dashed" :color "#5c6bc0" :arrowhead "diamond"}

           ;; Template + source → transform
           {:from "template" :to "transform" :label "template" :penwidth "2.0" :color "#e65100"}
           {:from "source"   :to "transform" :label ":src"     :penwidth "2.0" :color "#e65100"}

           ;; Transform → job
           {:from "transform" :to "job" :label "PipelineJob\n+ PipelineSteps" :penwidth "2.0" :color "#2e7d32"}

           ;; Job stored in XTDB
           {:from "job" :to "xtdb" :label "put-docs\n:pipeline-jobs\n:pipeline-steps" :color "#283593" :penwidth "1.5"}

           ;; Template stored in XTDB
           {:from "template" :to "xtdb" :label "put-docs\n:pipeline-templates" :color "#283593" :style "dashed"}

           ;; Job → steps
           {:from "job" :to "ps-proxy" :color "#2e7d32" :penwidth "1.5"}
           {:from "job" :to "ps-audio" :color "#2e7d32" :penwidth "1.5"}
           {:from "job" :to "ps-copy"  :color "#2e7d32" :penwidth "1.5"}
           {:from "job" :to "ps-stills" :color "#2e7d32" :penwidth "1.5"}
           {:from "job" :to "ps-transcribe" :color "#2e7d32" :penwidth "1.5"}

           ;; Dependency edges (UUID-based, resolved)
           {:from "ps-proxy" :to "ps-stills"
            :label ":deps [#uuid aa11]"
            :style "dashed" :color "#c62828" :penwidth "2.0" :arrowhead "diamond"}
           {:from "ps-proxy" :to "ps-transcribe"
            :label ":deps"
            :style "dashed" :color "#c62828" :penwidth "2.0" :arrowhead "diamond"}
           {:from "ps-audio" :to "ps-transcribe"
            :label ":deps"
            :style "dashed" :color "#c62828" :penwidth "2.0" :arrowhead "diamond"}

           ;; Step → output artifacts
           {:from "ps-proxy"  :to "out-proxy"      :color "#388e3c"}
           {:from "ps-audio"  :to "out-audio"       :color "#388e3c"}
           {:from "ps-copy"   :to "out-copy"        :color "#388e3c"}
           {:from "ps-stills" :to "out-stills"      :color "#512da8"}
           {:from "ps-transcribe" :to "out-transcript" :color "#512da8"}

           ;; Legend
           {:from "leg-tpl"   :to "leg-ready"   :style "invis"}
           {:from "leg-ready"  :to "leg-blocked" :style "invis"}
           {:from "leg-blocked" :to "leg-output" :style "invis"}]

   ;; ── Subgraphs / Clusters ──────────────────────────────────────

   :subgraphs
   [{:id "cluster_template"
     :nodes ["template" "tpl-s1" "tpl-s2" "tpl-s3" "tpl-s4" "tpl-s5"]
     :default-attributes
     {:graph {:label "PipelineTemplate  (reusable blueprint, stored in XTDB)"
              :style "rounded,filled"
              :fillcolor "#e8eaf6"
              :color "#3949ab"
              :fontsize "12"
              :fontcolor "#1a237e"}}}

    {:id "cluster_instantiate"
     :nodes ["transform" "source"]
     :default-attributes
     {:graph {:label "Instantiation"
              :style "rounded,filled"
              :fillcolor "#fff8e1"
              :color "#ff8f00"
              :fontsize "12"
              :fontcolor "#e65100"}}}

    {:id "cluster_job"
     :nodes ["job"]
     :default-attributes
     {:graph {:label "PipelineJob  (concrete execution instance)"
              :style "rounded,filled"
              :fillcolor "#e8f5e9"
              :color "#2e7d32"
              :fontsize "12"
              :fontcolor "#1b5e20"}}}

    {:id "cluster_wave1"
     :nodes ["ps-proxy" "ps-audio" "ps-copy"]
     :default-attributes
     {:graph {:label "Wave 1 — No Dependencies  (get-ready-steps returns these)"
              :style "rounded,filled"
              :fillcolor "#f1f8e9"
              :color "#558b2f"
              :fontsize "11"
              :fontcolor "#33691e"}}}

    {:id "cluster_wave2"
     :nodes ["ps-stills" "ps-transcribe"]
     :default-attributes
     {:graph {:label "Wave 2 — Blocked  (deps not yet :completed)"
              :style "rounded,filled,dashed"
              :fillcolor "#fce4ec"
              :color "#c62828"
              :fontsize "11"
              :fontcolor "#b71c1c"}}}

    {:id "cluster_outputs"
     :nodes ["out-proxy" "out-audio" "out-copy" "out-stills" "out-transcript"]
     :default-attributes
     {:graph {:label "Output Artifacts"
              :style "rounded,filled"
              :fillcolor "#f3e5f5"
              :color "#7b1fa2"
              :fontsize "11"
              :fontcolor "#4a148c"}}}

    {:id "cluster_legend"
     :nodes ["leg-tpl" "leg-ready" "leg-blocked" "leg-output"]
     :default-attributes
     {:graph {:label "Legend"
              :style "rounded,filled"
              :fillcolor "#f5f5f5"
              :color "#bdbdbd"
              :fontsize "10"
              :rank "sink"}}}]})

(def ^:private diagrams
  [{:graph pipeline-concept-graph :name "pipeline-concept"}])

(defn render-all!
  "Render all diagrams in this namespace to docs/diagrams/ as PNG + SVG."
  []
  (let [out-dir "docs/diagrams"]
    (.mkdirs (java.io.File. out-dir))
    (doseq [{:keys [graph name]} diagrams]
      (render-graph graph {:filename (str out-dir "/" name ".png")
                           :layout-algorithm :dot})
      (render-graph graph {:filename (str out-dir "/" name ".svg")
                           :format :svg
                           :layout-algorithm :dot})
      (println "  ✓" name))))

(comment
  (render-all!)
  ;;
  )
