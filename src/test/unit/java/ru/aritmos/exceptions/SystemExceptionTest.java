package ru.aritmos.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

class SystemExceptionTest {

    @Test
    void sendsSystemErrorEventOnCreation() {
        EventService eventService = Mockito.mock(EventService.class);

        SystemException exception = new SystemException("boom", eventService);
        assertEquals("boom", exception.getMessage());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());

        Event event = captor.getValue();
        assertEquals("SYSTEM_ERROR", event.getEventType());
        assertTrue(event.getBody() instanceof SystemException.BusinessError);
        SystemException.BusinessError error = (SystemException.BusinessError) event.getBody();
        assertEquals("boom", error.getMessage());
    }
}

