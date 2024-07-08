package ru.aritmos.events.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@Serdeable
@Introspected
/*
Класс события
 */
public class Event {
    String senderService;
    ZonedDateTime eventDate;
    String eventType;
    Map<String,String> params;
    Object body;
}
