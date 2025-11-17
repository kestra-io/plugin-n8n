package io.kestra.plugin.n8n.webhook;

import io.kestra.core.http.client.configurations.AbstractAuthConfiguration;
import io.kestra.core.http.client.configurations.BasicAuthConfiguration;
import io.kestra.core.http.client.configurations.BearerAuthConfiguration;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.junit.annotations.ExecuteFlow;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.executions.TaskRun;
import io.kestra.core.models.flows.State;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.n8n.HttpMethod;
import jakarta.inject.Inject;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@KestraTest(startRunner = true)
class TriggerWorkflowTest {
    @Inject
    RunContextFactory runContextFactory = new RunContextFactory();

    private static final String N8N_PATH = "http://localhost:5678";
    private static final String WEBHOOK_PATH = "webhook";


    @Test
    void givenTriggerWorkflowWithWaitFalse_whenRun_thenBodyIsNull() throws Exception {
        RunContext runContext = runContextFactory.of();

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .uri(Property.ofValue(createWebhookUri(WebhookEndpoints.NO_AUTH_ENDPOINT)))
            .wait(Property.ofValue(false))
            .method(Property.ofValue(HttpMethod.GET))
            .build();

        TriggerWorkflow.Output output = triggerWorkflow.run(runContext);
        assertNull(output.body());
    }

    @Test
    void givenAbstractTriggerWorkflow_whenRequestBuiltWithBasicAuth_thenCorrectAuthHeadersAdded() throws Exception {
        RunContext runContext = runContextFactory.of();
        final String USERNAME = "username";
        final String PASSWORD = "password";

        BasicAuthConfiguration authentication = BasicAuthConfiguration.builder()
            .type(AbstractAuthConfiguration.AuthType.BASIC)
            .username(Property.ofValue(USERNAME))
            .password(Property.ofValue(PASSWORD))
            .build();

        HttpConfiguration httpConfiguration = HttpConfiguration.builder()
            .auth(authentication)
            .build();

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .uri(Property.ofValue(createWebhookUri(WebhookEndpoints.BASIC_AUTH_ENDPOINT)))
            .method(Property.ofValue(HttpMethod.GET))
            .options(httpConfiguration)
            .build();

        TriggerWorkflow.Output output = triggerWorkflow.run(runContext);
        Map<?,?> outputBody = assertInstanceOf(Map.class, output.body());
        Map<?,?> outputEchoedHeaders = assertInstanceOf(Map.class, outputBody.get("headers"));

        assertEquals(
            "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes()),
            outputEchoedHeaders.get("authorization")
        );
    }

    @Test
    void givenAbstractTriggerWorkflow_whenRequestBuiltWithBearer_thenCorrectAuthHeadersAdded() throws Exception {
        RunContext runContext = runContextFactory.of();

        // This token is signed with the key setup in the bearer-auth-webhook n8n workflow.
        final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.8VKCTiBegJPuPIZlp0wbV0Sbdn5BS6TE5DCx6oYNc5o";

        BearerAuthConfiguration authentication = BearerAuthConfiguration.builder()
            .type(AbstractAuthConfiguration.AuthType.BEARER)
            .token(Property.ofValue(TOKEN))
            .build();

        HttpConfiguration httpConfiguration = HttpConfiguration.builder()
            .auth(authentication)
            .build();

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .uri(Property.ofValue(createWebhookUri(WebhookEndpoints.BEARER_AUTH_ENDPOINT)))
            .method(Property.ofValue(HttpMethod.GET))
            .options(httpConfiguration)
            .build();

        TriggerWorkflow.Output output = triggerWorkflow.run(runContext);
        Map<?,?> outputBody = assertInstanceOf(Map.class, output.body());
        Map<?,?> outputEchoedHeaders = assertInstanceOf(Map.class, outputBody.get("headers"));

        assertEquals(
            "Bearer " + TOKEN,
            outputEchoedHeaders.get("authorization")
        );
    }

    @Test
    void givenAbstractTriggerWorkflow_whenRequestBuiltWithAuthHeaders_thenCorrectAuthHeadersAdded() throws Exception {
        RunContext runContext = runContextFactory.of();

        final String AUTH_HEADER_NAME = "name";
        final String AUTH_HEADER_VALUE = "value";

        Map<String, String> headers = Map.of(
            AUTH_HEADER_NAME, AUTH_HEADER_VALUE
        );

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .uri(Property.ofValue(createWebhookUri(WebhookEndpoints.HEADER_AUTH_ENDPOINT)))
            .method(Property.ofValue(HttpMethod.GET))
            .headers(Property.ofValue(headers))
            .build();

        TriggerWorkflow.Output output = triggerWorkflow.run(runContext);
        Map<?,?> outputBody = assertInstanceOf(Map.class, output.body());
        Map<?,?> outputEchoedHeaders = assertInstanceOf(Map.class, outputBody.get("headers"));

        assertEquals(AUTH_HEADER_VALUE, outputEchoedHeaders.get(AUTH_HEADER_NAME));
    }

    @Test
    void givenTriggerWorkflowWithWaitTrue_whenRun_thenBodyIsWebhookResponse() throws Exception {
        RunContext runContext = runContextFactory.of();

        Map<String, ?> body = Map.of(
            "key", "value"
        );

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .body(Property.ofValue(body))
            .uri(Property.ofValue(createWebhookUri(WebhookEndpoints.NO_AUTH_TEXT_RESPONSE)))
            .wait(Property.ofValue(true))
            .method(Property.ofValue(HttpMethod.GET))
            .build();

        TriggerWorkflow.Output actualOutput = triggerWorkflow.run(runContext);
        String outputBody = assertInstanceOf(String.class, actualOutput.body());
        assertEquals(outputBody, "Webhook triggered successfully");
    }

    @Test
    void givenTriggerWorkflowWith_whenJsonContentReturned_thenResponseBodyIsMap() throws Exception {
        RunContext runContext = runContextFactory.of();

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .uri(Property.ofValue(createWebhookUri(WebhookEndpoints.NO_AUTH_ENDPOINT)))
            .wait(Property.ofValue(true))
            .method(Property.ofValue(HttpMethod.GET))
            .build();

        TriggerWorkflow.Output actualOutput = triggerWorkflow.run(runContext);
        assertInstanceOf(Map.class, actualOutput.body());
    }


    private String createWebhookUri(WebhookEndpoints webhookId) {
        return String.format("%s/%s/%s", N8N_PATH, WEBHOOK_PATH, webhookId.getEndpoint());
    }

    @Test
    @ExecuteFlow("flows/webhook-with-basic-auth.yaml")
    void givenValidFlowYamlWithTriggerWorkflowWithBasicAuth_whenFlowExecuted_thenErrorIsNotThrown(Execution execution) {
        assertEquals(execution.getState().getCurrent(), State.Type.SUCCESS);
    }

    @Test
    @ExecuteFlow("flows/webhook-with-jwt-auth.yaml")
    void givenValidFlowYamlWithTriggerWorkflowWithBearerAuth_whenFlowExecuted_thenFlowFinishesSuccessfully(Execution execution) {
        assertEquals(execution.getState().getCurrent(), State.Type.SUCCESS);
    }

    @Test
    @ExecuteFlow("flows/webhook-text-response.yaml")
    void givenValidFlowYamlWithTriggerWorkflowWithBearerTextResponse_whenFlowExecuted_thenFlowFinishesSuccessfully(Execution execution) {
        assertEquals(execution.getState().getCurrent(), State.Type.SUCCESS);

        TaskRun webhookTaskRun = execution.getTaskRunList().getFirst();
        assertEquals(webhookTaskRun.getOutputs().get("body"), "Webhook triggered successfully");
    }

    @Getter
    private enum WebhookEndpoints {
        NO_AUTH_ENDPOINT("no-auth-webhook"),
        BASIC_AUTH_ENDPOINT("basic-auth-webhook"),
        HEADER_AUTH_ENDPOINT("header-auth-webhook"),
        BEARER_AUTH_ENDPOINT("bearer-auth-webhook"),
        NO_AUTH_TEXT_RESPONSE("no-auth-webhook-text-response");

        private final String endpoint;

        WebhookEndpoints(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}
