package ru.aritmos.events.services;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;

@Slf4j
public class EventTask implements Runnable {
  private final String destinationService;
  private final Boolean sendToOtherBus;
  private final Event event;
  private final EventService eventService;

  public EventTask(
      String destinationService, Boolean sendToOtherBus, Event event, EventService eventService) {
    this.destinationService = destinationService;
    this.sendToOtherBus = sendToOtherBus;
    this.event = event;
    this.eventService = eventService;
  }

  @Override
  @ExecuteOn(value = TaskExecutors.IO)
  public void run() {
    eventService.send(destinationService, sendToOtherBus, event);
    log.info("Delayed event {} sent to {}", event, destinationService);
  }
}
