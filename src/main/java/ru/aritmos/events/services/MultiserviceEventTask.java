package ru.aritmos.events.services;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;

import java.util.List;
@Slf4j
public class MultiserviceEventTask implements Runnable {
  private final List<String> destinationServices;
  private final Boolean sendToOtherBus;
  private final Event event;
  private final EventService eventService;
  public MultiserviceEventTask(List<String> destinationServices, Boolean sendToOtherBus, Event event, EventService eventService)
  {
    this.destinationServices=destinationServices;
    this.sendToOtherBus=sendToOtherBus;
    this.event=event;
      this.eventService = eventService;
  }

  @Override
  @ExecuteOn(value = TaskExecutors.IO)
  public void run() {
    eventService.send(destinationServices,sendToOtherBus,event);
    log.info("Delayed event {} sent to {}", event, destinationServices);
  }
}
