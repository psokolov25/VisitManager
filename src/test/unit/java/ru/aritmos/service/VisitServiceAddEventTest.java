package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

class VisitServiceAddEventTest {

    @DisplayName("проверяется сценарий «adds event when sequence is valid»")
    @Test
    void addsEventWhenSequenceIsValid() {
        Visit visit = Visit.builder()
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        EventService eventService = mock(EventService.class);
        VisitService service = new VisitService();

        service.addEvent(visit, VisitEvent.CREATED, eventService);
        service.addEvent(visit, VisitEvent.PLACED_IN_QUEUE, eventService);

        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE), visit.getVisitEvents());
        assertEquals(2, visit.getEvents().size());
        assertEquals(VisitEvent.PLACED_IN_QUEUE, visit.getEvents().get(1).getVisitEvent());
    }

    @DisplayName("проверяется сценарий «first event must be created»")
    @Test
    void firstEventMustBeCreated() {
        Visit visit = Visit.builder()
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        EventService eventService = mock(EventService.class);
        VisitService service = new VisitService();

        assertThrows(HttpStatusException.class,
                () -> service.addEvent(visit, VisitEvent.CALLED, eventService));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @DisplayName("проверяется сценарий «throws when event cannot follow previous»")
    @Test
    void throwsWhenEventCannotFollowPrevious() {
        Visit visit = Visit.builder()
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        EventService eventService = mock(EventService.class);
        VisitService service = new VisitService();

        service.addEvent(visit, VisitEvent.CREATED, eventService);
        service.addEvent(visit, VisitEvent.PLACED_IN_QUEUE, eventService);
        assertThrows(HttpStatusException.class,
                () -> service.addEvent(visit, VisitEvent.CREATED, eventService));
        verify(eventService).send(eq("*"), eq(false), any());
    }
}
