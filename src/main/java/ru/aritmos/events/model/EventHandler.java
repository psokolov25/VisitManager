package ru.aritmos.events.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import ru.aritmos.exceptions.SystemException;

public interface EventHandler {
  @ExecuteOn(TaskExecutors.IO)
  void Handle(Event event) throws JsonProcessingException, SystemException, IllegalAccessException;
}
