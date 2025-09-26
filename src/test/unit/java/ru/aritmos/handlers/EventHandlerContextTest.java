package ru.aritmos.handlers;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.model.EventHandler;
import ru.aritmos.events.services.EventService;
import ru.aritmos.events.services.KafkaListener;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.keycloak.UserSession;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Configuration;
import ru.aritmos.service.VisitService;

class EventHandlerContextTest {

    static class Body {
        Branch b1;
        Body(Branch branch) { this.b1 = branch; }
    }

    @DisplayName("Обработчик с кодом BRANCH_PUBLIC преобразует тело события в карту")
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

    @DisplayName("Простые обработчики ограничиваются логированием")
    @Test
    void simpleHandlersJustLog() {
        Event event = Event.builder().eventType("TEST").build();
        assertDoesNotThrow(() -> new EventHandlerContext.BusinesErrorHandler().Handle(event));
        assertDoesNotThrow(() -> new EventHandlerContext.SystemErrorHandler().Handle(event));
        assertDoesNotThrow(() -> new EventHandlerContext.EntityChangedHandler().Handle(event));
    }

    @DisplayName("Непринудительный выход закрывает точку и завершает визит")
    @Test
    void notForceLogoutClosesPointAndEndsVisit() throws Exception {
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        Branch branch = new Branch("b1", "B1");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setBranchId("b1");
        User user = new User();
        user.setName("u1");
        sp.setUser(user);
        Visit visit = Visit.builder().id("v1").build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        when(branchService.getDetailedBranches()).thenReturn(new HashMap<>(Map.of("b1", branch)));

        EventHandlerContext.NotForceUserLogoutHandler handler =
                new EventHandlerContext.NotForceUserLogoutHandler(visitService, eventService);

        UserSession session = UserSession.builder().login("u1").build();
        Event event = Event.builder().body(session).build();

        handler.Handle(event);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("frontend"), eq(false), captor.capture());
        assertEquals("PROCESSING_USER_LOGOUT_FORCE", captor.getValue().getEventType());
        verify(visitService).visitEnd("b1", "sp1", true, "USER_SESSION_KILLED");
        verify(branchService)
                .closeServicePoint("b1", "sp1", visitService, false, false, "", false, "");
    }

    @DisplayName("Принудительный выход закрывает точку обслуживания")
    @Test
    void forceLogoutClosesPoint() throws Exception {
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        Branch branch = new Branch("b1", "B1");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setBranchId("b1");
        User user = new User();
        user.setName("u1");
        sp.setUser(user);
        branch.getServicePoints().put(sp.getId(), sp);

        when(branchService.getDetailedBranches()).thenReturn(new HashMap<>(Map.of("b1", branch)));

        EventHandlerContext.ForceUserLogoutHandler handler =
                new EventHandlerContext.ForceUserLogoutHandler(visitService, eventService);

        UserSession session = UserSession.builder().login("u1").build();
        Event event = Event.builder().body(session).senderService("external").build();

        handler.Handle(event);

        verify(branchService)
                .closeServicePoint("b1", "sp1", visitService, false, false, "", true, "USER_SESSION_KILLED");
        verifyNoInteractions(eventService);
    }

    /**
     * Проверяет, что метод {@link EventHandlerContext#AddHandlers()} регистрирует все типы событий
     * и публикует демонстрационную конфигурацию отделений.
     */
    @DisplayName("Регистрация обработчиков добавляет слушателей и публикует демо-конфигурацию")
    @Test
    void addHandlersRegistersAllListenersAndPublishesDemoConfiguration() throws Exception {
        Field allHandlersField = KafkaListener.class.getDeclaredField("allHandlers");
        Field serviceHandlersField = KafkaListener.class.getDeclaredField("serviceHandlers");
        allHandlersField.setAccessible(true);
        serviceHandlersField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, EventHandler> originalAllHandlers = new HashMap<>((Map<String, EventHandler>) allHandlersField.get(null));
        @SuppressWarnings("unchecked")
        Map<String, EventHandler> originalServiceHandlers =
                new HashMap<>((Map<String, EventHandler>) serviceHandlersField.get(null));

        try {
            @SuppressWarnings("unchecked")
            Map<String, EventHandler> allHandlers = (Map<String, EventHandler>) allHandlersField.get(null);
            @SuppressWarnings("unchecked")
            Map<String, EventHandler> serviceHandlers =
                    (Map<String, EventHandler>) serviceHandlersField.get(null);
            allHandlers.clear();
            serviceHandlers.clear();

            Configuration configuration = mock(Configuration.class);
            VisitService visitService = mock(VisitService.class);
            EventService eventService = mock(EventService.class);
            BranchService branchService = mock(BranchService.class);

            EventHandlerContext context = new EventHandlerContext();
            context.configuration = configuration;
            context.visitService = visitService;
            context.eventService = eventService;
            context.branchService = branchService;

            Map<String, Branch> demoBranches = Map.of("demo", new Branch("demo", "Demo"));
            when(configuration.createDemoBranch()).thenReturn(demoBranches);

            context.AddHandlers();

            assertTrue(allHandlers.containsKey("BUSINESS_ERROR"));
            assertTrue(allHandlers.get("BUSINESS_ERROR") instanceof EventHandlerContext.BusinesErrorHandler);
            assertTrue(allHandlers.containsKey("SYSTEM_ERROR"));
            assertTrue(allHandlers.get("SYSTEM_ERROR") instanceof EventHandlerContext.SystemErrorHandler);
            assertTrue(allHandlers.containsKey("ENTITY_CHANGED"));
            assertTrue(allHandlers.get("ENTITY_CHANGED") instanceof EventHandlerContext.EntityChangedHandler);

            assertTrue(serviceHandlers.containsKey("ENTITY_CHANGED"));
            assertTrue(serviceHandlers.get("ENTITY_CHANGED") instanceof EventHandlerContext.EntityChangedHandler);
            assertTrue(serviceHandlers.containsKey("BRANCH_PUBLIC"));
            assertTrue(serviceHandlers.get("BRANCH_PUBLIC") instanceof EventHandlerContext.BranchPublicHandler);
            assertTrue(serviceHandlers.containsKey("PROCESSING_USER_LOGOUT_FORCE"));
            assertTrue(serviceHandlers.get("PROCESSING_USER_LOGOUT_FORCE")
                    instanceof EventHandlerContext.ForceUserLogoutHandler);
            assertTrue(serviceHandlers.containsKey("PROCESSING_USER_LOGOUT_NOT_FORCE"));
            assertTrue(serviceHandlers.get("PROCESSING_USER_LOGOUT_NOT_FORCE")
                    instanceof EventHandlerContext.NotForceUserLogoutHandler);

            verify(configuration).createDemoBranch();
            verify(configuration).createBranchConfiguration(demoBranches);
        } finally {
            @SuppressWarnings("unchecked")
            Map<String, EventHandler> allHandlers = (Map<String, EventHandler>) allHandlersField.get(null);
            @SuppressWarnings("unchecked")
            Map<String, EventHandler> serviceHandlers =
                    (Map<String, EventHandler>) serviceHandlersField.get(null);
            allHandlers.clear();
            allHandlers.putAll(originalAllHandlers);
            serviceHandlers.clear();
            serviceHandlers.putAll(originalServiceHandlers);
        }
    }
}
