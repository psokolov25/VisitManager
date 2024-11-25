package ru.aritmos.events.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

public interface EventHandler {
  @ExecuteOn(TaskExecutors.IO)
  void Handle(Event event) throws JsonProcessingException;
}
