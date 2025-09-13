package ru.aritmos.events.services;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import io.micronaut.scheduling.TaskScheduler;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import org.junit.jupiter.api.Disabled;

/**
 * Юнит-тесты для {@link DelayedEvents}.
 */
class DelayedEventsTest {

    /** Проверяет планирование события для одного адресата. */
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
    @Disabled("Not yet implemented")
    @Test
    void delayedEventServiceTest() {
        // TODO implement
    }

}
