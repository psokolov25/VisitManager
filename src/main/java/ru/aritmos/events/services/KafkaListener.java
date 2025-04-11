package ru.aritmos.events.services;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
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

@Slf4j
@io.micronaut.configuration.kafka.annotation.KafkaListener(offsetReset = OffsetReset.LATEST)
public class KafkaListener {
  private static final HashMap<String, EventHandler> allHandlers = new HashMap<>();
  private static final HashMap<String, EventHandler> serviceHandlers = new HashMap<>();
  @Inject ObjectMapper objectMapper;

  public static void addAllEventHandler(String eventType, EventHandler handler) {
    allHandlers.put(eventType, handler);
  }

  public static void addServiceEventHandler(String eventType, EventHandler handler) {
    serviceHandlers.put(eventType, handler);
  }

  /**
   * Получает сообщения от шины дынных адресованные данной службе
   *
   * @param key Ключ сообщения кафка
   * @param event Тело события класса Event
   * @throws IOException Обрабатываемые исключения
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
   * Получает сообщения от шины дынных адресованные для всех служб
   *
   * @param key Ключ сообщения кафка
   * @param event Тело события класса Event
   * @throws IOException Обрабатываемые исключения
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
