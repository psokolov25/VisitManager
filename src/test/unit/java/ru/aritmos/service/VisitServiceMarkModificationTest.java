package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
 * Юнит-тесты для методов {@link VisitService#addMark(String, String, Mark)} и
 * {@link VisitService#deleteMark(String, String, Mark)}.
 */
class VisitServiceMarkModificationTest {

    @DisplayName("Добавление отметки дополняет список отметок визита")
    @Test
    void addMarkAppendsToVisitMarks() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        User user = new User("u1", "User", null);
        sp.setUser(user);
        Service service = new Service("s1", "Service", 10, "q1");
        Visit visit = Visit.builder()
                .id("v1")
                .currentService(service)
                .visitMarks(new ArrayList<>())
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService serviceBean = new VisitService();
        serviceBean.branchService = branchService;
        serviceBean.eventService = mock(EventService.class);

        Mark mark = new Mark("m1", "text", null, null);
        Visit result = serviceBean.addMark("b1", "sp1", mark);
        assertEquals(1, result.getVisitMarks().size());
        Mark stored = result.getVisitMarks().get(0);
        assertEquals("m1", stored.getId());
        assertEquals("text", stored.getValue());
        assertEquals("u1", stored.getAuthor().getId());
        assertNotNull(stored.getMarkDate());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(serviceBean));
    }

    @DisplayName("Удаление отметки убирает её из визита")
    @Test
    void deleteMarkRemovesFromVisit() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        User user = new User("u1", "User", null);
        sp.setUser(user);
        Service service = new Service("s1", "Service", 10, "q1");
        Mark mark = Mark.builder().id("m1").value("note").markDate(ZonedDateTime.now()).author(user).build();
        Visit visit = Visit.builder()
                .id("v1")
                .currentService(service)
                .visitMarks(new ArrayList<>(List.of(mark)))
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService serviceBean = new VisitService();
        serviceBean.branchService = branchService;
        serviceBean.eventService = mock(EventService.class);

        Visit result = serviceBean.deleteMark("b1", "sp1", mark);
        assertTrue(result.getVisitMarks().isEmpty());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(serviceBean));
    }

    @DisplayName("Добавление отметки выбрасывает исключение при отсутствии текущей услуги")
    @Test
    void addMarkThrowsWhenCurrentServiceMissing() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        Visit visit = Visit.builder()
                .id("v1")
                .visitMarks(new ArrayList<>())
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        EventService eventService = mock(EventService.class);
        VisitService serviceBean = new VisitService();
        serviceBean.branchService = branchService;
        serviceBean.eventService = eventService;

        Mark mark = new Mark("m1", "text", null, null);
        assertThrows(HttpStatusException.class, () -> serviceBean.addMark("b1", "sp1", mark));
        verify(eventService).send(eq("*"), eq(false), any());
    }
}
