package ru.aritmos.exceptions;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.serde.annotation.Serdeable;
import java.io.IOException;
import java.time.ZonedDateTime;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

/**
 * Исключение уровня бизнес‑логики с публикацией события об ошибке.
 */
@Slf4j
@SuppressWarnings("unused")
public class BusinessException extends RuntimeException {

  /** Сервис отправки событий. */
  final EventService eventService;

  /**
   * Создать исключение с сообщением и HTTP‑статусом.
   * Публикует событие BUSINESS_ERROR и выбрасывает {@link HttpStatusException}.
   *
   * @param errorMessage сообщение об ошибке
   * @param eventService сервис событий
   * @param status HTTP‑статус
   */
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

  /**
   * Создать исключение с произвольным телом и HTTP‑статусом.
   *
   * @param errorMessage тело ошибки (будет сериализовано)
   * @param eventService сервис событий
   * @param status HTTP‑статус
   * @param mapper сериализатор
   * @throws IOException ошибка сериализации
   */
  public BusinessException(
      Object errorMessage,
      EventService eventService,
      HttpStatus status,
      io.micronaut.serde.ObjectMapper mapper)
      throws IOException {
    super(mapper.writeValueAsString(errorMessage));
    BusinessError businessError = new BusinessError();
    businessError.setMessage(mapper.writeValueAsString(errorMessage));
    this.eventService = eventService;
    eventService.send(
        "*",
        false,
        Event.builder()
            .eventDate(ZonedDateTime.now())
            .eventType("BUSINESS_ERROR")
            .body(businessError)
            .build());
    log.error(mapper.writeValueAsString(errorMessage));
    throw new HttpStatusException(status, errorMessage);
  }

  /**
   * Создать исключение с сообщением и отдельным сообщением для лога.
   *
   * @param errorMessage сообщение для клиента
   * @param errorLogMessage сообщение для лога
   * @param eventService сервис событий
   * @param status HTTP‑статус
   */
  public BusinessException(
      String errorMessage, String errorLogMessage, EventService eventService, HttpStatus status) {
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

  /**
   * Создать исключение с произвольным телом и отдельным сообщением для лога.
   *
   * @param errorMessage тело ошибки
   * @param errorLogMessage сообщение для лога
   * @param eventService сервис событий
   * @param status HTTP‑статус
   */
  public BusinessException(
      Object errorMessage, String errorLogMessage, EventService eventService, HttpStatus status) {
    super(errorLogMessage);
    BusinessError businessError = new BusinessError();
    businessError.setMessage(errorLogMessage);
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

  /**
   * Создать исключение с отдельным сообщением для клиента, лога и события.
   *
   * @param errorMessage сообщение для клиента
   * @param errorLogMessage сообщение для лога
   * @param errorEventMessage сообщение для события
   * @param eventService сервис событий
   * @param status HTTP‑статус
   */
  public BusinessException(
      String errorMessage,
      String errorLogMessage,
      String errorEventMessage,
      EventService eventService,
      HttpStatus status) {
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
