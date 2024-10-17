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

@Singleton
public class DelayedEvents {
    protected final TaskScheduler taskScheduler;



    public DelayedEvents(@Named(TaskExecutors.SCHEDULED) TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;



    }
    @ExecuteOn(TaskExecutors.IO)
    public void delayedEventService(String destinationService, Boolean sendToOtherBus, Event event,Long delayInSeconds,EventService eventService){
        event.setEventDate(ZonedDateTime.now());
        EventTask eventTask=new EventTask(destinationService,sendToOtherBus,event,eventService);
        taskScheduler.schedule(Duration.ofSeconds(delayInSeconds),eventTask);
    }
    @ExecuteOn(TaskExecutors.IO)
    public void delayedEventService(List<String> destinationServices, Boolean sendToOtherBus, Event event, Long delayInSeconds,EventService eventService){
        event.setEventDate(ZonedDateTime.now());
        MultiserviceEventTask eventTask=new MultiserviceEventTask(destinationServices,sendToOtherBus,event,eventService);
        taskScheduler.schedule(Duration.ofSeconds(delayInSeconds),eventTask);
    }
}
