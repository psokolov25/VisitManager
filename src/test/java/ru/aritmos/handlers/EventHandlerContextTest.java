package ru.aritmos.handlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Configuration;
import ru.aritmos.service.VisitService;

class EventHandlerContextTest {

    static class Body {
        Branch b1;
        Body(Branch branch) { this.b1 = branch; }
    }

    @Test
    void branchPublicHandlerConvertsBodyToMap() throws Exception {
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        BranchService branchService = mock(BranchService.class);
        Configuration configuration = mock(Configuration.class);

        EventHandlerContext.BranchPublicHandler handler =
                new EventHandlerContext.BranchPublicHandler(visitService, eventService, branchService, configuration);

        Branch branch = new Branch("b1", "Branch1");
        Event event = Event.builder().body(new Body(branch)).build();

        handler.Handle(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Branch>> captor = ArgumentCaptor.forClass(Map.class);
        verify(configuration).createBranchConfiguration(captor.capture());
        assertEquals(branch, captor.getValue().get("b1"));
    }

    @Test
    void simpleHandlersJustLog() {
        Event event = Event.builder().eventType("TEST").build();
        assertDoesNotThrow(() -> new EventHandlerContext.BusinesErrorHandler().Handle(event));
        assertDoesNotThrow(() -> new EventHandlerContext.SystemErrorHandler().Handle(event));
        assertDoesNotThrow(() -> new EventHandlerContext.EntityChangedHandler().Handle(event));
    }
}
