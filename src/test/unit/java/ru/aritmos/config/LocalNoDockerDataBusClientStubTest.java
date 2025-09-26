package ru.aritmos.config;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class LocalNoDockerDataBusClientStubTest {

    @DisplayName("Заглушка отправки возвращает предопределённые статус и тип")
    @Test
    void sendReturnsStubbedStatusAndType() {
        LocalNoDockerDataBusClientStub client = new LocalNoDockerDataBusClientStub();

        Map<String, String> result = Mono.from(client.send("svc", false, "2024-01-01", "sender", "TYPE", null)).block();

        assertEquals("stubbed", result.get("status"));
        assertEquals("TYPE", result.get("type"));
    }
}

