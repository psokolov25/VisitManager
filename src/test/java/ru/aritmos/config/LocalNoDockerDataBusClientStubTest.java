package ru.aritmos.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.Disabled;

class LocalNoDockerDataBusClientStubTest {

    @Test
    void sendReturnsStubbedStatusAndType() {
        LocalNoDockerDataBusClientStub client = new LocalNoDockerDataBusClientStub();

        Map<String, String> result = Mono.from(client.send("svc", false, "2024-01-01", "sender", "TYPE", null)).block();

        assertEquals("stubbed", result.get("status"));
        assertEquals("TYPE", result.get("type"));
    }
    @Disabled("Not yet implemented")
    @Test
    void sendTest() {
        // TODO implement
    }

}

