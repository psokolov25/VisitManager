package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.WorkProfile;
import ru.aritmos.model.visit.Visit;

class VisitServiceGetQueuesTest {

    @DisplayName("проверяется сценарий «returns queues for service point»")
    @Test
    void returnsQueuesForServicePoint() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q1", "A", 30);
        Visit waiting = Visit.builder().id("v1").status("WAITING").build();
        Visit served = Visit.builder().id("v2").status("SERVED").build();
        queue.getVisits().addAll(List.of(waiting, served));
        branch.getQueues().put(queue.getId(), queue);

        WorkProfile wp = new WorkProfile("wp1", "WP");
        wp.getQueueIds().add("q1");
        branch.getWorkProfiles().put(wp.getId(), wp);

        User user = new User("u1", "User", null);
        user.setCurrentWorkProfileId("wp1");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setUser(user);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Optional<List<Queue>> result = service.getQueues("b1", "sp1");
        assertTrue(result.isPresent());
        List<Queue> queues = result.get();
        assertEquals(1, queues.size());
        assertEquals("q1", queues.get(0).getId());
        assertEquals(1, queues.get(0).getVisits().size());
        assertEquals("v1", queues.get(0).getVisits().get(0).getId());
    }

    @DisplayName("проверяется сценарий «throws when user not logged in»")
    @Test
    void throwsWhenUserNotLoggedIn() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.getQueues("b1", "sp1"));
        verify(eventService).send(eq("*"), eq(false), any());
    }
}
