package ru.aritmos.events.services;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@KafkaListener(offsetReset = OffsetReset.EARLIEST)
public class KaffkaListener {
    @Topic("event_${micronaut.application.name}")
    public void recieve(@KafkaKey String key, String event) {
        log.info("Recieve key {} value {}", key, event);
    }

    @Topic("events")
    public void recieveAll(@KafkaKey String key, String event) {
        log.info("Recieve key {} value {}", key, event);
    }
}
