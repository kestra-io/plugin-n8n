package io.kestra.plugin.templates;

import io.kestra.core.http.HttpRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HeaderAuthentication extends Authentication {
    @NotNull
    private String name;

    @NotNull
    private String value;

    @Override
    void applyAuthentication(HttpRequest.HttpRequestBuilder requestBuilder) {
        requestBuilder.addHeader(name, value);
    }
}
