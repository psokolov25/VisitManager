package ru.aritmos.events.services;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.model.EventHandler;
import ru.aritmos.exceptions.SystemException;

/**
 * Подписчик Kafka для обработки событий шины данных.
 */
@Slf4j
@Requires(notEnv = "local-no-docker")
@io.micronaut.configuration.kafka.annotation.KafkaListener(offsetReset = OffsetReset.LATEST)
public class KafkaListener {
  /** Обработчики общих событий (topic `events`). */
  private static final HashMap<String, EventHandler> allHandlers = new HashMap<>();
  /** Обработчики событий для сервиса (topic `event_${micronaut.application.name}`). */
  private static final HashMap<String, EventHandler> serviceHandlers = new HashMap<>();
  /** Сериализатор/десериализатор JSON. */
  @Inject ObjectMapper objectMapper;

  /**
   * Регистрация обработчика для общих событий (topic `events`).
   *
   * @param eventType тип события
   * @param handler обработчик события
   */
  public static void addAllEventHandler(String eventType, EventHandler handler) {
    allHandlers.put(eventType, handler);
  }

  /**
   * Регистрация обработчика событий сервиса (topic `event_${micronaut.application.name}`).
   *
   * @param eventType тип события
   * @param handler обработчик события
   */
  public static void addServiceEventHandler(String eventType, EventHandler handler) {
    serviceHandlers.put(eventType, handler);
  }

  /**
   * Получает сообщения от шины данных, адресованные данной службе.
   *
   * @param key ключ сообщения Kafka
   * @param event тело события класса {@link Event}
   * @throws IOException ошибка десериализации события
   * @throws SystemException системная ошибка обработчика
   * @throws IllegalAccessException ошибка доступа при рефлексии в обработчике
   */
  @Topic("event_${micronaut.application.name}")
  @ExecuteOn(TaskExecutors.IO)
  public void receive(@KafkaKey String key, String event)
      throws IOException, SystemException, IllegalAccessException {

    log.debug("Receive key {} value {}", key, event);
    Event event1 = objectMapper.readValue(event, Event.class);
    if (serviceHandlers.containsKey(event1.getEventType())) {
      serviceHandlers.get(event1.getEventType()).Handle(event1);
    }
  }

  /**
   * Получает сообщения от шины данных, адресованные для всех служб.
   *
   * @param key ключ сообщения Kafka
   * @param event тело события класса {@link Event}
   * @throws IOException ошибка десериализации события
   * @throws SystemException системная ошибка обработчика
   * @throws IllegalAccessException ошибка доступа при рефлексии в обработчике
   */
  @Topic("events")
  @ExecuteOn(TaskExecutors.IO)
  public void receiveAll(@KafkaKey String key, String event)
      throws IOException, SystemException, IllegalAccessException {
    log.debug("Receive broadcast message key {} value {}", key, event);
    Event event1 = objectMapper.readValue(event, Event.class);
    if (allHandlers.containsKey(event1.getEventType())) {
      allHandlers.get(event1.getEventType()).Handle(event1);
    }
  }
}
