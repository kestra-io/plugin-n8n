package io.kestra.plugin.n8n;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.n8n.models.Authentication;
import io.kestra.plugin.n8n.models.HttpMethod;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration test for TriggerWorkflow task
 * Note: This test requires a running n8n instance or mock server
 */
@KestraTest
class TriggerWorkflowRunnerTest {
    
    @Inject
    private RunContextFactory runContextFactory;
    
    @Test
    void testBasicWorkflowTrigger() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());
        
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://httpbin.org/post") // Using httpbin for testing
            .method(HttpMethod.POST)
            .wait(false)
            .headers(Map.of("Content-Type", "application/json"))
            .body(Map.of("message", "Hello from Kestra"))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        // This test would work with a real HTTP endpoint
        // For now, we're just testing the task configuration
        assertThat(task.getUrl(), is("https://httpbin.org/post"));
        assertThat(task.getMethod(), is(HttpMethod.POST));
        assertThat(task.getWait(), is(false));
    }
    
    @Test
    void testAuthenticationHeaders() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());
        
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.BASIC_AUTH)
            .username("testuser")
            .password("testpass")
            .build();
        
        // Test that authentication headers are generated correctly
        Map<String, String> authHeaders = auth.getHeaders(runContext);
        
        assertThat(authHeaders.containsKey("Authorization"), is(true));
        assertThat(authHeaders.get("Authorization").startsWith("Basic "), is(true));
    }
    
    @Test
    void testJwtAuthenticationHeaders() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());
        
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.JWT)
            .jwtToken("test-jwt-token")
            .build();
        
        Map<String, String> authHeaders = auth.getHeaders(runContext);
        
        assertThat(authHeaders.containsKey("Authorization"), is(true));
        assertThat(authHeaders.get("Authorization"), is("Bearer test-jwt-token"));
    }
    
    @Test
    void testHeaderAuthenticationHeaders() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());
        
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.HEADER_AUTH)
            .headerName("X-API-Key")
            .headerValue("secret-key")
            .build();
        
        Map<String, String> authHeaders = auth.getHeaders(runContext);
        
        assertThat(authHeaders.containsKey("X-API-Key"), is(true));
        assertThat(authHeaders.get("X-API-Key"), is("secret-key"));
    }
    
    @Test
    void testCustomHeadersAuthentication() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());
        
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.CUSTOM)
            .customHeaders(Map.of(
                "X-Custom-Header", "custom-value",
                "X-Another-Header", "another-value"
            ))
            .build();
        
        Map<String, String> authHeaders = auth.getHeaders(runContext);
        
        assertThat(authHeaders.containsKey("X-Custom-Header"), is(true));
        assertThat(authHeaders.get("X-Custom-Header"), is("custom-value"));
        assertThat(authHeaders.containsKey("X-Another-Header"), is(true));
        assertThat(authHeaders.get("X-Another-Header"), is("another-value"));
    }
    
    @Test
    void testTaskWithAllOptions() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of("namespace", "test.namespace"));
        
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.JWT)
            .jwtToken("{{ secret('N8N_TOKEN') }}")
            .build();
        
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://n8n.example.com/webhook/{{ flow.namespace }}")
            .method(HttpMethod.POST)
            .wait(true)
            .pollFrequency(Duration.ofSeconds(2))
            .timeout(Duration.ofMinutes(5))
            .headers(Map.of(
                "Content-Type", "application/json",
                "User-Agent", "Kestra-n8n-Plugin"
            ))
            .body(Map.of(
                "message", "Hello from {{ flow.namespace }}",
                "timestamp", "{{ now() }}"
            ))
            .from(java.util.List.of("/data/files/input.csv"))
            .authentication(auth)
            .build();
        
        // Test that all properties are set correctly
        assertThat(task.getUrl(), is("https://n8n.example.com/webhook/{{ flow.namespace }}"));
        assertThat(task.getMethod(), is(HttpMethod.POST));
        assertThat(task.getWait(), is(true));
        assertThat(task.getPollFrequency(), is(Duration.ofSeconds(2)));
        assertThat(task.getTimeout(), is(Duration.ofMinutes(5)));
        assertThat(task.getHeaders(), is(Map.of(
            "Content-Type", "application/json",
            "User-Agent", "Kestra-n8n-Plugin"
        )));
        assertThat(task.getBody(), is(Map.of(
            "message", "Hello from {{ flow.namespace }}",
            "timestamp", "{{ now() }}"
        )));
        assertThat(task.getFrom(), is(java.util.List.of("/data/files/input.csv")));
        assertThat(task.getAuthentication(), is(notNullValue()));
        assertThat(task.getAuthentication().getType(), is(Authentication.AuthType.JWT));
    }
}
