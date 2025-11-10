package io.kestra.plugin.templates.webhook;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.templates.ContentType;
import io.kestra.plugin.templates.HttpMethod;
import io.kestra.plugin.templates.authentication.Authentication;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractTriggerWorkflow extends Task {
    private static final ContentType DEFAULT_CONTENT_TYPE = ContentType.BINARY;
    protected static final boolean DEFAULT_WAIT = true;

    @Schema(
        title = "Authentication Details for the N8N webhook",
        description = "Authentication method for the n8n webhook. Supports Basic auth, Header auth, JWT auth, or None. Configure credentials to secure your webhook endpoint."
    )
    private Property<Authentication> authentication;

    @Schema(title = "The HTTP client configuration.")
    HttpConfiguration options;

    @Schema(
        title = "N8N webhook URL",
        description = "The webhook URL endpoint from your n8n workflow. Use the Test URL for development or Production URL for live workflows. n8n generates unique URLs to avoid conflicts."
    )
    @NotNull
    private Property<String> uri;


    @Schema(
        title = "Content Type",
        description = "Format of the request body data. Choose BINARY for files, JSON for structured data, XML for XML documents, or TEXT for plain text content."
    )
    @Builder.Default
    private Property<ContentType> contentType = Property.ofValue(ContentType.BINARY);

    @Schema(
        title = "Request Body",
        description = "JSON data to send in the request body. Maximum payload size is 16MB. Use this for POST, PUT, or PATCH requests to send structured data to the n8n webhook."
    )
    private Property<Map<String, ?>> body;

    @Schema(
        title = "Query Parameters",
        description = "URL query parameters to append to the webhook URL. These parameters will be available in the n8n workflow as part of the incoming request data."
    )
    private Property<Map<String, ?>> queryParameters;

    @Schema(
        title = "HTTP Headers",
        description = "Custom HTTP headers to include with the webhook request. Headers are useful for authentication, content type specification, or passing additional metadata to the n8n workflow."
    )
    private Property<Map<String, ?>> headers;

    @Schema(
        title = "File Source URI",
        description = "URI pointing to a file in Kestra storage to send as the request body. Use this instead of 'body' when sending binary data, files, or large content to the n8n webhook."
    )
    private Property<URI> from;

    @Schema(
        title = "HTTP Method",
        description = "HTTP request method for the webhook call. n8n supports DELETE, GET, HEAD, PATCH, POST, and PUT methods. Choose the method that matches your n8n webhook configuration."
    )
    @NotNull
    private Property<HttpMethod> method;

    @Schema(
        title = "Wait for Response",
        description = "Whether to wait for the n8n webhook response. When true, waits for the workflow to complete based on the webhook's response mode (immediate, deferred, or streaming)."
    )
    @Builder.Default
    protected Property<Boolean> wait = Property.ofValue(DEFAULT_WAIT);

    protected HttpRequest buildRequest(RunContext runContext) throws Exception {
        String uri = runContext.render(this.uri).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("URL cannot be null")
        );

        URI from = runContext.render(this.from).as(URI.class).orElse(null);
        HttpMethod rMethod = runContext.render(this.method).as(HttpMethod.class)
            .orElseThrow(() -> new IllegalArgumentException("HTTP Method cannot be null"));

        ContentType contentType = runContext.render(this.contentType).as(ContentType.class).orElse(DEFAULT_CONTENT_TYPE);
        Map<String, ?> body = runContext.render(this.body).asMap(String.class, Object.class);
        Map<String, ?> queryParameters = runContext.render(this.queryParameters).asMap(String.class, Object.class);
        Map<String, ?> headers = runContext.render(this.headers).asMap(String.class, Object.class);

        if (from != null && !body.isEmpty()) {
            throw new IllegalArgumentException("You cannot set both 'from' and 'body' properties at the same time");
        }

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(buildUri(uri, queryParameters))
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

        return requestBuilder.build();
    }

    private URI buildUri(String url, Map<String, ?> queryParameters) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url);
        queryParameters.forEach((key, value) -> {
            uriBuilder.addParameter(key, value.toString());
        });

        return uriBuilder.build();
    }

    private static void setRequestBody(HttpRequest.HttpRequestBuilder requestBuilder, Map<String, ?> body) {
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
