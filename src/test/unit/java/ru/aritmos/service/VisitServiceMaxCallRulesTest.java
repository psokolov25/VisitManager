package ru.aritmos.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты максимальных стратегий вызова визитов {@link VisitService}.
 */
@Slf4j
@ExtendWith(TestLoggingExtension.class)
class VisitServiceMaxCallRulesTest {

    @DisplayName("Вызов по максимальному ожиданию использует результат правила")
    @Test
    void visitCallWithMaximalWaitingTimeUsesRuleResult() {
        log.info("Готовим отделение и точку обслуживания с оператором для сценария максимального ожидания");
        Branch branch = new Branch("b-max-wait", "Главное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-main", "Окно №1");
        User operator = new User();
        operator.setId("staff-001");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-main");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit waitingVisit = Visit.builder()
                .id("visit-wait")
                .branchId(branch.getId())
                .parameterMap(new HashMap<>())
                .build();
        Visit limitedQueueVisit = Visit.builder()
                .id("visit-queue")
                .branchId(branch.getId())
                .parameterMap(new HashMap<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule waitingRule = mock(CallRule.class);
        CallRule lifeRule = mock(CallRule.class);
        when(waitingRule.call(branch, servicePoint)).thenReturn(Optional.of(waitingVisit));
        List<String> queueIds = List.of("q-1", "q-2");
        when(waitingRule.call(branch, servicePoint, queueIds)).thenReturn(Optional.of(limitedQueueVisit));

        VisitService service = spy(new VisitService());
        service.branchService = branchService;
        service.eventService = eventService;
        service.waitingTimeCallRule = waitingRule;
        service.lifeTimeCallRule = lifeRule;
        doAnswer(invocation -> Optional.of(invocation.getArgument(2)))
                .when(service)
                .visitCall(anyString(), anyString(), any(Visit.class), anyString());

        log.info("Запускаем вызов без ограничения очередей");
        Optional<Visit> firstResult = service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId());
        assertTrue(firstResult::isPresent, "Ожидаем, что правило вернуло кандидата на вызов");
        assertSame(waitingVisit, firstResult.orElseThrow());

        log.info("Повторяем вызов с ограничением по списку очередей {}", queueIds);
        Optional<Visit> secondResult = service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId(), queueIds);
        assertTrue(secondResult::isPresent, "Визит из ограниченного набора должен быть найден");
        assertSame(limitedQueueVisit, secondResult.orElseThrow());

        verify(waitingRule).call(branch, servicePoint);
        verify(waitingRule).call(branch, servicePoint, queueIds);
        verify(service, times(2)).visitCall(eq(branch.getId()), eq(servicePoint.getId()), any(Visit.class), eq("callNext"));
    }

    @DisplayName("Вызов по максимальному ожиданию включает автодовызов при пустом ответе правила")
    @Test
    void visitCallWithMaximalWaitingTimeEnablesAutocallWhenRuleReturnsEmpty() {
        log.info("Настраиваем отделение в режиме авто-вызова для проверки перехода в автодовызов");
        Branch branch = new Branch("b-auto-wait", "Отделение с авто-вызовом");
        branch.getParameterMap().put("autoCallMode", true);
        ServicePoint servicePoint = new ServicePoint("sp-auto", "Авто окно");
        User operator = new User();
        operator.setId("staff-777");
        operator.setName("Пётр Автовызов");
        operator.setCurrentWorkProfileId("wp-auto");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule waitingRule = mock(CallRule.class);
        when(waitingRule.call(branch, servicePoint)).thenReturn(Optional.empty());
        List<String> queueIds = List.of("priority-1");
        when(waitingRule.call(branch, servicePoint, queueIds)).thenReturn(Optional.empty());

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.waitingTimeCallRule = waitingRule;
        service.lifeTimeCallRule = mock(CallRule.class);

        log.info("Запускаем вызов без очередных ограничений: ожидаем исключение со статусом 207");
        HttpStatusException firstException = assertThrows(
                HttpStatusException.class,
                () -> service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId()));
        assertEquals(HttpStatus.MULTI_STATUS, firstException.getStatus());
        assertTrue(servicePoint.getAutoCallMode(), "Флаг автодовызова должен включиться после первой попытки");

        log.info("Повторно запускаем сценарий для варианта с конкретным набором очередей {}", queueIds);
        servicePoint.setAutoCallMode(false);
        HttpStatusException secondException = assertThrows(
                HttpStatusException.class,
                () -> service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId(), queueIds));
        assertEquals(HttpStatus.MULTI_STATUS, secondException.getStatus());
        assertTrue(servicePoint.getAutoCallMode(), "Флаг автодовызова должен включиться и во втором сценарии");

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService, times(2)).send(eq("frontend"), eq(false), eventCaptor.capture());
        for (Event event : eventCaptor.getAllValues()) {
            assertEquals("SERVICEPOINT_AUTOCALL_MODE_TURN_ON", event.getEventType());
            Map<String, String> params = event.getParams();
            assertEquals(branch.getId(), params.get("branchId"));
            assertEquals(servicePoint.getId(), params.get("servicePointId"));
            assertSame(servicePoint, event.getBody());
        }
        verify(branchService, times(2)).add(branch.getId(), branch);
    }

    @DisplayName("Вызов по максимальному ожиданию возвращает 404 при отсутствии точки обслуживания")
    @Test
    void visitCallWithMaximalWaitingTimeFailsWhenServicePointMissing() {
        log.info("Проверяем сценарий, когда точка обслуживания отсутствует в конфигурации отделения");
        Branch branch = new Branch("b-missing-wait", "Отделение без точки");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);
        service.waitingTimeCallRule = mock(CallRule.class);
        service.lifeTimeCallRule = mock(CallRule.class);

        log.info("Вызываем метод без ограничений очередей и ожидаем ошибку 404");
        HttpStatusException first = assertThrows(HttpStatusException.class,
                () -> service.visitCallWithMaximalWaitingTime(branch.getId(), "absent"));
        assertEquals(HttpStatus.NOT_FOUND, first.getStatus());

        log.info("Повторяем вызов для варианта со списком очередей");
        HttpStatusException second = assertThrows(HttpStatusException.class,
                () -> service.visitCallWithMaximalWaitingTime(branch.getId(), "absent", List.of("q1")));
        assertEquals(HttpStatus.NOT_FOUND, second.getStatus());
    }

    @DisplayName("Вызов по времени жизни возвращает 404 при отсутствии точки обслуживания")
    @Test
    void visitCallWithMaxLifeTimeFailsWhenServicePointMissing() {
        log.info("Конфигурация отделения не содержит точку обслуживания, ожидаем ошибку при вызове по времени жизни");
        Branch branch = new Branch("b-missing-life", "Отделение без точек");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);
        service.lifeTimeCallRule = mock(CallRule.class);
        service.waitingTimeCallRule = mock(CallRule.class);

        log.info("Запрос без списка очередей приводит к 404");
        HttpStatusException first = assertThrows(HttpStatusException.class,
                () -> service.visitCallWithMaxLifeTime(branch.getId(), "missing"));
        assertEquals(HttpStatus.NOT_FOUND, first.getStatus());

        log.info("Проверяем поведение для варианта со списком очередей");
        HttpStatusException second = assertThrows(HttpStatusException.class,
                () -> service.visitCallWithMaxLifeTime(branch.getId(), "missing", List.of("q1")));
        assertEquals(HttpStatus.NOT_FOUND, second.getStatus());
    }

    @DisplayName("Вызов по максимальному ожиданию возвращает 403 при отсутствии авторизованного оператора")
    @Test
    void visitCallWithMaximalWaitingTimeFailsWhenUserNotLogged() {
        log.info("Создаём точку обслуживания без оператора и проверяем реакцию метода");
        Branch branch = new Branch("b-no-user", "Отделение без оператора");
        ServicePoint servicePoint = new ServicePoint("sp-free", "Свободная точка");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);
        service.waitingTimeCallRule = mock(CallRule.class);
        service.lifeTimeCallRule = mock(CallRule.class);

        log.info("Метод должен вернуть 403 при отсутствии оператора");
        HttpStatusException first = assertThrows(HttpStatusException.class,
                () -> service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId()));
        assertEquals(HttpStatus.FORBIDDEN, first.getStatus());

        log.info("Проверяем ту же ситуацию для варианта со списком очередей");
        HttpStatusException second = assertThrows(HttpStatusException.class,
                () -> service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId(), List.of("q1")));
        assertEquals(HttpStatus.FORBIDDEN, second.getStatus());
    }

    @DisplayName("Вызов по времени жизни возвращает 403 при отсутствии авторизованного оператора")
    @Test
    void visitCallWithMaxLifeTimeFailsWhenUserNotLogged() {
        log.info("Имитация точки без оператора для правила по времени жизни");
        Branch branch = new Branch("b-no-user-life", "Отделение без оператора");
        ServicePoint servicePoint = new ServicePoint("sp-free-life", "Свободная точка");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);
        service.lifeTimeCallRule = mock(CallRule.class);
        service.waitingTimeCallRule = mock(CallRule.class);

        log.info("Основной метод должен вернуть 403");
        HttpStatusException first = assertThrows(HttpStatusException.class,
                () -> service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId()));
        assertEquals(HttpStatus.FORBIDDEN, first.getStatus());

        log.info("Аналогичный результат ожидается и при указании списка очередей");
        HttpStatusException second = assertThrows(HttpStatusException.class,
                () -> service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId(), List.of("q2")));
        assertEquals(HttpStatus.FORBIDDEN, second.getStatus());
    }

    @DisplayName("Вызов по максимальному ожиданию возвращает пустой результат без автодовызова")
    @Test
    void visitCallWithMaximalWaitingTimeReturnsEmptyWhenNoAutoCallConfigured() {
        log.info("Готовим отделение без режима автодовызова и ожидаем пустой результат");
        Branch branch = new Branch("b-no-autocall", "Обычное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-regular", "Рабочее окно");
        User operator = new User();
        operator.setId("staff-regular");
        operator.setName("Иван Рабочий");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule waitingRule = mock(CallRule.class);
        when(waitingRule.call(branch, servicePoint)).thenReturn(Optional.empty());
        when(waitingRule.call(branch, servicePoint, List.of("q-empty"))).thenReturn(Optional.empty());

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.waitingTimeCallRule = waitingRule;
        service.lifeTimeCallRule = mock(CallRule.class);

        log.info("Без списка очередей метод возвращает пустой результат");
        Optional<Visit> direct = service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId());
        assertTrue(direct::isEmpty);

        log.info("С ограничением по очереди поведение аналогично");
        Optional<Visit> limited = service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId(), List.of("q-empty"));
        assertTrue(limited::isEmpty);

        verify(eventService, never()).send(anyString(), anyBoolean(), any(Event.class));
    }

    @DisplayName("Вызов по времени жизни возвращает пустой результат без автодовызова")
    @Test
    void visitCallWithMaxLifeTimeReturnsEmptyWhenNoAutoCallConfigured() {
        log.info("Отделение без автодовызова должно возвращать пустой результат при отсутствии кандидатов по времени жизни");
        Branch branch = new Branch("b-no-autocall-life", "Обычное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-life-regular", "Рабочее окно");
        User operator = new User();
        operator.setId("staff-life-regular");
        operator.setName("Мария Рабочая");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule lifeRule = mock(CallRule.class);
        when(lifeRule.call(branch, servicePoint)).thenReturn(Optional.empty());
        when(lifeRule.call(branch, servicePoint, List.of("life-empty"))).thenReturn(Optional.empty());

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.lifeTimeCallRule = lifeRule;
        service.waitingTimeCallRule = mock(CallRule.class);

        log.info("Проверяем сценарий без списка очередей");
        Optional<Visit> direct = service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId());
        assertTrue(direct::isEmpty);

        log.info("С ограничением по очередям результат также пустой");
        Optional<Visit> limited = service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId(), List.of("life-empty"));
        assertTrue(limited::isEmpty);

        verify(eventService, never()).send(anyString(), anyBoolean(), any(Event.class));
    }

    @DisplayName("Вызов по времени жизни использует результат правила")
    @Test
    void visitCallWithMaxLifeTimeUsesRuleResult() {
        log.info("Готовим отделение для сценария вызова по максимальному времени жизни визита");
        Branch branch = new Branch("b-max-life", "Отделение по времени жизни");
        ServicePoint servicePoint = new ServicePoint("sp-life", "Окно времени жизни");
        User operator = new User();
        operator.setId("staff-life");
        operator.setName("Мария Жизненная");
        operator.setCurrentWorkProfileId("wp-life");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit lifeTimeCandidate = Visit.builder()
                .id("visit-life")
                .branchId(branch.getId())
                .parameterMap(new HashMap<>())
                .build();
        Visit limitedLifeCandidate = Visit.builder()
                .id("visit-life-queue")
                .branchId(branch.getId())
                .parameterMap(new HashMap<>())
                .build();
        List<String> queueIds = List.of("life-1", "life-2");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule lifeRule = mock(CallRule.class);
        CallRule waitingRule = mock(CallRule.class);
        when(lifeRule.call(branch, servicePoint)).thenReturn(Optional.of(lifeTimeCandidate));
        when(lifeRule.call(branch, servicePoint, queueIds)).thenReturn(Optional.of(limitedLifeCandidate));

        VisitService service = spy(new VisitService());
        service.branchService = branchService;
        service.eventService = eventService;
        service.lifeTimeCallRule = lifeRule;
        service.waitingTimeCallRule = waitingRule;
        doAnswer(invocation -> Optional.of(invocation.getArgument(2)))
                .when(service)
                .visitCall(anyString(), anyString(), any(Visit.class), anyString());

        log.info("Запускаем вызов по максимальному времени жизни без ограничений очередей");
        Optional<Visit> firstResult = service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId());
        assertTrue(firstResult::isPresent, "Правило максимального времени жизни должно вернуть визит");
        assertSame(lifeTimeCandidate, firstResult.orElseThrow());

        log.info("Проверяем сценарий с ограниченным набором очередей {}");
        Optional<Visit> secondResult = service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId(), queueIds);
        assertTrue(secondResult::isPresent, "Из выбранных очередей также должен быть выбран визит");
        assertSame(limitedLifeCandidate, secondResult.orElseThrow());

        verify(lifeRule).call(branch, servicePoint);
        verify(lifeRule).call(branch, servicePoint, queueIds);
        verify(service, times(2)).visitCall(eq(branch.getId()), eq(servicePoint.getId()), any(Visit.class), eq("callNext"));
    }

    @DisplayName("Вызов по времени жизни включает автодовызов при пустом ответе правила")
    @Test
    void visitCallWithMaxLifeTimeEnablesAutocallWhenRuleReturnsEmpty() {
        log.info("Создаём отделение с авто-вызовом для проверки поведения правил по времени жизни");
        Branch branch = new Branch("b-auto-life", "Авто отделение времени жизни");
        branch.getParameterMap().put("autoCallMode", true);
        ServicePoint servicePoint = new ServicePoint("sp-auto-life", "Авто окно времени жизни");
        User operator = new User();
        operator.setId("staff-auto-life");
        operator.setName("Сергей Авто");
        operator.setCurrentWorkProfileId("wp-auto-life");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule lifeRule = mock(CallRule.class);
        CallRule waitingRule = mock(CallRule.class);
        when(lifeRule.call(branch, servicePoint)).thenReturn(Optional.empty());
        List<String> queueIds = List.of("ql-1");
        when(lifeRule.call(branch, servicePoint, queueIds)).thenReturn(Optional.empty());

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.lifeTimeCallRule = lifeRule;
        service.waitingTimeCallRule = waitingRule;

        log.info("Запускаем вызов без ограничений — ожидаем включение автодовызова");
        HttpStatusException firstException = assertThrows(
                HttpStatusException.class,
                () -> service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId()));
        assertEquals(HttpStatus.MULTI_STATUS, firstException.getStatus());
        assertTrue(servicePoint.getAutoCallMode(), "Автодовызов должен включиться после первого запроса");

        log.info("Повторяем сценарий для конкретного списка очередей {}");
        servicePoint.setAutoCallMode(false);
        HttpStatusException secondException = assertThrows(
                HttpStatusException.class,
                () -> service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId(), queueIds));
        assertEquals(HttpStatus.MULTI_STATUS, secondException.getStatus());
        assertTrue(servicePoint.getAutoCallMode(), "Автодовызов должен включиться и во втором случае");

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService, times(2)).send(eq("frontend"), eq(false), eventCaptor.capture());
        for (Event event : eventCaptor.getAllValues()) {
            assertEquals("SERVICEPOINT_AUTOCALL_MODE_TURN_ON", event.getEventType());
            Map<String, String> params = event.getParams();
            assertEquals(branch.getId(), params.get("branchId"));
            assertEquals(servicePoint.getId(), params.get("servicePointId"));
            assertSame(servicePoint, event.getBody());
        }
        verify(branchService, times(2)).add(branch.getId(), branch);
    }

    @DisplayName("стратегии вызова не включают автообзвон для занятой точки обслуживания")
    @Test
    void visitCallStrategiesDoNotTriggerAutocallForBusyServicePoint() {
        log.info("Готовим отделение с точкой, на которой уже обслуживается визит");
        Branch branch = new Branch("b-busy", "Отделение с занятым окном");
        ServicePoint servicePoint = new ServicePoint("sp-busy", "Занятая точка");
        User operator = new User();
        operator.setId("staff-busy");
        operator.setName("Галина Оператор");
        servicePoint.setUser(operator);
        servicePoint.setVisit(Visit.builder().id("active").branchId(branch.getId()).build());
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getParameterMap().put("autoCallMode", true);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule waitingRule = mock(CallRule.class);
        CallRule lifeRule = mock(CallRule.class);
        when(waitingRule.call(branch, servicePoint)).thenReturn(Optional.empty());
        when(waitingRule.call(branch, servicePoint, List.of("q-special"))).thenReturn(Optional.empty());
        when(lifeRule.call(branch, servicePoint)).thenReturn(Optional.empty());
        when(lifeRule.call(branch, servicePoint, List.of("life-special"))).thenReturn(Optional.empty());

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.waitingTimeCallRule = waitingRule;
        service.lifeTimeCallRule = lifeRule;

        log.info("Метод по времени ожидания должен вернуть пустой результат без изменения флагов");
        Optional<Visit> waitResult = service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId());
        assertTrue(waitResult::isEmpty, "Визит не должен быть подобран");
        assertFalse(Boolean.TRUE.equals(servicePoint.getAutoCallMode()));

        log.info("Повторяем вызов для варианта со списком очередей");
        Optional<Visit> waitLimited = service.visitCallWithMaximalWaitingTime(branch.getId(), servicePoint.getId(), List.of("q-special"));
        assertTrue(waitLimited::isEmpty);
        assertFalse(Boolean.TRUE.equals(servicePoint.getAutoCallMode()));

        log.info("Проверяем методы по времени жизни визита");
        Optional<Visit> lifeResult = service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId());
        assertTrue(lifeResult::isEmpty);
        Optional<Visit> lifeLimited = service.visitCallWithMaxLifeTime(branch.getId(), servicePoint.getId(), List.of("life-special"));
        assertTrue(lifeLimited::isEmpty);

        verify(branchService, never()).add(anyString(), any());
        verify(eventService, never()).send(anyString(), anyBoolean(), any(Event.class));
    }

}
