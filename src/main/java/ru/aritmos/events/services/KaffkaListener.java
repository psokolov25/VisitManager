package ru.aritmos.events.services;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.model.EventHandler;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
@KafkaListener(offsetReset = OffsetReset.EARLIEST)
public class KaffkaListener {
    private static  HashMap<String, EventHandler> allHandlers = new HashMap<>();
    private static  HashMap<String, EventHandler> serviceHandlers = new HashMap<>();
    public static void addAllEventHandler(String eventType,EventHandler handler)
    {
        allHandlers.put(eventType,handler);
    }
    public static void addServiceEventHandler(String eventType,EventHandler handler)
    {
        serviceHandlers.put(eventType,handler);
    }
    @Inject
    ObjectMapper objectMapper;

    @Topic("event_${micronaut.application.name}")
    public void recieve(@KafkaKey String key, String event) throws IOException {

        log.info("Recieve key {} value {}", key, event);
        Event event1 = objectMapper.readValue(event, Event.class);
        if(serviceHandlers.containsKey(event1.getEventType()))
        {
            serviceHandlers.get(event1.getEventType()).Handle(event1);
        }

    }

    @Topic("events")
    public void recieveAll(@KafkaKey String key, String event) throws IOException {
        log.info("Recieve key {} value {}", key, event);
        Event event1 = objectMapper.readValue(event, Event.class);
        if(allHandlers.containsKey(event1.getEventType()))
        {
            allHandlers.get(event1.getEventType()).Handle(event1);
        }
    }
}
