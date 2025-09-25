package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Outcome;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

/**
 * Юнит-тесты для операций с итогами услуг в {@link VisitService}.
 */
class VisitServiceOutcomeTest {


    @DisplayName("Назначение исхода услуги фиксирует выбранный результат")
    @Test
    void addOutcomeServiceSetsOutcome() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");

        Service current = new Service("s1", "Service", 10, "q1");
        Outcome outcome = new Outcome("o1", "Done");
        current.getPossibleOutcomes().put(outcome.getId(), outcome);

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

        Visit result = service.addOutcomeService("b1", "sp1", "o1");
        assertEquals("o1", result.getCurrentService().getOutcome().getId());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }


    @DisplayName("Назначение исхода выбрасывает исключение при отсутствии подходящего результата")
    @Test
    void addOutcomeServiceThrowsWhenOutcomeMissing() {
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

        assertThrows(HttpStatusException.class, () -> service.addOutcomeService("b1", "sp1", "o1"));
        verify(eventService).send(eq("*"), eq(false), any());
    }


    @DisplayName("Удаление исхода очищает результат услуги")

    @Test
    void deleteOutcomeServiceClearsOutcome() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");

        Service current = new Service("s1", "Service", 10, "q1");
        Outcome outcome = new Outcome("o1", "Done");
        current.setOutcome(outcome);

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

        Visit result = service.deleteOutcomeService("b1", "sp1", "s1");
        assertNull(result.getCurrentService().getOutcome());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }
}

