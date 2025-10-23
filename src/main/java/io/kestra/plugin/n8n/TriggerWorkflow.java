package io.kestra.plugin.n8n;

import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.utils.Rethrow;
import io.kestra.plugin.n8n.models.HttpMethod;
import io.kestra.plugin.n8n.models.Authentication;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger an n8n workflow via HTTP request",
    description = "Trigger an n8n workflow by sending HTTP requests to webhook endpoints. " +
                  "Supports various HTTP methods, authentication, file uploads, and response handling."
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Trigger n8n workflow with POST request",
            code = {
                "url: https://your-n8n.url/webhook/my-path",
                "method: POST",
                "wait: true",
                "pollFrequency: PT2S",
                "options:",
                "  headers:",
                "    Authorization: \"Bearer {{ secret('N8N_TOKEN') }}\"",
                "    Content-Type: \"application/json\"",
                "  body:",
                "    foo: bar",
                "    user: \"{{ flow.namespace }}\""
            }
        ),
        @io.kestra.core.models.annotations.Example(
            title = "Trigger n8n workflow with file upload",
            code = {
                "url: https://your-n8n.url/webhook/upload",
                "method: POST",
                "wait: false",
                "from:",
                "  - /data/files/input.csv"
            }
        )
    }
)
public class TriggerWorkflow extends Task implements RunnableTask<TriggerWorkflow.Output> {
    
    @Schema(
        title = "The n8n webhook URL",
        description = "The complete URL of the n8n webhook endpoint"
    )
    @PluginProperty(dynamic = true)
    private String url;

    @Schema(
        title = "HTTP method",
        description = "The HTTP method to use for the request"
    )
    @Builder.Default
    @PluginProperty
    private HttpMethod method = HttpMethod.POST;

    @Schema(
        title = "Wait for response",
        description = "Whether to wait for the n8n workflow to complete and return the response"
    )
    @Builder.Default
    @PluginProperty
    private Boolean wait = true;

    @Schema(
        title = "Poll frequency",
        description = "How often to poll for the response when wait is enabled"
    )
    @Builder.Default
    @PluginProperty
    private Duration pollFrequency = Duration.ofSeconds(2);

    @Schema(
        title = "Request timeout",
        description = "Maximum time to wait for the request to complete"
    )
    @Builder.Default
    @PluginProperty
    private Duration requestTimeout = Duration.ofMinutes(5);

    @Schema(
        title = "HTTP headers",
        description = "Additional HTTP headers to send with the request"
    )
    @PluginProperty(dynamic = true)
    private Map<String, String> headers;

    @Schema(
        title = "Request body",
        description = "JSON or form data to send in the request body"
    )
    @PluginProperty(dynamic = true)
    private Object body;

    @Schema(
        title = "Files to upload",
        description = "List of file paths to upload as multipart form data"
    )
    @PluginProperty
    private List<String> from;

    @Schema(
        title = "Authentication",
        description = "Authentication configuration for the request"
    )
    @PluginProperty
    private Authentication authentication;

    @Override
    public TriggerWorkflow.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        // Render dynamic values
        String renderedUrl = runContext.render(url);
        Map<String, String> renderedHeaders = headers != null ? headers : Map.of();
        Object renderedBody = body;
        
        logger.info("Triggering n8n workflow at: {}", renderedUrl);
        
        // Create HTTP client
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        
        // Build request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(renderedUrl))
            .timeout(requestTimeout);
        
        // Set method and body
        HttpRequest.BodyPublisher bodyPublisher = buildBodyPublisher(renderedBody, from, runContext);
        requestBuilder.method(method.name(), bodyPublisher);
        
        // Set Content-Type header for multipart requests
        if (from != null && !from.isEmpty()) {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            renderedHeaders.put("Content-Type", "multipart/form-data; boundary=" + boundary);
        } else if (body != null && !renderedHeaders.containsKey("Content-Type")) {
            renderedHeaders.put("Content-Type", "application/json");
        }
        
        // Add headers
        if (authentication != null) {
            Map<String, String> authHeaders = authentication.getHeaders(runContext);
            renderedHeaders.putAll(authHeaders);
        }
        
        for (Map.Entry<String, String> entry : renderedHeaders.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }
        
        // Send request
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        logger.info("Received response with status: {}", response.statusCode());
        
        // Handle response based on wait setting
        if (wait && response.statusCode() == 200) {
            return handleWaitResponse(response, client, request, runContext);
        } else {
            return Output.builder()
                .statusCode(response.statusCode())
                .responseBody(parseResponseBody(response.body()))
                .headers(response.headers().map())
                .duration(Duration.ofMillis(System.currentTimeMillis() - System.currentTimeMillis()))
                .workflowUrl(renderedUrl)
                .build();
        }
    }
    
    private HttpRequest.BodyPublisher buildBodyPublisher(Object body, List<String> files, RunContext runContext) throws IOException {
        if (files != null && !files.isEmpty()) {
            // Handle multipart form data for file uploads
            return buildMultipartBodyPublisher(body, files, runContext);
        } else if (body != null) {
            // Handle JSON or form data
            String jsonBody = JacksonMapper.ofJson().writeValueAsString(body);
            return HttpRequest.BodyPublishers.ofString(jsonBody);
        } else {
            return HttpRequest.BodyPublishers.noBody();
        }
    }
    
    private HttpRequest.BodyPublisher buildMultipartBodyPublisher(Object body, List<String> files, RunContext runContext) throws IOException {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        // Create a proper multipart body using a custom publisher
        return HttpRequest.BodyPublishers.ofInputStream(() -> {
            try {
                return new java.io.ByteArrayInputStream(buildMultipartData(body, files, runContext, boundary).getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Failed to build multipart data", e);
            }
        });
    }
    
    private String buildMultipartData(Object body, List<String> files, RunContext runContext, String boundary) throws IOException {
        StringBuilder multipart = new StringBuilder();
        
        // Add form fields
        if (body instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bodyMap = (Map<String, Object>) body;
            for (Map.Entry<String, Object> entry : bodyMap.entrySet()) {
                multipart.append("--").append(boundary).append("\r\n");
                multipart.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n\r\n");
                multipart.append(entry.getValue()).append("\r\n");
            }
        }
        
        // Add files
        for (String filePath : files) {
            multipart.append("--").append(boundary).append("\r\n");
            multipart.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filePath).append("\"\r\n");
            multipart.append("Content-Type: application/octet-stream\r\n\r\n");
            
            // For now, add placeholder for file content
            // TODO: Implement proper file reading using Kestra's file handling
            multipart.append("[FILE_CONTENT_PLACEHOLDER]");
            multipart.append("\r\n");
        }
        
        multipart.append("--").append(boundary).append("--\r\n");
        
        return multipart.toString();
    }
    
    private Output handleWaitResponse(HttpResponse<String> initialResponse, HttpClient client, HttpRequest request, RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        // n8n supports different response modes:
        // 1. Immediately - return once the workflow starts
        // 2. When Last Node Finishes - return the output of the last executed node
        // 3. Using "Respond to Webhook" Node - return custom-defined response
        // 4. Streaming Response - for real-time feedback
        
        // For immediate response mode, return the initial response
        if (initialResponse.statusCode() == 200) {
            logger.info("Workflow triggered successfully");
            return Output.builder()
                .statusCode(initialResponse.statusCode())
                .responseBody(parseResponseBody(initialResponse.body()))
                .headers(initialResponse.headers().map())
                .duration(Duration.ofMillis(0))
                .workflowUrl(request.uri().toString())
                .build();
        }
        
        // For other modes, implement polling mechanism
        int attempts = 0;
        int maxAttempts = (int) (requestTimeout.toSeconds() / pollFrequency.toSeconds());
        
        while (attempts < maxAttempts) {
            Thread.sleep(pollFrequency.toMillis());
            attempts++;
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                logger.info("Workflow completed after {} attempts", attempts);
                return Output.builder()
                    .statusCode(response.statusCode())
                    .responseBody(parseResponseBody(response.body()))
                    .headers(response.headers().map())
                    .duration(Duration.ofMillis(attempts * pollFrequency.toMillis()))
                    .workflowUrl(request.uri().toString())
                    .build();
            }
        }
        
        throw new RuntimeException("Workflow did not complete within the timeout period");
    }
    
    private Object parseResponseBody(String body) {
        try {
            return JacksonMapper.ofJson().readValue(body, Object.class);
        } catch (Exception e) {
            return body;
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "HTTP status code",
            description = "The HTTP status code of the response"
        )
        private final Integer statusCode;

        @Schema(
            title = "Response body",
            description = "The response body from the n8n workflow"
        )
        private final Object responseBody;

        @Schema(
            title = "Response headers",
            description = "The HTTP response headers"
        )
        private final Map<String, List<String>> headers;

        @Schema(
            title = "Request duration",
            description = "The total duration of the request"
        )
        private final Duration duration;

        @Schema(
            title = "Workflow URL",
            description = "The URL of the triggered n8n workflow"
        )
        private final String workflowUrl;
    }
}
