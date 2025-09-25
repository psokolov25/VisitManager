package ru.aritmos.exceptions;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

class BusinessExceptionTest {

    @DisplayName("Publishes Event And Throws")
    @Test
    void publishesEventAndThrows() {
        EventService eventService = mock(EventService.class);

        HttpStatusException thrown = assertThrows(
                HttpStatusException.class,
                () ->
                    new BusinessException(
                        "Error occurred", "Произошла ошибка", eventService, HttpStatus.BAD_REQUEST)
        );
        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatus());
        assertEquals("Error occurred", thrown.getMessage());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());
    }

    @DisplayName("Publishes Event With Log Message")
    @Test
    void publishesEventWithLogMessage() {
        EventService eventService = mock(EventService.class);

        HttpStatusException thrown = assertThrows(
                HttpStatusException.class,
                () ->
                    new BusinessException(
                        "Client message", "Сообщение для лога", eventService, HttpStatus.CONFLICT)
        );
        assertEquals("Client message", thrown.getMessage());
        assertEquals(HttpStatus.CONFLICT, thrown.getStatus());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());
    }

    @DisplayName("Publishes Event With Mapper")
    @Test
    void publishesEventWithMapper() throws Exception {
        EventService eventService = mock(EventService.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenReturn("json");

        HttpStatusException thrown = assertThrows(
                HttpStatusException.class,
                () ->
                    new BusinessException(
                        new Object(), "Сообщение для лога", eventService, HttpStatus.I_AM_A_TEAPOT, mapper)
        );
        assertEquals(HttpStatus.I_AM_A_TEAPOT, thrown.getStatus());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());
        verify(mapper, atLeastOnce()).writeValueAsString(any());
    }

    @DisplayName("Publishes Event With Object Body")
    @Test
    void publishesEventWithObjectBody() {
        EventService eventService = mock(EventService.class);
        Object body = new Object();

        HttpStatusException thrown = assertThrows(
                HttpStatusException.class,
                () ->
                    new BusinessException(
                        body, "Сообщение для лога", eventService, HttpStatus.NOT_FOUND)
        );
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());
    }

    @DisplayName("Publishes Event With Separate Event Message")
    @Test
    void publishesEventWithSeparateEventMessage() {
        EventService eventService = mock(EventService.class);

        HttpStatusException thrown = assertThrows(
                HttpStatusException.class,
                () ->
                    new BusinessException(
                        "Client message",
                        "Сообщение для лога",
                        "Event payload",
                        eventService,
                        HttpStatus.BAD_REQUEST)
        );
        assertEquals("Client message", thrown.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatus());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());
    }
}
