package ru.aritmos.exceptions;

import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

@Slf4j
public class SystemException extends Exception {

  final EventService eventService;

  final String errorMessage;

  public SystemException(String errorMessage, EventService eventService) {
    super(errorMessage);
    this.errorMessage = errorMessage;
    BusinessError businessError = new BusinessError();
    businessError.setMessage(errorMessage);
    this.eventService = eventService;
    eventService.send(
        "*",
        false,
        Event.builder()
            .eventDate(ZonedDateTime.now())
            .eventType("SYSTEM_ERROR")
            .body(businessError)
            .build());
    log.error(errorMessage);
  }

  @Data
  @Serdeable
  static class BusinessError {
    String message;
  }
}
