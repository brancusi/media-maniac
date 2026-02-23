---
name: dev-startup-shutdown
description: How to startup and shutdown the project.
---


## Prerequisites

- Docker must be running on the host machine

## Steps (in order)

### 1. Start Docker services

In a new terminal session start docker. Don't worry about reading the output. Just start it and move on. Name the terminal session "Docker Compose Up"

```bash
docker compose -f bases/grand-central/docker-compose.yaml up
```

### 2. Start nREPL + connect Calva

Run VS Code command `calva.jackIn`. This is pre-configured in `.vscode/settings.json` to:

Rename the new terminal session to "Clojure Repl"

- Use aliases: `:dev`, `:xtdb`, `:test`, `+default`
- Auto-run `(system/start-dev)` after REPL connects (starts Portal, XTDB, HTTP client, Goose job runner)

No manual REPL evaluation needed — the `afterCLJReplJackInCode` handles system boot.

### 3. Start test watcher (optional)

In a separate terminal, start Kaocha in watch mode so tests re-run automatically on file changes:

```bash
clojure -M:dev:test:kaocha --watch
```

Name this terminal session "Test Watcher". Press Ctrl-C to stop.

This uses the standalone `tests.edn` config at the project root. SLF4J warnings in output are harmless — ignore them.

## Expected system startup log

```
Starting system
Attempting to close the portal
Creating HTTP client
Starting Redis related
Starting xtdb node
Grand Central system initialized.
```

## Shutdown (in order)

### 1. Stop the test watcher (if running)

Kill the terminal session called "Test Watcher" (or press Ctrl-C in it)

### 2. Stop the REPL process

Kill the jackin terminal session called "Clojure Repl"

### 3. Stop Docker services

Kill the docker compose terminal sesssion called "Docker Compose Up"