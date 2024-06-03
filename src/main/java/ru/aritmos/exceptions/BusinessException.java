package ru.aritmos.exceptions;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

import java.util.Date;

@Slf4j
public class BusinessException extends RuntimeException {
    @Inject
    EventService eventService;
    @Value("${micronaut.application.name}")
    String applicationName;
    public BusinessException(String message)  {
        super(message);
        eventService.send("*",false, Event.builder()
                .eventDate(new Date())
                .eventType("BUSINESS_ERROR")
                .senderService(applicationName)
                        .body(this)
                .build());
        log.error(message);
    }
}
