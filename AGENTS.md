# Kestra n8n Plugin

## What

- Provides plugin components under `io.kestra.plugin.n8n`.
- Includes classes such as `HttpMethod`, `TriggerWorkflow`, `ContentType`.

## Why

- What user problem does this solve? Teams need to trigger n8n workflows from Kestra from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps n8n steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on n8n.

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
