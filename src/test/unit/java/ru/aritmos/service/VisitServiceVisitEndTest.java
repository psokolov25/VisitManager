package ru.aritmos.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.aritmos.test.LoggingAssertions.assertEquals;
import static ru.aritmos.test.LoggingAssertions.assertNotNull;
import static ru.aritmos.test.LoggingAssertions.assertNull;
import static ru.aritmos.test.LoggingAssertions.assertSame;
import static ru.aritmos.test.LoggingAssertions.assertThrows;
import static ru.aritmos.test.LoggingAssertions.assertTrue;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.model.visit.VisitEventInformation;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Модульные тесты для {@link VisitService#visitEnd(String, String, Boolean, String)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceVisitEndTest {

    @BeforeEach
    void clearVisitEventParametersBeforeTest() {
        clearVisitEventParameters();
    }

    @AfterEach
    void clearVisitEventParametersAfterTest() {
        clearVisitEventParameters();
    }

    private void clearVisitEventParameters() {
        VisitEvent.STOP_SERVING.getParameters().clear();
        VisitEvent.BACK_TO_QUEUE.getParameters().clear();
        VisitEvent.END.getParameters().clear();
    }

    @DisplayName("Visit End Returns Visit To Queue When Next Service Available")
    @Test
    void visitEndReturnsVisitToQueueWhenNextServiceAvailable() {
        Branch branch = new Branch("branch-1", "Отделение №1");
        ServicePoint servicePoint = new ServicePoint("sp-1", "Окно 1");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Иван Иванов");
        operator.setCurrentWorkProfileId("wp-1");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Service currentService = new Service("svc-1", "Консультация", 15, "queue-main");
        Service nextService = new Service("svc-2", "Доп. услуга", 10, "queue-follow");

        Visit visit = Visit.builder()
                .id("visit-1")
                .branchId(branch.getId())
                .currentService(currentService)
                .unservedServices(new ArrayList<>(List.of(nextService)))
                .servedServices(new ArrayList<>())
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Visit result = service.visitEnd(branch.getId(), servicePoint.getId(), Boolean.FALSE, "Клиент вернётся позже");

        assertSame(visit, result);
        assertEquals(1, visit.getServedServices().size());
        assertSame(currentService, visit.getServedServices().get(0));
        assertEquals("svc-2", visit.getCurrentService().getId());
        assertEquals("queue-follow", visit.getQueueId());
        assertTrue(visit.getUnservedServices().isEmpty());
        assertNotNull(visit.getTransferDateTime());
        assertNotNull(visit.getServedDateTime());
        assertNotNull(visit.getReturnDateTime());
        assertNull(visit.getCallDateTime());
        assertNull(visit.getStartServingDateTime());
        assertNull(visit.getServicePointId());

        Map<String, String> stopServingParameters = new HashMap<>(VisitEvent.STOP_SERVING.getParameters());
        assertEquals("false", stopServingParameters.get("isForced"));
        assertEquals("Клиент вернётся позже", stopServingParameters.get("reason"));
        assertEquals(servicePoint.getId(), stopServingParameters.get("servicePointId"));
        assertEquals(servicePoint.getName(), stopServingParameters.get("servicePointName"));
        assertEquals(branch.getId(), stopServingParameters.get("branchId"));
        assertEquals(operator.getId(), stopServingParameters.get("staffId"));
        assertEquals(operator.getName(), stopServingParameters.get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), stopServingParameters.get("workProfileId"));

        Map<String, String> backToQueueParameters = new HashMap<>(VisitEvent.BACK_TO_QUEUE.getParameters());
        assertEquals(branch.getId(), backToQueueParameters.get("branchId"));
        assertEquals("queue-follow", backToQueueParameters.get("queueId"));
        assertEquals(servicePoint.getId(), backToQueueParameters.get("servicePointId"));
        assertEquals(operator.getId(), backToQueueParameters.get("staffId"));
        assertEquals(operator.getName(), backToQueueParameters.get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), backToQueueParameters.get("workProfileId"));

        verify(branchService).getBranch(branch.getId());
        verify(branchService)
                .updateVisit(same(visit), same(VisitEvent.STOP_SERVING), same(service), eq(Boolean.TRUE));
        verify(branchService)
                .updateVisit(same(visit), same(VisitEvent.BACK_TO_QUEUE), same(service), eq(Boolean.TRUE));
        verifyNoMoreInteractions(branchService);
        verifyNoInteractions(eventService);
    }

    @DisplayName("Visit End Completes Visit When No Unserved Services Left")
    @Test
    void visitEndCompletesVisitWhenNoUnservedServicesLeft() {
        Branch branch = new Branch("branch-2", "Отделение №2");
        ServicePoint servicePoint = new ServicePoint("sp-2", "Окно 5");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Service currentService = new Service("svc-3", "Оформление", 12, "queue-initial");

        VisitEventInformation previousEvent = VisitEventInformation.builder()
                .visitEvent(VisitEvent.CALLED)
                .eventDateTime(ZonedDateTime.now().minusMinutes(5))
                .parameters(new HashMap<>(Map.of(
                        "staffId", "legacy-staff",
                        "staffName", "Архивный оператор",
                        "workProfileId", "legacy-profile")))
                .build();

        Visit visit = Visit.builder()
                .id("visit-2")
                .branchId(branch.getId())
                .currentService(currentService)
                .unservedServices(new ArrayList<>())
                .servedServices(new ArrayList<>())
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>(List.of(previousEvent)))
                .build();
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Visit result = service.visitEnd(branch.getId(), servicePoint.getId(), Boolean.TRUE, "Обслуживание завершено");

        assertSame(visit, result);
        assertEquals(1, visit.getServedServices().size());
        assertSame(currentService, visit.getServedServices().get(0));
        assertNull(visit.getCurrentService());
        assertNull(visit.getQueueId());
        assertNotNull(visit.getServedDateTime());
        assertNull(visit.getReturnDateTime());
        assertNull(visit.getServicePointId());

        Map<String, String> stopServingParameters = new HashMap<>(VisitEvent.STOP_SERVING.getParameters());
        assertEquals("true", stopServingParameters.get("isForced"));
        assertEquals("Обслуживание завершено", stopServingParameters.get("reason"));
        assertEquals(servicePoint.getId(), stopServingParameters.get("servicePointId"));
        assertEquals(servicePoint.getName(), stopServingParameters.get("servicePointName"));
        assertEquals(branch.getId(), stopServingParameters.get("branchId"));
        assertEquals("legacy-staff", stopServingParameters.get("staffId"));
        assertEquals("Архивный оператор", stopServingParameters.get("staffName"));
        assertEquals("legacy-profile", stopServingParameters.get("workProfileId"));

        assertTrue(VisitEvent.BACK_TO_QUEUE.getParameters().isEmpty());
        assertTrue(VisitEvent.END.getParameters().isEmpty());

        verify(branchService).getBranch(branch.getId());
        verify(branchService)
                .updateVisit(same(visit), same(VisitEvent.STOP_SERVING), same(service), eq(Boolean.TRUE));
        verify(branchService)
                .updateVisit(same(visit), same(VisitEvent.END), same(service), eq(Boolean.TRUE));
        verifyNoMoreInteractions(branchService);
        verifyNoInteractions(eventService);
    }

    @DisplayName("Visit End Fails When Service Point Missing")
    @Test
    void visitEndFailsWhenServicePointMissing() {
        Branch branch = new Branch("branch-3", "Отделение №3");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.visitEnd(branch.getId(), "missing", Boolean.FALSE, "Ошибка"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Service point not found in branch configuration", exception.getMessage());

        verify(branchService).getBranch(branch.getId());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never())
                .updateVisit(any(Visit.class), any(VisitEvent.class), any(VisitService.class), anyBoolean());
    }

    @DisplayName("Visit End Fails When Service Point Has No Visit")
    @Test
    void visitEndFailsWhenServicePointHasNoVisit() {
        Branch branch = new Branch("branch-4", "Отделение №4");
        ServicePoint servicePoint = new ServicePoint("sp-4", "Окно 7");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.visitEnd(branch.getId(), servicePoint.getId(), Boolean.TRUE, "Ошибка"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals(
                String.format("Visit not found in service point %s", servicePoint.getId()),
                exception.getMessage());

        verify(branchService).getBranch(branch.getId());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never())
                .updateVisit(any(Visit.class), any(VisitEvent.class), any(VisitService.class), anyBoolean());
    }
}
