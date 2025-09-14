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

}

