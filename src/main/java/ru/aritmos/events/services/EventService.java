package ru.aritmos.events.services;

import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.events.model.ChangedObject;
import ru.aritmos.events.model.Event;

/**
 * Сервис отправки событий в шину данных.
 *
 * <p>Пример использования:</p>
 * <pre>{@code
 * Event event = Event.builder()
 *     .eventType("PING")
 *     .eventDate(ZonedDateTime.now())
 *     .build();
 * eventService.send("frontend", false, event);
 * }</pre>
 *
 * <p>Диаграмма последовательности отправки события:</p>
 * <pre>
 * client -> EventService -> DataBusClient -> DataBus
 * </pre>
 *
 * @see <a href="../../../../../../../docs/diagrams/event-service-sequence.svg">Диаграмма последовательности</a>
 */
@Slf4j
@Singleton
public class EventService {
  /** Клиент для отправки событий в шину данных. */
  @Inject DataBusClient dataBusClient;

  /** Имя текущего сервиса-источника событий. */
  @Value("${micronaut.application.name}")
  String applicationName;

  /**
   * Конвертация даты типа {@link ZonedDateTime} в строку формата EEE, dd MMM yyyy HH:mm:ss zzz
   *
   * @param date дата типа {@link ZonedDateTime}
   * @return строка даты формата EEE, dd MMM yyyy HH:mm:ss zzz
   */
  String getDateString(ZonedDateTime date) {

    DateTimeFormatter format =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    return format.format(date);
  }

  /**
   * Отправка события на шину данных.
   *
   * @param destinationServices служба-адресат
   * @param sendToOtherBus флаг переправки события в соседние шины данных
   * @param event тело события
   */
  @ExecuteOn(value = TaskExecutors.IO)
  public void send(String destinationServices, Boolean sendToOtherBus, Event event) {
    event.setSenderService(applicationName);
    Mono.from(
            dataBusClient.send(
                destinationServices,
                sendToOtherBus,
                getDateString(ZonedDateTime.now()),
                applicationName,
                event.getEventType(),
                event.getBody()))
        .subscribe(s -> log.debug("Event {} sent!", s), e -> log.error(e.getMessage()));
  }

  /**
   * Отправка события на шину данных.
   *
   * @param destinationServices список служб-адресатов
   * @param sendToOtherBus флаг переправки события в соседние шины данных
   * @param event тело события
   */
  @SuppressWarnings("unused")
  public void send(List<String> destinationServices, Boolean sendToOtherBus, Event event) {
    destinationServices.forEach(f -> send(f, sendToOtherBus, event));
  }

  /**
   * Отправка события изменения сущности.
   *
   * @param destinationServices служба-адресат
   * @param sendToOtherBus флаг переправки события в соседние шины данных
   * @param oldValue старое значение
   * @param newValue новое значение
   * @param params дополнительные параметры
   * @param action действие над событием
   */
  public void sendChangedEvent(
      String destinationServices,
      Boolean sendToOtherBus,
      Object oldValue,
      Object newValue,
      Map<String, String> params,
      String action) {
    String className =
        oldValue != null ? oldValue.getClass().getName() : newValue.getClass().getName();
    Event event =
        Event.builder()
            .eventDate(ZonedDateTime.now())
            .eventType("ENTITY_CHANGED")
            .senderService(applicationName)
            .params(params)
            .body(
                ChangedObject.builder()
                    .className(className)
                    .action(action)
                    .newValue(newValue)
                    .oldValue(oldValue)
                    .build())
            .build();
    this.send(destinationServices, sendToOtherBus, event);
  }

  /**
   * Отправка события изменения сущности.
   *
   * @param destinationServices список служб-адресатов
   * @param sendToOtherBus флаг переправки события в соседние шины данных
   * @param oldValue старое значение
   * @param newValue новое значение
   * @param params дополнительные параметры
   * @param action действие над событием
   */
  @SuppressWarnings("unused")
  public void sendChangedEvent(
      List<String> destinationServices,
      Boolean sendToOtherBus,
      Object oldValue,
      Object newValue,
      Map<String, String> params,
      String action) {
    destinationServices.forEach(
        f -> sendChangedEvent(f, sendToOtherBus, oldValue, newValue, params, action));
  }
}
