package io.kestra.plugin.n8n;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractTriggerWorkflow extends Task {
    private static final ContentType DEFAULT_CONTENT_TYPE = ContentType.BINARY;
    protected static final boolean DEFAULT_WAIT = true;

    @Schema(title = "HTTP client configuration")
    protected HttpConfiguration options;

    @Schema(
        title = "n8n webhook URL",
        description = "Webhook endpoint from the target n8n workflow. Use the Test URL during development and switch to the Production URL for live runs."
    )
    @NotNull
    private Property<String> uri;

    @Schema(
        title = "Content type",
        description = "Format used for the request body. Default is BINARY for file sends; select JSON, XML, or TEXT when sending structured or textual payloads."
    )
    @Builder.Default
    private Property<ContentType> contentType = Property.ofValue(ContentType.BINARY);

    @Schema(
        title = "Request body",
        description = "JSON-compatible data to send in the body. Ignored if `from` is set. Maximum payload size 16 MB."
    )
    private Property<Map<String, ?>> body;

    @Schema(
        title = "Query parameters",
        description = "Extra query parameters appended to the webhook URL; available to the n8n workflow as request data."
    )
    private Property<Map<String, ?>> queryParameters;

    @Schema(
        title = "HTTP headers",
        description = "Custom headers for authentication, content negotiation, or metadata forwarded to n8n."
    )
    private Property<Map<String, ?>> headers;

    @Schema(
        title = "File source URI",
        description = "Kestra storage URI for the request body. Use instead of `body` when sending binaries or large content; mutually exclusive with `body`."
    )
    private Property<URI> from;

    @Schema(
        title = "HTTP method",
        description = "Request method sent to the webhook. Must match the method configured on the n8n webhook; supports DELETE, GET, PATCH, POST, and PUT."
    )
    @NotNull
    private Property<HttpMethod> method;

    @Schema(
        title = "Wait for response",
        description = "Whether to wait for the n8n response. Defaults to true; respects n8n response mode (immediate, deferred, streaming)."
    )
    @Builder.Default
    protected Property<Boolean> wait = Property.ofValue(DEFAULT_WAIT);

    protected HttpRequest buildRequest(RunContext runContext) throws Exception {
        String rUri = runContext.render(this.uri).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("URL cannot be null")
        );

        URI rFrom = runContext.render(this.from).as(URI.class).orElse(null);
        HttpMethod rMethod = runContext.render(this.method).as(HttpMethod.class)
            .orElseThrow(() -> new IllegalArgumentException("HTTP Method cannot be null"));

        ContentType rContentType = runContext.render(this.contentType).as(ContentType.class).orElse(DEFAULT_CONTENT_TYPE);
        Map<String, ?> rBody = runContext.render(this.body).asMap(String.class, Object.class);
        Map<String, ?> rQueryParameters = runContext.render(this.queryParameters).asMap(String.class, Object.class);
        Map<String, ?> rHeaders = runContext.render(this.headers).asMap(String.class, Object.class);

        if (rFrom != null && !rBody.isEmpty()) {
            throw new IllegalArgumentException("You cannot set both 'from' and 'body' properties at the same time");
        }

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(buildUri(rUri, rQueryParameters))
            .method(rMethod.name());

        rHeaders.forEach((key, value) -> requestBuilder.addHeader(key, value.toString()));

        if (!rBody.isEmpty()) {
            setRequestBody(requestBuilder, rBody);
        }

        if (rFrom != null) {
            setRequestBody(runContext, requestBuilder, rFrom, rContentType);
        }

        return requestBuilder.build();
    }

    private URI buildUri(String url, Map<String, ?> queryParameters) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url);
        queryParameters.forEach((key, value) -> uriBuilder.addParameter(key, value.toString()));

        return uriBuilder.build();
    }

    private static void setRequestBody(HttpRequest.HttpRequestBuilder requestBuilder, Map<String, ?> body) {
        requestBuilder.body(
            HttpRequest.JsonRequestBody
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
}
