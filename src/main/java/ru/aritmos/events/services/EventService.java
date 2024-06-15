package ru.aritmos.events.services;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.events.model.ChangedObject;
import ru.aritmos.events.model.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

@Slf4j
@Singleton
public class EventService {
    @Inject
    DataBusClient dataBusClient;
    @Value("${micronaut.application.name}")
    String applicationName;

    String getDateString(Date date) {

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        return format.format(date);
    }

    public void send(String destinationServices, Boolean sendToOtherBus, Event event) {
        event.setSenderService(applicationName);
        Mono.from(
                dataBusClient.send(destinationServices
                        , sendToOtherBus
                        , getDateString(event.getEventDate())
                        , event.getSenderService()
                        , event.getEventType()
                        , event.getBody())
        ).subscribe(s -> log.debug("Event {} sent!", s));

    }

    public void sendChangedEvent(String destinationServices, Boolean sendToOtherBus, String eventType, Object oldValue, Object newValue, HashMap<String, String> params) {
        Event event = Event.builder()
                .eventDate(new Date())
                .eventType(eventType)

                .senderService(applicationName)
                .params(params)
                .body(ChangedObject.builder()
                        .newValue(newValue)
                        .oldValue(oldValue).build())
                .build();
        this.send(destinationServices, sendToOtherBus, event);


    }
}
