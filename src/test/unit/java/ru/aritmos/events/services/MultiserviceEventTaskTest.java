package ru.aritmos.events.services;

import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.model.Event;

/**
 * Юнит-тест для {@link MultiserviceEventTask}.
 */
class MultiserviceEventTaskTest {

    /** Проверяет отправку события нескольким адресатам. */
    @DisplayName("Запуск задачи отправляет событие всем адресатам")
    @Test
    void runSendsEventToAllDestinations() {
        EventService eventService = mock(EventService.class);
        Event event = Event.builder().eventType("test").build();
        List<String> destinations = List.of("d1", "d2");
        MultiserviceEventTask task = new MultiserviceEventTask(destinations, false, event, eventService);

        task.run();

        verify(eventService).send(destinations, false, event);
    }
}
