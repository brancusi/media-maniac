```skill
---
name: diagrams
description: Generating architecture diagrams with clj-graphviz — best practices, common pitfalls, and doc integration.
---

## Library

- **clj-graphviz 0.6.4** — `com.phronemophobic/clj-graphviz`
- Core function: `(com.phronemophobic.clj-graphviz/render-graph graph opts)`
- Outputs: PNG (default), SVG (`:format :svg`)
- Layout: `:dot` (default), `:neato`, `:fdp`, `:circo`, `:twopi`

## CRITICAL: default-attributes

Every graphviz attribute used on **any** node, edge, or subgraph MUST have a default declared in the top-level `:default-attributes` map. If an attribute appears on a node/edge but has no default, **rendering will silently fail or crash**.

### Required node defaults

```clojure
:node {:fontname "Helvetica Neue" :fontsize "10" :shape "box"
       :style "rounded,filled" :fillcolor "#f0f4f8" :color "#4a6fa5"
       :penwidth "1.5" :label "" :fontcolor "#2d3436"
       :width "0" :height "0"}
```

### Required edge defaults

```clojure
:edge {:fontname "Helvetica Neue" :fontsize "8" :color "#636e72"
       :arrowsize "0.7" :label "" :style "solid"
       :penwidth "1.0" :arrowhead "normal"}
```

### Required graph defaults

```clojure
:graph {:fontname "Helvetica Neue" :fontsize "12" :bgcolor "#fdfdfd"
        :rankdir "TB" :label "" :labelloc "t" :style "filled"
        :color "white" :fillcolor "white" :rank "" :cluster ""
        :pad "0.5" :nodesep "0.6" :ranksep "0.8"}
```

**If you add a new attribute** (e.g. `:arrowhead "diamond"` on an edge), you MUST ensure `:arrowhead` is in the edge defaults. Same for `:width`, `:height` on nodes, `:rank` on graphs, etc.

## Graph structure

```clojure
{:flags #{:directed}
 :default-attributes {:graph {...} :node {...} :edge {...}}
 :nodes [{:id "node-id" :label "text" ...overrides...}]
 :edges [{:from "a" :to "b" :label "text" ...overrides...}]
 :subgraphs [{:id "cluster_name"
              :nodes ["node-id"]
              :default-attributes {:graph {:label "Group" :style "rounded,filled" ...}}}]}
```

### Key rules

- Node `:id` must be unique across the entire graph (including across subgraphs).
- Subgraph IDs **must** start with `cluster_` for graphviz to draw a box around them.
- Subgraph `:default-attributes` only needs `:graph` — node/edge defaults inherit from top level.
- All attribute values are **strings** (not numbers or keywords): `"1.5"` not `1.5`.
- Use `\n` in labels for line breaks, `\\l` for left-aligned lines in record shapes.
- For invisible edges (layout hints only): `{:style "invis"}`.

## Useful node shapes

| Shape | Use for |
|-------|---------|
| `box` | Default, general nodes |
| `record` | Structured data (uses `{header|field1\\lfield2\\l}` syntax) |
| `cylinder` | Databases, stores |
| `folder` | File system sources |
| `note` | Artifacts, outputs |
| `diamond` | Decision points, dispatchers |
| `hexagon` | Entry points, bases |
| `component` | UML-style components |
| `tab` | Templates, blueprints |
| `cds` | Transformation/processing |
| `ellipse` | External libraries |
| `house` | Accessors, resolvers |
| `parallelogram` | External tools |
| `plaintext` | Invisible labels |

## File organization

All diagram namespaces live in `development/src/diagram/`:

```
development/src/diagram/
├── all.clj               ← Top-level: regenerates everything
├── fiddle.clj            ← Architecture diagrams (system, polylith, internals, donut)
└── pipeline_concept.clj  ← Pipeline concept diagram (template → job → steps)
```

### Convention per file

Each diagram namespace must:

1. Define graph data as top-level `def` vars.
2. Collect all graphs in a `diagrams` vector of `{:graph g :name "filename-stem"}`.
3. Expose a `render-all!` function that writes PNG + SVG to `docs/diagrams/`.

```clojure
(def ^:private diagrams
  [{:graph my-graph :name "my-graph"}])

(defn render-all! []
  (let [out-dir "docs/diagrams"]
    (.mkdirs (java.io.File. out-dir))
    (doseq [{:keys [graph name]} diagrams]
      (render-graph graph {:filename (str out-dir "/" name ".png")
                           :layout-algorithm :dot})
      (render-graph graph {:filename (str out-dir "/" name ".svg")
                           :format :svg
                           :layout-algorithm :dot})
      (println "  ✓" name))))
```

### Adding a new diagram namespace

1. Create `development/src/diagram/my_thing.clj` with `(ns diagram.my-thing ...)`.
2. Define graph vars + `diagrams` vector + `render-all!` function.
3. Register in `diagram.all`:
   ```clojure
   (ns diagram.all
     (:require [diagram.my-thing :as my-thing] ...))
   ```
   Add to the `render-all!` body.

### Top-level regeneration

`diagram.all/render-all!` calls every namespace's `render-all!`. Evaluate it in the REPL to rebuild all diagrams:

```clojure
(require '[diagram.all :as diagrams])
(diagrams/render-all!)
```

## Output directory

All rendered images go to `docs/diagrams/`. Each diagram produces both `.png` and `.svg`.

## Integrating diagrams into docs

Docs live in `docs/pages/` (Logseq markdown). Reference diagrams using relative paths from the page:

```markdown
![Description](../diagrams/my-diagram.png)
```

After adding or updating a diagram:
1. Regenerate: `(diagram.all/render-all!)` in the REPL.
2. Add an `![alt text](../diagrams/filename.png)` reference in the relevant doc page.
3. Commit both the diagram source (`.clj`) and the rendered outputs (`.png`, `.svg`).

## Current diagram inventory

| Diagram | Namespace | Description |
|---------|-----------|-------------|
| `system-architecture` | `diagram.fiddle` | End-to-end media processing pipeline |
| `polylith-component-map` | `diagram.fiddle` | All components, bases, and their dependencies |
| `pipeline-internals` | `diagram.fiddle` | Pipeline component namespace & data flow |
| `donut-system-lifecycle` | `diagram.fiddle` | Donut System ds/ref wiring graph |
| `pipeline-concept` | `diagram.pipeline-concept` | Template → job → step execution lifecycle |

## Common pitfalls

1. **Missing default attribute** — diagram renders blank or crashes. Always check defaults when adding new attributes.
2. **Subgraph ID without `cluster_` prefix** — graphviz won't draw the bounding box.
3. **Duplicate node IDs** — silent rendering errors. Each `:id` must be globally unique.
4. **Numeric attribute values** — use `"1.5"` not `1.5`. Graphviz attributes are always strings.
5. **Record shape escaping** — use `\\l` not `\l` for left-aligned fields in Clojure strings.
6. **Edge between nodes in different subgraphs** — works fine, just declare the edge at the top level.

## Design guidelines

- Use consistent color palettes across diagrams (green for domain, blue for infra, orange for entry points).
- Keep node labels concise — use `\n─────────\n` separators for multi-section labels.
- Use dashed styles for TODO/stub/future components.
- Include a legend subgraph with `{:rank "sink"}` to push it to the bottom.
- Prefer `:dot` layout for hierarchical diagrams, `:neato` for network-style.
```
