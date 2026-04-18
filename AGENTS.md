# Kestra n8n Plugin

## What

- Provides plugin components under `io.kestra.plugin.n8n`.
- Includes classes such as `HttpMethod`, `TriggerWorkflow`, `ContentType`.

## Why

- This plugin integrates Kestra with n8n.
- It provides tasks that trigger n8n workflows from Kestra.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `n8n`

Infrastructure dependencies (Docker Compose services):

- `n8n`

### Key Plugin Classes

- `io.kestra.plugin.n8n.TriggerWorkflow`

### Project Structure

```
plugin-n8n/
├── src/main/java/io/kestra/plugin/n8n/
├── src/test/java/io/kestra/plugin/n8n/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
