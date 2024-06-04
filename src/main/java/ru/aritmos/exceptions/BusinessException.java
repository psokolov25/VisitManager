package ru.aritmos.exceptions;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

import java.util.Date;

@Slf4j
@Singleton

public class BusinessException extends RuntimeException {

    final EventService eventService;

    public BusinessException(String message, EventService eventService,String senderService)  {
        super(message);
        BusinessError businessError=new BusinessError();
        businessError.setMessage(message);
        this.eventService = eventService;
        eventService.send("*",false, Event.builder()
                .eventDate(new Date())
                .eventType("BUSINESS_ERROR")
                .senderService(senderService)
                        .body(businessError)
                .build());
        log.error(message);
    }
    @Data
    @Serdeable
    static class BusinessError{
        String message;
    }
}
