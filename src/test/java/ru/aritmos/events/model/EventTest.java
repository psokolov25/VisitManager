package ru.aritmos.events.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void builderSetsFields() {
        ZonedDateTime date = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        Map<String, String> params = Map.of("k", "v");
        Event event =
                Event.builder()
                        .senderService("svc")
                        .eventDate(date)
                        .eventType("TYPE")
                        .params(params)
                        .body("body")
                        .build();

        Assertions.assertEquals("svc", event.getSenderService());
        Assertions.assertEquals(date, event.getEventDate());
        Assertions.assertEquals("TYPE", event.getEventType());
        Assertions.assertEquals(params, event.getParams());
        Assertions.assertEquals("body", event.getBody());
    }
}
