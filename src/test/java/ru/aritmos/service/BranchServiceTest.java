package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.WorkProfile;

class BranchServiceTest {

    @Test
    void getBranchReturnsExistingBranch() {
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        service.branches.put(branch.getId(), branch);

        Branch result = service.getBranch("b1");
        assertSame(branch, result);
    }

    @Test
    void getBranchThrowsWhenMissing() {
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);

        assertThrows(HttpStatusException.class, () -> service.getBranch("missing"));
        verify(eventService).send(anyString(), eq(false), any());
    }

    @Test
    void branchExistsChecksPresence() {
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        service.branches.put(branch.getId(), branch);

        assertTrue(service.branchExists("b1"));
        assertFalse(service.branchExists("b2"));
        verify(eventService).send(anyString(), eq(false), any());
    }

    @Test
    void getBranchesReturnsCopyWithoutDetails() {
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        branch.setPrefix("PR");
        branch.getQueues().put("q1", new Queue("q1", "Queue", "A", 1));
        service.branches.put(branch.getId(), branch);

        HashMap<String, Branch> result = service.getBranches();
        assertEquals(1, result.size());
        Branch copy = result.get("b1");
        assertNotSame(branch, copy);
        assertEquals("PR", copy.getPrefix());
        assertTrue(copy.getQueues().isEmpty());
    }

    @Test
    void getDetailedBranchesReturnsOriginalReferences() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        service.branches.put(branch.getId(), branch);

        HashMap<String, Branch> result = service.getDetailedBranches();
        assertSame(branch, result.get("b1"));
        verify(service).getBranch("b1");
    }

    @Test
    void addStoresBranchAndSetsIds() {
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        branch.getQueues().put("q1", new Queue("q1", "Queue", "A", 1));
        branch.getServices().put("s1", new Service("s1", "Service", 1, "q1"));

        service.add("b1", branch);

        assertSame(branch, service.branches.get("b1"));
        assertEquals("b1", branch.getQueues().get("q1").getBranchId());
        assertEquals("b1", branch.getServices().get("s1").getBranchId());
        verify(eventService)
            .sendChangedEvent(eq("config"), eq(true), isNull(), eq(branch), anyMap(), eq("BRANCH_CREATED"));
    }

    @Test
    void changeUserWorkProfileInServicePointUpdatesUser() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        branch.getWorkProfiles().put("wp1", new WorkProfile("wp1", "WP1"));
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        User user = new User("u1", "User", null);
        user.setCurrentWorkProfileId("old");
        sp.setUser(user);
        branch.getServicePoints().put("sp1", sp);
        service.branches.put("b1", branch);

        User result = service.changeUserWorkProfileInServicePoint("b1", "sp1", "wp1");

        assertEquals("wp1", result.getCurrentWorkProfileId());
        verify(service).add("b1", branch);
    }

    @Test
    void closeServicePointDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        VisitService visitService = mock(VisitService.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP1"));
        doNothing()
            .when(branch)
            .closeServicePoint(anyString(), any(), any(), anyBoolean(), anyBoolean(), any(), anyBoolean(), anyString());
        service.branches.put("b1", branch);

        service.closeServicePoint("b1", "sp1", visitService, false, false, "", false, "");

        verify(branch)
            .closeServicePoint(eq("sp1"), eq(service.eventService), eq(visitService), eq(false),
                eq(false), eq(""), eq(false), eq(""));
        verify(service).add("b1", branch);
    }
}

