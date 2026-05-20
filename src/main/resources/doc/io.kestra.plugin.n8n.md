# How to use the n8n plugin

Trigger n8n workflows via webhook from Kestra flows.

## Authentication

Set `uri` to your n8n webhook URL (this is the secret credential — store it in a [secret](https://kestra.io/docs/concepts/secret)). For authenticated webhooks, pass credentials via `options.auth` (basic auth) or add an API key header via `headers`. Apply connection properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

`TriggerWorkflow` fires an n8n webhook — set `uri` and `method` (both required; `method` accepts `GET`, `POST`, `PUT`, `PATCH`, or `DELETE`). Pass a request payload via `body` (a map rendered as JSON), add query parameters via `queryParameters`, or stream a file from internal storage via `from`. Set `contentType` to control the Content-Type header (default `BINARY`). By default the task waits for the webhook response (`wait: true`); set `wait: false` to return immediately. The output includes `statusCode` and `body`.
