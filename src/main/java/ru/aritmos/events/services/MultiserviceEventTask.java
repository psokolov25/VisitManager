package ru.aritmos.events.services;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;

/**
 * Асинхронная задача отправки события нескольким адресатам.
 */
@Slf4j
public class MultiserviceEventTask implements Runnable {
  /** Получатели события. */
  private final List<String> destinationServices;
  /** Отправлять ли событие в соседнюю шину. */
  private final Boolean sendToOtherBus;
  /** Событие. */
  private final Event event;
  /** Сервис отправки событий. */
  private final EventService eventService;

  /**
   * Конструктор задачи отправки события нескольким сервисам.
   *
   * @param destinationServices список получателей
   * @param sendToOtherBus отправлять во внешнюю шину
   * @param event событие
   * @param eventService сервис отправки событий
   */
  public MultiserviceEventTask(
      List<String> destinationServices,
      Boolean sendToOtherBus,
      Event event,
      EventService eventService) {
    this.destinationServices = destinationServices;
    this.sendToOtherBus = sendToOtherBus;
    this.event = event;
    this.eventService = eventService;
  }

  /**
   * Выполняет рассылку события нескольким адресатам в отдельном потоке.
   */
  @Override
  @ExecuteOn(value = TaskExecutors.IO)
  public void run() {
    eventService.send(destinationServices, sendToOtherBus, event);
    log.info("Delayed event {} sent to {}", event, destinationServices);
  }
}
