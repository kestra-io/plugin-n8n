package io.kestra.plugin.n8n.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD;

    @JsonCreator
    public static HttpMethod fromString(String value) {
        return HttpMethod.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }
}
