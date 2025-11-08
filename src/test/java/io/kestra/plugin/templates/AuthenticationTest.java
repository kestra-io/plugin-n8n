package io.kestra.plugin.templates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.templates.authentication.Authentication;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

@KestraTest
class AuthenticationTest {
    @Inject
    RunContextFactory runContextFactory = new RunContextFactory();

    ObjectMapper objectMapper = JacksonMapper.ofJson()
        .copy()
        .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);



    @Test
    void idk() throws Exception {
        String json = "{\"type\":\"BasicAuth\",\"username\":\"fasdf\",\"password\":\"fasdfasdf\"}";
        System.out.println(json);
        Map authentication = objectMapper.convertValue(
            json,
            Map.class
        );
    }

    @Test
    void givenValidYaml_whenDeserialized_thenCorrectSubclass() throws Exception {
        Property<Map<String, Object>> property = Property.ofExpression("{\"type\":\"BasicAuth\",\"username\":\"fasdf\",\"password\":\"fasdfasdf\"}");
        RunContext runContext = runContextFactory.of();

        Map<String, Object> authenticationMap = runContext.render(property).asMap(String.class, Object.class);
        Authentication authentication = JacksonMapper.cast(authenticationMap, Authentication.class);
        System.out.println(authentication);
    }
}