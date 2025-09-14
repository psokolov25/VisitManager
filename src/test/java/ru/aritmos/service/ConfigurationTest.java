package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
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
}

