package ru.aritmos.events.services;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.scheduling.TaskScheduler;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;

/**
 * Юнит-тесты для {@link DelayedEvents}.
 */
class DelayedEventsTest {

    /** Проверяет планирование события для одного адресата. */
    @DisplayName("Планирование события для одного адресата")
    @Test
    void schedulesEventForSingleDestination() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        DelayedEvents delayed = new DelayedEvents(scheduler);
        EventService eventService = mock(EventService.class);
        Event event = Event.builder().eventType("t").build();

        delayed.delayedEventService("dest", true, event, 5L, eventService);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(eq(Duration.ofSeconds(5)), captor.capture());
        assertNotNull(event.getEventDate());
        captor.getValue().run();
        verify(eventService).send("dest", true, event);
    }

    /** Проверяет планирование события для списка адресатов. */
    @DisplayName("Планирование события для нескольких адресатов")
    @Test
    void schedulesEventForMultipleDestinations() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        DelayedEvents delayed = new DelayedEvents(scheduler);
        EventService eventService = mock(EventService.class);
        Event event = Event.builder().eventType("t").build();
        List<String> destinations = List.of("d1", "d2");

        delayed.delayedEventService(destinations, false, event, 3L, eventService);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(eq(Duration.ofSeconds(3)), captor.capture());
        assertNotNull(event.getEventDate());
        captor.getValue().run();
        verify(eventService).send(destinations, false, event);
    }
}
