package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.DeliveredService;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServiceGroup;
import ru.aritmos.model.SegmentationRuleData;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.WorkProfile;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

/**
 * Тесты для {@link BranchService}.
 */
class BranchServiceTest {

    /**
     * Возвращает существующее отделение по идентификатору.
     */
    @Test
    void getBranchReturnsExistingBranch() {
        // подготовка сервиса и данных
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        service.branches.put(branch.getId(), branch);

        // действие
        Branch result = service.getBranch("b1");

        // проверка
        assertSame(branch, result);
    }

    /**
     * Бросает исключение при отсутствии отделения.
     */
    @Test
    void getBranchThrowsWhenMissing() {
        // подготовка
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);

        // проверка исключения
        assertThrows(HttpStatusException.class, () -> service.getBranch("missing"));
        // событие об ошибке отправлено
        verify(eventService).send(eq("*"), eq(false), any());
    }

    /**
     * Добавляет группы услуг и связывает их с услугами отделения.
     */
    @Test
    void addUpdateServiceGroupsAssignsToServices() {
        // подготовка отделения с услугой
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        Service service1 = new Service("s1", "Service", 10, "q1");
        branch.getServices().put(service1.getId(), service1);
        service.branches.put("b1", branch);

        ServiceGroup group = new ServiceGroup("g1", "Group", List.of("s1"), branch.getId());
        HashMap<String, ServiceGroup> groups = new HashMap<>();
        groups.put(group.getId(), group);

        // действие
        service.addUpdateServiceGroups("b1", groups);

        // проверка: группа добавлена и услуга привязана
        assertSame(group, branch.getServiceGroups().get("g1"));
        assertEquals("g1", service1.getServiceGroupId());
        verify(service).add("b1", branch);
        verify(service.eventService)
            .sendChangedEvent(eq("config"), eq(false), isNull(), eq(groups), anyMap(), eq("Add or update service groups"));
    }

    /**
     * Бросает ошибку, если группа ссылается на несуществующую услугу.
     */
    @Test
    void addUpdateServiceGroupsThrowsWhenServiceMissing() {
        BranchService service = spy(new BranchService());
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        service.branches.put("b1", branch);

        ServiceGroup group = new ServiceGroup("g1", "Group", List.of("missing"), branch.getId());
        HashMap<String, ServiceGroup> groups = new HashMap<>();
        groups.put(group.getId(), group);

        assertThrows(HttpStatusException.class, () -> service.addUpdateServiceGroups("b1", groups));
        verify(service, never()).add(anyString(), any());
        verify(eventService, never())
            .sendChangedEvent(anyString(), anyBoolean(), any(), any(), anyMap(), anyString());
    }

    /**
     * Проверяет наличие отделения методом branchExists.
     */
    @Test
    void branchExistsChecksPresence() {
        // подготовка
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        service.branches.put(branch.getId(), branch);

        // действие и проверки
        assertTrue(service.branchExists("b1"));
        assertFalse(service.branchExists("b2"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    /**
     * Возвращает копию отделений без детальной информации.
     */
    @Test
    void getBranchesReturnsCopyWithoutDetails() {
        // подготовка
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        branch.setPrefix("PR");
        branch.getQueues().put("q1", new Queue("q1", "Queue", "A", 1));
        service.branches.put(branch.getId(), branch);

        // действие
        HashMap<String, Branch> result = service.getBranches();

        // проверки
        assertEquals(1, result.size());
        Branch copy = result.get("b1");
        assertNotSame(branch, copy);
        assertEquals("PR", copy.getPrefix());
        assertTrue(copy.getQueues().isEmpty());
    }

    /**
     * Возвращает оригинальные ссылки на отделения при запросе детализированного списка.
     */
    @Test
    void getDetailedBranchesReturnsOriginalReferences() {
        // подготовка
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        service.branches.put(branch.getId(), branch);

        // действие
        HashMap<String, Branch> result = service.getDetailedBranches();

        // проверки
        assertSame(branch, result.get("b1"));
        verify(service).getBranch("b1");
    }

    /**
     * Сохраняет отделение и проставляет идентификаторы зависимым сущностям.
     */
    @Test
    void addStoresBranchAndSetsIds() {
        // подготовка
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Test branch");
        branch.getQueues().put("q1", new Queue("q1", "Queue", "A", 1));
        branch.getServices().put("s1", new Service("s1", "Service", 1, "q1"));

        // действие
        service.add("b1", branch);

        // проверки
        assertSame(branch, service.branches.get("b1"));
        assertEquals("b1", branch.getQueues().get("q1").getBranchId());
        assertEquals("b1", branch.getServices().get("s1").getBranchId());
        verify(eventService)
            .sendChangedEvent(eq("config"), eq(true), isNull(), eq(branch), anyMap(), eq("BRANCH_CREATED"));
    }

    /**
     * Меняет рабочий профиль пользователя на точке обслуживания.
     */
    @Test
    void changeUserWorkProfileInServicePointUpdatesUser() {
        // подготовка
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

        // действие
        User result = service.changeUserWorkProfileInServicePoint("b1", "sp1", "wp1");

        // проверки
        assertEquals("wp1", result.getCurrentWorkProfileId());
        verify(service).add("b1", branch);
    }

    /**
     * Бросает исключение, если на точке отсутствует пользователь.
     */
    @Test
    void changeUserWorkProfileInServicePointThrowsWhenUserMissing() {
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        branch.getWorkProfiles().put("wp1", new WorkProfile("wp1", "WP1"));
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP1"));
        service.branches.put("b1", branch);

        HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> service.changeUserWorkProfileInServicePoint("b1", "sp1", "wp1"));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    /**
     * Делегирует закрытие точки обслуживания объекту Branch.
     */
    @Test
    void closeServicePointDelegatesToBranch() {
        // подготовка
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

        // действие
        service.closeServicePoint("b1", "sp1", visitService, false, false, "", false, "");

        // проверки
        verify(branch)
            .closeServicePoint(
                eq("sp1"), eq(service.eventService), eq(visitService), eq(false), eq(false), eq(""), eq(false), eq(""));
        verify(service).add("b1", branch);
    }

    /**
     * Обновляет существующее отделение и отправляет событие изменения.
     */
    @Test
    void addUpdatesExistingBranchSendsEvent() {
        // подготовка
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch oldBranch = new Branch("b1", "Старый");
        service.branches.put("b1", oldBranch);
        Branch updated = new Branch("b1", "Новый");

        // действие
        service.add("b1", updated);

        // проверки
        assertSame(updated, service.branches.get("b1"));
        verify(eventService)
            .sendChangedEvent(
                eq("config"), eq(true), eq(oldBranch), eq(updated), anyMap(), eq("BRANCH_CHANGED"));
    }

    /**
     * Удаляет отделение, закрывая точки и публикуя событие.
     */
    @Test
    void deleteRemovesBranchAndSendsEvent() {
        BranchService service = spy(new BranchService());
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        VisitService visitService = mock(VisitService.class);
        Branch branch = new Branch("b1", "Branch");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        servicePoint.setUser(new User("user", "User", null));
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        service.branches.put("b1", branch);

        doNothing()
            .when(service)
            .closeServicePoint(
                anyString(), anyString(), any(), anyBoolean(), anyBoolean(), anyString(), anyBoolean(), anyString());

        service.delete("b1", visitService);

        verify(service)
            .closeServicePoint(
                eq("b1"),
                eq("sp1"),
                eq(visitService),
                eq(true),
                eq(false),
                eq(""),
                eq(true),
                eq("BRANCH_DELETED"));
        verify(eventService)
            .sendChangedEvent(eq("config"), eq(true), eq(branch), isNull(), anyMap(), eq("BRANCH_DELETED"));
        assertFalse(service.branches.containsKey("b1"));
    }

    /**
     * Бросает исключение при попытке удалить неизвестное отделение.
     */
    @Test
    void deleteThrowsWhenBranchMissing() {
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        VisitService visitService = mock(VisitService.class);

        assertThrows(HttpStatusException.class, () -> service.delete("missing", visitService));
        verify(eventService, never())
            .sendChangedEvent(anyString(), anyBoolean(), any(), any(), anyMap(), anyString());
    }

    /**
     * Возвращает услуги, связанные с рабочим профилем.
     */
    @Test
    void getServicesByWorkProfileIdReturnsLinked() {
        // подготовка
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q", "A", 1);
        branch.getQueues().put(queue.getId(), queue);
        Service serviceModel = new Service("s1", "S", 1, "q1");
        branch.getServices().put(serviceModel.getId(), serviceModel);
        WorkProfile wp = new WorkProfile("wp1", "WP1");
        wp.getQueueIds().add("q1");
        branch.getWorkProfiles().put(wp.getId(), wp);
        service.branches.put("b1", branch);

        // действие
        List<Service> result = service.getServicesByWorkProfileId("b1", "wp1");

        // проверки
        assertEquals(1, result.size());
        assertEquals("s1", result.get(0).getId());
    }

    /**
     * Бросает ошибку, если рабочий профиль не найден.
     */
    @Test
    void getServicesByWorkProfileIdThrowsWhenMissing() {
        // подготовка
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        service.branches.put("b1", branch);

        // проверка
        assertThrows(
            HttpStatusException.class,
            () -> service.getServicesByWorkProfileId("b1", "wp1"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    /**
     * Возвращает услуги очереди по её идентификатору.
     */
    @Test
    void getServicesByQueueIdReturnsLinked() {
        // подготовка
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q", "A", 1);
        branch.getQueues().put(queue.getId(), queue);
        Service serviceModel = new Service("s1", "S", 1, "q1");
        branch.getServices().put(serviceModel.getId(), serviceModel);
        service.branches.put("b1", branch);

        // действие
        List<Service> result = service.getServicesByQueueId("b1", "q1");

        // проверки
        assertEquals(1, result.size());
        assertEquals("s1", result.get(0).getId());
    }

    /**
     * Бросает ошибку, если очередь не найдена.
     */
    @Test
    void getServicesByQueueIdThrowsWhenMissing() {
        // подготовка
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        service.branches.put("b1", branch);

        // проверка
        assertThrows(
            HttpStatusException.class,
            () -> service.getServicesByQueueId("b1", "q1"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    /**
     * Возвращает список возможных оказанных услуг отделения.
     */
    @Test
    void getDeliveredServicesByBranchIdReturnsList() {
        // подготовка
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        DeliveredService delivered = new DeliveredService("d1", "D1");
        branch.getPossibleDeliveredServices().put(delivered.getId(), delivered);
        service.branches.put("b1", branch);

        // действие
        List<DeliveredService> result = service.getDeliveredServicesByBranchId("b1");

        // проверки
        assertEquals(1, result.size());
        assertEquals("d1", result.get(0).getId());
    }

    /**
     * Делегирует обновление визита объекту Branch.
     */
    @Test
    void updateVisitDelegatesToBranch() {
        // подготовка: шпион Branch и сервис
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doNothing().when(branch).updateVisit(any(), any(), anyString(), any());
        service.branches.put("b1", branch);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();
        VisitService visitService = mock(VisitService.class);

        // действие
        service.updateVisit(visit, "ACTION", visitService);

        // проверки: вызван метод Branch.updateVisit с нужными параметрами
        verify(branch).updateVisit(visit, service.eventService, "ACTION", visitService);
        verify(service).getBranch("b1");
    }

    /**
     * Делегирует обновление визита по событию без дополнительных параметров.
     */
    @Test
    void updateVisitWithEventDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doNothing()
            .when(branch)
            .updateVisit(any(Visit.class), any(), any(VisitEvent.class), any(VisitService.class));
        service.branches.put("b1", branch);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();
        VisitService visitService = mock(VisitService.class);

        service.updateVisit(visit, VisitEvent.CREATED, visitService);

        verify(branch).updateVisit(visit, service.eventService, VisitEvent.CREATED, visitService);
        verify(service).getBranch("b1");
    }

    /**
     * Делегирует обновление визита по событию с флагом постановки в начало.
     */
    @Test
    void updateVisitWithEventAndStartFlagDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doNothing()
            .when(branch)
            .updateVisit(any(Visit.class), any(), any(VisitEvent.class), any(VisitService.class), anyBoolean());
        service.branches.put("b1", branch);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();
        VisitService visitService = mock(VisitService.class);

        service.updateVisit(visit, VisitEvent.CALLED, visitService, true);

        verify(branch)
            .updateVisit(visit, service.eventService, VisitEvent.CALLED, visitService, true);
        verify(service).getBranch("b1");
    }

    /**
     * Делегирует обновление визита по событию с указанием позиции.
     */
    @Test
    void updateVisitWithEventAndIndexDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doNothing()
            .when(branch)
            .updateVisit(any(Visit.class), any(), any(VisitEvent.class), any(VisitService.class), anyInt());
        service.branches.put("b1", branch);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();
        VisitService visitService = mock(VisitService.class);

        service.updateVisit(visit, VisitEvent.START_SERVING, visitService, 2);

        verify(branch)
            .updateVisit(visit, service.eventService, VisitEvent.START_SERVING, visitService, 2);
        verify(service).getBranch("b1");
    }

    /**
     * Открывает точку обслуживания, назначая пользователя и рабочий профиль.
     */
    @Test
    void openServicePointAssignsUserAndProfile() throws Exception {
        // подготовка: отделение с рабочим профилем и точкой
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.isUserModuleTypeByUserName("user", "admin")).thenReturn(false);
        when(keyCloackClient.getAllBranchesOfUser("user")).thenReturn(List.of());
        when(keyCloackClient.getUserInfo("user")).thenReturn(java.util.Optional.empty());
        service.keyCloackClient = keyCloackClient;
        VisitService visitService = mock(VisitService.class);

        Branch branch = new Branch("b1", "Branch");
        branch.getWorkProfiles().put("wp1", new WorkProfile("wp1", "WP"));
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP"));
        service.branches.put("b1", branch);

        // действие
        User user = service.openServicePoint("b1", "user", "sp1", "wp1", visitService);

        // проверки: пользователь назначен на точку и профиль
        assertEquals("sp1", user.getServicePointId());
        assertEquals("wp1", user.getCurrentWorkProfileId());
        assertSame(user, branch.getServicePoints().get("sp1").getUser());

        verify(service).add("b1", branch);
    }

    /**
     * Подгружает профиль сотрудника из Keycloak при первом открытии точки.
     */
    @Test
    void openServicePointUpdatesUserFromKeycloak() throws Exception {
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        UserRepresentation representation = new UserRepresentation();
        representation.setId("user-id");
        representation.setEmail("user@example.com");
        representation.setFirstName("Иван");
        representation.setLastName("Иванов");
        when(keyCloackClient.getUserInfo("user")).thenReturn(Optional.of(representation));
        GroupRepresentation group = new GroupRepresentation();
        HashMap<String, List<String>> attributes = new HashMap<>();
        attributes.put("branchPrefix", List.of("BR"));
        group.setAttributes(attributes);
        when(keyCloackClient.getAllBranchesOfUser("user")).thenReturn(List.of(group));
        when(keyCloackClient.isUserModuleTypeByUserName("user", "admin")).thenReturn(false);
        service.keyCloackClient = keyCloackClient;
        VisitService visitService = mock(VisitService.class);

        Branch branch = new Branch("b1", "Branch");
        branch.setPrefix("BR");
        branch.getWorkProfiles().put("wp1", new WorkProfile("wp1", "WP"));
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP"));
        service.branches.put("b1", branch);

        User user = service.openServicePoint("b1", "user", "sp1", "wp1", visitService);

        assertEquals("user-id", user.getId());
        assertEquals("user@example.com", user.getEmail());
        assertEquals("Иван", user.getFirstName());
        assertEquals("Иванов", user.getLastName());
        assertSame(user, branch.getUsers().get("user"));
    }

    /**

     * Публикует события о смене точки и профиля при открытии точки существующим сотрудником.
     */
    @Test
    void openServicePointPublishesUserChangeEvents() throws Exception {
        // подготовка: отделение с существующим пользователем
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.getUserInfo("user")).thenReturn(Optional.empty());
        service.keyCloackClient = keyCloackClient;
        VisitService visitService = mock(VisitService.class);

        Branch branch = new Branch("b1", "Branch");
        branch.setPrefix("BR");
        branch.getWorkProfiles().put("wp2", new WorkProfile("wp2", "Новый профиль"));
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "Окно 1"));
        User user = new User("u1", "user", null);
        user.setName("user");
        user.setIsAdmin(true);
        user.setAllBranches(List.of());
        user.setServicePointId("oldSp");
        user.setCurrentWorkProfileId("oldWp");
        branch.getUsers().put("user", user);
        service.branches.put("b1", branch);

        service.openServicePoint("b1", "user", "sp1", "wp2", visitService);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(service.eventService, atLeastOnce()).send(eq("stat"), eq(false), eventCaptor.capture());

        List<String> eventTypes = eventCaptor.getAllValues().stream().map(Event::getEventType).toList();
        assertTrue(eventTypes.contains("USER_SERVICE_POINT_CHANGED"));
        assertTrue(eventTypes.contains("USER_WORK_PROFILE_CHANGED"));
    }

    /**

     * Закрывает прежние точки обслуживания пользователя при назначении новой.
     */
    @Test
    void openServicePointClosesPreviousAssignments() throws Exception {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.getUserInfo("user")).thenReturn(Optional.empty());
        when(keyCloackClient.getAllBranchesOfUser("user")).thenReturn(List.of());
        when(keyCloackClient.isUserModuleTypeByUserName("user", "admin")).thenReturn(true);
        service.keyCloackClient = keyCloackClient;
        VisitService visitService = mock(VisitService.class);

        Branch branch = new Branch("b1", "Branch");
        branch.setPrefix("BR");
        branch.getWorkProfiles().put("wp1", new WorkProfile("wp1", "WP"));
        ServicePoint oldPoint = new ServicePoint("old", "Старое окно");
        User user = new User("u1", "user", null);
        user.setName("user");
        user.setIsAdmin(true);
        user.setAllBranches(List.of());
        user.setServicePointId("old");
        user.setCurrentWorkProfileId("oldProfile");
        oldPoint.setUser(user);
        ServicePoint newPoint = new ServicePoint("new", "Новое окно");
        branch.getServicePoints().put(oldPoint.getId(), oldPoint);
        branch.getServicePoints().put(newPoint.getId(), newPoint);
        branch.getWorkProfiles().put("oldProfile", new WorkProfile("oldProfile", "Старый"));
        branch.getUsers().put("user", user);
        service.branches.put("b1", branch);

        doNothing()
            .when(service)
            .closeServicePoint(
                anyString(), anyString(), any(), anyBoolean(), anyBoolean(), anyString(), anyBoolean(), anyString());

        service.openServicePoint("b1", "user", "new", "wp1", visitService);

        verify(service)
            .closeServicePoint(
                eq("b1"), eq("old"), eq(visitService), eq(false), eq(false), eq(""), eq(false), eq(""));
        assertEquals("new", branch.getUsers().get("user").getServicePointId());
        assertSame(branch.getServicePoints().get("new").getUser(), branch.getUsers().get("user"));
    }

    /**

     * Бросает ошибку, если у сотрудника нет доступа к отделению при открытии точки.
     */
    @Test
    void openServicePointThrowsWhenUserHasNoAccess() {
        // подготовка: пользователь без прав на филиал
        BranchService service = new BranchService();
        service.eventService = mock(EventService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.getUserInfo("user")).thenReturn(Optional.empty());
        service.keyCloackClient = keyCloackClient;
        VisitService visitService = mock(VisitService.class);

        Branch branch = new Branch("b1", "Branch");
        branch.setPrefix("BR");
        branch.getWorkProfiles().put("wp2", new WorkProfile("wp2", "Новый профиль"));
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "Окно 1"));
        User user = new User("u1", "user", null);
        user.setName("user");
        user.setIsAdmin(false);
        user.setAllBranches(List.of());
        branch.getUsers().put("user", user);
        service.branches.put("b1", branch);

        HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> service.openServicePoint("b1", "user", "sp1", "wp2", visitService));

        assertTrue(exception.getMessage().contains("dont have permissions"));
    }

    /**
     * Удаляет отделение, закрывая активные точки и отправляя событие.
     */
    @Test
    void deleteRemovesBranchAndClosesServicePoints() {
        // подготовка
        BranchService service = spy(new BranchService());
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        VisitService visitService = mock(VisitService.class);
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setUser(new User("u1", "User", null));
        branch.getServicePoints().put("sp1", sp);
        service.branches.put("b1", branch);

        doNothing()
            .when(service)
            .closeServicePoint(
                anyString(),
                anyString(),
                any(),
                anyBoolean(),
                anyBoolean(),
                any(),
                anyBoolean(),
                anyString());

        // действие
        service.delete("b1", visitService);

        // проверки
        assertFalse(service.branches.containsKey("b1"));
        verify(service)
            .closeServicePoint(
                eq("b1"),
                eq("sp1"),
                eq(visitService),
                eq(true),
                eq(false),
                eq(""),
                eq(true),
                eq("BRANCH_DELETED"));
        verify(eventService)
            .sendChangedEvent(
                eq("config"), eq(true), eq(branch), isNull(), anyMap(), eq("BRANCH_DELETED"));
    }

    /**
     * Бросает ошибку, если рабочий профиль не найден.
     */
    @Test
    void openServicePointThrowsWhenWorkProfileMissing() {
        // подготовка: отделение без рабочего профиля
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP1"));
        service.branches.put("b1", branch);

        // проверка исключения
        assertThrows(
            HttpStatusException.class,
            () -> service.openServicePoint("b1", "user", "sp1", "wp1", mock(VisitService.class)));
        // событие об ошибке отправлено
        verify(eventService).send(eq("*"), eq(false), any());
    }

    /**
     * Бросает ошибку, если точка обслуживания не найдена.
     */
    @Test
    void openServicePointThrowsWhenServicePointMissing() {
        // подготовка: отделение без точки обслуживания
        BranchService service = new BranchService();
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        branch.getWorkProfiles().put("wp1", new WorkProfile("wp1", "WP"));
        service.branches.put("b1", branch);

        // проверка исключения
        assertThrows(
            HttpStatusException.class,
            () -> service.openServicePoint("b1", "user", "sp1", "wp1", mock(VisitService.class)));
        // событие об ошибке отправлено
        verify(eventService).send(eq("*"), eq(false), any());
    }

    /**
     * Возвращает карту пользователей отделения без изменений.
     */
    @Test
    void getUsersReturnsBranchUsers() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        User user = new User("u1", "User", null);
        branch.getUsers().put(user.getName(), user);
        service.branches.put("b1", branch);

        HashMap<String, User> result = service.getUsers("b1");

        assertSame(branch.getUsers(), result);
        verify(service).getBranch("b1");
    }

    /**
     * Увеличивает счётчик талонов очереди и сохраняет отделение.
     */
    @Test
    void incrementTicketCounterUpdatesQueue() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q1", "A", 1);
        branch.getQueues().put(queue.getId(), queue);
        service.branches.put("b1", branch);

        Integer result = service.incrementTicketCounter("b1", queue);

        assertEquals(1, result);
        assertEquals(1, branch.getQueues().get("q1").getTicketCounter());
        verify(service).add("b1", branch);
    }

    /**
     * Возвращает -1, если очередь не найдена, но отделение сохраняется.
     */
    @Test
    void incrementTicketCounterReturnsMinusOneWhenQueueMissing() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = new Branch("b1", "Branch");
        service.branches.put("b1", branch);
        Queue queue = new Queue("q1", "Q1", "A", 1);

        Integer result = service.incrementTicketCounter("b1", queue);

        assertEquals(-1, result);
        verify(service).add("b1", branch);
    }

    /**
     * Делегирует обновление услуг объекта Branch и сохраняет отделение.
     */
    @Test
    void addUpdateServiceDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doReturn(branch).when(service).getBranch("b1");
        HashMap<String, Service> services = new HashMap<>();
        VisitService visitService = mock(VisitService.class);

        service.addUpdateService("b1", services, true, visitService);


        verify(branch)
            .addUpdateService(services, service.eventService, true, visitService);
        verify(service).add("b1", branch);
    }

    /**
     * Вызывает удаление услуг у Branch c передачей всех параметров.
     */
    @Test
    void deleteServicesDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doReturn(branch).when(service).getBranch("b1");
        VisitService visitService = mock(VisitService.class);
        List<String> serviceIds = List.of("s1", "s2");

        service.deleteServices("b1", serviceIds, true, visitService);

        verify(branch)
            .deleteServices(serviceIds, service.eventService, true, visitService);
    }

    /**
     * Передаёт обновление точек обслуживания в Branch и инициирует сохранение.
     */
    @Test
    void addUpdateServicePointDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doReturn(branch).when(service).getBranch("b1");
        HashMap<String, ServicePoint> points = new HashMap<>();


        service.addUpdateServicePoint("b1", points, true, false);

        verify(branch)
            .addUpdateServicePoint(points, true, false, service.eventService);
        verify(service).add("b1", branch);
    }

    /**
     * Делегирует удаление точек обслуживания и сохраняет изменения отделения.
     */
    @Test
    void deleteServicePointsDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doReturn(branch).when(service).getBranch("b1");
        List<String> servicePointIds = List.of("sp1", "sp2");

        service.deleteServicePoints("b1", servicePointIds);

        verify(branch).deleteServicePoints(servicePointIds, service.eventService);
        verify(service).add("b1", branch);
    }

    /**
     * Передаёт обновление очередей в Branch и инициирует сохранение отделения.
     */
    @Test
    void addUpdateQueuesDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doReturn(branch).when(service).getBranch("b1");
        HashMap<String, Queue> queues = new HashMap<>();

        service.addUpdateQueues("b1", queues, true);

        verify(branch).addUpdateQueues(queues, true, service.eventService);
        verify(service).add("b1", branch);
    }

    /**
     * Делегирует удаление очередей и фиксирует изменения через add.
     */
    @Test
    void deleteQueuesDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doReturn(branch).when(service).getBranch("b1");
        List<String> queueIds = List.of("q1", "q2");

        service.deleteQueues("b1", queueIds);

        verify(branch).deleteQueues(queueIds, service.eventService);
        verify(service).add("b1", branch);
    }

    /**
     * Передаёт обновление правил сегментации объекту Branch и сохраняет отделение.
     */
    @Test
    void addUpdateSegmentationRulesDelegatesToBranch() {
        BranchService service = spy(new BranchService());
        service.eventService = mock(EventService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        Branch branch = spy(new Branch("b1", "Branch"));
        doReturn(branch).when(service).getBranch("b1");
        HashMap<String, SegmentationRuleData> rules = new HashMap<>();

        service.addUpdateSegmentationRules("b1", rules);

        verify(branch).adUpdateSegmentRules(rules, service.eventService);
        verify(service).add("b1", branch);
    }

}
