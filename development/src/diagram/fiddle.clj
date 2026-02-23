(ns diagram.fiddle
  (:require [com.phronemophobic.clj-graphviz :refer [render-graph]]))

;; ═══════════════════════════════════════════════════════════════════
;; 1. SYSTEM ARCHITECTURE — End-to-end media processing pipeline
;; ═══════════════════════════════════════════════════════════════════

(def system-architecture
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
            :penwidth  "1.2"
            :label     ""
            :fontcolor "#2d3436"
            :width     "0"
            :height    "0"}
    :edge  {:fontname  "Helvetica Neue"
            :fontsize  "8"
            :color     "#636e72"
            :arrowsize "0.7"
            :label     ""
            :style     "solid"
            :penwidth  "1.0"
            :arrowhead "normal"}}

   :nodes [;; ── External sources ──
           {:id "sd-card"
            :label "SD Card / Source Media"
            :shape "folder"
            :fillcolor "#ffeaa7"
            :color "#d4a017"
            :penwidth "1.5"
            :fontsize "11"}

           ;; ── Ingest ──
           {:id "ingest"
            :label "media-ingest\n─────────────\nffprobe · metadata\ncamera detect\nxxh3-128 hash\nanalyze-sd-card"
            :fillcolor "#dfe6e9"
            :color "#2d3436"
            :penwidth "1.5"}

           ;; ── Pipeline (unified component) ──
           {:id "tpl-crud"
            :label "pipeline/template\n─────────────\ncreate-template\nget-template\ndelete-template\n─────────────\nMalli: PipelineTemplate"
            :fillcolor "#e8f5e9"
            :color "#2e7d32"
            :penwidth "1.5"}

           {:id "prepare"
            :label "pipeline/prepare\n─────────────\nprepare-job\n  string IDs → UUIDs\n  stamp :open status\ncreate-job-from-template\n  template → job"
            :fillcolor "#e8f5e9"
            :color "#2e7d32"
            :penwidth "1.5"}

           {:id "job-crud"
            :label "pipeline/job\n─────────────\ncreate-job  (validate → XTDB)\nget-job · get-all-jobs\ndelete-job\n─────────────\nMalli: PipelineJob"
            :fillcolor "#e8f5e9"
            :color "#2e7d32"
            :penwidth "1.5"}

           {:id "step-crud"
            :label "pipeline/step\n─────────────\nget-ready-steps\n  (dep resolution via XTQL)\nupdate-step · update-steps\nstep-completed?\n─────────────\nMalli: PipelineStep"
            :fillcolor "#e8f5e9"
            :color "#2e7d32"
            :penwidth "1.5"}

           ;; ── Storage ──
           {:id "xtdb"
            :label "XTDB v2\n─────────────\n:pipeline-templates\n:pipeline-jobs\n:pipeline-steps"
            :shape "cylinder"
            :fillcolor "#5c6bc0"
            :color "#3949ab"
            :fontcolor "white"
            :fontsize "10"
            :penwidth "1.5"}

           ;; ── Scheduling ──
           {:id "scheduler"
            :label "Scheduler (TODO)\n─────────────\ncron via Goose\nget-ready-steps\n→ queue eligible"
            :fillcolor "#fff3e0"
            :color "#e65100"
            :style "rounded,filled,dashed"
            :penwidth "1.5"}

           ;; ── Job Queue ──
           {:id "queue-job"
            :label "job-runner\n─────────────\nqueue-job\n→ goose/perform-async\ncreate-cron-job\nclear-all-jobs"
            :fillcolor "#dfe6e9"
            :color "#2d3436"
            :penwidth "1.5"}

           {:id "redis"
            :label "Redis\n─────────────\nJob Queues\ndefault · light · heavy"
            :shape "cylinder"
            :fillcolor "#ec407a"
            :color "#c2185b"
            :fontcolor "white"
            :penwidth "1.5"}

           ;; ── Workers ──
           {:id "w-default"
            :label "default\n5 threads"
            :fillcolor "#e0f2f1"
            :color "#00897b"
            :penwidth "1.5"}
           {:id "w-light"
            :label "light-process\n5 threads"
            :fillcolor "#e0f2f1"
            :color "#00897b"
            :penwidth "1.5"}
           {:id "w-heavy"
            :label "heavy-process\n1 thread"
            :fillcolor "#e0f2f1"
            :color "#00897b"
            :penwidth "1.5"}

           ;; ── Processors (multimethod dispatch) ──
           {:id "dispatch"
            :label "media-processor\nexecute-process\n─────────────\n(defmulti :type)"
            :fillcolor "#fff9c4"
            :color "#f9a825"
            :penwidth "1.5"
            :shape "diamond"
            :width "2.6"
            :height "1.2"}

           {:id "p-proxy"
            :label ":media/proxy\nFFmpeg ProRes"
            :fillcolor "#ffccbc"
            :color "#bf360c"
            :style "rounded,filled,dashed"}
           {:id "p-stills"
            :label ":media/extract-stills\nFFmpeg frames"
            :fillcolor "#ffccbc"
            :color "#bf360c"
            :style "rounded,filled,dashed"}
           {:id "p-audio"
            :label ":media/extract-audio\nFFmpeg WAV"
            :fillcolor "#ffccbc"
            :color "#bf360c"
            :style "rounded,filled,dashed"}
           {:id "p-transcribe"
            :label ":media/transcribe\nWhisper"
            :fillcolor "#ffccbc"
            :color "#bf360c"
            :style "rounded,filled,dashed"}
           {:id "p-copy"
            :label ":media/copy\nfile copy"
            :fillcolor "#ffccbc"
            :color "#bf360c"
            :style "rounded,filled,dashed"}

           ;; ── Completion ──
           {:id "complete"
            :label "update-step\n{:status :completed}"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :style "rounded,filled,dashed"}

           ;; ── Legend ──
           {:id "leg-impl"
            :label "Implemented"
            :fillcolor "#dfe6e9"
            :color "#2d3436"
            :shape "note"
            :fontsize "9"
            :penwidth "1.5"}
           {:id "leg-stub"
            :label "Stub / TODO"
            :fillcolor "#ffccbc"
            :color "#bf360c"
            :shape "note"
            :fontsize "9"
            :style "rounded,filled,dashed"}]

   :edges [;; Ingest flow
           {:from "sd-card"   :to "ingest"    :label "raw files"  :penwidth "1.5"}
           {:from "ingest"    :to "prepare"   :label "file info"  :penwidth "1.5"}

           ;; Template path
           {:from "tpl-crud"  :to "xtdb"      :label "put-docs\n:pipeline-templates" :color "#2e7d32"}
           {:from "tpl-crud"  :to "prepare"   :label "fetch\ntemplate" :style "dashed" :color "#2e7d32"}

           ;; Pipeline setup
           {:from "prepare"   :to "job-crud"  :label "prepared\njob data" :color "#2e7d32" :penwidth "1.5"}
           {:from "job-crud"  :to "xtdb"      :label "put-docs\n:pipeline-jobs\n:pipeline-steps" :penwidth "1.5"}
           {:from "step-crud" :to "xtdb"      :label "query/patch\n:pipeline-steps" :style "dashed" :color "#5c6bc0"}

           ;; Scheduling loop
           {:from "xtdb"      :to "scheduler" :label ":open steps"   :style "dashed" :color "#e65100"}
           {:from "scheduler" :to "queue-job" :label "eligible"      :style "dashed" :color "#e65100"}
           {:from "step-crud" :to "scheduler" :label "get-ready-steps" :style "dashed" :color "#e65100"}
           {:from "queue-job" :to "redis"     :label "enqueue"       :penwidth "1.5"}

           ;; Workers consume from Redis
           {:from "redis"     :to "w-default" :label "default"}
           {:from "redis"     :to "w-light"   :label "light"}
           {:from "redis"     :to "w-heavy"   :label "heavy"}

           ;; Workers invoke dispatch
           {:from "w-heavy"   :to "dispatch"  :penwidth "1.5"}
           {:from "w-light"   :to "dispatch"  :penwidth "1.0"}
           {:from "w-default" :to "dispatch"  :penwidth "1.0"}

           ;; Dispatch fans out to processors
           {:from "dispatch"  :to "p-proxy"      :style "dashed" :color "#bf360c"}
           {:from "dispatch"  :to "p-stills"     :style "dashed" :color "#bf360c"}
           {:from "dispatch"  :to "p-audio"      :style "dashed" :color "#bf360c"}
           {:from "dispatch"  :to "p-transcribe" :style "dashed" :color "#bf360c"}
           {:from "dispatch"  :to "p-copy"       :style "dashed" :color "#bf360c"}

           ;; Completion loop
           {:from "p-proxy"      :to "complete" :style "dashed" :color "#2e7d32"}
           {:from "p-stills"     :to "complete" :style "dashed" :color "#2e7d32"}
           {:from "p-audio"      :to "complete" :style "dashed" :color "#2e7d32"}
           {:from "p-transcribe" :to "complete" :style "dashed" :color "#2e7d32"}
           {:from "p-copy"       :to "complete" :style "dashed" :color "#2e7d32"}
           {:from "complete"     :to "xtdb"     :label "patch-docs\n:completed" :style "dashed" :color "#2e7d32"}

           ;; Legend
           {:from "leg-impl"  :to "leg-stub"  :style "invis"}]

   :subgraphs
   [{:id "cluster_ingest"
     :nodes ["sd-card" "ingest"]
     :default-attributes
     {:graph {:label "Ingest"
              :style "rounded,filled"
              :fillcolor "#fafafa"
              :color "#b0bec5"
              :fontsize "12"
              :fontcolor "#37474f"}}}

    {:id "cluster_pipeline"
     :nodes ["tpl-crud" "prepare" "job-crud" "step-crud"]
     :default-attributes
     {:graph {:label "pipeline  (unified component)"
              :style "rounded,filled"
              :fillcolor "#f1f8e9"
              :color "#558b2f"
              :fontsize "12"
              :fontcolor "#33691e"}}}

    {:id "cluster_schedule"
     :nodes ["scheduler" "queue-job"]
     :default-attributes
     {:graph {:label "Scheduling (TODO)"
              :style "rounded,filled,dashed"
              :fillcolor "#fff8e1"
              :color "#e65100"
              :fontsize "11"
              :fontcolor "#bf360c"}}}

    {:id "cluster_workers"
     :nodes ["w-default" "w-light" "w-heavy"]
     :default-attributes
     {:graph {:label "Goose Workers"
              :style "rounded,filled"
              :fillcolor "#e0f2f1"
              :color "#00695c"
              :fontsize "11"
              :fontcolor "#004d40"}}}

    {:id "cluster_processors"
     :nodes ["dispatch" "p-proxy" "p-stills" "p-audio" "p-transcribe" "p-copy"]
     :default-attributes
     {:graph {:label "media-processor  (multimethod dispatch)"
              :style "rounded,filled,dashed"
              :fillcolor "#fff3e0"
              :color "#bf360c"
              :fontsize "11"
              :fontcolor "#bf360c"}}}

    {:id "cluster_legend"
     :nodes ["leg-impl" "leg-stub"]
     :edges [["leg-impl" "leg-stub"]]
     :default-attributes
     {:graph {:label "Legend"
              :style "rounded,filled"
              :fillcolor "#f5f5f5"
              :color "#bdbdbd"
              :fontsize "10"
              :rank "sink"}
      :edge {:style "invis"}}}]})

;; ═══════════════════════════════════════════════════════════════════
;; 2. POLYLITH COMPONENT MAP — inter-component dependencies
;; ═══════════════════════════════════════════════════════════════════

(def polylith-component-map
  {:flags #{:directed}

   :default-attributes
   {:graph {:fontname  "Helvetica Neue"
            :fontsize  "12"
            :bgcolor   "#fdfdfd"
            :rankdir   "BT"
            :label     "Media Maniac — Polylith Component Map"
            :labelloc  "t"
            :style     "filled"
            :color     "white"
            :fillcolor "white"
            :rank      ""
            :cluster   ""
            :pad       "0.5"
            :nodesep   "0.8"
            :ranksep   "1.0"}
    :node  {:fontname  "Helvetica Neue"
            :fontsize  "10"
            :shape     "component"
            :style     "filled"
            :fillcolor "#e3f2fd"
            :color     "#1565c0"
            :penwidth  "1.5"
            :label     ""
            :fontcolor "#1a237e"
            :width     "0"
            :height    "0"}
    :edge  {:fontname  "Helvetica Neue"
            :fontsize  "8"
            :color     "#78909c"
            :arrowsize "0.6"
            :label     ""
            :style     "solid"
            :penwidth  "1.0"
            :arrowhead "vee"}}

   :nodes [;; ── Base (entry point) ──
           {:id "gc"
            :label "grand-central\n(base)"
            :shape "hexagon"
            :fillcolor "#ffe0b2"
            :color "#e65100"
            :fontcolor "#bf360c"
            :penwidth "2.0"
            :fontsize "11"}

           ;; ── Domain components ──
           {:id "pipeline"
            :label "pipeline\n─────────────\ntemplate · prepare\njob · step · specs"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :fontcolor "#1b5e20"
            :penwidth "2.0"}

           {:id "media-processor"
            :label "media-processor\n─────────────\nexecute-process\ngenerate-proxy\nextract-frames"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :fontcolor "#1b5e20"
            :penwidth "2.0"}

           {:id "media-ingest"
            :label "media-ingest\n─────────────\nffprobe · metadata\nhashing · sd-card"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :fontcolor "#1b5e20"
            :penwidth "2.0"}

           {:id "job-runner"
            :label "job-runner\n─────────────\nGoose wrapper\nproducer · consumer\nworker · cron"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :fontcolor "#1b5e20"
            :penwidth "2.0"}

           ;; ── Infrastructure components ──
           {:id "database"
            :label "database\n─────────────\nXTDB v2 node\nstart · stop"
            :fillcolor "#e3f2fd"
            :color "#1565c0"
            :fontcolor "#0d47a1"}

           {:id "config"
            :label "config\n─────────────\nAero EDN reader\ncreate-config"
            :fillcolor "#e3f2fd"
            :color "#1565c0"
            :fontcolor "#0d47a1"}

           {:id "core-utils"
            :label "core-utils\n─────────────\nfile · string · url\nhashing · base64"
            :fillcolor "#e3f2fd"
            :color "#1565c0"
            :fontcolor "#0d47a1"}

           {:id "http-client"
            :label "http-client\n─────────────\nHTTP client pool"
            :fillcolor "#e3f2fd"
            :color "#1565c0"
            :fontcolor "#0d47a1"}

           {:id "user"
            :label "user\n─────────────\n(scaffold)"
            :fillcolor "#f3e5f5"
            :color "#7b1fa2"
            :fontcolor "#4a148c"
            :style "filled,dashed"}

           ;; ── External services ──
           {:id "xtdb-ext"
            :label "XTDB v2"
            :shape "cylinder"
            :fillcolor "#5c6bc0"
            :color "#283593"
            :fontcolor "white"
            :fontsize "10"}

           {:id "redis-ext"
            :label "Redis"
            :shape "cylinder"
            :fillcolor "#ec407a"
            :color "#880e4f"
            :fontcolor "white"
            :fontsize "10"}

           {:id "ffmpeg-ext"
            :label "FFmpeg / FFprobe"
            :shape "parallelogram"
            :fillcolor "#78909c"
            :color "#37474f"
            :fontcolor "white"
            :fontsize "10"}

           ;; ── Legend ──
           {:id "leg-base"
            :label "Base\n(entry point)"
            :shape "hexagon"
            :fillcolor "#ffe0b2"
            :color "#e65100"
            :fontsize "8"
            :fontcolor "#bf360c"}
           {:id "leg-domain"
            :label "Domain\nComponent"
            :shape "component"
            :fillcolor "#c8e6c9"
            :color "#2e7d32"
            :fontsize "8"
            :fontcolor "#1b5e20"}
           {:id "leg-infra"
            :label "Infrastructure\nComponent"
            :shape "component"
            :fillcolor "#e3f2fd"
            :color "#1565c0"
            :fontsize "8"
            :fontcolor "#0d47a1"}
           {:id "leg-ext"
            :label "External\nService"
            :shape "cylinder"
            :fillcolor "#78909c"
            :color "#37474f"
            :fontsize "8"
            :fontcolor "white"}]

   :edges [;; Base depends on components
           {:from "gc"  :to "pipeline"        :label "uses" :penwidth "1.5" :color "#e65100"}
           {:from "gc"  :to "media-processor" :label "uses" :penwidth "1.5" :color "#e65100"}
           {:from "gc"  :to "media-ingest"    :label "uses" :penwidth "1.5" :color "#e65100"}
           {:from "gc"  :to "job-runner"      :label "uses" :penwidth "1.5" :color "#e65100"}
           {:from "gc"  :to "database"        :label "wires" :color "#e65100"}
           {:from "gc"  :to "config"          :label "wires" :color "#e65100"}
           {:from "gc"  :to "http-client"     :label "wires" :color "#e65100"}

           ;; Inter-component deps (via interfaces)
           {:from "media-processor" :to "core-utils"    :label "interface"}
           {:from "media-ingest"    :to "core-utils"    :label "interface"}
           {:from "media-ingest"    :to "http-client"   :label "interface"}

           ;; External service connections
           {:from "database"        :to "xtdb-ext"      :label "embeds"    :style "dashed" :color "#283593"}
           {:from "job-runner"      :to "redis-ext"     :label "connects"  :style "dashed" :color "#880e4f"}
           {:from "media-processor" :to "ffmpeg-ext"    :label "shells"    :style "dashed" :color "#37474f"}
           {:from "media-ingest"    :to "ffmpeg-ext"    :label "shells"    :style "dashed" :color "#37474f"}

           ;; Legend
           {:from "leg-base" :to "leg-domain" :style "invis"}
           {:from "leg-domain" :to "leg-infra" :style "invis"}
           {:from "leg-infra" :to "leg-ext" :style "invis"}]

   :subgraphs
   [{:id "cluster_base"
     :nodes ["gc"]
     :default-attributes
     {:graph {:label "Base" :style "rounded,filled" :fillcolor "#fff3e0"
              :color "#e65100" :fontsize "11" :fontcolor "#bf360c"}}}

    {:id "cluster_domain"
     :nodes ["pipeline" "media-processor" "media-ingest" "job-runner"]
     :default-attributes
     {:graph {:label "Domain Components" :style "rounded,filled" :fillcolor "#f1f8e9"
              :color "#2e7d32" :fontsize "11" :fontcolor "#1b5e20"}}}

    {:id "cluster_infra"
     :nodes ["database" "config" "core-utils" "http-client" "user"]
     :default-attributes
     {:graph {:label "Infrastructure Components" :style "rounded,filled" :fillcolor "#e8eaf6"
              :color "#1565c0" :fontsize "11" :fontcolor "#0d47a1"}}}

    {:id "cluster_external"
     :nodes ["xtdb-ext" "redis-ext" "ffmpeg-ext"]
     :default-attributes
     {:graph {:label "External Services" :style "rounded,filled" :fillcolor "#eceff1"
              :color "#546e7f" :fontsize "11" :fontcolor "#37474f"}}}

    {:id "cluster_legend_poly"
     :nodes ["leg-base" "leg-domain" "leg-infra" "leg-ext"]
     :default-attributes
     {:graph {:label "Legend" :style "rounded,filled" :fillcolor "#f5f5f5"
              :color "#bdbdbd" :fontsize "10" :rank "sink"}
      :edge {:style "invis"}}}]})

;; ═══════════════════════════════════════════════════════════════════
;; 3. PIPELINE COMPONENT INTERNALS — namespace & data flow
;; ═══════════════════════════════════════════════════════════════════

(def pipeline-internals
  {:flags #{:directed}

   :default-attributes
   {:graph {:fontname  "Helvetica Neue"
            :fontsize  "11"
            :bgcolor   "#fdfdfd"
            :rankdir   "LR"
            :label     "pipeline component — internal namespace & data flow"
            :labelloc  "t"
            :style     "filled"
            :color     "white"
            :fillcolor "white"
            :rank      ""
            :cluster   ""
            :pad       "0.4"
            :nodesep   "0.5"
            :ranksep   "1.2"}
    :node  {:fontname  "Helvetica Neue"
            :fontsize  "9"
            :shape     "record"
            :style     "filled"
            :fillcolor "#f5f5f5"
            :color     "#455a64"
            :penwidth  "1.2"
            :label     ""
            :fontcolor "#263238"
            :width     "0"
            :height    "0"}
    :edge  {:fontname  "Helvetica Neue"
            :fontsize  "8"
            :color     "#78909c"
            :arrowsize "0.6"
            :label     ""
            :style     "solid"
            :penwidth  "1.0"
            :arrowhead "vee"}}

   :nodes [;; Interface (public boundary)
           {:id "iface"
            :label "{interface.clj|Template CRUD\\lcreate-template\\lget-template\\ldelete-template\\l|Job Prep\\lprepare-job\\lcreate-job-from-template\\l|Job CRUD\\ljob-valid?\\lcreate-job\\lget-job · get-all-jobs\\ldelete-job\\l|Step CRUD\\lget-step · get-all-steps\\lupdate-step · update-steps\\lget-ready-steps\\lstep-completed?\\l}"
            :fillcolor "#e8f5e9"
            :color "#2e7d32"
            :penwidth "2.0"
            :fontsize "9"}

           ;; Specs
           {:id "specs"
            :label "{specs.clj|StepDefinition\\l  :id :string\\l  :type :enum\\l  :opts :map\\l  :deps [:vector :string]\\l|PipelineTemplate\\l  :name :string\\l  :steps [:vector StepDefinition]\\l|PipelineStep\\l  :xt/id :uuid\\l  :pipeline-job-id :uuid\\l  :status :enum\\l  :type :enum\\l  :deps [:vector :uuid]\\l|PipelineJob\\l  :xt/id :uuid\\l  :src :string\\l  :template-id :uuid\\l  :steps [:vector PipelineStep]\\l}"
            :fillcolor "#fff9c4"
            :color "#f9a825"
            :penwidth "1.5"}

           ;; Template
           {:id "template"
            :label "{template.clj|template-valid?\\lexplain-invalid-template\\lcreate-template\\lget-template\\lget-all-templates\\ldelete-template\\ldelete-all-templates\\l|XTDB table:\\l:pipeline-templates\\l}"
            :fillcolor "#e3f2fd"
            :color "#1565c0"}

           ;; Prepare
           {:id "prepare"
            :label "{prepare.clj|prepare-job\\l  string IDs → UUIDs\\l  stamp :open + :pipeline-job-id\\l|create-job-from-template\\l  fetch template → convert → prepare\\l|template-step->raw-step\\l  :id → :xt/id\\l|update-step-deps  (Specter)\\l}"
            :fillcolor "#fce4ec"
            :color "#c62828"}

           ;; Job
           {:id "job"
            :label "{job.clj|job-valid? · explain-invalid-job\\lcreate-job\\l  split → :pipeline-jobs\\l       + :pipeline-steps\\lget-job  (pull* steps)\\lget-all-jobs\\ldelete-job · delete-all-jobs\\l}"
            :fillcolor "#e3f2fd"
            :color "#1565c0"}

           ;; Step
           {:id "step"
            :label "{step.clj|get-ready-steps\\l  query :open\\l  pull* dep statuses\\l  filter all-deps-completed\\l|step-completed?\\lall-step-deps-completed?\\lupdate-step · update-steps\\lget-step · get-all-steps\\ldelete-step · delete-all-steps\\l}"
            :fillcolor "#e3f2fd"
            :color "#1565c0"}

           ;; External deps
           {:id "malli"
            :label "Malli 0.20"
            :shape "ellipse"
            :fillcolor "#eeeeee"
            :color "#9e9e9e"
            :fontsize "8"}

           {:id "specter"
            :label "Specter 1.1.6"
            :shape "ellipse"
            :fillcolor "#eeeeee"
            :color "#9e9e9e"
            :fontsize "8"}

           {:id "xtdb"
            :label "XTDB v2"
            :shape "cylinder"
            :fillcolor "#5c6bc0"
            :color "#283593"
            :fontcolor "white"
            :fontsize "9"}]

   :edges [;; Interface delegates to impl namespaces
           {:from "iface" :to "template" :label "delegates" :penwidth "1.5" :color "#2e7d32"}
           {:from "iface" :to "prepare"  :label "delegates" :penwidth "1.5" :color "#2e7d32"}
           {:from "iface" :to "job"      :label "delegates" :penwidth "1.5" :color "#2e7d32"}
           {:from "iface" :to "step"     :label "delegates" :penwidth "1.5" :color "#2e7d32"}
           {:from "iface" :to "specs"    :label "re-exports" :style "dashed" :color "#f9a825"}

           ;; Impl cross-refs
           {:from "prepare" :to "template" :label "fetch\ntemplate" :style "dashed" :color "#c62828"}
           {:from "template" :to "specs"   :label "validate" :style "dashed" :color "#78909c"}
           {:from "job"      :to "specs"   :label "validate" :style "dashed" :color "#78909c"}

           ;; External deps
           {:from "template" :to "malli"   :color "#9e9e9e" :style "dotted"}
           {:from "job"      :to "malli"   :color "#9e9e9e" :style "dotted"}
           {:from "prepare"  :to "specter" :color "#9e9e9e" :style "dotted"}
           {:from "template" :to "xtdb"    :color "#283593" :style "dashed"}
           {:from "job"      :to "xtdb"    :color "#283593" :style "dashed"}
           {:from "step"     :to "xtdb"    :color "#283593" :style "dashed"}]

   :subgraphs
   [{:id "cluster_public"
     :nodes ["iface"]
     :default-attributes
     {:graph {:label "Public API (interface.clj)" :style "rounded,filled"
              :fillcolor "#e8f5e9" :color "#2e7d32" :fontsize "11" :fontcolor "#1b5e20"}}}

    {:id "cluster_impl"
     :nodes ["template" "prepare" "job" "step" "specs"]
     :default-attributes
     {:graph {:label "Private Implementation" :style "rounded,filled"
              :fillcolor "#fafafa" :color "#90a4ae" :fontsize "11" :fontcolor "#455a64"}}}

    {:id "cluster_deps"
     :nodes ["malli" "specter" "xtdb"]
     :default-attributes
     {:graph {:label "External Dependencies" :style "rounded,filled"
              :fillcolor "#f5f5f5" :color "#bdbdbd" :fontsize "10" :fontcolor "#616161"}}}]})

;; ═══════════════════════════════════════════════════════════════════
;; 4. DONUT SYSTEM LIFECYCLE — component wiring
;; ═══════════════════════════════════════════════════════════════════

(def donut-system-graph
  {:flags #{:directed}

   :default-attributes
   {:graph {:fontname  "Helvetica Neue"
            :fontsize  "12"
            :bgcolor   "#fdfdfd"
            :rankdir   "LR"
            :label     "Donut System — Component Lifecycle Graph"
            :labelloc  "t"
            :style     "filled"
            :color     "white"
            :fillcolor "white"
            :rank      ""
            :cluster   ""
            :pad       "0.4"
            :nodesep   "0.6"
            :ranksep   "1.5"}
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
            :arrowsize "0.6"
            :label     ""
            :style     "solid"
            :penwidth  "1.2"
            :arrowhead "vee"}}

   :nodes [;; Config group
           {:id "cfg-env"
            :label "[:config :env]\n─────────────\nconfig/system-config\nAero EDN → map"
            :fillcolor "#fff3e0"
            :color "#e65100"
            :penwidth "2.0"}
           {:id "cfg-path"
            :label "[:config :config-path]\n─────────────\n\"grand-central/config.edn\"\n(static value)"
            :fillcolor "#fff3e0"
            :color "#e65100"
            :shape "note"}

           ;; Database group
           {:id "db-node"
            :label "[:database :node]\n─────────────\ndatabase/system-config\nXTDB v2 node\n::ds/start → start-xtdb-node\n::ds/stop  → stop-xtdb-node"
            :fillcolor "#e3f2fd"
            :color "#1565c0"
            :penwidth "2.0"}

           ;; HTTP client group
           {:id "http-inst"
            :label "[:http-client :instance]\n─────────────\nhttp-client/system-config\nHTTP connection pool"
            :fillcolor "#e3f2fd"
            :color "#1565c0"}

           ;; Job runner group
           {:id "jr-runner"
            :label "[:job-runner :job-runner]\n─────────────\njob-runner/system-config\nGoose producer\n+ worker pool\n+ consumer"
            :fillcolor "#e0f2f1"
            :color "#00695c"
            :penwidth "2.0"}

           ;; Resolver (runtime accessor)
           {:id "resolver"
            :label "resolver.clj\n─────────────\nget-xtdb-node\nget-producer\nget-config\nget-http-client\nget-job-runner"
            :fillcolor "#f3e5f5"
            :color "#6a1b9a"
            :shape "house"
            :penwidth "2.0"
            :fontsize "10"}]

   :edges [;; Config is root — everything reads from it
           {:from "cfg-path" :to "cfg-env"    :label "ds/local-ref\n[:config-path]" :color "#e65100" :penwidth "1.5"}
           {:from "cfg-env"  :to "db-node"    :label "ds/ref\n[:config :env\n :xtdb-config]" :penwidth "1.5"}
           {:from "cfg-env"  :to "jr-runner"  :label "ds/ref\n[:config :env\n :job-runner]" :penwidth "1.5"}
           {:from "cfg-env"  :to "http-inst"  :label "ds/ref\n[:config :env]" :style "dashed"}

           ;; Resolver reads running instances
           {:from "cfg-env"  :to "resolver"   :style "dashed" :color "#6a1b9a" :label "instances"}
           {:from "db-node"  :to "resolver"   :style "dashed" :color "#6a1b9a" :label "instances"}
           {:from "jr-runner" :to "resolver"  :style "dashed" :color "#6a1b9a" :label "instances"}
           {:from "http-inst" :to "resolver"  :style "dashed" :color "#6a1b9a" :label "instances"}]

   :subgraphs
   [{:id "cluster_config_grp"
     :nodes ["cfg-env" "cfg-path"]
     :default-attributes
     {:graph {:label ":config group" :style "rounded,filled" :fillcolor "#fff8e1"
              :color "#ff8f00" :fontsize "11" :fontcolor "#e65100"}}}

    {:id "cluster_db_grp"
     :nodes ["db-node"]
     :default-attributes
     {:graph {:label ":database group" :style "rounded,filled" :fillcolor "#e8eaf6"
              :color "#3949ab" :fontsize "11" :fontcolor "#1a237e"}}}

    {:id "cluster_http_grp"
     :nodes ["http-inst"]
     :default-attributes
     {:graph {:label ":http-client group" :style "rounded,filled" :fillcolor "#e8eaf6"
              :color "#3949ab" :fontsize "11" :fontcolor "#1a237e"}}}

    {:id "cluster_jr_grp"
     :nodes ["jr-runner"]
     :default-attributes
     {:graph {:label ":job-runner group" :style "rounded,filled" :fillcolor "#e0f2f1"
              :color "#00695c" :fontsize "11" :fontcolor "#004d40"}}}

    {:id "cluster_resolver"
     :nodes ["resolver"]
     :default-attributes
     {:graph {:label "Runtime Access" :style "rounded,filled" :fillcolor "#f3e5f5"
              :color "#7b1fa2" :fontsize "11" :fontcolor "#4a148c"}}}]})

;; ═══════════════════════════════════════════════════════════════════
;; RENDER
;; ═══════════════════════════════════════════════════════════════════

(def ^:private diagrams
  [{:graph system-architecture  :name "system-architecture"}
   {:graph polylith-component-map :name "polylith-component-map"}
   {:graph pipeline-internals    :name "pipeline-internals"}
   {:graph donut-system-graph    :name "donut-system-lifecycle"}])

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
  ;; Or render a single diagram:
  (render-graph system-architecture
                {:filename "docs/diagrams/system-architecture.png"
                 :layout-algorithm :dot})
  ;    
  )