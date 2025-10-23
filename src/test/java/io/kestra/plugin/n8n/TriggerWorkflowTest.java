package io.kestra.plugin.n8n;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.n8n.models.Authentication;
import io.kestra.plugin.n8n.models.HttpMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@KestraTest
@ExtendWith(MockitoExtension.class)
class TriggerWorkflowTest {
    
    @Inject
    private RunContextFactory runContextFactory;
    
    @Mock
    private RunContext mockRunContext;
    
    @Test
    void testBasicWorkflowTrigger() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());
        
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://n8n.example.com/webhook/test")
            .method(HttpMethod.POST)
            .wait(false)
            .headers(Map.of("Content-Type", "application/json"))
            .body(Map.of("message", "Hello from Kestra"))
            .build();
        
        // Note: This test would need to be adapted to work with actual HTTP calls
        // For now, we're testing the task configuration
        assertThat(task.getUrl(), is("https://n8n.example.com/webhook/test"));
        assertThat(task.getMethod(), is(HttpMethod.POST));
        assertThat(task.getWait(), is(false));
        assertThat(task.getHeaders(), is(Map.of("Content-Type", "application/json")));
        assertThat(task.getBody(), is(Map.of("message", "Hello from Kestra")));
    }
    
    @Test
    void testAuthenticationConfiguration() {
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.BASIC_AUTH)
            .username("testuser")
            .password("testpass")
            .build();
        
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://n8n.example.com/webhook/test")
            .authentication(auth)
            .build();
        
        assertThat(task.getAuthentication(), is(notNullValue()));
        assertThat(task.getAuthentication().getType(), is(Authentication.AuthType.BASIC_AUTH));
        assertThat(task.getAuthentication().getUsername(), is("testuser"));
        assertThat(task.getAuthentication().getPassword(), is("testpass"));
    }
    
    @Test
    void testJwtAuthentication() {
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.JWT)
            .jwtToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            .build();
        
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://n8n.example.com/webhook/test")
            .authentication(auth)
            .build();
        
        assertThat(task.getAuthentication().getType(), is(Authentication.AuthType.JWT));
        assertThat(task.getAuthentication().getJwtToken(), is("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."));
    }
    
    @Test
    void testHeaderAuthentication() {
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.HEADER_AUTH)
            .headerName("X-API-Key")
            .headerValue("secret-key")
            .build();
        
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://n8n.example.com/webhook/test")
            .authentication(auth)
            .build();
        
        assertThat(task.getAuthentication().getType(), is(Authentication.AuthType.HEADER_AUTH));
        assertThat(task.getAuthentication().getHeaderName(), is("X-API-Key"));
        assertThat(task.getAuthentication().getHeaderValue(), is("secret-key"));
    }
    
    @Test
    void testCustomHeadersAuthentication() {
        Authentication auth = Authentication.builder()
            .type(Authentication.AuthType.CUSTOM)
            .customHeaders(Map.of(
                "X-Custom-Header", "custom-value",
                "X-Another-Header", "another-value"
            ))
            .build();
        
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://n8n.example.com/webhook/test")
            .authentication(auth)
            .build();
        
        assertThat(task.getAuthentication().getType(), is(Authentication.AuthType.CUSTOM));
        assertThat(task.getAuthentication().getCustomHeaders(), is(Map.of(
            "X-Custom-Header", "custom-value",
            "X-Another-Header", "another-value"
        )));
    }
    
    @Test
    void testFileUploadConfiguration() {
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://n8n.example.com/webhook/upload")
            .method(HttpMethod.POST)
            .from(java.util.List.of("/data/files/input.csv", "/data/files/output.json"))
            .build();
        
        assertThat(task.getFrom(), is(java.util.List.of("/data/files/input.csv", "/data/files/output.json")));
    }
    
    @Test
    void testPollingConfiguration() {
        TriggerWorkflow task = TriggerWorkflow.builder()
            .url("https://n8n.example.com/webhook/test")
            .wait(true)
            .pollFrequency(Duration.ofSeconds(5))
            .timeout(Duration.ofMinutes(10))
            .build();
        
        assertThat(task.getWait(), is(true));
        assertThat(task.getPollFrequency(), is(Duration.ofSeconds(5)));
        assertThat(task.getTimeout(), is(Duration.ofMinutes(10)));
    }
    
    @Test
    void testAllHttpMethods() {
        for (HttpMethod method : HttpMethod.values()) {
            TriggerWorkflow task = TriggerWorkflow.builder()
                .url("https://n8n.example.com/webhook/test")
                .method(method)
                .build();
            
            assertThat(task.getMethod(), is(method));
        }
    }
}
