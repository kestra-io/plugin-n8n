package io.kestra.plugin.n8n.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.kestra.core.serializers.JacksonMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationTest {

    @Test
    void shouldDeserializeBasicAuthentication() throws JsonProcessingException {
        Map<String, ?> map = Map.of(
        "type", "BasicAuth",
        "username", "username",
        "password", "password"
        );

        Authentication authentication = JacksonMapper.cast(map, Authentication.class);

        assertInstanceOf(BasicAuthentication.class, authentication);
        assertEquals("username", ((BasicAuthentication) authentication).getUsername());
        assertEquals("password", ((BasicAuthentication) authentication).getPassword());
    }

    @Test
    void shouldDeserializeHeaderAuthentication() throws JsonProcessingException {
        Map<String, ?> map = Map.of(
            "type", "HeaderAuth",
            "name", "name",
            "value", "value"
        );

        Authentication authentication = JacksonMapper.cast(map, Authentication.class);

        assertInstanceOf(HeaderAuthentication.class, authentication);
        assertEquals("name", ((HeaderAuthentication) authentication).getName());
        assertEquals("value", ((HeaderAuthentication) authentication).getValue());
    }

    @Test
    void shouldDeserializeJWTAuthentication() throws JsonProcessingException {
        Map<String, ?> map = Map.of(
            "type", "JWTAuth",
            "jwt", "token"
        );

        Authentication authentication = JacksonMapper.cast(map, Authentication.class);

        assertInstanceOf(JWTAuthentication.class, authentication);
        assertEquals("token", ((JWTAuthentication) authentication).getJwt());
    }
}