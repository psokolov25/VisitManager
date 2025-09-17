package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Mark;
import ru.aritmos.model.Queue;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;

/**
 * Юнит-тесты для {@link VisitService#getMarks(String, String)}.
 */
class VisitServiceGetMarksTest {

    @Test
    void returnsMarksOfVisit() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q1", "A", 30);
        Mark mark = Mark.builder()
                .id("m1")
                .value("note")
                .markDate(ZonedDateTime.now())
                .author(new User("u1", "User", null))
                .build();
        Visit visit = Visit.builder().id("v1").visitMarks(List.of(mark)).build();
        queue.getVisits().add(visit);
        branch.getQueues().put(queue.getId(), queue);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<Mark> marks = service.getMarks("b1", "v1");
        assertEquals(1, marks.size());
        assertSame(mark, marks.get(0));
    }

    @Test
    void throwsWhenVisitMissing() {
        Branch branch = new Branch("b1", "Branch");

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.getMarks("b1", "missing"));
        verify(eventService).send(eq("*"), eq(false), any());
    }
}

