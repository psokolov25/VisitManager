package ru.aritmos;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;
import ru.aritmos.test.LoggingAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import reactor.core.publisher.Mono;
import ru.aritmos.events.clients.DataBusClient;

@MicronautTest
class DataBusClientMockTest {

    @Inject DataBusClient dataBusClient;

    @DisplayName("проверяется сценарий «data bus client stubbed»")
    @Test
    void dataBusClientStubbed() {
        Map<String, String> result =
                Mono.from(
                                dataBusClient.send(
                                        "dest",
                                        true,
                                        "Wed, 09 Apr 2008 23:55:38 GMT",
                                        "svc",
                                        "TEST",
                                        Collections.emptyMap()))
                        .block();

        LoggingAssertions.assertEquals("stubbed", result.get("status"));
        LoggingAssertions.assertEquals("TEST", result.get("type"));
    }
}