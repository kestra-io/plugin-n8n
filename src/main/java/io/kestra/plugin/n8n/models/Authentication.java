package io.kestra.plugin.n8n.models;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Base64;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Authentication configuration for n8n webhook requests",
    description = "Supports various authentication methods for n8n webhook endpoints"
)
public class Authentication {
    
    @Schema(
        title = "Authentication type",
        description = "The type of authentication to use"
    )
    @PluginProperty
    private AuthType type;

    @Schema(
        title = "Username",
        description = "Username for Basic Auth"
    )
    @PluginProperty(dynamic = true)
    private String username;

    @Schema(
        title = "Password",
        description = "Password for Basic Auth"
    )
    @PluginProperty(dynamic = true)
    private String password;

    @Schema(
        title = "Header name",
        description = "Header name for Header Auth"
    )
    @PluginProperty(dynamic = true)
    private String headerName;

    @Schema(
        title = "Header value",
        description = "Header value for Header Auth"
    )
    @PluginProperty(dynamic = true)
    private String headerValue;

    @Schema(
        title = "JWT token",
        description = "JWT token for JWT authentication"
    )
    @PluginProperty(dynamic = true)
    private String jwtToken;

    @Schema(
        title = "Custom headers",
        description = "Custom headers for authentication"
    )
    @PluginProperty(dynamic = true)
    private Map<String, String> customHeaders;

    public Map<String, String> getHeaders(RunContext runContext) throws Exception {
        switch (type) {
            case BASIC_AUTH:
                return getBasicAuthHeaders(runContext);
            case HEADER_AUTH:
                return getHeaderAuthHeaders(runContext);
            case JWT:
                return getJwtHeaders(runContext);
            case CUSTOM:
                return getCustomHeaders(runContext);
            default:
                return Map.of();
        }
    }

    private Map<String, String> getBasicAuthHeaders(RunContext runContext) throws Exception {
        String renderedUsername = runContext.render(username);
        String renderedPassword = runContext.render(password);
        String credentials = renderedUsername + ":" + renderedPassword;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return Map.of("Authorization", "Basic " + encodedCredentials);
    }

    private Map<String, String> getHeaderAuthHeaders(RunContext runContext) throws Exception {
        String renderedHeaderName = runContext.render(headerName);
        String renderedHeaderValue = runContext.render(headerValue);
        return Map.of(renderedHeaderName, renderedHeaderValue);
    }

    private Map<String, String> getJwtHeaders(RunContext runContext) throws Exception {
        String renderedToken = runContext.render(jwtToken);
        return Map.of("Authorization", "Bearer " + renderedToken);
    }

    private Map<String, String> getCustomHeaders(RunContext runContext) {
        if (customHeaders == null) {
            return Map.of();
        }
        return customHeaders;
    }

    @Schema(enumAsRef = true)
    public enum AuthType {
        BASIC_AUTH,
        HEADER_AUTH,
        JWT,
        CUSTOM
    }
}
