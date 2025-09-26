package ru.aritmos.events.clients;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * Integration test that exercises the real {@link DataBusClient} against a local stub within the
 * "integration" profile.
 */
@MicronautTest(environments = "integration")
@Property(name = "micronaut.application.dataBusUrl", value = "/")
@Property(name = "micronaut.server.port", value = "-1")
class DataBusClientIT {

    @Inject
    DataBusClient dataBusClient;

    @Inject
    DataBusStubRecorder stubRecorder;

    @DisplayName("Интеграционный клиент получает ответ от локальной заглушки сервиса")
    @Test
    void realServiceResponds() {
        Map<String, String> response =
                Mono.from(dataBusClient.send("dest", false, "2024-01-01", "vm", "TEST", Map.of()))
                        .block(Duration.ofSeconds(5));

        assertNotNull(response);
        assertEquals("delivered", response.get("status"));
        assertEquals("TEST", response.get("type"));

        DataBusStubRecorder.RecordedRequest recorded = stubRecorder.getLastRequest();
        assertNotNull(recorded, "запрос должен быть зафиксирован заглушкой");
        assertEquals("dest", recorded.destination());
        assertFalse(recorded.sendToOtherBus());
        assertEquals("2024-01-01", recorded.sendDate());
        assertEquals("vm", recorded.senderService());
        assertEquals("TEST", recorded.type());
        assertEquals(Map.of(), recorded.body());
    }

    @Controller("/databus/events/types")
    @Requires(env = "integration")
    static class DataBusStubController {

        private final DataBusStubRecorder recorder;

        DataBusStubController(DataBusStubRecorder recorder) {
            this.recorder = recorder;
        }

        @Post("/{type}")
        Map<String, String> publish(
                @Header("Service-Destination") String destination,
                @Header("Send-To-OtherBus") Boolean sendToOtherBus,
                @Header("Send-Date") String sendDate,
                @Header("Service-Sender") String senderService,
                @PathVariable String type,
                @Body Map<String, Object> body) {
            recorder.record(destination, sendToOtherBus, sendDate, senderService, type, body);
            return Map.of("status", "delivered", "type", type);
        }
    }

    @Singleton
    @Requires(env = "integration")
    static class DataBusStubRecorder {

        private final AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();

        void record(
                String destination,
                Boolean sendToOtherBus,
                String sendDate,
                String senderService,
                String type,
                Map<String, Object> body) {
            lastRequest.set(new RecordedRequest(destination, sendToOtherBus, sendDate, senderService, type, body));
        }

        RecordedRequest getLastRequest() {
            return lastRequest.get();
        }

        record RecordedRequest(
                String destination,
                Boolean sendToOtherBus,
                String sendDate,
                String senderService,
                String type,
                Map<String, Object> body) {}
    }
}
