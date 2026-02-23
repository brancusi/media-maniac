- # Getting Started — Dev Environment
- This guide walks through setting up a local development environment for [[Media Maniac]] from scratch.
-
- ## Prerequisites
  - ### Required Software
    - **Java 21+** — XTDB v2 requires modern JVM with native access
      - ```bash
        # macOS (Homebrew)
        brew install openjdk@21
        ```
    - **Clojure CLI** (tools.deps)
      - ```bash
        brew install clojure/tools/clojure
        ```
    - **Docker & Docker Compose** — for Redis and supporting services
      - ```bash
        brew install --cask docker
        ```
    - **FFmpeg & FFprobe** — media processing engine
      - ```bash
        brew install ffmpeg
        ```
    - **xxhsum** — fast file hashing
      - ```bash
        brew install xxhash
        ```
    - **Polylith CLI** (optional, for workspace management)
      - Runs via the `:poly` alias: `clj -M:poly`
  - ### Recommended Editor
    - **VS Code** with [Calva](https://calva.io) extension for Clojure REPL integration
    - Alternatively: IntelliJ with Cursive, Emacs with CIDER, or Neovim with Conjure
-
- ## Step-by-Step Setup
  - ### 1. Clone the repo
    - ```bash
      git clone <repo-url> media-maniac
      cd media-maniac
      ```
  - ### 2. Start infrastructure services
    - Redis (job queue broker) and RedisInsight (GUI) run via Docker Compose.
    - ```bash
      docker compose -f bases/grand-central/docker-compose.yaml up -d
      ```
    - This starts:
      - **Redis** on `localhost:6379`
      - **RedisInsight** on `localhost:5540` (web UI for inspecting Redis)
    - Verify:
      - ```bash
        docker ps
        # Should show redis and redisinsight containers running
        ```
  - ### 3. Create secrets file
    - The config system expects a secrets file that is gitignored:
    - ```bash
      touch bases/grand-central/resources/grand-central/.secrets.edn
      ```
    - Add any required secrets as an EDN map:
    - ```clojure
      {:api-key "your-key-here"}
      ```
    - This file is loaded via Aero `#include` from the main config
  - ### 4. Start the REPL
    - The primary development workflow is REPL-driven. Start a REPL with the `:dev` and `:xtdb` aliases:
    - ```bash
      clj -M:dev:xtdb
      ```
    - Or connect via your editor:
      - **Calva (VS Code):** `Ctrl+Alt+C Ctrl+Alt+J` → select `deps.edn` → check `:dev` and `:xtdb` aliases
      - **Cursive:** Create a deps.edn run config with aliases `:dev,:xtdb`
  - ### 5. Start the system from the REPL
    - Once your REPL is connected, the `user` namespace is loaded automatically. Start the full system:
    - ```clojure
      ;; In the REPL:
      (require '[system :as sys])

      ;; Start everything (Portal debug UI + grand-central system)
      (sys/start-dev)

      ;; To restart after code changes:
      (sys/refresh-and-restart)
      ```

    - This will:
      - Start **Portal** — a data inspector that captures all `tap>` output (opens in browser or VS Code)
      - Read config from `grand-central/config.edn`
      - Start the **XTDB** database node
      - Create the **Hato HTTP client**
      - Start the **Goose job runner** — 1 Redis producer + 3 worker pools

  - ### XTDB Storage (Dev)
    - By default, XTDB is configured to store its transaction log locally at `./tmp/mm/log`. This is set in `bases/grand-central/resources/grand-central/config.edn`:
    - ```clojure
      :xtdb-config {:log [:local {:path "./tmp/mm/log"}]}
      ```
    - The `tmp/` directory is gitignored. This is fine for local development — the node is embedded and requires no external database server.
    - To reset the database, stop the system and delete the log directory:
    - ```bash
      rm -rf tmp/mm/log
      ```
    - TODO Determine production XTDB storage backend (Kafka log, remote object store, PostgreSQL) and document deployment configuration
      :LOGBOOK:
      :END:
  - ### 6. Verify everything is running
    - ```clojure
      ;; Check the running system instances:
      (require '[com.atd.mm.grand-central.resolver :as r])

      ;; Get the XTDB node
      (r/get-xtdb-node)

      ;; Get the job runner
      (r/get-job-runner)

      ;; Get the loaded config
      (r/get-config)
      ```

    - Portal should be open in your browser showing a live data stream

-
- ## Project Layout
  - ```
    media-maniac/
    ├── deps.edn                  ← Root dependency config (aliases: :dev, :test, :xtdb, :poly)
    ├── workspace.edn             ← Polylith workspace config
    ├── bases/
    │   └── grand-central/        ← Deployable base (system wiring, models, entry point)
    │       ├── docker-compose.yaml
    │       ├── resources/grand-central/config.edn
    │       └── src/com/atd/mm/grand_central/
    ├── components/
    │   ├── config/               ← Aero config reader
    │   ├── core-utils/           ← Shared utilities
    │   ├── database/             ← XTDB v2 node + schema
    │   ├── http-client/          ← HTTP client
    │   ├── job-runner/           ← Goose job queue
    │   ├── media-converter/      ← FFmpeg processing pipeline
    │   ├── media-ingest/         ← Media discovery + metadata extraction
    │   └── user/                 ← User management (scaffold)
    ├── development/
    │   └── src/
    │       ├── user.clj          ← REPL entry namespace
    │       ├── system.clj        ← Dev system start/stop/restart
    │       ├── logging.clj       ← Portal config
    │       └── debug.clj         ← Debugging utilities (spy, stash)
    ├── docs/                     ← Logseq knowledge base
    └── tmp/                      ← Local data (XTDB logs, Redis dumps) — gitignored
    ```
-
- ## Common Workflows
  - ### Restart after code changes
    - ```clojure
      (sys/refresh-and-restart)
      ```
    - Uses `tools.namespace` to reload changed namespaces, then restarts the donut.system
  - ### Run Polylith commands
    - ```bash
      # Check workspace info
      clj -M:poly info

      # Check dependencies between components
      clj -M:poly deps

      # Validate workspace
      clj -M:poly check

      # See what changed since last stable
      clj -M:poly diff
      ```

  - ### Run tests
    - ```bash
      # Run all tests via Polylith (one-shot, uses Kaocha under the hood)
      clj -M:poly test :dev

      # Run all tests via standalone Kaocha
      clojure -M:dev:test:kaocha

      # Watch mode — re-runs on file change, stays running
      clojure -M:dev:test:kaocha --watch
      ```

    - Watch mode uses `tests.edn` at the project root. Press Ctrl-C to stop.
    - SLF4J warnings in output are harmless — ignore them.

  - ### Inspect data with Portal
    - Anything sent to `tap>` appears in Portal:
    - ```clojure
      (tap> {:some "data"})
      (tap> (r/get-config))
      ```
    - Use the debug utilities:
    - ```clojure
      (require '[debug :as d])
      (d/spy :label some-value)     ;; tap> with label
      (d/stash :key some-value)     ;; save to atom for later
      (d/peek-stash :key)           ;; retrieve stashed value
      ```
  - ### Work with the database
    - ```clojure
      (require '[xtdb.api :as xt])

      ;; Get the node
      (def node (r/get-xtdb-node))

      ;; Submit a document
      (xt/execute-tx node [[:put-docs :media {:xt/id (random-uuid)
                                              :media/original-filename "clip001.mov"
                                              :media/file-type :video}]])

      ;; Query
      (xt/q node '(from :media [*]))
      ```

  - ### Analyze media files
    - ```clojure
      (require '[com.atd.mm.media-ingest.interface :as ingest])

      ;; Get full metadata for a file
      (ingest/get-comprehensive-file-info "/path/to/video.mov")

      ;; Analyze an SD card
      (ingest/analyze-sd-card "/Volumes/SDCARD")
      ```

-
- ## Ports & Services Reference
  - | Service      | Port     | URL                      |
    | ------------ | -------- | ------------------------ |
    | Redis        | 6379     | `redis://localhost:6379` |
    | RedisInsight | 5540     | `http://localhost:5540`  |
    | XTDB         | embedded | (in-process, no port)    |
    | Portal       | dynamic  | Opens in browser/VS Code |
-
- ## Troubleshooting
  - ### XTDB fails to start
    - Ensure you're using the `:xtdb` alias which adds required JVM flags:
    - ```
      --add-opens=java.base/java.nio=ALL-UNNAMED
      -Dio.netty.tryReflectionSetAccessible=true
      --enable-native-access=ALL-UNNAMED
      ```
  - ### Redis connection refused
    - Make sure Docker containers are running:
    - ```bash
      docker compose -f bases/grand-central/docker-compose.yaml up -d
      ```
  - ### Namespace not found after adding a component
    - Run `(sys/refresh-and-restart)` to reload all namespaces
    - If still failing, restart the REPL entirely
