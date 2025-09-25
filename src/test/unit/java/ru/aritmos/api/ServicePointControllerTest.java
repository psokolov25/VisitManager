package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

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
import ru.aritmos.model.DeliveredService;
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

    @DisplayName("Get Free Service Points Delegates To Visit Service")
    @Test
    void getFreeServicePointsDelegatesToVisitService() {
        ServicePointController controller = controller();
        HashMap<String, ServicePoint> points = new HashMap<>();
        when(controller.visitService.getStringServicePointHashMap("b1")).thenReturn(points);

        assertSame(points, controller.getFreeServicePoints("b1"));
        verify(controller.visitService).getStringServicePointHashMap("b1");
    }

    @DisplayName("Get Users Of Branch Uses Branch Service")
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

    @DisplayName("Change User Workprofile Delegates")
    @Test
    void changeUserWorkprofileDelegates() {
        ServicePointController controller = controller();
        User user = new User("id", "u1", null);
        when(controller.branchService.changeUserWorkProfileInServicePoint("b1", "sp1", "wp1")).thenReturn(user);

        assertSame(user, controller.changeUserWorkprofile("b1", "sp1", "wp1"));
        verify(controller.branchService).changeUserWorkProfileInServicePoint("b1", "sp1", "wp1");
    }

    @DisplayName("Open Service Point Delegates")
    @Test
    void openServicePointDelegates() throws IOException {
        ServicePointController controller = controller();
        User user = new User("id", "u1", null);
        when(controller.branchService.openServicePoint("b1", "u1", "sp1", "wp1", controller.visitService)).thenReturn(user);

        assertSame(user, controller.openServicePoint("b1", "u1", "sp1", "wp1"));
        verify(controller.branchService).openServicePoint("b1", "u1", "sp1", "wp1", controller.visitService);
    }

    @DisplayName("Close Service Point Delegates")
    @Test
    void closeServicePointDelegates() {
        ServicePointController controller = controller();

        controller.closeServicePoint("b1", "sp1", false, null, false, "");
        verify(controller.branchService)
            .closeServicePoint(eq("b1"), eq("sp1"), eq(controller.visitService), eq(false),
                eq(false), isNull(), eq(false), eq(""));
    }

    @DisplayName("Get Printers Delegates")
    @Test
    void getPrintersDelegates() {
        ServicePointController controller = controller();
        List<Entity> printers = List.of(new Entity("p1", "Printer"));
        when(controller.visitService.getPrinters("b1")).thenReturn(printers);
        assertEquals(printers, controller.getPrinters("b1"));
    }

    @DisplayName("Get Queues Delegates")
    @Test
    void getQueuesDelegates() {
        ServicePointController controller = controller();
        List<Entity> queues = List.of(new Entity("q1", "Queue"));
        when(controller.visitService.getQueus("b1")).thenReturn(queues);
        assertEquals(queues, controller.getQueues("b1"));
    }

    @DisplayName("Get Full Queues Delegates")
    @Test
    void getFullQueuesDelegates() {
        ServicePointController controller = controller();
        List<Queue> queues = List.of(new Queue("q1", "Queue", "Q", 1));
        when(controller.visitService.getFullQueus("b1")).thenReturn(queues);
        assertEquals(queues, controller.getFullQueues("b1"));
    }

    @DisplayName("Get Service Points Maps To Tiny")
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

    @DisplayName("Get Detailed Service Points Delegates")
    @Test
    void getDetailedServicePointsDelegates() {
        ServicePointController controller = controller();
        ServicePoint sp = new ServicePoint("sp1", "SP");
        when(controller.visitService.getServicePointHashMap("b1"))
            .thenReturn(new HashMap<>(Map.of("sp1", sp)));
        assertEquals(List.of(sp), controller.getDetailedServicePoints("b1"));
    }

    @DisplayName("Get Service Points By User Name Returns Point")
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

    @DisplayName("Get All Working Users Delegates")
    @Test
    void getAllWorkingUsersDelegates() {
        ServicePointController controller = controller();
        HashMap<String, User> users = new HashMap<>(Map.of("u1", new User("id", "u1", null)));
        when(controller.visitService.getAllWorkingUsers("b1")).thenReturn(users);
        List<User> result = controller.getAllWorkingUsersOfBranch("b1");
        assertEquals(1, result.size());
        verify(controller.visitService).getAllWorkingUsers("b1");
    }

    @DisplayName("Global Get Service Point By User Name Delegates")
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

    @DisplayName("Get User By User Name Returns User")
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

    @DisplayName("Get Work Profiles Delegates")
    @Test
    void getWorkProfilesDelegates() {
        ServicePointController controller = controller();
        List<TinyClass> profiles = List.of(new TinyClass("wp1", "Profile"));
        when(controller.visitService.getWorkProfiles("b1")).thenReturn(profiles);
        assertEquals(profiles, controller.getWorkProfiles("b1"));
    }

    @DisplayName("Logout User Delegates")
    @Test
    void logoutUserDelegates() {
        ServicePointController controller = controller();
        controller.logoutUser("b1", "sp1", false, null, false, "");
        verify(controller.branchService)
            .closeServicePoint(eq("b1"), eq("sp1"), eq(controller.visitService), eq(true),
                eq(false), isNull(), eq(false), eq(""));
    }

    @DisplayName("Get Visits With Limit Delegates")
    @Test
    void getVisitsWithLimitDelegates() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1", 5L))
            .thenReturn(List.of(Visit.builder().build()));
        assertEquals(1, controller.getVisits("b1", "q1", 5L).size());
    }

    @DisplayName("Get Visits Delegates")
    @Test
    void getVisitsDelegates() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1"))
            .thenReturn(List.of(Visit.builder().build()));
        assertEquals(1, controller.getVisits("b1", "q1").size());
    }

    @DisplayName("Get All Visits Delegates")
    @Test
    void getAllVisitsDelegates() {
        ServicePointController controller = controller();
        HashMap<String, Visit> visits = new HashMap<>(Map.of("v1", Visit.builder().build()));
        when(controller.visitService.getAllVisits("b1")).thenReturn(visits);
        assertEquals(visits, controller.getAllVisits("b1"));
    }

    @DisplayName("Get Visit Delegates")
    @Test
    void getVisitDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        assertEquals(visit, controller.getVisit("b1", "v1"));
    }

    @DisplayName("Get Visits By Statuses Delegates")
    @Test
    void getVisitsByStatusesDelegates() {
        ServicePointController controller = controller();
        HashMap<String, Visit> visits = new HashMap<>();
        when(controller.visitService.getVisitsByStatuses("b1", List.of("NEW")))
            .thenReturn(visits);
        assertEquals(visits, controller.getVisitsByStatuses("b1", List.of("NEW")));
    }

    @DisplayName("Visit Call With Max Waiting Delegates")
    @Test
    void visitCallWithMaxWaitingDelegates() {
        ServicePointController controller = controller();
        Optional<Visit> visit = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallWithMaximalWaitingTime("b1", "sp1"))
            .thenReturn(visit);
        assertEquals(visit, controller.visitCallWithMaximalWaitingTime("b1", "sp1"));
    }

    @DisplayName("Get Visit From Queue Returns Match")
    @Test
    void getVisitFromQueueReturnsMatch() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisits("b1", "q1")).thenReturn(List.of(visit));

        Visit result = controller.getVisit("b1", "q1", "v1");

        assertSame(visit, result);
        verify(controller.visitService).getVisits("b1", "q1");
    }

    @DisplayName("Get Visit From Queue Throws When Missing")
    @Test
    void getVisitFromQueueThrowsWhenMissing() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1")).thenReturn(List.of());

        HttpStatusException exception =
            assertThrows(HttpStatusException.class, () -> controller.getVisit("b1", "q1", "v1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @DisplayName("Call Visit Delegates To Service")
    @Test
    void callVisitDelegatesToService() {
        ServicePointController controller = controller();
        Optional<Visit> visit = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCall("b1", "sp1", "v1")).thenReturn(visit);

        assertEquals(visit, controller.callVisit("b1", "sp1", "v1"));
    }

    @DisplayName("Visit Call For Confirm Uses Service Result")
    @Test
    void visitCallForConfirmUsesServiceResult() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        Optional<Visit> expected = Optional.of(visit);
        when(controller.visitService.visitCallForConfirmWithMaxWaitingTime("b1", "sp1", visit))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCallForConfirm("b1", "sp1", visit));
    }

    @DisplayName("Visit Call For Confirm By Id Loads Visit First")
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

    @DisplayName("Visit Call For Confirm Max Waiting Delegates")
    @Test
    void visitCallForConfirmMaxWaitingDelegates() {
        ServicePointController controller = controller();
        Optional<Visit> expected = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallForConfirmWithMaxWaitingTime("b1", "sp1"))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCallForConfirmMaxWaitingTime("b1", "sp1"));
    }

    @DisplayName("Visit Call From Queues Delegates")
    @Test
    void visitCallFromQueuesDelegates() {
        ServicePointController controller = controller();
        List<String> queueIds = List.of("q1", "q2");
        Optional<Visit> expected = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallWithMaximalWaitingTime("b1", "sp1", queueIds))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCall("b1", "sp1", queueIds));
    }

    @DisplayName("Visit Call From Queues With Confirmation Delegates")
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

    @DisplayName("Visit No Show Delegates To Service")
    @Test
    void visitNoShowDelegatesToService() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        Optional<Visit> expected = Optional.of(visit);
        when(controller.visitService.visitNoShow("b1", "sp1", visit)).thenReturn(expected);

        assertEquals(expected, controller.visitNoShow("b1", "sp1", visit));
    }

    @DisplayName("Visit Call No Show Retrieves Visit")
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

    @DisplayName("Visit Re Call For Confirm Delegates")
    @Test
    void visitReCallForConfirmDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.visitReCallForConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitReCallForConfirm("b1", "sp1", visit));
    }

    @DisplayName("Visit Re Call For Confirm By Id Loads Visit")
    @Test
    void visitReCallForConfirmByIdLoadsVisit() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        when(controller.visitService.visitReCallForConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitReCallForConfirm("b1", "sp1", "v1"));
        verify(controller.visitService).getVisit("b1", "v1");
    }

    @DisplayName("Visit Confirm Delegates")
    @Test
    void visitConfirmDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.visitConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitConfirm("b1", "sp1", visit));
    }

    @DisplayName("Visit Confirm By Id Delegates")
    @Test
    void visitConfirmByIdDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        when(controller.visitService.visitConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitConfirm("b1", "sp1", "v1"));
        verify(controller.visitService).getVisit("b1", "v1");
    }

    @DisplayName("Cancel Auto Call Delegates")
    @Test
    void cancelAutoCallDelegates() {
        ServicePointController controller = controller();
        Optional<ServicePoint> expected = Optional.of(new ServicePoint("sp1", "SP"));
        when(controller.visitService.cancelAutoCallModeOfServicePoint("b1", "sp1"))
            .thenReturn(expected);

        assertEquals(expected, controller.cancelAutoCallModeOfServicePoint("b1", "sp1"));
    }

    @DisplayName("Start Auto Call Enables Branch And Point")
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

    @DisplayName("Get Outcomes Returns Configured Map")
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

    @DisplayName("Get Outcomes Throws When Service Missing")
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

    @DisplayName("Visit Call Max Life Time Delegates To Service")
    @Test
    void visitCallMaxLifeTimeDelegatesToService() {
        ServicePointController controller = controller();
        Optional<Visit> expected = Optional.of(Visit.builder().id("visit-1").build());
        when(controller.visitService.visitCallWithMaxLifeTime("b1", "sp1")).thenReturn(expected);

        Optional<Visit> actual = controller.visitCallMaxLifeTime("b1", "sp1");

        assertSame(expected, actual);
        verify(controller.visitService).visitCallWithMaxLifeTime("b1", "sp1");
    }

    @DisplayName("Visit Call Max Life Time From Queues Delegates To Service")
    @Test
    void visitCallMaxLifeTimeFromQueuesDelegatesToService() {
        ServicePointController controller = controller();
        Optional<Visit> expected = Optional.of(Visit.builder().id("visit-queues").build());
        List<String> queueIds = List.of("q1", "q2");
        when(controller.visitService.visitCallWithMaxLifeTime("b1", "sp1", queueIds)).thenReturn(expected);

        Optional<Visit> actual = controller.visitCallMaxLifeTime("b1", "sp1", queueIds);

        assertSame(expected, actual);
        verify(controller.visitService).visitCallWithMaxLifeTime("b1", "sp1", queueIds);
    }

    @DisplayName("Visit Call For Confirm Max Life Time Delegates To Service")
    @Test
    void visitCallForConfirmMaxLifeTimeDelegatesToService() {
        ServicePointController controller = controller();
        Optional<Visit> expected = Optional.of(Visit.builder().id("confirm-visit").build());
        when(controller.visitService.visitCallForConfirmWithMaxLifeTime("b1", "sp1"))
            .thenReturn(expected);

        Optional<Visit> actual = controller.visitCallForConfirmMaxLifeTime("b1", "sp1");

        assertSame(expected, actual);
        verify(controller.visitService).visitCallForConfirmWithMaxLifeTime("b1", "sp1");
    }

    @DisplayName("Visit Call For Confirm Max Life Time From Queues Delegates To Service")
    @Test
    void visitCallForConfirmMaxLifeTimeFromQueuesDelegatesToService() {
        ServicePointController controller = controller();
        Optional<Visit> expected = Optional.of(Visit.builder().id("confirm-queues").build());
        List<String> queueIds = List.of("qa", "qb");
        when(controller.visitService.visitCallForConfirmWithMaxLifeTime("b1", "sp1", queueIds))
            .thenReturn(expected);

        Optional<Visit> actual = controller.visitCallForConfirmMaxLifeTime("b1", "sp1", queueIds);

        assertSame(expected, actual);
        verify(controller.visitService).visitCallForConfirmWithMaxLifeTime("b1", "sp1", queueIds);
    }

    @DisplayName("Get Delivered Service Filters By Service Id")
    @Test
    void getDeliveredServiceFiltersByServiceId() {
        ServicePointController controller = controller();
        Branch branch = new Branch("b1", "Branch-1");
        Service service = new Service("s1", "Service 1", 1, "queue-1");
        branch.getServices().put("s1", service);
        DeliveredService delivered = new DeliveredService("ds1", "Delivered 1");
        delivered.getServiceIds().add("s1");
        DeliveredService ignored = new DeliveredService("ds2", "Delivered 2");
        ignored.getServiceIds().add("s2");
        branch.getPossibleDeliveredServices().put("ds1", delivered);
        branch.getPossibleDeliveredServices().put("ds2", ignored);
        when(controller.branchService.getBranch("b1")).thenReturn(branch);

        Map<String, DeliveredService> result = controller.getDeliveredService("b1", "s1");

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ds1"));
        assertSame(delivered, result.get("ds1"));
        verify(controller.branchService).getBranch("b1");
    }

    @DisplayName("Get Delivered Service Of Current Service Delegates To Visit Service")
    @Test
    void getDeliveredServiceOfCurrentServiceDelegatesToVisitService() {
        ServicePointController controller = controller();
        Map<String, DeliveredService> delivered = Map.of("ds1", new DeliveredService("ds1", "Delivered"));
        when(controller.visitService.getDeliveredServices("b1", "sp1")).thenReturn(delivered);

        Map<String, DeliveredService> result = controller.getDeliveredServiceOfCurrentService("b1", "sp1");

        assertSame(delivered, result);
        verify(controller.visitService).getDeliveredServices("b1", "sp1");
    }

    @DisplayName("Get Services By Work Profile Id Delegates To Branch Service")
    @Test
    void getServicesByWorkProfileIdDelegatesToBranchService() {
        ServicePointController controller = controller();
        List<Service> services = List.of(new Service("s1", "Service", 1, "queue-2"));
        when(controller.branchService.getServicesByWorkProfileId("b1", "wp1")).thenReturn(services);

        List<Service> result = controller.getServicesByWorkProfileId("b1", "wp1");

        assertSame(services, result);
        verify(controller.branchService).getServicesByWorkProfileId("b1", "wp1");
    }

    @DisplayName("Get Services By Queue Id Delegates To Branch Service")
    @Test
    void getServicesByQueueIdDelegatesToBranchService() {
        ServicePointController controller = controller();
        List<Service> services = List.of(new Service("s1", "Service", 1, "queue-3"));
        when(controller.branchService.getServicesByQueueId("b1", "q1")).thenReturn(services);

        List<Service> result = controller.getServicesByQueueId("b1", "q1");

        assertSame(services, result);
        verify(controller.branchService).getServicesByQueueId("b1", "q1");
    }

    @DisplayName("Get Delivered Services By Branch Id Delegates To Branch Service")
    @Test
    void getDeliveredServicesByBranchIdDelegatesToBranchService() {
        ServicePointController controller = controller();
        List<DeliveredService> delivered = List.of(new DeliveredService("ds1", "Delivered"));
        when(controller.branchService.getDeliveredServicesByBranchId("b1")).thenReturn(delivered);

        List<DeliveredService> result = controller.getDeliveredServicesByBranchId("b1");

        assertSame(delivered, result);
        verify(controller.branchService).getDeliveredServicesByBranchId("b1");
    }

    @DisplayName("Add Delivered Service Delegates To Visit Service")
    @Test
    void addDeliveredServiceDelegatesToVisitService() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v-delivered").build();
        when(controller.visitService.addDeliveredService("b1", "sp1", "ds1")).thenReturn(visit);

        Visit result = controller.addDeliveredService("b1", "sp1", "ds1");

        assertSame(visit, result);
        verify(controller.visitService).addDeliveredService("b1", "sp1", "ds1");
    }

    @DisplayName("Add Services Invokes Visit Service Sequentially")
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

    @DisplayName("Delete Visit Removes Visit When Found")
    @Test
    void deleteVisitRemovesVisitWhenFound() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        HashMap<String, Visit> visits = new HashMap<>(Map.of("v1", visit));
        when(controller.visitService.getAllVisits("b1")).thenReturn(visits);

        controller.deleteVisit("b1", "v1");

        verify(controller.visitService).deleteVisit(visit);
    }

    @DisplayName("Delete Visit Throws When Missing")
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
