package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.VisitParameters;
import ru.aritmos.model.visit.Visit;

/**
 * Юнит-тесты для {@link VisitService#createVirtualVisit(String, String, VisitParameters, String)}.
 */
class VisitServiceCreateVirtualVisitTest {

    /** Проверяет, что метод выбрасывает конфликт при недавнем визите на точке обслуживания. */
    @DisplayName("Выбрасывает конфликт, если точка обслуживания недавно создала визит")
    @Test
    void throwsConflictWhenServicePointRecentlyCreatedVisit() throws SystemException {
        VisitService service = spy(new VisitService());
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        Branch branch = new Branch("b1", "Отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Точка");
        servicePoint.setVisit(Visit.builder().createDateTime(ZonedDateTime.now().minusSeconds(1)).build());
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getServices().put("svc1", new Service("svc1", "Услуга", 60, "q1"));

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        service.branchService = branchService;

        VisitParameters parameters = VisitParameters.builder()
            .serviceIds(new ArrayList<>(List.of("svc1")))
            .build();

        assertThrows(
            HttpStatusException.class,
            () -> service.createVirtualVisit(branch.getId(), servicePoint.getId(), parameters, "sid"));

        verify(service, never())
            .createVirtualVisit2(anyString(), anyString(), any(ArrayList.class), any(HashMap.class), anyString());
    }

    /** Убеждаемся, что в createVirtualVisit2 передаются клоны услуг и исходные параметры. */
    @DisplayName("Передаёт клонированные услуги при создании виртуального визита")
    @Test
    void passesClonedServicesToCreateVirtualVisit2() throws SystemException {
        VisitService service = spy(new VisitService());
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        Branch branch = new Branch("b1", "Отделение");
        Service serviceModel = new Service("svc1", "Услуга", 60, "q1");
        branch.getServices().put(serviceModel.getId(), serviceModel);
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "Точка"));

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        service.branchService = branchService;

        HashMap<String, String> params = new HashMap<>();
        params.put("source", "workstation");
        VisitParameters parameters = VisitParameters.builder()
            .serviceIds(new ArrayList<>(List.of(serviceModel.getId())))
            .parameters(params)
            .build();

        Visit expected = Visit.builder().id("visit").build();
        doReturn(expected)
            .when(service)
            .createVirtualVisit2(
                eq(branch.getId()),
                eq("sp1"),
                any(ArrayList.class),
                any(HashMap.class),
                eq("sid"));

        Visit result = service.createVirtualVisit(branch.getId(), "sp1", parameters, "sid");

        assertSame(expected, result);

        ArgumentCaptor<ArrayList<Service>> servicesCaptor = ArgumentCaptor.forClass(ArrayList.class);
        ArgumentCaptor<HashMap<String, String>> paramsCaptor = ArgumentCaptor.forClass(HashMap.class);

        verify(service)
            .createVirtualVisit2(
                eq(branch.getId()),
                eq("sp1"),
                servicesCaptor.capture(),
                paramsCaptor.capture(),
                eq("sid"));

        ArrayList<Service> capturedServices = servicesCaptor.getValue();
        assertEquals(1, capturedServices.size());
        assertEquals(serviceModel.getId(), capturedServices.get(0).getId());
        assertNotSame(serviceModel, capturedServices.get(0));
        assertSame(parameters.getParameters(), paramsCaptor.getValue());
    }
}
