package io.kestra.plugin.n8n.webhook;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.n8n.HttpMethod;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.junit.jupiter.api.Assertions.*;

@KestraTest
class TriggerWorkflowTest {
    @Inject
    RunContextFactory runContextFactory = new RunContextFactory();

    private WireMockServer wireMockServer;
    private static final String LOCALHOST = "localhost";
    private static final String WEBHOOK_PATH = "/webhook-test/213e8fbc-f843-428c-9860-ab9f64e5ef3b";

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(0));
        wireMockServer.start();
        WireMock.configureFor(LOCALHOST, wireMockServer.port());
    }

    @Test
    void givenTriggerWorkflowWithWaitFalse_whenRun_thenBodyIsNull() throws Exception {
        RunContext runContext = runContextFactory.of();


        stubFor(
            post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                )
        );

        TriggerWorkflow.Output expectedOutput = TriggerWorkflow.Output.builder()
            .statusCode(200)
            .body(null)
            .build();

        Map<String, ?> authentication = Map.of(
            "type", "BasicAuth",
            "username", "username",
            "password", "password"
        );

        Map<String, ?> body = Map.of(
            "key", "value"
        );

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .authentication(new Property<>(authentication))
            .body(Property.ofValue(body))
            .uri(Property.ofValue(createWebhookUri()))
            .wait(Property.ofValue(false))
            .method(Property.ofValue(HttpMethod.POST))
            .build();

        TriggerWorkflow.Output actualOutput = triggerWorkflow.run(runContext);
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    void givenTriggerWorkflowWithWaitTrue_whenRun_thenBodyIsWebhookResponse() throws Exception {
        RunContext runContext = runContextFactory.of();


        stubFor(
            post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("Webhook triggered successfully")
                )
        );

        TriggerWorkflow.Output expectedOutput = TriggerWorkflow.Output.builder()
            .statusCode(200)
            .body("Webhook triggered successfully")
            .build();

        Map<String, ?> authentication = Map.of(
            "type", "BasicAuth",
            "username", "username",
            "password", "password"
        );

        Map<String, ?> body = Map.of(
            "key", "value"
        );

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .authentication(new Property<>(authentication))
            .body(Property.ofValue(body))
            .uri(Property.ofValue(createWebhookUri()))
            .wait(Property.ofValue(true))
            .method(Property.ofValue(HttpMethod.POST))
            .build();

        TriggerWorkflow.Output actualOutput = triggerWorkflow.run(runContext);
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    void givenTriggerWorkflowWith_whenJsonContentReturned_thenResponseBodyIsMap() throws Exception {
        RunContext runContext = runContextFactory.of();

        stubFor(
            post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"keyOne\": \"valueOne\"}")
                )
        );

        TriggerWorkflow.Output expectedOutput = TriggerWorkflow.Output.builder()
            .statusCode(200)
            .body(Map.of(
                "keyOne", "valueOne"
            )).build();

        Map<String, ?> authentication = Map.of(
            "type", "BasicAuth",
            "username", "username",
            "password", "password"
        );

        TriggerWorkflow triggerWorkflow = TriggerWorkflow.builder()
            .authentication(new Property<>(authentication))
            .uri(Property.ofValue(createWebhookUri()))
            .wait(Property.ofValue(true))
            .method(Property.ofValue(HttpMethod.POST))
            .build();

        TriggerWorkflow.Output actualOutput = triggerWorkflow.run(runContext);
        assertEquals(expectedOutput, actualOutput);
    }



    private String createWebhookUri() {
        return String.format("http://%s:%s%s", LOCALHOST, wireMockServer.port(), WEBHOOK_PATH);
    }
    }