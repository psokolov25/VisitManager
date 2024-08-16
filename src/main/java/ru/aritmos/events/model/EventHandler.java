package ru.aritmos.events.model;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

public interface EventHandler {
@ExecuteOn(TaskExecutors.IO)
    void Handle(Event event) ;
}
