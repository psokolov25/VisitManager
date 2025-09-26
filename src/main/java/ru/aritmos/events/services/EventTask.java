package ru.aritmos.events.services;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;

/** Асинхронная задача отправки события в шину. */
@Slf4j
public class EventTask implements Runnable {
  /** Получатель события. */
  private final String destinationService;

  /** Отправлять ли событие в соседнюю шину. */
  private final Boolean sendToOtherBus;

  /** Событие. */
  private final Event event;

  /** Сервис отправки событий. */
  private final EventService eventService;

  /**
   * Конструктор задачи отправки события.
   *
   * @param destinationService получатель
   * @param sendToOtherBus отправлять на общий bus дополнительно
   * @param event событие
   * @param eventService сервис отправки событий
   */
  public EventTask(
      String destinationService, Boolean sendToOtherBus, Event event, EventService eventService) {
    this.destinationService = destinationService;
    this.sendToOtherBus = sendToOtherBus;
    this.event = event;
    this.eventService = eventService;
  }

  /** Запускает отправку события в отдельном потоке. Логирует результат отправки. */
  @Override
  @ExecuteOn(value = TaskExecutors.IO)
  public void run() {
    eventService.send(destinationService, sendToOtherBus, event);
    log.info("Delayed event {} sent to {}", event, destinationService);
  }
}
