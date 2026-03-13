# Kestra n8n Plugin

## What

description = 'n8n Plugin for Kestra Exposes 1 plugin components (tasks, triggers, and/or conditions).

## Why

Enables Kestra workflows to interact with n8n, allowing orchestration of n8n-based operations as part of data pipelines and automation workflows.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `n8n`

Infrastructure dependencies (Docker Compose services):

- `n8n`

### Key Plugin Classes

- `io.kestra.plugin.n8n.webhook.TriggerWorkflow`

### Project Structure

```
plugin-n8n/
├── src/main/java/io/kestra/plugin/n8n/webhook/
├── src/test/java/io/kestra/plugin/n8n/webhook/
├── build.gradle
└── README.md
```

### Important Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run tests
./gradlew test

# Build without tests
./gradlew shadowJar -x test
```

### Configuration

All tasks and triggers accept standard Kestra plugin properties. Credentials should use
`{{ secret('SECRET_NAME') }}` — never hardcode real values.

## Agents

**IMPORTANT:** This is a Kestra plugin repository (prefixed by `plugin-`, `storage-`, or `secret-`). You **MUST** delegate all coding tasks to the `kestra-plugin-developer` agent. Do NOT implement code changes directly — always use this agent.
