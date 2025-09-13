package ru.aritmos.exceptions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import org.junit.jupiter.api.Disabled;

class BusinessExceptionTest {

    @Test
    void publishesEventAndThrows() {
        EventService eventService = mock(EventService.class);

        HttpStatusException thrown = assertThrows(
                HttpStatusException.class,
                () -> new BusinessException("ошибка", eventService, HttpStatus.BAD_REQUEST)
        );
        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatus());
        assertEquals("ошибка", thrown.getMessage());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());
    }

    @Test
    void publishesEventWithLogMessage() {
        EventService eventService = mock(EventService.class);

        HttpStatusException thrown = assertThrows(
                HttpStatusException.class,
                () -> new BusinessException("client", "log", eventService, HttpStatus.CONFLICT)
        );
        assertEquals("client", thrown.getMessage());
        assertEquals(HttpStatus.CONFLICT, thrown.getStatus());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());
    }
}
