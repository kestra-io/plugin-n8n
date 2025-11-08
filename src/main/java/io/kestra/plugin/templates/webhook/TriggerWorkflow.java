package io.kestra.plugin.templates.webhook;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger a N8N workflow via a webhook",
    description = "Full description of this task"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Simple revert",
            code = { "format: \"Text to be reverted\"" }
        )
    }
)
public class TriggerWorkflow extends AbstractTriggerWorkflow implements RunnableTask<Output> {
    @Override
    public Output run(RunContext runContext) throws Exception {
        HttpRequest httpRequest = buildRequest(runContext);
        boolean wait = runContext.render(this.wait).as(Boolean.class).orElse(DEFAULT_WAIT);

        return makeRequest(runContext, httpRequest, wait);
    }

    public record Output(
        int statusCode,
        String body
    ) implements io.kestra.core.models.tasks.Output {
    }

    private TriggerWorkflow.Output makeRequest(RunContext runContext, HttpRequest request, boolean wait) throws Exception {
        CompletableFuture<TriggerWorkflow.Output> completableFuture = new CompletableFuture<>();
        try (HttpClient client = new HttpClient(runContext, options)) {
            client.request(request, handleResponse(wait, completableFuture));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return completableFuture.get();
    }

    private Consumer<HttpResponse<InputStream>> handleResponse(boolean wait, CompletableFuture<Output> completableFuture) {
        return (HttpResponse<InputStream> response) -> {
            if (response.getStatus().getCode() != 200) {
                completableFuture.completeExceptionally(new Exception("Received non-200 response from Apify API: " + response.getStatus().getCode()));
                return;
            }

            if (!wait) {
                completableFuture.complete(new Output(
                    response.getStatus().getCode(),
                    ""
                ));
                return;
            }

            try {
                completableFuture.complete(new Output(
                    response.getStatus().getCode(),
                    new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)
                ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
