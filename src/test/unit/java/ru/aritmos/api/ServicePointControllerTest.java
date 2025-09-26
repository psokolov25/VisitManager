package ru.aritmos.api;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.DeliveredService;
import ru.aritmos.model.Entity;
import ru.aritmos.model.Outcome;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.model.tiny.TinyServicePoint;
import ru.aritmos.model.visit.Visit;
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

    @DisplayName("Получение свободных точек делегирует загрузку сервису визитов")
    @Test
    void getFreeServicePointsDelegatesToVisitService() {
        ServicePointController controller = controller();
        HashMap<String, ServicePoint> points = new HashMap<>();
        when(controller.visitService.getStringServicePointHashMap("b1")).thenReturn(points);

        assertSame(points, controller.getFreeServicePoints("b1"));
        verify(controller.visitService).getStringServicePointHashMap("b1");
    }

    @DisplayName("Запрос сотрудников отделения выполняется через сервис отделений")
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

    @DisplayName("Смена рабочего профиля сотрудника выполняется сервисом отделений")
    @Test
    void changeUserWorkprofileDelegates() {
        ServicePointController controller = controller();
        User user = new User("id", "u1", null);
        when(controller.branchService.changeUserWorkProfileInServicePoint("b1", "sp1", "wp1")).thenReturn(user);

        assertSame(user, controller.changeUserWorkprofile("b1", "sp1", "wp1"));
        verify(controller.branchService).changeUserWorkProfileInServicePoint("b1", "sp1", "wp1");
    }

    @DisplayName("Открытие точки обслуживания выполняется сервисом отделений")
    @Test
    void openServicePointDelegates() throws IOException {
        ServicePointController controller = controller();
        User user = new User("id", "u1", null);
        when(controller.branchService.openServicePoint("b1", "u1", "sp1", "wp1", controller.visitService)).thenReturn(user);

        assertSame(user, controller.openServicePoint("b1", "u1", "sp1", "wp1"));
        verify(controller.branchService).openServicePoint("b1", "u1", "sp1", "wp1", controller.visitService);
    }

    @DisplayName("Закрытие точки обслуживания выполняется сервисом отделений")
    @Test
    void closeServicePointDelegates() {
        ServicePointController controller = controller();

        controller.closeServicePoint("b1", "sp1", false, null, false, "");
        verify(controller.branchService)
            .closeServicePoint(eq("b1"), eq("sp1"), eq(controller.visitService), eq(false),
                eq(false), isNull(), eq(false), eq(""));
    }

    @DisplayName("Получение списка принтеров выполняется сервисом визитов")
    @Test
    void getPrintersDelegates() {
        ServicePointController controller = controller();
        List<Entity> printers = List.of(new Entity("p1", "Printer"));
        when(controller.visitService.getPrinters("b1")).thenReturn(printers);
        assertEquals(printers, controller.getPrinters("b1"));
    }

    @DisplayName("Запрос списка очередей выполняется сервисом визитов")
    @Test
    void getQueuesDelegates() {
        ServicePointController controller = controller();
        List<Entity> queues = List.of(new Entity("q1", "Queue"));
        when(controller.visitService.getQueus("b1")).thenReturn(queues);
        assertEquals(queues, controller.getQueues("b1"));
    }

    @DisplayName("Запрос очередей с деталями обрабатывает сервис визитов")
    @Test
    void getFullQueuesDelegates() {
        ServicePointController controller = controller();
        List<Queue> queues = List.of(new Queue("q1", "Queue", "Q", 1));
        when(controller.visitService.getFullQueus("b1")).thenReturn(queues);
        assertEquals(queues, controller.getFullQueues("b1"));
    }

    @DisplayName("Загрузка точек обслуживания преобразует их в упрощенную модель")
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

    @DisplayName("Получение детальной информации о точках выполняет сервис визитов")
    @Test
    void getDetailedServicePointsDelegates() {
        ServicePointController controller = controller();
        ServicePoint sp = new ServicePoint("sp1", "SP");
        when(controller.visitService.getServicePointHashMap("b1"))
            .thenReturn(new HashMap<>(Map.of("sp1", sp)));
        assertEquals(List.of(sp), controller.getDetailedServicePoints("b1"));
    }

    @DisplayName("Поиск точки по имени пользователя возвращает соответствующий объект")
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

    @DisplayName("Запрос работающих сотрудников отделения выполняет сервис визитов")
    @Test
    void getAllWorkingUsersDelegates() {
        ServicePointController controller = controller();
        HashMap<String, User> users = new HashMap<>(Map.of("u1", new User("id", "u1", null)));
        when(controller.visitService.getAllWorkingUsers("b1")).thenReturn(users);
        List<User> result = controller.getAllWorkingUsersOfBranch("b1");
        assertEquals(1, result.size());
        verify(controller.visitService).getAllWorkingUsers("b1");
    }

    @DisplayName("Глобальный поиск точки по имени пользователя выполняет сервис отделений")
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

    @DisplayName("Поиск пользователя по имени возвращает данные сервиса визитов")
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

    @DisplayName("Запрос рабочих профилей выполняет сервис визитов")
    @Test
    void getWorkProfilesDelegates() {
        ServicePointController controller = controller();
        List<TinyClass> profiles = List.of(new TinyClass("wp1", "Profile"));
        when(controller.visitService.getWorkProfiles("b1")).thenReturn(profiles);
        assertEquals(profiles, controller.getWorkProfiles("b1"));
    }

    @DisplayName("Выход пользователя закрывает точку обслуживания средствами сервиса отделений")
    @Test
    void logoutUserDelegates() {
        ServicePointController controller = controller();
        controller.logoutUser("b1", "sp1", false, null, false, "");
        verify(controller.branchService)
            .closeServicePoint(eq("b1"), eq("sp1"), eq(controller.visitService), eq(true),
                eq(false), isNull(), eq(false), eq(""));
    }

    @DisplayName("Запрос ограниченного списка визитов выполняет сервис визитов")
    @Test
    void getVisitsWithLimitDelegates() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1", 5L))
            .thenReturn(List.of(Visit.builder().build()));
        assertEquals(1, controller.getVisits("b1", "q1", 5L).size());
    }

    @DisplayName("Запрос полного списка визитов выполняет сервис визитов")
    @Test
    void getVisitsDelegates() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1"))
            .thenReturn(List.of(Visit.builder().build()));
        assertEquals(1, controller.getVisits("b1", "q1").size());
    }

    @DisplayName("Запрос всех визитов возвращает карту сервиса визитов")
    @Test
    void getAllVisitsDelegates() {
        ServicePointController controller = controller();
        HashMap<String, Visit> visits = new HashMap<>(Map.of("v1", Visit.builder().build()));
        when(controller.visitService.getAllVisits("b1")).thenReturn(visits);
        assertEquals(visits, controller.getAllVisits("b1"));
    }

    @DisplayName("Поиск конкретного визита выполняет сервис визитов")
    @Test
    void getVisitDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        assertEquals(visit, controller.getVisit("b1", "v1"));
    }

    @DisplayName("Фильтрация визитов по статусам выполняется сервисом визитов")
    @Test
    void getVisitsByStatusesDelegates() {
        ServicePointController controller = controller();
        HashMap<String, Visit> visits = new HashMap<>();
        when(controller.visitService.getVisitsByStatuses("b1", List.of("NEW")))
            .thenReturn(visits);
        assertEquals(visits, controller.getVisitsByStatuses("b1", List.of("NEW")));
    }

    @DisplayName("Вызов визита с максимальным ожиданием выполняет сервис визитов")
    @Test
    void visitCallWithMaxWaitingDelegates() {
        ServicePointController controller = controller();
        Optional<Visit> visit = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallWithMaximalWaitingTime("b1", "sp1"))
            .thenReturn(visit);
        assertEquals(visit, controller.visitCallWithMaximalWaitingTime("b1", "sp1"));
    }

    @DisplayName("Поиск визита в очереди возвращает найденный объект")
    @Test
    void getVisitFromQueueReturnsMatch() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisits("b1", "q1")).thenReturn(List.of(visit));

        Visit result = controller.getVisit("b1", "q1", "v1");

        assertSame(visit, result);
        verify(controller.visitService).getVisits("b1", "q1");
    }

    @DisplayName("Поиск визита в очереди выбрасывает 404 при отсутствии результата")
    @Test
    void getVisitFromQueueThrowsWhenMissing() {
        ServicePointController controller = controller();
        when(controller.visitService.getVisits("b1", "q1")).thenReturn(List.of());

        HttpStatusException exception =
            assertThrows(HttpStatusException.class, () -> controller.getVisit("b1", "q1", "v1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @DisplayName("Обращение к вызову визита выполняется сервисом визитов")
    @Test
    void callVisitDelegatesToService() {
        ServicePointController controller = controller();
        Optional<Visit> visit = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCall("b1", "sp1", "v1")).thenReturn(visit);

        assertEquals(visit, controller.callVisit("b1", "sp1", "v1"));
    }

    @DisplayName("Повторный вызов для подтверждения возвращает результат сервиса визитов")
    @Test
    void visitCallForConfirmUsesServiceResult() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        Optional<Visit> expected = Optional.of(visit);
        when(controller.visitService.visitCallForConfirmWithMaxWaitingTime("b1", "sp1", visit))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCallForConfirm("b1", "sp1", visit));
    }

    @DisplayName("Подтверждение визита по идентификатору загружает данные и делегируется сервису")
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

    @DisplayName("Подтверждение визита с ограничением ожидания выполняет сервис визитов")
    @Test
    void visitCallForConfirmMaxWaitingDelegates() {
        ServicePointController controller = controller();
        Optional<Visit> expected = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallForConfirmWithMaxWaitingTime("b1", "sp1"))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCallForConfirmMaxWaitingTime("b1", "sp1"));
    }

    @DisplayName("Вызов визита среди нескольких очередей делегирует выбор сервису")
    @Test
    void visitCallFromQueuesDelegates() {
        ServicePointController controller = controller();
        List<String> queueIds = List.of("q1", "q2");
        Optional<Visit> expected = Optional.of(Visit.builder().build());
        when(controller.visitService.visitCallWithMaximalWaitingTime("b1", "sp1", queueIds))
            .thenReturn(expected);

        assertEquals(expected, controller.visitCall("b1", "sp1", queueIds));
    }

    @DisplayName("Подтверждение с максимальным ожиданием по нескольким очередям делегирует выбор сервису")
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

    @DisplayName("Фиксация неявки клиента выполняется сервисом визитов")
    @Test
    void visitNoShowDelegatesToService() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        Optional<Visit> expected = Optional.of(visit);
        when(controller.visitService.visitNoShow("b1", "sp1", visit)).thenReturn(expected);

        assertEquals(expected, controller.visitNoShow("b1", "sp1", visit));
    }

    @DisplayName("Повторный вызов отсутствующего клиента загружает визит и делегирует обработку сервису")
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

    @DisplayName("Повторное приглашение на подтверждение выполняется сервисом визитов")
    @Test
    void visitReCallForConfirmDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.visitReCallForConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitReCallForConfirm("b1", "sp1", visit));
    }

    @DisplayName("Повторное приглашение по идентификатору загружает визит через сервис визитов")
    @Test
    void visitReCallForConfirmByIdLoadsVisit() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        when(controller.visitService.visitReCallForConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitReCallForConfirm("b1", "sp1", "v1"));
        verify(controller.visitService).getVisit("b1", "v1");
    }

    @DisplayName("Подтверждение визита выполняется сервисом визитов")
    @Test
    void visitConfirmDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.visitConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitConfirm("b1", "sp1", visit));
    }

    @DisplayName("Подтверждение визита по идентификатору загружает данные и передает их сервису")
    @Test
    void visitConfirmByIdDelegates() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        when(controller.visitService.visitConfirm("b1", "sp1", visit)).thenReturn(visit);

        assertEquals(visit, controller.visitConfirm("b1", "sp1", "v1"));
        verify(controller.visitService).getVisit("b1", "v1");
    }

    @DisplayName("Отключение автоматического вызова точки выполняется сервисом визитов")
    @Test
    void cancelAutoCallDelegates() {
        ServicePointController controller = controller();
        Optional<ServicePoint> expected = Optional.of(new ServicePoint("sp1", "SP"));
        when(controller.visitService.cancelAutoCallModeOfServicePoint("b1", "sp1"))
            .thenReturn(expected);

        assertEquals(expected, controller.cancelAutoCallModeOfServicePoint("b1", "sp1"));
    }

    @DisplayName("Включение автоматического вызова охватывает отделение и конкретную точку")
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

    @DisplayName("Запрос исходов выполняется сервисом отделений")
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

    @DisplayName("Запрос исходов возвращает 404 при отсутствии услуги")
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

    @DisplayName("Вызов визита по стратегии максимального времени делегируется сервису")
    @Test
    void visitCallMaxLifeTimeDelegatesToService() {
        ServicePointController controller = controller();
        Optional<Visit> expected = Optional.of(Visit.builder().id("visit-1").build());
        when(controller.visitService.visitCallWithMaxLifeTime("b1", "sp1")).thenReturn(expected);

        Optional<Visit> actual = controller.visitCallMaxLifeTime("b1", "sp1");

        assertSame(expected, actual);
        verify(controller.visitService).visitCallWithMaxLifeTime("b1", "sp1");
    }

    @DisplayName("Вызов визита по стратегии максимального времени для очередей выполняет сервис визитов")
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

    @DisplayName("Подтверждение визита по стратегии максимального времени делегируется сервису")
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

    @DisplayName("Подтверждение визита по стратегии максимального времени для очередей выполняет сервис визитов")
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

    @DisplayName("Получение фактической услуги фильтрует данные по идентификатору")
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

    @DisplayName("Получение фактических услуг текущей услуги выполняет сервис визитов")
    @Test
    void getDeliveredServiceOfCurrentServiceDelegatesToVisitService() {
        ServicePointController controller = controller();
        Map<String, DeliveredService> delivered = Map.of("ds1", new DeliveredService("ds1", "Delivered"));
        when(controller.visitService.getDeliveredServices("b1", "sp1")).thenReturn(delivered);

        Map<String, DeliveredService> result = controller.getDeliveredServiceOfCurrentService("b1", "sp1");

        assertSame(delivered, result);
        verify(controller.visitService).getDeliveredServices("b1", "sp1");
    }

    @DisplayName("Запрос услуг по рабочему профилю выполняет сервис отделений")
    @Test
    void getServicesByWorkProfileIdDelegatesToBranchService() {
        ServicePointController controller = controller();
        List<Service> services = List.of(new Service("s1", "Service", 1, "queue-2"));
        when(controller.branchService.getServicesByWorkProfileId("b1", "wp1")).thenReturn(services);

        List<Service> result = controller.getServicesByWorkProfileId("b1", "wp1");

        assertSame(services, result);
        verify(controller.branchService).getServicesByWorkProfileId("b1", "wp1");
    }

    @DisplayName("Запрос услуг по очереди выполняет сервис отделений")
    @Test
    void getServicesByQueueIdDelegatesToBranchService() {
        ServicePointController controller = controller();
        List<Service> services = List.of(new Service("s1", "Service", 1, "queue-3"));
        when(controller.branchService.getServicesByQueueId("b1", "q1")).thenReturn(services);

        List<Service> result = controller.getServicesByQueueId("b1", "q1");

        assertSame(services, result);
        verify(controller.branchService).getServicesByQueueId("b1", "q1");
    }

    @DisplayName("Получение фактических услуг отделения выполняет сервис отделений")
    @Test
    void getDeliveredServicesByBranchIdDelegatesToBranchService() {
        ServicePointController controller = controller();
        List<DeliveredService> delivered = List.of(new DeliveredService("ds1", "Delivered"));
        when(controller.branchService.getDeliveredServicesByBranchId("b1")).thenReturn(delivered);

        List<DeliveredService> result = controller.getDeliveredServicesByBranchId("b1");

        assertSame(delivered, result);
        verify(controller.branchService).getDeliveredServicesByBranchId("b1");
    }

    @DisplayName("Добавление фактической услуги выполняет сервис визитов")
    @Test
    void addDeliveredServiceDelegatesToVisitService() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v-delivered").build();
        when(controller.visitService.addDeliveredService("b1", "sp1", "ds1")).thenReturn(visit);

        Visit result = controller.addDeliveredService("b1", "sp1", "ds1");

        assertSame(visit, result);
        verify(controller.visitService).addDeliveredService("b1", "sp1", "ds1");
    }

    @DisplayName("Пакетное добавление услуг выполняется последовательными вызовами сервиса визитов")
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

    @DisplayName("Удаление найденного визита выполняется сервисом визитов")
    @Test
    void deleteVisitRemovesVisitWhenFound() {
        ServicePointController controller = controller();
        Visit visit = Visit.builder().id("v1").build();
        HashMap<String, Visit> visits = new HashMap<>(Map.of("v1", visit));
        when(controller.visitService.getAllVisits("b1")).thenReturn(visits);

        controller.deleteVisit("b1", "v1");

        verify(controller.visitService).deleteVisit(visit);
    }

    @DisplayName("Удаление визита возвращает 404 при отсутствии записи")
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
