package ru.aritmos.events.services;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.model.EventHandler;

@MicronautTest
class KafkaListenerTest {

    @Inject KafkaListener kafkaListener;
    @Inject ObjectMapper objectMapper;

    @BeforeEach
    void clearHandlers() throws Exception {
        Field all = KafkaListener.class.getDeclaredField("allHandlers");
        all.setAccessible(true);
        ((Map<?, ?>) all.get(null)).clear();
        Field service = KafkaListener.class.getDeclaredField("serviceHandlers");
        service.setAccessible(true);
        ((Map<?, ?>) service.get(null)).clear();
    }

    @DisplayName("Добавление обработчика сервисных событий регистрирует обработчик")
    @Test
    void addServiceEventHandlerRegistersHandler() throws Exception {
        EventHandler handler = mock(EventHandler.class);
        KafkaListener.addServiceEventHandler("t", handler);
        Field f = KafkaListener.class.getDeclaredField("serviceHandlers");
        f.setAccessible(true);
        Map<String, EventHandler> map = (Map<String, EventHandler>) f.get(null);
        ru.aritmos.test.LoggingAssertions.assertSame(handler, map.get("t"));
    }

    @DisplayName("Добавление обработчика всех событий регистрирует обработчик")
    @Test
    void addAllEventHandlerRegistersHandler() throws Exception {
        EventHandler handler = mock(EventHandler.class);
        KafkaListener.addAllEventHandler("t", handler);
        Field f = KafkaListener.class.getDeclaredField("allHandlers");
        f.setAccessible(true);
        Map<String, EventHandler> map = (Map<String, EventHandler>) f.get(null);
        ru.aritmos.test.LoggingAssertions.assertSame(handler, map.get("t"));
    }

    @DisplayName("Метод receive вызывает сервисный обработчик")
    @Test
    void receiveCallsServiceHandler() throws Exception {
        Event event = Event.builder().eventType("t").build();
        String json = objectMapper.writeValueAsString(event);
        EventHandler handler = mock(EventHandler.class);
        KafkaListener.addServiceEventHandler("t", handler);
        kafkaListener.receive("k", json);
        verify(handler).Handle(event);
    }

    @DisplayName("Метод receiveAll вызывает обработчик всех событий")
    @Test
    void receiveAllCallsAllHandler() throws Exception {
        Event event = Event.builder().eventType("t").build();
        String json = objectMapper.writeValueAsString(event);
        EventHandler handler = mock(EventHandler.class);
        KafkaListener.addAllEventHandler("t", handler);
        kafkaListener.receiveAll("k", json);
        verify(handler).Handle(event);
    }
}
