package io.kestra.plugin.n8n.authentication;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.kestra.core.http.HttpRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = BasicAuthentication.class, name = "BasicAuth"),
    @JsonSubTypes.Type(value = HeaderAuthentication.class, name = "HeaderAuth"),
    @JsonSubTypes.Type(value = JWTAuthentication.class, name = "JWTAuth")
})
public abstract class Authentication {
    @NotNull
    protected String type;

    abstract public void applyAuthentication(HttpRequest.HttpRequestBuilder requestBuilder);
}
