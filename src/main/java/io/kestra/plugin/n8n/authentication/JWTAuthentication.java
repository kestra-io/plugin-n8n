package io.kestra.plugin.n8n.authentication;

import io.kestra.core.http.HttpRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JWTAuthentication extends Authentication {
    private String jwt;

    @Override
    public void applyAuthentication(HttpRequest.HttpRequestBuilder requestBuilder) {
        requestBuilder.addHeader("Authorization", "Bearer " + jwt);
    }
}
