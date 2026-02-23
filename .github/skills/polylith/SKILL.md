---
name: polylith
description: Polylith best practices — naming, component granularity, CLI, and wiring.
---

## Naming

- **Always singular nouns**: `user`, `pipeline`, `database` — never plurals.
- One word for one domain concept: `pipeline` not `pipeline-job`, `article` not `article-handler`.
- Infrastructure wrappers name the thing they wrap: `database`, `config`, `log`.
- External integrations use the service name: `http-client`, `crm-api`.

## When to split vs merge components

**One component** when the concepts share a single domain identity — e.g. pipeline templates and pipeline job execution are both "pipeline".

**Split into two** only when:
1. Component A is used in projects that don't need Component B (genuine independent reuse).
2. There is no direct `require` from one to the other's interface — if A already requires B's interface, they're coupled.
3. The concepts genuinely belong to different bounded contexts (e.g. `user` vs `billing`).

**Red flags for over-splitting:** bridge functions that cross between two components, one component that can't function without the other, identical XTDB table references.

## Component structure

Use `core.clj` for small components. For larger ones, use named implementation files:

```
components/pipeline/src/com/atd/mm/pipeline/
├── interface.clj    ← Public API (only namespace others may require)
├── specs.clj        ← Malli schemas
├── template.clj     ← Template CRUD
├── prepare.clj      ← Job preparation
├── job.clj          ← Job CRUD
└── step.clj         ← Step CRUD
```

## Creating a component

```bash
clj -M:poly create component name:<name>
```

Then register in root `deps.edn`:
- `:dev` → `:extra-deps`: `poly/<name> {:local/root "components/<name>"}`
- `:test` → `:extra-paths`: `"components/<name>/test"`

External deps go in `components/<name>/deps.edn`. Never list other Polylith components there.

## Interface rules

- `interface.clj` is the **only** namespace other components/bases may require.
- Every public function must appear in `interface.clj` as a pass-through.
- Implementation namespaces (`core.clj`, `template.clj`, etc.) are private.

## Donut System lifecycle (optional)

For stateful components, expose `system-config` in the impl and re-export via interface:

```clojure
;; core.clj
(def system-config
  #::ds{:start  (fn [{{:keys [setting]} ::ds/config}] (create-thing setting))
        :stop   (fn [{::ds/keys [instance]}] (.close instance))
        :config {:setting (ds/ref [:config :env :my-feature])}})
```

Wire into `system.clj` under `::ds/defs`, add accessor in `resolver.clj`, add config in `config.edn`.

## Useful CLI commands

| Command | Purpose |
|---------|---------|
| `clj -M:poly check` | Validate workspace integrity |
| `clj -M:poly info` | Show components, bases, changes since last stable |
| `clj -M:poly deps` | Show inter-component dependencies |
| `clj -M:poly libs` | Show all external libraries |
| `clj -M:poly test` | Run tests for changed components only |
| `clj -M:poly create component name:<n>` | Scaffold a new component |
| `clj -M:poly create base name:<n>` | Scaffold a new base |
