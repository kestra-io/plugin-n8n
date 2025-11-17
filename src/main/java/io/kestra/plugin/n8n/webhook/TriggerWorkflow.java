package io.kestra.plugin.n8n.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger a n8n workflow via a webhook",
    description = "This task allows you to execute n8n workflows from within Kestra by calling their webhook URLs. " +
        "See the [n8n Webhook Docs](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook/) " +
        "for more information on getting started with n8n Webhooks."
)
@Plugin(
    examples = {
        @Example(
            title = "Simple Trigger Workflow",
            full = true,
            code = """
                id: n8n_webhook_trigger
                namespace: company.team

                tasks:
                  - id: trigger_workflow
                    type: io.kestra.plugin.n8n.webhook.TriggerWorkflow
                    method: POST
                    uri: https://n8n.example.com/webhook/213e8fbc-f843-428c-9860-ab9f64e5ef3b
                """
        ),
        @Example(
            title = "Trigger Workflow With Basic Auth",
            full = true,
            code = """
                id: n8n_webhook_trigger_with_auth
                namespace: company.team

                tasks:
                  - id: trigger_workflow
                    type: io.kestra.plugin.n8n.webhook.TriggerWorkflow
                    options:
                      auth:
                        type: BASIC
                        username: "{{ secret('N8N_WEBHOOK_USERNAME') }}"
                        password: "{{ secret('N8N_WEBHOOK_PASSWORD') }}"
                    method: POST
                    uri: https://n8n.example.com/webhook/213e8fbc-f843-428c-9860-ab9f64e5ef3b
                """
        ),
        @Example(
            title = "Trigger Workflow With Body",
            full = true,
            code = """
                id: n8n_webhook_trigger_with_body
                namespace: company.team

                tasks:
                  - id: trigger_workflow
                    type: io.kestra.plugin.n8n.webhook.TriggerWorkflow
                    body:
                      keyOne: valueOne
                    method: POST
                    uri: http://n8n:5678/webhook/213e8fbc-f843-428c-9860-ab9f64e5ef3b
                """
        )
    }
)
public class TriggerWorkflow extends AbstractTriggerWorkflow implements RunnableTask<Output> {
    private final static ObjectMapper mapper = JacksonMapper.ofJson(false);

    @Override
    public Output run(RunContext runContext) throws Exception {
        HttpRequest httpRequest = buildRequest(runContext);
        boolean rWait = runContext.render(this.wait).as(Boolean.class).orElse(DEFAULT_WAIT);

        return makeRequest(runContext, httpRequest, rWait);
    }

    @Builder
    public record Output(
        int statusCode,
        Object body
    ) implements io.kestra.core.models.tasks.Output {
    }

    private TriggerWorkflow.Output makeRequest(RunContext runContext, HttpRequest request, boolean wait) throws Exception {
        CompletableFuture<TriggerWorkflow.Output> completableFuture = new CompletableFuture<>();
        try (HttpClient client = new HttpClient(runContext, this.options)) {
            client.request(request, handleResponse(wait, completableFuture));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return completableFuture.get();
    }

    private static Consumer<HttpResponse<InputStream>> handleResponse(boolean wait, CompletableFuture<Output> completableFuture) {
        return (HttpResponse<InputStream> response) -> {
            if (response.getStatus().getCode() != 200) {
                completableFuture.completeExceptionally(new Exception("Received non-200 response from Webhook: " + response.getStatus().getCode()));
                return;
            }

            if (!wait) {
                completableFuture.complete(new Output(
                    response.getStatus().getCode(),
                    null
                ));
                return;
            }

            try {
                String contentTypeHeader = response.getHeaders().firstValue("Content-Type").orElse(null);
                completableFuture.complete(new Output(
                    response.getStatus().getCode(),
                    parseBodyForContentType(response.getBody(), contentTypeHeader)
                ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Object parseBodyForContentType(InputStream inputStream, @Nullable String contentType) throws IOException {
        if (contentType != null && contentType.contains("application/json")) {
            return mapper.readValue(inputStream, Map.class);
        }

        return new String(inputStream.readAllBytes());
    }
}
