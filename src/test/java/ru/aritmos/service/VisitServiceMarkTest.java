package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Mark;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

/**
 * Юнит-тесты для добавления и удаления пометок визита.
 */
class VisitServiceMarkTest {

    @Test
    void addMarkAppendsWithAuthor() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        User user = new User("u1", "User", null);
        sp.setUser(user);
        Service current = new Service("s1", "Service", 10, "q1");
        Visit visit = Visit.builder()
                .id("v1")
                .currentService(current)
                .visitMarks(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Mark mark = Mark.builder().id("m1").value("note").build();

        Visit result = service.addMark("b1", "sp1", mark);
        assertEquals(1, result.getVisitMarks().size());
        Mark stored = result.getVisitMarks().get(0);
        assertEquals("m1", stored.getId());
        assertEquals("u1", stored.getAuthor().getId());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }

    @Test
    void deleteMarkRemovesFromVisit() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        Service current = new Service("s1", "Service", 10, "q1");
        Mark mark = Mark.builder()
                .id("m1")
                .value("note")
                .markDate(ZonedDateTime.now())
                .author(new User("u1", "User", null))
                .build();
        Visit visit = Visit.builder()
                .id("v1")
                .currentService(current)
                .visitMarks(new ArrayList<>(java.util.List.of(mark)))
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.deleteMark("b1", "sp1", mark);
        assertTrue(result.getVisitMarks().isEmpty());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }

    @Test
    void addMarkByIdThrowsWhenMissing() {
        Branch branch = new Branch("b1", "Branch");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.addMark("b1", "sp1", "m1"));
        verify(eventService).send(eq("*"), eq(false), any());
    }
}
