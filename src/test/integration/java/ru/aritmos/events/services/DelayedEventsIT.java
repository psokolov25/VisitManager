package ru.aritmos.events.services;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.aritmos.events.model.Event;

/**
 * Интеграционный тест {@link DelayedEvents} с использованием контекста Micronaut.
 */
@MicronautTest(environments = "integration")
class DelayedEventsIT {

    @Inject DelayedEvents delayedEvents;
    @Inject TaskScheduler scheduler;
    @Inject EventService eventService;

    @MockBean(TaskScheduler.class)
    @Named(TaskExecutors.SCHEDULED)
    TaskScheduler scheduler() {
        return Mockito.mock(
                TaskScheduler.class,
                invocation ->
                        ScheduledFuture.class.isAssignableFrom(invocation.getMethod().getReturnType())
                                ? Mockito.mock(ScheduledFuture.class)
                                : org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation));
    }

    @MockBean(EventService.class)
    EventService mockEventService() {
        return Mockito.mock(EventService.class);
    }

    @DisplayName("Планирование события с использованием контекста")
    @Test
    void schedulesEventWithContext() {
        Event event = Event.builder().eventType("t").build();
        delayedEvents.delayedEventService("dest", false, event, 2L, eventService);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(eq(Duration.ofSeconds(2)), captor.capture());
        captor.getValue().run();
        verify(eventService).send("dest", false, event);
    }
}
