# Media Maniac — Coding Agent Instructions

## Project Summary

Clojure 1.12 monorepo (Polylith architecture) for managing a media archive. Processes raw footage through pipelines: ingest from SD cards, generate proxies (FFmpeg), extract frames, transcribe audio, and create subclips. Uses XTDB v2 (embedded), Goose job queue (Redis), Donut System lifecycle. No CI/CD pipelines — validation is manual via `clj -M:poly check` and `clj -M:poly test :dev`.

**Top namespace:** `com.atd.mm` | **Runtime:** Java 21+, Clojure CLI 1.12.x | **External tools:** FFmpeg, xxhash, Docker

## Build & Validation Commands

Always run these commands from the repository root.

### Validate workspace integrity (always run after structural changes)
```bash
clj -M:poly check
```
Exit 0 = success (no output). Catches missing interface functions, broken deps, namespace violations.

### Run all tests (always run after code changes)
```bash
clj -M:poly test :dev
```
Runs all test suites via Kaocha (polylith-kaocha integration). SLF4J warnings in output are harmless — ignore them. Tests do NOT require Docker/Redis to be running.

### Run tests in watch mode (re-runs on file change)
```bash
clojure -M:dev:test:kaocha --watch
```
Stays running and re-runs affected tests when source or test files change. Uses standalone Kaocha with `tests.edn` config. Press Ctrl-C to stop.

### Lint (clj-kondo)
```bash
clj-kondo --lint bases components development
```
Config at `.clj-kondo/config.edn`. Specter macros are configured as lint-as rules.

### Start infrastructure (required only for REPL/runtime, not for tests)
```bash
docker compose -f bases/grand-central/docker-compose.yaml up -d
```
Starts Redis (:6379) and RedisInsight (:5540).

### Create a new Polylith component
```bash
clj -M:poly create component name:<component-name>
```
Then register it — see "Adding a Component" below.

## Project Layout

```
deps.edn                        ← Root: aliases :dev, :test, :xtdb, :kaocha, :poly
tests.edn                       ← Kaocha config (standalone watch mode)
workspace.edn                   ← Polylith config (top-namespace: com.atd.mm)
bases/grand-central/             ← Single deployable base (entry point, system wiring)
  src/com/atd/mm/grand_central/
    system.clj                   ← Donut System assembly (wires all components)
    resolver.clj                 ← Typed accessors for running system instances
    core.clj                     ← init!, shutdown!, -main
    model/specs.clj              ← Legacy schema re-exports
  resources/grand-central/
    config.edn                   ← Aero config (reads .secrets.edn via #include)
  docker-compose.yaml            ← Redis + RedisInsight
components/                      ← 9 components, each with interface.clj
  config/                        ← Aero config reader (aero 1.1.6)
  core-utils/                    ← Shared utilities (impl/ subdirectory pattern)
  database/                      ← XTDB v2 node + schema (xtdb 2.x-SNAPSHOT)
  http-client/                   ← Hato HTTP client
  job-runner/                    ← Goose job queue (goose 0.6.0)
  media-ingest/                  ← SD card analysis, ffprobe, camera detection
  media-processor/               ← FFmpeg processing, multimethod dispatch
    processors/core.clj          ← defmulti execute-process (dispatch on :processor)
    processors/{proxy,extract_stills,copy,extract_audio,transcribe}.clj
  pipeline/                      ← Pipeline templates, jobs, steps, Malli specs
  user/                          ← Placeholder/scaffold
development/src/
  system.clj                     ← Dev system (Portal + start/stop/restart helpers)
  user.clj                       ← REPL entry (requires system, debug, logging)
.clj-kondo/config.edn           ← Linting config (Specter macros)
.vscode/settings.json            ← Calva jack-in config (aliases: dev, xtdb, test)
```

## Architecture Rules (MUST follow)

### Polylith Interface Pattern
- Every component exposes its API through `<component>/interface.clj` ONLY
- Other components/bases may NEVER require `core.clj` or internal namespaces directly
- All public functions must appear in `interface.clj` as pass-throughs to implementation
- Implementation goes in `core.clj` or `impl/*.clj` (private to the component)

### Namespace Convention
- Source: `com.atd.mm.<component-name>.<file>` (e.g., `com.atd.mm.pipeline.job`)
- Interface: `com.atd.mm.<component-name>.interface`
- Tests: `com.atd.mm.<component-name>.interface-test`
- File paths use underscores for hyphens: `com.atd.mm.core-utils` → `core_utils/`

### Adding a Component (complete checklist)
1. `clj -M:poly create component name:<name>`
2. Root `deps.edn` → `:dev` → `:extra-deps`: add `poly/<name> {:local/root "components/<name>"}`
3. Root `deps.edn` → `:test` → `:extra-paths`: add `"components/<name>/test"`
4. If stateful: add `system-config` to impl, re-export via interface, wire into `bases/grand-central/src/com/atd/mm/grand_central/system.clj`, add accessor in `resolver.clj`, add config key in `config.edn`
5. Add source path to `development/src/system.clj` → `tn-repl/set-refresh-dirs`
6. Run `clj -M:poly check` to verify

### Adding a Media Processor
1. Create `components/media-processor/src/com/atd/mm/media_processor/processors/<name>.clj`
2. Implement `(defmethod core/execute-process :media/<type> ...)`
3. Require the new file in `processors/interface.clj`
4. Add the type keyword to `pipeline/specs.clj` processor enum
5. Run `clj -M:poly test :dev`

## Donut System Lifecycle

Components with state (DB connections, clients) use Donut System:
```clojure
(def system-config
  #::ds{:start  (fn [{{:keys [setting]} ::ds/config}] (create-thing setting))
        :stop   (fn [{::ds/keys [instance]}] (.close instance))
        :config {:setting (ds/ref [:config :env :my-key])}})
```
- `ds/ref [:group :component & path]` — cross-component reference
- `ds/local-ref [:key]` — sibling reference within same group
- System graph: `:config` → `:http-client` → `:database` → `:job-runner` → `:job-schedule`

## Key Dependencies & Patterns

| Library | Purpose | Key namespace |
|---------|---------|---------------|
| XTDB v2 | Database (XTQL queries, embedded) | `xtdb.api` |
| Goose | Job queue (Redis broker) | `goose.client`, `goose.worker` |
| Malli | Data validation schemas | `malli.core` |
| Aero | Config EDN reader | `aero.core` |
| Specter | Data navigation/transformation | `com.rpl.specter` |
| Hato | HTTP client | `hato.client` |
| Donut System | Component lifecycle | `donut.system` / `party.donut.system` |

**XTDB v2 requires JVM flags** (provided by `:xtdb` alias):
```
--add-opens=java.base/java.nio=ALL-UNNAMED
-Dio.netty.tryReflectionSetAccessible=true
--enable-native-access=ALL-UNNAMED
```

## Known Issues & Workarounds

- **SLF4J warnings** during test runs ("No SLF4J providers were found") are harmless — ignore them
- **Secrets file must exist**: `bases/grand-central/resources/grand-central/.secrets.edn` — create with `{}` if missing. It is gitignored but required by Aero `#include`
- **`clj -M:poly test :all`** without a stable git tag returns "No tests to run" — always use `clj -M:poly test :dev` instead
- **`clj -M:poly info`** uses ANSI colors that may not render in all terminals — add `:no-colors` flag if needed
- **No GitHub Actions / CI** — there are no workflow files. Validate locally with `clj -M:poly check` then `clj -M:poly test :dev`

## Code Style

- Small, composable functions; prefer Clojure idioms over Java interop
- Namespaced keywords for domain data: `:media/id`, `:pipeline/status`, `:transfer/type`
- Data validation via Malli schemas (defined in `specs.clj` files)
- FFmpeg/FFprobe via `clojure.java.shell/sh`
- Debugging via `tap>` (flows to Portal in dev)
- Tests use `clojure.test` with Kaocha runner (via polylith-kaocha for `poly test`, standalone for watch mode)

## Trust these instructions. Only search the codebase if information here is incomplete or found to be in error.
