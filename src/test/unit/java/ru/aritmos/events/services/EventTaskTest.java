package ru.aritmos.events.services;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.events.model.Event;

/**
 * Юнит-тест для {@link EventTask}.
 */
class EventTaskTest {

    /** Проверяет делегирование сервису отправки событий. */
    @DisplayName("проверяется сценарий «run sends event via service»")
    @Test
    void runSendsEventViaService() {
        EventService eventService = mock(EventService.class);
        Event event = Event.builder().eventType("test").build();
        EventTask task = new EventTask("dest", true, event, eventService);

        task.run();

        verify(eventService).send("dest", true, event);
    }
}