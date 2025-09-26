package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.DeliveredService;
import ru.aritmos.model.Outcome;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

/**
 * Юнит-тесты для операций с фактическими услугами в {@link VisitService}.
 */
class VisitServiceDeliveredServicesTest {

    @DisplayName("Получение фактических услуг возвращает записи текущей услуги визита")
    @Test
    void returnsDeliveredServicesOfCurrentService() {
        VisitService service = new VisitService();
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");

        Service current = new Service("s1", "Service", 10, "q1");
        DeliveredService delivered = new DeliveredService("ds1", "Delivered");
        current.getDeliveredServices().put(delivered.getId(), delivered);

        Visit visit = Visit.builder()
                .id("v1")
                .currentService(current)
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Map<String, DeliveredService> result = service.getDeliveredServices("b1", "sp1");
        assertEquals(1, result.size());
        assertSame(delivered, result.get("ds1"));
    }

    @DisplayName("Получение фактических услуг выбрасывает исключение при отсутствии текущей услуги")
    @Test
    void throwsWhenCurrentServiceMissing() {
        VisitService service = new VisitService();
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setVisit(Visit.builder().id("v1").build());
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.getDeliveredServices("b1", "sp1"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @DisplayName("Добавление фактической услуги прикрепляет запись к текущей услуге визита")
    @Test
    void addDeliveredServiceAddsToCurrentService() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");

        Service current = new Service("s1", "Service", 10, "q1");
        Visit visit = Visit.builder()
                .id("v1")
                .currentService(current)
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        DeliveredService delivered = new DeliveredService("ds1", "Delivered");
        delivered.getServiceIds().add("s1");
        branch.getPossibleDeliveredServices().put(delivered.getId(), delivered);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.addDeliveredService("b1", "sp1", "ds1");
        assertEquals(1, result.getCurrentService().getDeliveredServices().size());
        assertTrue(result.getCurrentService().getDeliveredServices().containsKey("ds1"));
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }

    @DisplayName("Удаление фактической услуги удаляет запись из текущей услуги визита")
    @Test
    void deleteDeliveredServiceRemovesFromCurrentService() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");

        Service current = new Service("s1", "Service", 10, "q1");
        DeliveredService delivered = new DeliveredService("ds1", "Delivered");
        delivered.getServiceIds().add("s1");
        current.getDeliveredServices().put(delivered.getId(), delivered);
        Visit visit = Visit.builder()
                .id("v1")
                .currentService(current)
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);
        branch.getPossibleDeliveredServices().put(delivered.getId(), delivered);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.deleteDeliveredService("b1", "sp1", "ds1");
        assertTrue(result.getCurrentService().getDeliveredServices().isEmpty());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }


    @DisplayName("Установка исхода фактической услуги фиксирует результат обслуживания")
    @Test
    void addOutcomeOfDeliveredServiceSetsOutcome() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");

        Service current = new Service("s1", "Service", 10, "q1");
        DeliveredService delivered = new DeliveredService("ds1", "Delivered");
        Outcome outcome = new Outcome("o1", "Done");
        delivered.getPossibleOutcomes().put(outcome.getId(), outcome);
        current.getDeliveredServices().put(delivered.getId(), delivered);

        Visit visit = Visit.builder()
                .id("v1")
                .currentService(current)
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.addOutcomeOfDeliveredService("b1", "sp1", "ds1", "o1");
        assertEquals("o1", result.getCurrentService().getDeliveredServices().get("ds1").getOutcome().getId());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }

    @DisplayName("Установка исхода фактической услуги выбрасывает исключение при отсутствии записи")
    @Test
    void addOutcomeOfDeliveredServiceThrowsWhenMissingDeliveredService() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        Service current = new Service("s1", "Service", 10, "q1");
        Visit visit = Visit.builder()
                .id("v1")
                .currentService(current)
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        assertThrows(HttpStatusException.class,
                () -> service.addOutcomeOfDeliveredService("b1", "sp1", "missing", "o1"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @DisplayName("Удаление исхода фактической услуги очищает результат обслуживания")
    @Test
    void deleteOutcomeDeliveredServiceClearsOutcome() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");

        Service current = new Service("s1", "Service", 10, "q1");
        DeliveredService delivered = new DeliveredService("ds1", "Delivered");
        Outcome outcome = new Outcome("o1", "Done");
        delivered.getPossibleOutcomes().put(outcome.getId(), outcome);
        delivered.setOutcome(outcome);
        current.getDeliveredServices().put(delivered.getId(), delivered);

        Visit visit = Visit.builder()
                .id("v1")
                .currentService(current)
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.deleteOutcomeDeliveredService("b1", "sp1", "ds1");
        assertNull(result.getCurrentService().getDeliveredServices().get("ds1").getOutcome());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }
}

