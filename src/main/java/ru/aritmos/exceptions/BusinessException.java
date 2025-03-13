package ru.aritmos.exceptions;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

@Slf4j
@SuppressWarnings("unused")
public class BusinessException extends RuntimeException {

  final EventService eventService;

  public BusinessException(String errorMessage, EventService eventService, HttpStatus status) {
    super(errorMessage);
    BusinessError businessError = new BusinessError();
    businessError.setMessage(errorMessage);
    this.eventService = eventService;
    eventService.send(
        "*",
        false,
        Event.builder()
            .eventDate(ZonedDateTime.now())
            .eventType("BUSINESS_ERROR")
            .body(businessError)
            .build());
    log.error(errorMessage);
    throw new HttpStatusException(status, errorMessage);
  }
  public BusinessException(String errorMessage,String errorLogMessage, EventService eventService, HttpStatus status) {
    super(errorMessage);
    BusinessError businessError = new BusinessError();
    businessError.setMessage(errorMessage);
    this.eventService = eventService;
    eventService.send(
            "*",
            false,
            Event.builder()
                    .eventDate(ZonedDateTime.now())
                    .eventType("BUSINESS_ERROR")
                    .body(businessError)
                    .build());
    log.error(errorLogMessage);
    throw new HttpStatusException(status, errorMessage);
  }

  public BusinessException(String errorMessage,String errorLogMessage,String errorEventMessage, EventService eventService, HttpStatus status) {
    super(errorMessage);
    BusinessError businessError = new BusinessError();
    businessError.setMessage(errorEventMessage);
    this.eventService = eventService;
    eventService.send(
            "*",
            false,
            Event.builder()
                    .eventDate(ZonedDateTime.now())
                    .eventType("BUSINESS_ERROR")
                    .body(businessError)
                    .build());
    log.error(errorLogMessage);
    throw new HttpStatusException(status, errorMessage);
  }

  @Data
  @Serdeable
  @SuppressWarnings("unused")
  static class BusinessError {
    String message;
  }
}
