---
name: clojure-interactive-dev
description: Interactive Clojure development using Calva Backseat Driver REPL tools — evaluation, symbol lookup, structural editing, and REPL-driven workflow.
---

# Clojure Interactive Development

This skill teaches how to use the Calva Backseat Driver tools for interactive Clojure development with a live REPL.

## Available REPL Tools

### Core evaluation

- **`clojure_evaluate_code`** — Evaluate Clojure code in the connected REPL. Requires `code`, `namespace`, and `replSessionKey` (use `"clj"` for this project).
- **`clojure_list_sessions`** — List active REPL sessions to discover available `replSessionKey` values.
- **`clojure_repl_output_log`** — Read recent REPL output (stdout, stderr, tap> output). Check this regularly during development to see application logs and tap> debug output flowing to Portal.

### Symbol & documentation lookup

- **`clojure_symbol_info`** — Look up a symbol's docstring, argument list, source file, and type. Use this to understand functions before calling them.
- **`clojuredocs_info`** — Look up Clojure core (and core-adjacent) symbols on clojuredocs.org for docs, examples, and see-also references.

### Structural editing

- **`replace_top_level_form`** — Replace an existing top-level form with new code. Includes automatic bracket balancing and formatting. Use for modifying existing functions/defs.
- **`insert_top_level_form`** — Insert a new top-level form at a specific location. Includes automatic bracket balancing and formatting. Use for adding new functions/defs.
- **`clojure_balance_brackets`** — Fix bracket balance in a code string. Use when constructing complex forms.
- **`clojure_create_file`** — Create a new Clojure file with properly balanced brackets.
- **`clojure_append_code`** — Append code to the end of a Clojure file with automatic bracket balancing.

## Interactive Development Workflow

### REPL-first approach

Always follow the REPL-first workflow for Clojure development:

1. **Understand** — Use `clojure_symbol_info` and `clojure_evaluate_code` to explore existing code and data shapes before making changes.
2. **Experiment** — Evaluate small expressions in the REPL to test hypotheses and understand behavior.
3. **Implement** — Use structural editing tools (`replace_top_level_form`, `insert_top_level_form`) to make changes.
4. **Verify** — Evaluate the changed code in the REPL to confirm it works with real data.

### Evaluation best practices

- Always specify the correct `namespace` when evaluating. Use the namespace of the file being worked on.
- Use `replSessionKey: "clj"` for all evaluations in this project.
- For exploring a namespace, evaluate `(dir some.namespace)` or `(ns-publics 'some.namespace)`.
- To see a function's source: `(clojure.repl/source some-fn)`.
- To test functions, evaluate them with real or sample data rather than guessing at behavior.
- When evaluating code that requires specific namespaces, include a `(require '[...])` form first.

### Example: Exploring and modifying code

```clojure
;; 1. Look up a function
;; Use clojure_symbol_info for: com.atd.mm.pipeline.interface/create-pipeline

;; 2. Evaluate to understand current behavior
(require '[com.atd.mm.pipeline.interface :as pipeline])
(pipeline/create-pipeline {:name "test"})

;; 3. After modifying, re-evaluate the changed namespace
(require '[com.atd.mm.pipeline.core :reload])
```

### Checking application state

```clojure
;; Check if systems are running
(require '[donut.system.repl.state :as dsr-state])
(some? dsr-state/system) ;; => true if dev system (Portal) is up

(require '[com.atd.mm.grand-central.resolver :as resolver])
(some? resolver/rs)      ;; => true if grand-central is up

;; Access the XTDB node
(resolver/get-xtdb-node)

;; Access config
(resolver/get-config)
```

## Structural Editing Guidelines

When using `replace_top_level_form` or `insert_top_level_form`:

- Target forms by their first line content (the `(defn name` or `(def name` part).
- Provide the complete replacement form — these tools replace entire top-level forms.
- The tools automatically handle bracket balancing and formatting.
- After editing, evaluate the modified form in the REPL to load the new definition.
- Prefer these tools over `replace_string_in_file` for Clojure code — they understand Clojure structure.

## Debugging with tap> and Portal

This project uses Portal as the dev data inspector. Data sent via `tap>` appears in the Portal UI.

```clojure
;; Send data to Portal for inspection
(tap> {:some "data" :to "inspect"})

;; Add tap> to trace function calls
(defn my-fn [x]
  (tap> {:fn :my-fn :input x})
  (let [result (process x)]
    (tap> {:fn :my-fn :result result})
    result))
```

Check `clojure_repl_output_log` to see tap> output and application logs.

## Common Patterns in This Project

### Requiring component interfaces

```clojure
;; Always require through the interface namespace — never internal namespaces
(require '[com.atd.mm.config.interface :as config])
(require '[com.atd.mm.database.interface :as database])
(require '[com.atd.mm.pipeline.interface :as pipeline])
(require '[com.atd.mm.core-utils.interface :as utils])
```

### XTDB queries (v2 / XTQL)

```clojure
(require '[xtdb.api :as xt])
(let [node (resolver/get-xtdb-node)]
  ;; Basic query
  (xt/q node '(from :table [*]))
  ;; Parametric query
  (xt/q node '(from :table [{:xt/id $id} *]) {:args {:id some-uuid}}))
```

### System lifecycle (dev workflow)

```clojure
;; These are available in the system namespace
(system/start-dev)        ;; Start Portal + grand-central (initial boot)
(system/restart)          ;; Quick restart (no ns reload)
(system/full-refresh)     ;; Nuclear: stop → unload all ns → reload → restart
(system/nuke-xtdb!)       ;; Wipe XTDB data and restart fresh
```
