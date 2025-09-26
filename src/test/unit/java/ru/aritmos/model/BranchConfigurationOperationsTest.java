package ru.aritmos.model;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.User;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.VisitService;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Дополнительные проверки конфигурационных операций {@link Branch}.
 */
@Slf4j
@ExtendWith(TestLoggingExtension.class)
class BranchConfigurationOperationsTest {

    @DisplayName("Обновление визита произвольным действием размещает сущности и отправляет события")
    @Test
    void updateVisitWithCustomActionPlacesEntitiesAndSendsEvents() {
        log.info("Формируем отделение с очередью, пулом точек и сотрудниками");
        Branch branch = new Branch("b-config", "Отделение конфигурации");

        Queue queue = new Queue("queue-main", "Основная очередь", "A", 1);
        Visit queueVisitHead = Visit.builder().id("visit-head").build();
        Visit queueVisitTail = Visit.builder().id("visit-tail").build();
        queue.getVisits().addAll(List.of(queueVisitHead, queueVisitTail));
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint mainPoint = new ServicePoint("sp-main", "Основная точка");
        Visit legacyVisit = Visit.builder().id("legacy").build();
        mainPoint.setVisit(legacyVisit);
        branch.getServicePoints().put(mainPoint.getId(), mainPoint);

        ServicePoint poolPoint = new ServicePoint("sp-pool", "Пул точки");
        User poolPointUser = new User();
        poolPointUser.setId("user-pool-point");
        poolPointUser.setName("Сотрудник пула точки");
        poolPoint.setUser(poolPointUser);
        poolPoint.getVisits().add(Visit.builder().id("pool-existing").build());
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        ServicePoint userPoolPoint = new ServicePoint("sp-user", "Пул сотрудника");
        User poolUser = new User();
        poolUser.setId("user-pool");
        poolUser.setName("Сотрудник пула");
        poolUser.getVisits().add(Visit.builder().id("user-existing").build());
        userPoolPoint.setUser(poolUser);
        branch.getServicePoints().put(userPoolPoint.getId(), userPoolPoint);

        Visit visit = Visit.builder()
                .id("visit-new")
                .branchId(branch.getId())
                .queueId(queue.getId())
                .servicePointId(mainPoint.getId())
                .poolServicePointId(poolPoint.getId())
                .poolUserId(poolUser.getId())
                .events(new ArrayList<>())
                .visitEvents(new ArrayList<>())
                .servedServices(new ArrayList<>())
                .unservedServices(new ArrayList<>())
                .build();

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        log.info("Вызываем updateVisit с произвольным действием");
        branch.updateVisit(visit, eventService, "CUSTOM_ACTION", visitService);

        assertSame(visit, branch.getServicePoints().get(mainPoint.getId()).getVisit());
        assertEquals(List.of(queueVisitHead.getId(), queueVisitTail.getId(), visit.getId()),
                branch.getQueues().get(queue.getId()).getVisits().stream().map(Visit::getId).toList());
        assertTrue(poolPointUser.getVisits().contains(visit));
        assertTrue(poolUser.getVisits().contains(visit));

        verify(branchService).add(branch.getId(), branch);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService, times(2)).send(anyString(), eq(false), eventCaptor.capture());
        List<Event> events = eventCaptor.getAllValues();
        assertEquals("VISIT_CUSTOM_ACTION", events.get(0).getEventType());
        assertEquals("VISIT_CUSTOM_ACTION", events.get(1).getEventType());
        assertEquals(visit.getId(), ((Visit) events.get(0).getBody()).getId());
    }

    @DisplayName("Обновление визита произвольным действием выбрасывает исключение при занятой точке")
    @Test
    void updateVisitWithCustomActionThrowsWhenPointBusy() {
        log.info("Готовим отделение с занятой точкой обслуживания");
        Branch branch = new Branch("b-config", "Отделение конфигурации");
        ServicePoint servicePoint = new ServicePoint("sp-main", "Основная точка") {
            @Override
            public void setVisit(Visit newVisit) {
                if (newVisit != null) {
                    super.setVisit(newVisit);
                }
            }
        };
        servicePoint.setVisit(Visit.builder().id("busy").build());
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit visit = Visit.builder()
                .id("visit-new")
                .servicePointId(servicePoint.getId())
                .build();

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        log.info("Пытаемся обновить визит — ожидаем конфликт");
        HttpStatusException exception = assertThrows(HttpStatusException.class,
                () -> branch.updateVisit(visit, eventService, "CUSTOM_ACTION", visitService));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @DisplayName("Обновление точек обслуживания восстанавливает состояние и публикует события")
    @Test
    void addUpdateServicePointRestoresStateAndPublishesEvents() {
        log.info("Настраиваем отделение с существующей точкой и назначенным визитом");
        Branch branch = new Branch("b-service-point", "Отделение с точками");
        ServicePoint existing = new ServicePoint("sp-existing", "Точка с визитом");
        Visit visit = Visit.builder().id("visit-existing").build();
        existing.setVisit(visit);
        User staff = new User();
        staff.setId("staff-existing");
        staff.setName("Анна Сотрудник");
        existing.setUser(staff);
        branch.getServicePoints().put(existing.getId(), existing);

        ServicePoint updated = new ServicePoint(existing.getId(), "Обновлённая точка");
        ServicePoint newcomer = new ServicePoint("sp-new", "Новая точка");
        HashMap<String, ServicePoint> payload = new HashMap<>();
        payload.put(existing.getId(), updated);
        payload.put(newcomer.getId(), newcomer);

        EventService eventService = mock(EventService.class);

        log.info("Обновляем конфигурацию точек с восстановлением визита и сотрудника");
        branch.addUpdateServicePoint(payload, true, true, eventService);

        ServicePoint restored = branch.getServicePoints().get(existing.getId());
        assertSame(visit, restored.getVisit());
        assertSame(staff, restored.getUser());
        assertSame(newcomer, branch.getServicePoints().get(newcomer.getId()));

        verify(eventService).sendChangedEvent(eq("config"), eq(false), eq(existing), eq(updated), anyMap(), eq("Update service point"));
        verify(eventService).sendChangedEvent(eq("config"), eq(false), isNull(), eq(newcomer), anyMap(), eq("Add service point"));
    }

    @DisplayName("Обновление очередей восстанавливает визиты и уведомляет потребителей")
    @Test
    void addUpdateQueuesRestoresVisitsAndNotifiesConsumers() {
        log.info("Готовим отделение с очередями и точкой обслуживания");
        Branch branch = new Branch("b-queues", "Очередное отделение");
        Queue existingQueue = new Queue("queue-existing", "Старая очередь", "A", 5);
        existingQueue.getVisits().add(Visit.builder().id("v-old").build());
        existingQueue.setTicketCounter(42);
        branch.getQueues().put(existingQueue.getId(), existingQueue);
        ServicePoint mirroredPoint = new ServicePoint(existingQueue.getId(), "Связанная точка");
        branch.getServicePoints().put(mirroredPoint.getId(), mirroredPoint);

        Queue updatedQueue = new Queue(existingQueue.getId(), "Обновлённая очередь", "A", 5);
        Queue newQueue = new Queue("queue-new", "Новая очередь", "B", 3);
        HashMap<String, Queue> payload = new HashMap<>();
        payload.put(existingQueue.getId(), updatedQueue);
        payload.put(newQueue.getId(), newQueue);

        EventService eventService = mock(EventService.class);

        log.info("Обновляем набор очередей с восстановлением списка визитов");
        branch.addUpdateQueues(payload, true, eventService);

        Queue restoredQueue = branch.getQueues().get(existingQueue.getId());
        assertSame(updatedQueue, restoredQueue);
        assertEquals(42, restoredQueue.getTicketCounter());
        assertEquals(existingQueue.getVisits().stream().map(Visit::getId).toList(),
                restoredQueue.getVisits().stream().map(Visit::getId).toList());
        assertSame(newQueue, branch.getQueues().get(newQueue.getId()));

        verify(eventService).sendChangedEvent(eq("config"), eq(false), eq(existingQueue), eq(updatedQueue), anyMap(), eq("Update queue"));
        verify(eventService).sendChangedEvent(eq("config"), eq(false), eq(mirroredPoint), eq(updatedQueue), anyMap(), eq("Update service"));
        verify(eventService).sendChangedEvent(eq("config"), eq(false), isNull(), eq(newQueue), anyMap(), eq("Add queue"));
        verify(eventService).sendChangedEvent(eq("config"), eq(false), isNull(), eq(newQueue), anyMap(), eq("Add service"));
    }
}
