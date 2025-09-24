package ru.aritmos.exceptions;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.serde.annotation.Serdeable;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
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
   * Создать исключение с сообщениями для клиента и лога.
   *
   * @param clientMessage сообщение для HTTP-ответа (английский)
   * @param logMessage сообщение для лога (русский)
   * @param eventService сервис событий
   * @param status HTTP-статус
   */
  public BusinessException(
      String clientMessage, String logMessage, EventService eventService, HttpStatus status) {
    super(logMessage);
    this.eventService = eventService;
    throw toHttpStatusException(clientMessage, clientMessage, logMessage, logMessage, status);
  }

  /**
   * Создать исключение с сообщениями для клиента, лога и события.
   *
   * @param clientMessage сообщение для HTTP-ответа (английский)
   * @param logMessage сообщение для лога (русский)
   * @param eventMessage сообщение для события
   * @param eventService сервис событий
   * @param status HTTP-статус
   */
  public BusinessException(
      String clientMessage,
      String logMessage,
      String eventMessage,
      EventService eventService,
      HttpStatus status) {
    super(logMessage);
    this.eventService = eventService;
    throw toHttpStatusException(clientMessage, clientMessage, logMessage, eventMessage, status);
  }

  /**
   * Создать исключение с произвольным телом ответа и сообщением для лога.
   *
   * @param responseBody тело ответа (англоязычное)
   * @param logMessage сообщение для лога (русский)
   * @param eventService сервис событий
   * @param status HTTP-статус
   */
  public BusinessException(
      Object responseBody, String logMessage, EventService eventService, HttpStatus status) {
    super(logMessage);
    this.eventService = eventService;
    throw toHttpStatusException(responseBody, null, logMessage, logMessage, status);
  }

  /**
   * Создать исключение с произвольным телом ответа, сообщением для лога и отдельным сообщением для
   * события.
   *
   * @param responseBody тело ответа (англоязычное)
   * @param logMessage сообщение для лога (русский)
   * @param eventMessage сообщение для события
   * @param eventService сервис событий
   * @param status HTTP-статус
   */
  public BusinessException(
      Object responseBody,
      String logMessage,
      String eventMessage,
      EventService eventService,
      HttpStatus status) {
    super(logMessage);
    this.eventService = eventService;
    throw toHttpStatusException(responseBody, null, logMessage, eventMessage, status);
  }

  /**
   * Создать исключение с произвольным телом, сообщением для лога и сериализацией сообщения события.
   *
   * @param responseBody тело ответа (англоязычное)
   * @param logMessage сообщение для лога (русский)
   * @param eventService сервис событий
   * @param status HTTP-статус
   * @param mapper сериализатор тела
   * @throws IOException ошибка сериализации
   */
  public BusinessException(
      Object responseBody,
      String logMessage,
      EventService eventService,
      HttpStatus status,
      io.micronaut.serde.ObjectMapper mapper)
      throws IOException {
    super(logMessage);
    this.eventService = eventService;
    throw toHttpStatusException(
        responseBody,
        null,
        logMessage,
        mapper != null ? mapper.writeValueAsString(responseBody) : String.valueOf(responseBody),
        status);
  }

  private HttpStatusException toHttpStatusException(
      Object responseBody,
      String clientMessage,
      String logMessage,
      String eventMessage,
      HttpStatus status) {
    EventService currentEventService = this.eventService;
    String effectiveLogMessage =
        Objects.requireNonNullElse(logMessage, Objects.requireNonNullElse(clientMessage, eventMessage));
    String effectiveEventMessage = Objects.requireNonNullElse(eventMessage, effectiveLogMessage);
    Object responseBodyToSend =
        responseBody != null
            ? responseBody
            : Objects.requireNonNullElse(clientMessage, effectiveEventMessage);
    if (currentEventService != null) {
      BusinessError businessError = new BusinessError();
      businessError.setMessage(effectiveEventMessage);
      currentEventService.send(
          "*",
          false,
          Event.builder()
              .eventDate(ZonedDateTime.now())
              .eventType("BUSINESS_ERROR")
              .body(businessError)
              .build());
    }
    log.error(effectiveLogMessage);
    HttpStatus httpStatus = Objects.requireNonNull(status, "HTTP status must not be null");
    String messageForClient = Objects.requireNonNullElse(clientMessage, effectiveEventMessage);
    return new DetailedHttpStatusException(httpStatus, messageForClient, responseBodyToSend);
  }

  @Data
  @Serdeable
  @SuppressWarnings("unused")
  static class BusinessError {
    String message;
  }

  private static final class DetailedHttpStatusException extends HttpStatusException {
    private final Object responseBody;

    private DetailedHttpStatusException(HttpStatus status, String message, Object responseBody) {
      super(status, message);
      this.responseBody = responseBody;
    }

    @Override
    public Optional<Object> getBody() {
      return Optional.ofNullable(responseBody);
    }
  }
}
