package ru.aritmos;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import ru.aritmos.events.clients.DataBusClient;

@MicronautTest
class DataBusClientMockTest {

    @Inject DataBusClient dataBusClient;

    @Test
    void dataBusClientStubbed() {
        Map<String, String> result = Mono.from(
                dataBusClient.send(
                        "dest",
                        true,
                        "Wed, 09 Apr 2008 23:55:38 GMT",
                        "svc",
                        "TEST",
                        Collections.emptyMap()))
                .block();

        Assertions.assertEquals("stubbed", result.get("status"));
        Assertions.assertEquals("TEST", result.get("type"));
        Assertions.assertTrue(Mockito.mockingDetails(dataBusClient).isMock());
    }
}
