package ru.aritmos.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;

/**
 * Тесты сервиса публикации конфигурации отделений.
 */
class ConfigurationTest {

    /**
     * Проверяет успешную публикацию конфигурации отделений.
     */
    @DisplayName("Создание конфигурации отделения публикует события")
    @Test
    void createBranchConfigurationPublishesEvents() {
        // создаём сервис и подставляем заглушки зависимостей
        Configuration configuration = new Configuration();
        configuration.branchService = mock(BranchService.class);
        configuration.keyCloackClient = mock(KeyCloackClient.class);
        configuration.visitService = mock(VisitService.class);
        configuration.eventService = mock(EventService.class);

        // входные данные: один новый филиал
        Map<String, Branch> branches = new HashMap<>();
        Branch branch = new Branch("b1", "Филиал");
        branches.put(branch.getId(), branch);

        // заглушки поведения сервисов
        when(configuration.branchService.branchExists("b1")).thenReturn(false);
        when(configuration.branchService.getBranches()).thenReturn(new HashMap<>());
        when(configuration.branchService.getDetailedBranches()).thenReturn(new HashMap<>(branches));

        // вызываем метод
        Map<String, Branch> result = configuration.createBranchConfiguration(branches);

        // проверяем, что событие начала публикации отправлено
        verify(configuration.eventService)
            .send(eq("stat"), eq(false), argThat((Event e) -> e.getEventType().equals("PUBLIC_STARTED")));
        // проверяем добавление филиала
        verify(configuration.branchService).add("b1", branch);
        // проверяем отправку события завершения публикации филиала
        verify(configuration.eventService)
            .send(eq("stat"), eq(false), argThat((Event e) -> e.getEventType().equals("BRANCH_PUBLIC_COMPLETE")));
        // проверяем отправку события завершения публикации
        verify(configuration.eventService)
            .send(eq("stat"), eq(false), argThat((Event e) -> e.getEventType().equals("PUBLIC_COMPLETE")));
        // возвращаемое значение соответствует детализированным филиалам
        assertEquals(branches, result);
    }

    /**
     * Проверяет откат публикации при ошибке.
     */
    @DisplayName("Создание конфигурации отделения откатывает изменения при ошибке")
    @Test
    void createBranchConfigurationRollbacksOnError() {
        // сервис с заглушками
        Configuration configuration = new Configuration();
        configuration.branchService = mock(BranchService.class);
        configuration.keyCloackClient = mock(KeyCloackClient.class);
        configuration.visitService = mock(VisitService.class);
        configuration.eventService = mock(EventService.class);

        // существующий филиал для восстановления
        Map<String, Branch> existing = new HashMap<>();
        Branch oldBranch = new Branch("b0", "Старый");
        existing.put(oldBranch.getId(), oldBranch);

        // новый филиал вызывает ошибку при добавлении
        Map<String, Branch> branches = Map.of("b1", new Branch("b1", "Новый"));

        when(configuration.branchService.getBranches()).thenReturn(new HashMap<>(existing));
        when(configuration.branchService.getDetailedBranches()).thenReturn(new HashMap<>(existing));
        when(configuration.branchService.branchExists("b1")).thenReturn(false);
        when(configuration.branchService.branchExists("b0")).thenReturn(true);
        doThrow(new RuntimeException("boom"))
            .when(configuration.branchService)
            .add(eq("b1"), any());

        configuration.createBranchConfiguration(branches);

        // проверяем отправку событий ошибки и отката
        verify(configuration.eventService)
            .send(eq("stat"), eq(false), argThat((Event e) -> e.getEventType().equals("PUBLIC_FAILED")));
        verify(configuration.eventService)
            .send(eq("stat"), eq(false), argThat((Event e) -> e.getEventType().equals("ROLLBACK_PUBLIC_STARTED")));
        verify(configuration.eventService)
            .send(eq("stat"), eq(false), argThat((Event e) -> e.getEventType().equals("ROLLBACK_COMPLETE")));
    }


    /**
     * Создаёт демонстрационное отделение с заполненными данными.
     */
    @DisplayName("Создание демо-отделения формирует образец")
    @Test
    void createDemoBranchBuildsSample() {
        // конфигурация с заглушкой клиента Keycloak
        Configuration configuration = new Configuration();
        configuration.keyCloackClient = mock(KeyCloackClient.class);
        configuration.branchService = mock(BranchService.class);
        when(configuration.keyCloackClient.getBranchPathByBranchPrefix("REGION", "TVR"))
            .thenReturn("/demo");

        // вызываем метод
        Map<String, Branch> result = configuration.createDemoBranch();

        // карта содержит основной демо-филиал и дополнительные
        assertTrue(result.size() >= 5);
        Branch branch = result.get("37493d1c-8282-4417-a729-dceac1f3e2b4");
        assertNotNull(branch);
        assertEquals("Клиника на Тверской", branch.getName());
        assertEquals("TVR", branch.getPrefix());
        assertEquals("/demo", branch.getPath());
        assertTrue(branch.getMarks().containsKey("04992364-9e96-4ec9-8a05-923766aa57e7"));
        // в процессе создаются дополнительные отделения
        verify(configuration.branchService)
            .add(eq("e73601bd-2fbb-4303-9a58-16cbc4ad6ad3"), any());
    }
}

