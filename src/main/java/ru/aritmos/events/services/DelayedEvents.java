package ru.aritmos.events.services;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import ru.aritmos.events.model.Event;

/** Планировщик отложенной отправки событий в шину. */
@Singleton
public class DelayedEvents {
  /** Планировщик задач. */
  protected final TaskScheduler taskScheduler;

  /**
   * Конструктор.
   *
   * @param taskScheduler планировщик задач Micronaut
   */
  public DelayedEvents(@Named(TaskExecutors.SCHEDULED) TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
  }

  /**
   * Отправить событие через заданную задержку.
   *
   * @param destinationService адресат события
   * @param sendToOtherBus признак отправки во внешнюю шину
   * @param event событие
   * @param delayInSeconds задержка в секундах
   * @param eventService сервис отправки событий
   */
  @ExecuteOn(TaskExecutors.IO)
  public void delayedEventService(
      String destinationService,
      Boolean sendToOtherBus,
      Event event,
      Long delayInSeconds,
      EventService eventService) {
    event.setEventDate(ZonedDateTime.now());
    EventTask eventTask = new EventTask(destinationService, sendToOtherBus, event, eventService);
    taskScheduler.schedule(Duration.ofSeconds(delayInSeconds), eventTask);
  }

  /**
   * Отправить событие нескольким адресатам через заданную задержку.
   *
   * @param destinationServices список адресатов
   * @param sendToOtherBus признак отправки во внешнюю шину
   * @param event событие
   * @param delayInSeconds задержка в секундах
   * @param eventService сервис отправки событий
   */
  @SuppressWarnings("unused")
  @ExecuteOn(TaskExecutors.IO)
  public void delayedEventService(
      List<String> destinationServices,
      Boolean sendToOtherBus,
      Event event,
      Long delayInSeconds,
      EventService eventService) {
    event.setEventDate(ZonedDateTime.now());
    MultiserviceEventTask eventTask =
        new MultiserviceEventTask(destinationServices, sendToOtherBus, event, eventService);
    taskScheduler.schedule(Duration.ofSeconds(delayInSeconds), eventTask);
  }
}
