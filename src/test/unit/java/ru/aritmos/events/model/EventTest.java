package ru.aritmos.events.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.test.LoggingAssertions;

class EventTest {

    @DisplayName("Построитель события заполняет все поля")
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

        LoggingAssertions.assertEquals("svc", event.getSenderService());
        LoggingAssertions.assertEquals(date, event.getEventDate());
        LoggingAssertions.assertEquals("TYPE", event.getEventType());
        LoggingAssertions.assertEquals(params, event.getParams());
        LoggingAssertions.assertEquals("body", event.getBody());
    }
}
