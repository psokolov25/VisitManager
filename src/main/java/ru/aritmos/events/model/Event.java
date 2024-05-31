package ru.aritmos.events.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@Serdeable
public class Event {
    String senderService;
    Date eventDate;
    String eventType;
    Object body;
}
