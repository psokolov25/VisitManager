package ru.aritmos;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micronaut.test.annotation.MockBean;
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

    @MockBean(DataBusClient.class)
    DataBusClient dataBusClient() {
        DataBusClient mock = mock(DataBusClient.class);
        when(mock.send(any(), anyBoolean(), any(), any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            String type = invocation.getArgument(4, String.class);
                            return Mono.just(Map.of("status", "stubbed", "type", type));
                        });
        return mock;
    }

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

        Assertions.assertEquals("stubbed", result.get("status"));
        Assertions.assertEquals("TEST", result.get("type"));
        Assertions.assertTrue(Mockito.mockingDetails(dataBusClient).isMock());
    }
}
