package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.EntryPoint;
import ru.aritmos.model.Entity;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.WorkProfile;
import ru.aritmos.model.Reception;
import ru.aritmos.model.tiny.TinyClass;

class VisitServiceTest {

    @Test
    void getVisitReturnsExistingVisit() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 1);
        Visit visit = Visit.builder().id("v1").branchId("b1").queueId("q1").build();
        queue.getVisits().add(visit);
        branch.getQueues().put(queue.getId(), queue);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.getVisit("b1", "v1");
        assertSame(visit, result);
    }

    @Test
    void getVisitThrowsWhenMissing() {
        Branch branch = new Branch("b1", "Branch");
        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        EventService eventService = mock(EventService.class);
        service.branchService = branchService;
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.getVisit("b1", "missing"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @Test
    void getStringServicePointHashMapFiltersBusyPoints() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint free = new ServicePoint("sp1", "SP1");
        ServicePoint busy = new ServicePoint("sp2", "SP2");
        busy.setUser(new User("u1", "User", null));
        branch.getServicePoints().put(free.getId(), free);
        branch.getServicePoints().put(busy.getId(), busy);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        HashMap<String, ServicePoint> result = service.getStringServicePointHashMap("b1");
        assertEquals(1, result.size());
        assertTrue(result.containsKey("sp1"));
        assertFalse(result.containsKey("sp2"));
    }

    @Test
    void getServicePointHashMapReturnsAllPoints() {
        Branch branch = new Branch("b1", "Branch");
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP1"));
        branch.getServicePoints().put("sp2", new ServicePoint("sp2", "SP2"));

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        HashMap<String, ServicePoint> result = service.getServicePointHashMap("b1");
        assertEquals(2, result.size());
        assertTrue(result.containsKey("sp1"));
        assertTrue(result.containsKey("sp2"));
    }

    @Test
    void getWorkProfilesReturnsTinyClasses() {
        Branch branch = new Branch("b1", "Branch");
        branch.getWorkProfiles().put("wp1", new WorkProfile("wp1", "Profile"));

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<TinyClass> profiles = service.getWorkProfiles("b1");
        assertEquals(1, profiles.size());
        assertEquals(new TinyClass("wp1", "Profile"), profiles.get(0));
    }

    @Test
    void getUsersReturnsBranchUsers() {
        Branch branch = new Branch("b1", "Branch");
        branch.getUsers().put("u1", new User("u1", "User", null));

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<User> users = service.getUsers("b1");
        assertEquals(1, users.size());
        assertEquals("u1", users.get(0).getId());
    }

    @Test
    void getPrintersCollectsUniquePrinters() {
        Branch branch = new Branch("b1", "Branch");
        EntryPoint ep = new EntryPoint();
        ep.setId("e1");
        ep.setName("EP");
        ep.setPrinter(new Entity("p1", "P1"));
        branch.getEntryPoints().put("e1", ep);
        Reception reception = new Reception();
        reception.setBranchId("b1");
        reception.setPrinters(new java.util.ArrayList<>(List.of(new Entity("p1", "P1"), new Entity("p2", "P2"))));
        branch.setReception(reception);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<Entity> printers = service.getPrinters("b1");
        assertEquals(2, printers.size());
        assertTrue(printers.contains(new Entity("p1", "P1")));
        assertTrue(printers.contains(new Entity("p2", "P2")));
    }

    @Test
    void getQueusReturnsEntityList() {
        Branch branch = new Branch("b1", "Branch");
        branch.getQueues().put("q1", new Queue("q1", "Q1", "A", 1));

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<Entity> queues = service.getQueus("b1");
        assertEquals(List.of(new Entity("q1", "Q1")), queues);
    }

    @Test
    void getFullQueusReturnsQueues() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q1", "A", 1);
        branch.getQueues().put("q1", queue);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<Queue> queues = service.getFullQueus("b1");
        assertEquals(List.of(queue), queues);
    }

    @Test
    void getAllWorkingUsersAggregatesFromServicePoints() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        User user = new User("u1", "User", null);
        sp.setUser(user);
        branch.getServicePoints().put("sp1", sp);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        HashMap<String, User> users = service.getAllWorkingUsers("b1");
        assertEquals(1, users.size());
        assertSame(user, users.get("u1"));
    }
}
