package io.kestra.plugin.templates;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
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
public class TriggerWorkflow extends Task implements RunnableTask<Output> {
    private static final ContentType DEFAULT_CONTENT_TYPE = ContentType.BINARY;
    private static final boolean DEFAULT_WAIT = true;

    @Schema(
        title = "Authentication Details for the N8N webhook",
        description = "Full description of this input"
    )
    private Property<Authentication> authentication;

    @Schema(title = "The HTTP client configuration.")
    HttpConfiguration options;

    @Schema(
        title = "N8N webhook URL",
        description = "Full description of this input"
    )
    @NotNull
    private Property<URI> url;


    @Schema(
        title = "N8N webhook URL",
        description = "Full description of this input"
    )
    @Builder.Default
    private Property<ContentType> contentTypeProperty = Property.ofValue(ContentType.BINARY);

    @Schema(
        title = "N8N webhook URL",
        description = "Full description of this input"
    )
    private Property<Map<String, Object>> body;

    @Schema(
        title = "N8N webhook URL",
        description = "Full description of this input"
    )
    private Property<Map<String, Object>> queryParameters;

    @Schema(
        title = "N8N webhook URL",
        description = "Full description of this input"
    )
    private Property<Map<String, Object>> headers;

    @Schema(
        title = "N8N webhook URL",
        description = "Full description of this input"
    )
    private Property<URI> from;

    @Schema(
        title = "method",
        description = "Full description of this input"
    )
    @NotNull
    private Property<HttpMethod> method;

    @Schema(
        title = "method",
        description = "Full description of this input"
    )
    @Builder.Default
    private Property<Boolean> wait = Property.ofValue(DEFAULT_WAIT);

    @Override
    public Output run(RunContext runContext) throws Exception {
        URI uri = runContext.render(this.url).as(URI.class).orElseThrow(
            () -> new IllegalArgumentException("URL cannot be null")
        );

        URI from = runContext.render(this.from).as(URI.class).orElse(null);
        HttpMethod rMethod = runContext.render(this.method).as(HttpMethod.class)
            .orElseThrow(() -> new IllegalArgumentException("HTTP Method cannot be null"));
        boolean wait = runContext.render(this.wait).as(Boolean.class).orElse(DEFAULT_WAIT);
        ContentType contentType = runContext.render(this.contentTypeProperty).as(ContentType.class).orElse(DEFAULT_CONTENT_TYPE);
        Map<String, Object> body = runContext.render(this.body).asMap(String.class, Object.class);
        Map<String, Object> queryParameters = runContext.render(this.queryParameters).asMap(String.class, Object.class);
        Map<String, Object> headers = runContext.render(this.headers).asMap(String.class, Object.class);

        if (from != null && !body.isEmpty()) {
            throw new IllegalArgumentException("You cannot set both 'from' and 'body' properties at the same time");
        }




        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(buildUri(uri.toString(), queryParameters))
            .method(rMethod.name());

        headers.forEach((key, value) -> requestBuilder.addHeader(key, value.toString()));

        if (!body.isEmpty()) {
            setRequestBody(requestBuilder, body);
        }

        if (from != null) {
            setRequestBody(runContext, requestBuilder, from, contentType);
        }

        getAuthentication(runContext).ifPresent(authentication -> {
            authentication.applyAuthentication(requestBuilder);
        });



        return makeRequest(runContext, requestBuilder.build(), wait);
    }

    public record Output(
        int statusCode,
        String body
    ) implements io.kestra.core.models.tasks.Output {
    }

    private URI buildUri(String url, Map<String, Object> queryParameters) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url);
        queryParameters.forEach((key, value) -> {
            uriBuilder.addParameter(key, value.toString());
        });

        return uriBuilder.build();
    }

    private String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Output makeRequest(RunContext runContext, HttpRequest request, boolean wait) throws Exception {
        CompletableFuture<Output> completableFuture = new CompletableFuture<>();
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


    private static void setRequestBody(HttpRequest.HttpRequestBuilder requestBuilder, Map<String, Object> body) {
        requestBuilder.body(HttpRequest.JsonRequestBody
            .builder()
            .content(body)
            .build()
        );
    }

    private static void setRequestBody(RunContext runContext, HttpRequest.HttpRequestBuilder requestBuilder, URI uri, ContentType contentType) throws IOException {
        InputStream bodyInputStream = runContext.storage().getFile(uri);
        HttpRequest.RequestBody requestBody = switch (contentType) {
            case XML, JSON, TEXT -> HttpRequest.StringRequestBody.builder()
                .contentType(contentType.name())
                .content(new String(bodyInputStream.readAllBytes()))
                .build();
            case BINARY -> HttpRequest.ByteArrayRequestBody.builder()
                .contentType(contentType.name())
                .content(bodyInputStream.readAllBytes())
                .build();
        };

        requestBuilder.body(requestBody);
    }

    /*
    Polymorphic configuration objects with @JsonTypeInfo/@JsonSubTypes don't deserialize correctly
    when wrapped in Property<>. Jackson cannot resolve the type discriminator ('type' field)
    during Property deserialization, causing "missing type id property 'type'" errors.
    To get round this, we first render the Property as a Map and then cast it to the target class.
     */
    private Optional<Authentication> getAuthentication(RunContext runContext) throws Exception {
        Map<?, ?> rAuthentication = (Map<?,?>) runContext.render(this.authentication).asMap(String.class, Object.class);

        if (rAuthentication.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(JacksonMapper.cast(
            rAuthentication,
            Authentication.class
        ));
    }
}
