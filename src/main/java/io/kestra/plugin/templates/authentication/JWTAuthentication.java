package io.kestra.plugin.templates.authentication;

import io.kestra.core.http.HttpRequest;
import lombok.Data;

@Data
public class JWTAuthentication extends Authentication {
    private String jwt;

    @Override
    void applyAuthentication(HttpRequest.HttpRequestBuilder requestBuilder) {
        requestBuilder.addHeader("Authorization", "Bearer " + jwt);
    }
}
