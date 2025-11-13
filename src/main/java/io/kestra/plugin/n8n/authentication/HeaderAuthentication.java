package io.kestra.plugin.n8n.authentication;

import io.kestra.core.http.HttpRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HeaderAuthentication extends Authentication {
    @NotNull
    private String name;

    @NotNull
    private String value;

    @Override
    public void applyAuthentication(HttpRequest.HttpRequestBuilder requestBuilder) {
        requestBuilder.addHeader(name, value);
    }
}
