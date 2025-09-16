package ru.aritmos.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Entity;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Outcome;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.tiny.TinyServicePoint;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

class ServicePointControllerTest {

    private ServicePointController controller() {
        ServicePointController controller = new ServicePointController();
        controller.services = mock(Services.class);
        controller.branchService = mock(BranchService.class);
        controller.visitService = mock(VisitService.class);
        controller.eventService = mock(EventService.class);
        controller.keyCloackClient = mock(KeyCloackClient.class);
        return controller;
    }

    @Test
    void getFreeServicePointsDelegatesToVisitService() {
        ServicePointController controller = controller();
        HashMap<String, ServicePoint> points = new HashMap<>();
        when(controller.visitService.getStringServicePointHashMap("b1")).thenReturn(points);

        assertSame(points, controller.getFreeServicePoints("b1"));
        verify(controller.visitService).getStringServicePointHashMap("b1");
    }

    @Test
    void getUsersOfBranchUsesBranchService() {
        ServicePointController controller = controller();
        HashMap<String, User> users = new HashMap<>();
        users.put("u1", new User("u1", "u1", null));
        when(controller.branchService.getUsers("b1")).thenReturn(users);

        List<User> result = controller.getUsersOfBranch("b1");
        assertEquals(1, result.size());
        verify(controller.branchService).getUsers("b1");
    }

    @Test
    void changeUserWorkprofileDelegates() {
        ServicePointController controller = controller();
        User user = new User("id", "u1", null);
        when(controller.branchService.changeUserWorkProfileInServicePoint("b1", "sp1", "wp1")).thenReturn(user);

        assertSame(user, controller.changeUserWorkprofile("b1", "sp1", "wp1"));
        verify(controller.branchService).changeUserWorkProfileInServicePoint("b1", "sp1", "wp1");
    }

    @Test
    void openServicePointDelegates() throws IOException {
        ServicePointController controller = controller();
        User user = new User("id", "u1", null);
        when(controller.branchService.openServicePoint("b1", "u1", "sp1", "wp1", controller.visitService)).thenReturn(user);

        assertSame(user, controller.openServicePoint("b1", "u1", "sp1", "wp1"));
        verify(controller.branchService).openServicePoint("b1", "u1", "sp1", "wp1", controller.visitService);
    }

    @Test
    void closeServicePointDelegates() {
        ServicePointController controller = controller();

        controller.closeServicePoint("b1", "sp1", false, null, false, "");
        verify(controller.branchService)
            .closeServicePoint(eq("b1"), eq("sp1"), eq(controller.visitService), eq(false),
                eq(false), isNull(), eq(false), eq(""));
    }

    @Test
    void getPrintersDelegates() {
        ServicePointController controller = controller();
        List<Entity> printers = List.of(new Entity("p1", "Printer"));
        when(controller.visitService.getPrinters("b1")).thenReturn(printers);
        assertEquals(printers, controller.getPrinters("b1"));
    }

    @Test
    void getQueuesDelegates() {
        ServicePointController controller = controller();
        List<Entity> queues = List.of(new Entity("q1", "Queue"));
        when(controller.visitService.getQueus("b1")).thenReturn(queues);
        assertEquals(queues, controller.getQueues("b1"));
    }

    @Test
    void getFullQueuesDelegates() {
        ServicePointController controller = controller();
        List<Queue> queues = List.of(new Queue("q1", "Queue", "Q", 1));
        when(controller.visitService.getFullQueus("b1")).thenReturn(queues);
        assertEquals(queues, controller.getFullQueues("b1"));
    }

    @Test
    void getServicePointsMapsToTiny() {
        ServicePointController controller = controller();
        ServicePoint sp = new ServicePoint("sp1", "SP");
        sp.setUser(null);
        when(controller.visitService.getServicePointHashMap("b1"))
            .thenReturn(new HashMap<>(Map.of("sp1", sp)));
        List<TinyServicePoint> result = controller.getServicePoints("b1");
        assertEquals(1, result.size());
        assertEquals("sp1", result.get(0).getId());
    }

    @Test
    void getDetailedServicePointsDelegates() {
        ServicePointController controller = controller();
        ServicePoint sp = new ServicePoint("sp1", "SP");
        when(controller.visitService.getServicePointHashMap("b1"))
            .thenReturn(new HashMap<>(Map.of("sp1", sp)));
        assertEquals(List.of(sp), controller.getDetailedServicePoints("b1"));
    }

    @Test
    void getServicePointsByUserNameReturnsPoint() {
        ServicePointController controller = controller();
        ServicePoint sp = new ServicePoint("sp1", "SP");
        when(controller.visitService.getServicePointHashMap(anyString()))
            .thenReturn(new HashMap<>(Map.of("sp1", sp)));
        User user = new User("id", "u1", null);
        user.setLastBreakStartTime(java.time.ZonedDateTime.now());
        user.setLastServicePointId("sp1");
        when(controller.visitService.getUsers(anyString())).thenReturn(List.of(user));
        Optional<ServicePoint> result = controller.getServicePointsByUserName("b1", "u1");
        assertTrue(result.isPresent());
        assertEquals(sp, result.get());
    }

    @Test
    void getAllWorkingUsersDelegates() {
        ServicePointController controller = controller();
        HashMap<String, User> users = new HashMap<>(Map.of("u1", new User("id", "u1", null)));
        when(controller.visitService.getAllWorkingUsers("b1")).thenReturn(users);
        List<User> result = controller.getAllWorkingUsersOfBranch("b1");
        assertEquals(1, result.size());
        verify(controller.visitService).getAllWorkingUsers("b1");
    }

    @Test
    void globalGetServicePointByUserNameDelegates() {
        ServicePointController controller = controller();
        User user = new User("id", "u1", null);
        ServicePoint sp = new ServicePoint("sp1", "SP");
        sp.setUser(user);
        BranchService branchService = controller.branchService;
        Branch branch = new Branch("b1", "B");
        branch.getServicePoints().put("sp1", sp);
        when(branchService.getDetailedBranches())
            .thenReturn(new HashMap<>(Map.of("b1", branch)));
        Optional<ServicePoint> result = controller.getServicePointsByUserName("u1");
        assertTrue(result.isPresent());
    }

    @Test
    void getUserByUserNameReturnsUser() {
        ServicePointController controller = controller();
        User user = new User("id", "u1", null);
        ServicePoint sp = new ServicePoint("sp1", "SP");
        sp.setUser(user);
        when(controller.visitService.getServicePointHashMap("b1"))
            .thenReturn(new HashMap<>(Map.of("sp1", sp)));
        Optional<User> result = controller.getUserByUserName("b1", "u1");
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void getWorkProfilesDelegates() {
        ServicePointController controller = controller();
        List<TinyClass> profiles = List.of(new TinyClass("wp1", "Profile"));
        when(controller.visitService.getWorkProfiles("b1")).thenReturn(profiles);
        assertEquals(profiles, controller.getWorkProfiles("b1"));
    }

    @Test
    void logoutUserDelegates() {
        ServicePointController controller = controller();
        controller.logoutUser("b1", "sp1", false, null, false, "");
        verify(controller.branchService)
            .closeServicePoint(eq("b1"), eq("sp1"), eq(controller.visitService), eq(true),
                eq(false), isNull(), eq(false), eq(""));
    }

    @Test
    void getVisitsWithLimitDelegates() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1", 5L))
            .thenReturn(List.of(Visit.builder().build()));
        assertEquals(1, controller.getVisits("b1", "q1", 5L).size());
    }

    @Test
    void getVisitsDelegates() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1"))
            .thenReturn(List.of(Visit.builder().build()));
        assertEquals(1, controller.getVisits("b1", "q1").size());
    }

    @Test
    void getAllVisitsDelegates() {
        ServicePointController controller = controller();
        HashMap<String, Visit> visits = new HashMap<>(Map.of("v1", Visit.builder().build()));
        when(controller.visitService.getAllVisits("b1")).thenReturn(visits);
        assertEquals(visits, controller.getAllVisits("b1"));
    }

    @Test
    void getVisitDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        assertEquals(visit, controller.getVisit("b1", "v1"));
    }

    @Test
    void getVisitsByStatusesDelegates() {
        ServicePointController controller = controller();
        HashMap<String, Visit> visits = new HashMap<>();
        when(controller.visitService.getVisitsByStatuses("b1", List.of("NEW")))
            .thenReturn(visits);
        assertEquals(visits, controller.getVisitsByStatuses("b1", List.of("NEW")));
    }

    @Test
    void visitCallWithMaxWaitingDelegates() {
        ServicePointController controller = controller();
        Optional<Visit> visit = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallWithMaximalWaitingTime("b1", "sp1"))
            .thenReturn(visit);
        assertEquals(visit, controller.visitCallWithMaximalWaitingTime("b1", "sp1"));
    }

    @Test
    void getVisitFromQueueReturnsMatch() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisits("b1", "q1")).thenReturn(List.of(visit));

        Visit result = controller.getVisit("b1", "q1", "v1");

        assertSame(visit, result);
        verify(controller.visitService).getVisits("b1", "q1");
    }

    @Test
    void getVisitFromQueueThrowsWhenMissing() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1")).thenReturn(List.of());

        HttpStatusException exception =
            assertThrows(HttpStatusException.class, () -> controller.getVisit("b1", "q1", "v1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void callVisitDelegatesToService() {
        ServicePointController controller = controller();
        Optional<Visit> visit = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCall("b1", "sp1", "v1")).thenReturn(visit);

        assertEquals(visit, controller.callVisit("b1", "sp1", "v1"));
    }

    @Test
    void visitCallForConfirmUsesServiceResult() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        Optional<Visit> expected = Optional.of(visit);
        when(controller.visitService.visitCallForConfirmWithMaxWaitingTime("b1", "sp1", visit))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCallForConfirm("b1", "sp1", visit));
    }

    @Test
    void visitCallForConfirmByIdLoadsVisitFirst() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        Optional<Visit> expected = Optional.of(visit);
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        when(controller.visitService.visitCallForConfirmWithMaxWaitingTime("b1", "sp1", visit))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCallForConfirmByVisitId("b1", "sp1", "v1"));
        verify(controller.visitService).getVisit("b1", "v1");
    }

    @Test
    void visitCallForConfirmMaxWaitingDelegates() {
        ServicePointController controller = controller();
        Optional<Visit> expected = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallForConfirmWithMaxWaitingTime("b1", "sp1"))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCallForConfirmMaxWaitingTime("b1", "sp1"));
    }

    @Test
    void visitCallFromQueuesDelegates() {
        ServicePointController controller = controller();
        List<String> queueIds = List.of("q1", "q2");
        Optional<Visit> expected = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallWithMaximalWaitingTime("b1", "sp1", queueIds))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCall("b1", "sp1", queueIds));
    }

    @Test
    void visitCallFromQueuesWithConfirmationDelegates() {
        ServicePointController controller = controller();
        List<String> queueIds = List.of("q1");
        Optional<Visit> expected = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallForConfirmWithMaxWaitingTime("b1", "sp1", queueIds))
            .thenReturn(expected);

        assertEquals(
            expected, controller.visitCallForConfirmMaxWaitingTime("b1", "sp1", queueIds));
    }

    @Test
    void visitNoShowDelegatesToService() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        Optional<Visit> expected = Optional.of(visit);
        when(controller.visitService.visitNoShow("b1", "sp1", visit)).thenReturn(expected);

        assertEquals(expected, controller.visitNoShow("b1", "sp1", visit));
    }

    @Test
    void visitCallNoShowRetrievesVisit() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        Optional<Visit> expected = Optional.of(visit);
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        when(controller.visitService.visitNoShow("b1", "sp1", visit)).thenReturn(expected);

        assertEquals(expected, controller.visitCallNoShow("b1", "sp1", "v1"));
        verify(controller.visitService).getVisit("b1", "v1");
    }

    @Test
    void visitReCallForConfirmDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.visitReCallForConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitReCallForConfirm("b1", "sp1", visit));
    }

    @Test
    void visitReCallForConfirmByIdLoadsVisit() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        when(controller.visitService.visitReCallForConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitReCallForConfirm("b1", "sp1", "v1"));
        verify(controller.visitService).getVisit("b1", "v1");
    }

    @Test
    void visitConfirmDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.visitConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitConfirm("b1", "sp1", visit));
    }

    @Test
    void visitConfirmByIdDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        when(controller.visitService.visitConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitConfirm("b1", "sp1", "v1"));
        verify(controller.visitService).getVisit("b1", "v1");
    }

    @Test
    void cancelAutoCallDelegates() {
        ServicePointController controller = controller();
        Optional<ServicePoint> expected = Optional.of(new ServicePoint("sp1", "SP"));
        when(controller.visitService.cancelAutoCallModeOfServicePoint("b1", "sp1"))
            .thenReturn(expected);

        assertEquals(expected, controller.cancelAutoCallModeOfServicePoint("b1", "sp1"));
    }

    @Test
    void startAutoCallEnablesBranchAndPoint() {
        ServicePointController controller = controller();
        Optional<ServicePoint> expected = Optional.of(new ServicePoint("sp1", "SP"));
        when(controller.visitService.startAutoCallModeOfServicePoint("b1", "sp1"))
            .thenReturn(expected);

        assertEquals(expected, controller.startAutoCallModeOfServicePoint("b1", "sp1"));
        verify(controller.visitService).setAutoCallModeOfBranch("b1", true);
        verify(controller.visitService).startAutoCallModeOfServicePoint("b1", "sp1");
    }

    @Test
    void getOutcomesReturnsConfiguredMap() {
        ServicePointController controller = controller();
        Branch branch = new Branch("b1", "Отделение");
        Service service = new Service("s1", "Услуга", 1, null);
        HashMap<String, Outcome> outcomes = new HashMap<>();
        Outcome outcome = new Outcome("o1", "Итог");
        outcomes.put("o1", outcome);
        service.setPossibleOutcomes(outcomes);
        branch.getServices().put("s1", service);
        when(controller.branchService.getBranch("b1")).thenReturn(branch);

        HashMap<String, Outcome> result = controller.getOutcomes("b1", "s1");

        assertSame(outcomes, result);
        verify(controller.branchService).getBranch("b1");
    }

    @Test
    void getOutcomesThrowsWhenServiceMissing() {
        ServicePointController controller = controller();
        Branch branch = new Branch("b1", "Отделение");
        when(controller.branchService.getBranch("b1")).thenReturn(branch);

        HttpStatusException exception =
            assertThrows(HttpStatusException.class, () -> controller.getOutcomes("b1", "absent"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(controller.eventService).send(eq("*"), eq(false), any());
    }

    @Test
    void addServicesInvokesVisitServiceSequentially() {
        ServicePointController controller = controller();
        Visit first = Visit.builder().id("v1").build();
        Visit second = Visit.builder().id("v2").build();
        when(controller.visitService.addService("b1", "sp1", "s1")).thenReturn(first);
        when(controller.visitService.addService("b1", "sp1", "s2")).thenReturn(second);

        Visit result = controller.addServices("b1", "sp1", List.of("s1", "s2"));

        assertSame(second, result);
        verify(controller.visitService).addService("b1", "sp1", "s1");
        verify(controller.visitService).addService("b1", "sp1", "s2");
    }

    @Test
    void deleteVisitRemovesVisitWhenFound() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        HashMap<String, Visit> visits = new HashMap<>(Map.of("v1", visit));
        when(controller.visitService.getAllVisits("b1")).thenReturn(visits);

        controller.deleteVisit("b1", "v1");

        verify(controller.visitService).deleteVisit(visit);
    }

    @Test
    void deleteVisitThrowsWhenMissing() {
        ServicePointController controller = controller();
        when(controller.visitService.getAllVisits("b1")).thenReturn(new HashMap<>());

        HttpStatusException exception =
            assertThrows(HttpStatusException.class, () -> controller.deleteVisit("b1", "unknown"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(controller.eventService).send(eq("*"), eq(false), any());
    }
}
