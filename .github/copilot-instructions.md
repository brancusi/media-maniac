# Media Maniac — Copilot Instructions

## Project Overview

Media Maniac is a Clojure application for managing a large media archive. It processes raw footage through automated pipelines: ingesting from SD cards, generating proxies, extracting frames for visual search, transcribing audio, and creating lossless subclips for remote delivery.

**Top namespace:** `com.atd.mm`

## Architecture: Polylith

This project uses [Polylith](https://polylith.gitbook.io/polylith) as its architectural framework. All code lives in a single monorepo with strict separation between components, bases, and projects.

### Workspace Structure

```
bases/          → Deployable entry points (currently: grand-central)
components/     → Reusable building blocks with interface/implementation separation
projects/       → Deployable artifacts (build configs)
development/    → REPL-driven dev environment
```

### Creating a New Component

Use the Polylith CLI:

```bash
clj -M:poly create component name:<component-name>
```

This creates:

```
components/<component-name>/
├── deps.edn
├── resources/<component-name>/
├── src/com/atd/mm/<component_name>/
│   └── interface.clj      ← Public API (required)
└── test/com/atd/mm/<component_name>/
    └── interface_test.clj
```

### The Interface Pattern

Every component MUST expose its API through `interface.clj`. Implementation goes in `core.clj` or `impl/` subdirectories. The interface delegates to implementation:

```clojure
;; components/my-feature/src/com/atd/mm/my_feature/interface.clj
(ns com.atd.mm.my-feature.interface
  (:require [com.atd.mm.my-feature.core :as impl]))

;; Public API — other components/bases call ONLY these functions
(defn do-something [arg]
  (impl/do-something arg))

(def system-config impl/system-config)  ;; If this component has lifecycle
```

```clojure
;; components/my-feature/src/com/atd/mm/my_feature/core.clj
(ns com.atd.mm.my-feature.core
  (:require [party.donut.system :as ds]))

(defn do-something [arg]
  ;; actual implementation here
  )
```

**Rules:**
- Other components and bases may ONLY require `<component>.interface` — never `<component>.core` or internal namespaces
- All public functions must be declared in `interface.clj`
- Implementation namespaces (`core.clj`, `impl/*.clj`) are private to the component
- If a component has many implementation files, use an `impl/` subdirectory (see `core-utils` for example)

### For Deeper Implementation Hierarchies

When a component has multiple implementation concerns (like `core-utils`):

```
components/core-utils/src/com/atd/mm/core_utils/
├── interface.clj           ← Single public API
└── impl/
    ├── file.clj            ← File operations
    ├── string.clj          ← String operations
    ├── url.clj             ← URL parsing
    └── ...
```

The interface requires all impl namespaces and re-exports selected functions:

```clojure
(ns com.atd.mm.core-utils.interface
  (:require [com.atd.mm.core-utils.impl.file :as file]
            [com.atd.mm.core-utils.impl.string :as string]))

(defn hash-file [path] (file/hash-file path))
(defn force-int [v] (string/force-int v))
```

### Registering a Component in the Workspace

After creating a component, add it to the root `deps.edn` under the `:dev` alias:

```clojure
;; deps.edn → :aliases → :dev → :extra-deps
poly/my-feature {:local/root "components/my-feature"}
```

And add its test path:

```clojure
;; deps.edn → :aliases → :test → :extra-paths
"components/my-feature/test"
```

### Component Dependencies

Declare inter-component dependencies in the component's own `deps.edn`:

```clojure
;; components/my-feature/deps.edn
{:paths ["src" "resources"]
 :deps {;; External deps only
        some.lib/library {:mvn/version "1.0.0"}}
 :aliases {:test {:extra-paths ["test"]
                  :extra-deps {}}}}
```

At runtime, components reference each other through interface namespaces:

```clojure
(require '[com.atd.mm.config.interface :as config])
(require '[com.atd.mm.core-utils.interface :as utils])
```

## Component Lifecycle: Donut System

[Donut System](https://github.com/donut-engineering/system) manages component start/stop lifecycle.

### Defining a System Component

Components that need lifecycle (connections, stateful resources) expose a `system-config` var:

```clojure
(ns com.atd.mm.my-feature.core
  (:require [party.donut.system :as ds]))

(def system-config
  #::ds{:start  (fn [{{:keys [some-config]} ::ds/config}]
                  ;; Return the running instance
                  (create-thing some-config))
        :stop   (fn [{::ds/keys [instance]}]
                  ;; Clean up
                  (.close instance))
        :config {:some-config (ds/ref [:config :env :my-feature])}})
```

**Key patterns:**
- `::ds/start` receives a map containing `::ds/config` (resolved config values)
- `::ds/stop` receives a map containing `::ds/instance` (whatever start returned)
- `ds/ref [:config :env :key]` — reference another component's output in the system graph. Path is `[group-key component-key & path]`
- `ds/local-ref [:key]` — reference a sibling key in the same component group

### Wiring Components into the System

The base's `system.clj` assembles all components:

```clojure
;; bases/grand-central/src/com/atd/mm/grand_central/system.clj
(ns com.atd.mm.grand-central.system
  (:require [party.donut.system :as ds]
            [com.atd.mm.config.interface :as config]
            [com.atd.mm.database.interface :as database]
            [com.atd.mm.my-feature.interface :as my-feature]))

(def system-config
  {::ds/defs
   {:config      {:env config/system-config
                   :config-path "grand-central/config.edn"}

    :database    {:node database/system-config}

    :my-feature  {:instance my-feature/system-config}}})
```

**System map structure:**
```
{::ds/defs
 {<group-key>  {<component-key>  <component-config>
                <sibling-key>    <static-value>}}}
```

- Groups organize related components (`:config`, `:database`, `:job-runner`, etc.)
- Each group can have multiple keys — some are donut.system components (with `::ds/start`), others are plain values (like `:config-path`)
- References resolve across the graph: `(ds/ref [:config :env])` points to group `:config`, key `:env`, getting whatever its `::ds/start` returned

### Starting/Stopping the System

```clojure
;; Start
(def running-system (ds/signal system-config ::ds/start))

;; Stop
(ds/signal running-system ::ds/stop)
```

In dev, use the helpers in `development/src/system.clj`:

```clojure
(require '[system :as sys])
(sys/start-dev)              ;; Start Portal + grand-central system
(sys/refresh-and-restart)    ;; Reload code + restart
```

### Accessing Running Instances (Resolver Pattern)

The `grand-central.resolver` namespace provides typed accessors for the running system:

```clojure
(ns com.atd.mm.grand-central.resolver)

(defonce rs nil)  ;; Holds the running system

(defn get-xtdb-node []
  (get-in (ds/instances rs) [:database :node]))

(defn get-config []
  (get-in (ds/instances rs) [:config :env]))

(defn get-job-runner []
  (get-in (ds/instances rs) [:job-runner :job-runner]))
```

When adding a new system component, add a corresponding accessor in resolver.

## Processing Pipeline Pattern

Media processing uses a multimethod-based dispatch system.

### Defining a New Processor

1. Create a file in `components/media-converter/src/com/atd/mm/media_converter/processors/`:

```clojure
(ns com.atd.mm.media-converter.processors.my-processor
  (:require [com.atd.mm.media-converter.processors.core :as core]))

(defmethod core/process-rule :media/my-process
  [{:keys [opts]}]
  ;; Process the media file according to opts
  ;; Return result map
  )
```

2. Require the new processor in `processors/interface.clj` to register the multimethod:

```clojure
(ns com.atd.mm.media-converter.processors.interface
  (:require [com.atd.mm.media-converter.processors.proxy]
            [com.atd.mm.media-converter.processors.my-processor]  ;; Add this
            ...))
```

3. Add the new type to the Malli spec in `grand-central/model/specs.clj`:

```clojure
[:type [:enum :media/proxy :media/extract-audio :media/extract-stills
        :media/transcribe :media/copy :media/my-process]]  ;; Add here
```

## Configuration

Config uses [Aero](https://github.com/juxt/aero) to read EDN with tagged literals.

Main config: `bases/grand-central/resources/grand-central/config.edn`

```clojure
{:env #or [#env ENV :dev]
 :local #include "grand-central/.secrets.edn"
 :xtdb-config {:log [:local {:path "./tmp/mm/log"}]}
 :job-runner {:redis {:url "redis://localhost:6379"
                      :pool-opts {:max-total-per-key 10
                                  :max-idle-per-key  10
                                  :min-idle-per-key  2}}
              :workers [...]}}
```

When adding a new config section:
1. Add the key to `config.edn`
2. Reference it in the system component: `(ds/ref [:config :env :my-new-key])`

## Database: XTDB v2

XTDB v2 uses XTQL query syntax. Key patterns:

```clojure
;; Basic query
(xt/q node '(from :table [*]))

;; Parametric query
(xt/q node '(from :table [{:xt/id $id} *])
      {:args {:id some-uuid}})

;; With pull (nested)
(xt/q node '(-> (from :pipelines [{:xt/id $id} *])
                (return {:processes (pull* (from :processes [{:xt/id $process-id} *]))})))
```

Schema is defined as attribute maps in `components/database/src/com/atd/mm/database/schema.clj`.

## Job Queue: Goose

Background jobs use Goose with Redis broker.

### Enqueueing a Job

```clojure
(require '[com.atd.mm.job-runner.interface :as jobs])

;; Queue a job — handler-fn must be a fully qualified symbol
(jobs/queue-job 'com.atd.mm.media-converter.interface/process-video
                [{:src "/path/to/file.mov" :rules [...]}]
                {:queue "heavy-process"
                 :producer (get-producer)})
```

### Worker Queues

| Queue | Threads | Use For |
|-------|---------|---------|
| `default` | 5 | General tasks |
| `light-process` | 5 | Quick operations (metadata, hashing) |
| `heavy-process` | 1 | FFmpeg transcoding, large file ops |

## Code Style & Conventions

- Write small, composable functions
- Prefer Clojure idioms over Java interop unless necessary
- Use `tap>` for debugging output (flows to Portal)
- Use namespaced keywords for domain data (`:media/id`, `:transfer/status`)
- All data validation via Malli schemas
- FFmpeg/FFprobe invocations go through `clojure.java.shell/sh`
- File hashing uses imoHash (fast) for dedup, XXH3-64 for verification

## Infrastructure

- **Local dev:** Everything runs on the dev machine. XTDB is embedded (no external DB server).
- **Services:** Redis and RedisInsight run via Docker Compose from `bases/grand-central/docker-compose.yaml`
- **Storage:** XTDB transaction log in `./tmp/mm/log`, Redis data in `./tmp/redis`
