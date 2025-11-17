package io.kestra.plugin.n8n.webhook;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.Storage;
import io.kestra.plugin.n8n.ContentType;
import io.kestra.plugin.n8n.HttpMethod;
import jakarta.inject.Inject;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@KestraTest
class AbstractTriggerWorkflowTest {
    @Inject
    RunContextFactory runContextFactory = new RunContextFactory();

    private TestTriggerWorkflow.TestTriggerWorkflowBuilder<?,?> getBaseTriggerWorkflowBuilder() {
        return TestTriggerWorkflow.builder()
            .uri(Property.ofValue("http://example.com"))
            .method(Property.ofValue(HttpMethod.POST));
    }

    @Test
    void givenAbstractTriggerWorkflowWithQueryParameters_whenRequestBuilt_thenCorrectQueryParametersAreAdded() throws Exception {
        RunContext runContext = runContextFactory.of();
        Map<String, String> queryParameters = new TreeMap<>(Map.of(
        "keyOne", "valueOne",
        "keyTwo", "valueTwo"
        ));


        TestTriggerWorkflow abstractTriggerWorkflow = getBaseTriggerWorkflowBuilder()
            .queryParameters(Property.ofValue(queryParameters))
            .build();

        HttpRequest httpRequest = abstractTriggerWorkflow.buildRequest(runContext);

        assertEquals(
            "keyOne=valueOne&keyTwo=valueTwo",
            httpRequest.getUri().getQuery()
        );
    }

    @Test
    void givenAbstractTriggerWorkflowWithCustomHeaders_whenRequestBuilt_thenCorrectCustomHeadersAreAdded() throws Exception {
        RunContext runContext = runContextFactory.of();
        Map<String, String> headers = new TreeMap<>(Map.of(
            "keyOne", "valueOne",
            "keyTwo", "valueTwo"
        ));


        TestTriggerWorkflow abstractTriggerWorkflow = getBaseTriggerWorkflowBuilder()
            .headers(Property.ofValue(headers))
            .build();

        HttpRequest httpRequest = abstractTriggerWorkflow.buildRequest(runContext);

        Map<String, List<String>> requestHeaders = httpRequest.getHeaders().map();

        assertEquals(2, requestHeaders.size());
        assertEquals(requestHeaders.get("keyOne"), List.of("valueOne"));
        assertEquals(requestHeaders.get("keyTwo"), List.of("valueTwo"));
    }

    @Test
    void givenAbstractTriggerWorkflowWithBody_whenRequestBuilt_thenCorrectBody() throws Exception {
        RunContext runContext = runContextFactory.of();
        Map<String, String> body = Map.of(
        "keyOne", "valueOne",
        "keyTwo", "valueTwo"
        );


        TestTriggerWorkflow abstractTriggerWorkflow = getBaseTriggerWorkflowBuilder()
            .body(Property.ofValue(body))
            .build();

        HttpRequest httpRequest = abstractTriggerWorkflow.buildRequest(runContext);

        HttpRequest.RequestBody requestBody = httpRequest.getBody();

        assertEquals(body, requestBody.getContent());
    }

    @Test
    void givenAbstractTriggerWorkflowWithFrom_whenRequestBuilt_thenCorrectBody() throws Exception {
        RunContext runContext = runContextFactory.of();
        URI path = URI.create("example-path/file.txt");
        byte[] fileBytes = "[]".getBytes();
        InputStream fileInputStream = new ByteArrayInputStream(fileBytes);


        Storage storageSpy = Mockito.spy(runContext.storage());
        RunContext runContextSpy = Mockito.spy(runContext);

        Mockito.doReturn(fileInputStream).when(storageSpy)
                .getFile(eq(path));

        Mockito.doReturn(storageSpy).when(runContextSpy)
            .storage();

        TestTriggerWorkflow abstractTriggerWorkflow = getBaseTriggerWorkflowBuilder()
            .from(Property.ofValue(path))
            .build();

        HttpRequest httpRequest = abstractTriggerWorkflow.buildRequest(runContextSpy);

        assertArrayEquals(
            fileBytes,
            (byte[]) httpRequest.getBody().getContent()
        );
    }

    @Test
    void givenAbstractTriggerWorkflowWithFromAndStringContentType_whenRequestBuilt_thenCorrectBody() throws Exception {
        RunContext runContext = runContextFactory.of();
        URI path = URI.create("example-path/file.txt");
        byte[] fileBytes = "[]".getBytes();
        InputStream fileInputStream = new ByteArrayInputStream(fileBytes);


        Storage storageSpy = Mockito.spy(runContext.storage());
        RunContext runContextSpy = Mockito.spy(runContext);

        Mockito.doReturn(fileInputStream).when(storageSpy)
            .getFile(eq(path));

        Mockito.doReturn(storageSpy).when(runContextSpy)
            .storage();

        TestTriggerWorkflow abstractTriggerWorkflow = getBaseTriggerWorkflowBuilder()
            .from(Property.ofValue(path))
            .contentType(Property.ofValue(ContentType.JSON))
            .build();

        HttpRequest httpRequest = abstractTriggerWorkflow.buildRequest(runContextSpy);

        assertEquals( "[]", httpRequest.getBody().getContent());
    }

    @Test
    void givenAbstractTriggerWorkflowWithFromAndBody_whenRequestBuilt_thenErrorIsThrown() throws Exception {
        RunContext runContext = runContextFactory.of();

        TestTriggerWorkflow abstractTriggerWorkflow = getBaseTriggerWorkflowBuilder()
            .from(Property.ofValue(
                new URI("examplePath"))
            )
            .body(Property.ofValue(
                Map.of("keyOne", "valueOnw")
            ))
            .contentType(Property.ofValue(ContentType.JSON))
            .build();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> abstractTriggerWorkflow.buildRequest(runContext)
        );
        assertEquals("You cannot set both 'from' and 'body' properties at the same time", exception.getMessage());
    }

    @SuperBuilder
    @NoArgsConstructor
    @Plugin
    static public class TestTriggerWorkflow extends AbstractTriggerWorkflow {}
}
