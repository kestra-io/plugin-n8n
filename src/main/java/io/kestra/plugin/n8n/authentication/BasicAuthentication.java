package io.kestra.plugin.n8n.authentication;

import io.kestra.core.http.HttpRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Base64;

@Data
@AllArgsConstructor
public class BasicAuthentication extends Authentication {
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @NotNull
    private String username;

    @NotNull
    private String password;

    @Override
    public void applyAuthentication(HttpRequest.HttpRequestBuilder requestBuilder) {
        requestBuilder.addHeader(
            "Authorization",
            "Basic " + ENCODER.encodeToString((username + ":" + password).getBytes())
        );
    }
}
