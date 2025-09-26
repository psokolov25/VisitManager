package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;

/**
 * Юнит-тесты для {@link VisitService#setConfirmRequiredModeOfServicePoint(String, String, Boolean)}.
 */
class VisitServiceConfirmRequiredModeTest {

    @DisplayName("Включение режима обязательного подтверждения обновляет точку обслуживания и отправляет событие")
    @Test
    void enableConfirmRequiredModeUpdatesServicePointAndSendsEvent() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Optional<ServicePoint> result = service.setConfirmRequiredModeOfServicePoint("b1", "sp1", true);

        assertTrue(result.isPresent());
        assertTrue(branch.getServicePoints().get("sp1").getIsConfirmRequired());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("frontend"), eq(false), captor.capture());
        Event published = captor.getValue();
        assertEquals("SERVICEPOINT_CONFIRM_REQUIRED_MODE_TURN_ON", published.getEventType());
        assertEquals("b1", published.getParams().get("branchId"));
        assertEquals("sp1", published.getParams().get("servicePointId"));
        assertSame(servicePoint, published.getBody());
    }

    @DisplayName("Отключение режима обязательного подтверждения публикует событие выключения")
    @Test
    void disableConfirmRequiredModePublishesOffEvent() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        servicePoint.setIsConfirmRequired(true);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Optional<ServicePoint> result = service.setConfirmRequiredModeOfServicePoint("b1", "sp1", false);

        assertTrue(result.isPresent());
        assertFalse(branch.getServicePoints().get("sp1").getIsConfirmRequired());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("frontend"), eq(false), captor.capture());
        Event published = captor.getValue();
        assertEquals("SERVICEPOINT_CONFIRM_REQUIRED_MODE_TURN_OFF", published.getEventType());
        assertEquals("b1", published.getParams().get("branchId"));
        assertEquals("sp1", published.getParams().get("servicePointId"));
        assertSame(servicePoint, published.getBody());
    }

    @DisplayName("Установка режима обязательного подтверждения выбрасывает исключение при отсутствии точки обслуживания")
    @Test
    void setConfirmRequiredModeThrowsWhenServicePointMissing() {
        Branch branch = new Branch("b1", "Branch");

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        assertThrows(io.micronaut.http.exceptions.HttpStatusException.class,
                () -> service.setConfirmRequiredModeOfServicePoint("b1", "missing", true));
        verify(eventService).send(eq("*"), eq(false), any());
    }
}
