package ru.aritmos.model;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.aritmos.test.LoggingAssertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.VisitService;

/**
 * Проверяет перегрузки {@link Branch#updateVisit} с точной фиксацией поведения.
 */
class BranchUpdateVisitOverloadsTest {

    private static final Logger log = LoggerFactory.getLogger(BranchUpdateVisitOverloadsTest.class);

    @DisplayName("Обновление визита с флагом размещает запись в начале очереди")
    @Test
    void updateVisitWithBooleanPlacesVisitAtStart() {
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getParameters().clear();
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.dateTime = null;

        String branchId = "branch-start";
        log.info("Подготавливаем отделение {} для размещения визита в начале.", branchId);
        Branch branch = new Branch(branchId, "Отделение старт");
        Queue queue = new Queue("queue-start", "Очередь", "Q", 1);
        Visit queueHead = Visit.builder().id("queue-head").build();
        Visit queueTail = Visit.builder().id("queue-tail").build();
        queue.getVisits().addAll(List.of(queueHead, queueTail));
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint poolPoint = new ServicePoint("sp-start", "Пул точки");
        Visit poolExisting = Visit.builder().id("pool-existing").build();
        poolPoint.getVisits().add(poolExisting);
        User poolStaff = new User("pool-staff-start", null);
        Visit userExisting = Visit.builder().id("user-existing").build();
        poolStaff.getVisits().add(userExisting);
        poolPoint.setUser(poolStaff);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        Visit visit = Visit.builder()
                .id("visit-start")
                .branchId(branchId)
                .queueId(queue.getId())
                .poolServicePointId(poolPoint.getId())
                .poolUserId(poolStaff.getId())
                .visitEvents(new ArrayList<>(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE)))
                .events(new ArrayList<>())
                .build();

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        log.info("Вызываем updateVisit с указанием размещения визита в начале списка.");
        branch.updateVisit(visit, eventService, VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, visitService, true);

        log.info("Проверяем обновленный статус и расположение визита.");
        assertEquals(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getState().name(), visit.getStatus());
        assertEquals(
                List.of("visit-start", "queue-head", "queue-tail"),
                queue.getVisits().stream().map(Visit::getId).toList());
        assertEquals(
                List.of("visit-start", "pool-existing"),
                poolPoint.getVisits().stream().map(Visit::getId).toList());
        assertEquals(
                List.of("visit-start", "user-existing"),
                poolStaff.getVisits().stream().map(Visit::getId).toList());
        assertEquals(poolStaff, branch.getUsers().get(poolStaff.getName()));

        verify(visitService).addEvent(visit, VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, eventService);
        verify(branchService).add(branchId, branch);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), eventCaptor.capture());
        assertEquals("VISIT_TRANSFER_TO_SERVICE_POINT_POOL", eventCaptor.getValue().getEventType());
        assertEquals("visit-start", ((Visit) eventCaptor.getValue().getBody()).getId());
        verify(eventService).send(
                eq("stat"),
                eq(false),
                argThat(evt -> "VISIT_TRANSFER_TO_SERVICE_POINT_POOL".equals(evt.getEventType())));
        verify(eventService).send(
                eq("frontend"),
                eq(false),
                argThat(evt -> "VISIT_TRANSFER_TO_SERVICE_POINT_POOL".equals(evt.getEventType())));
    }

    @DisplayName("Обновление визита с флагом помещает запись в конец очереди")
    @Test
    void updateVisitWithBooleanPlacesVisitAtEnd() {
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getParameters().clear();
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.dateTime = null;

        log.info("Подготавливаем отделение для проверки добавления визита в конец.");
        Branch branch = new Branch("branch-end", "Отделение конец");
        Queue queue = new Queue("queue-end", "Очередь", "Q", 1);
        Visit queueFirst = Visit.builder().id("queue-first").build();
        Visit queueSecond = Visit.builder().id("queue-second").build();
        queue.getVisits().addAll(List.of(queueFirst, queueSecond));
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint poolPoint = new ServicePoint("sp-end", "Пул конца");
        Visit poolFirst = Visit.builder().id("pool-first").build();
        poolPoint.getVisits().add(poolFirst);
        User poolStaff = new User("pool-staff-end", null);
        Visit userFirst = Visit.builder().id("user-first").build();
        Visit userSecond = Visit.builder().id("user-second").build();
        poolStaff.getVisits().addAll(List.of(userFirst, userSecond));
        poolPoint.setUser(poolStaff);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        Visit visit = Visit.builder()
                .id("visit-end")
                .branchId(branch.getId())
                .queueId(queue.getId())
                .poolServicePointId(poolPoint.getId())
                .poolUserId(poolStaff.getId())
                .visitEvents(new ArrayList<>(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE)))
                .events(new ArrayList<>())
                .build();

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        log.info("Вызываем updateVisit с размещением визита в конец.");
        branch.updateVisit(visit, eventService, VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, visitService, false);

        log.info("Проверяем, что визит оказался в хвосте очереди и пула.");
        assertEquals(
                List.of("queue-first", "queue-second", "visit-end"),
                queue.getVisits().stream().map(Visit::getId).toList());
        assertEquals(
                List.of("pool-first", "visit-end"),
                poolPoint.getVisits().stream().map(Visit::getId).toList());
        assertEquals(
                List.of("user-first", "user-second", "visit-end"),
                poolStaff.getVisits().stream().map(Visit::getId).toList());
        assertEquals(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getState().name(), visit.getStatus());
        assertEquals(poolStaff, branch.getUsers().get(poolStaff.getName()));

        verify(visitService).addEvent(visit, VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, eventService);
        verify(branchService).add(branch.getId(), branch);
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(eventService).send(eq("stat"), eq(false), any(Event.class));
        verify(eventService).send(eq("frontend"), eq(false), any(Event.class));
    }

    @DisplayName("Обновление визита без флага по умолчанию ставит запись в начало")
    @Test
    void updateVisitWithoutBooleanPlacesVisitAtStartByDefault() {
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getParameters().clear();
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.dateTime = null;

        log.info("Проверяем дефолтное поведение перегрузки без параметра isToStart.");
        Branch branch = new Branch("branch-default", "Отделение по умолчанию");
        Queue queue = new Queue("queue-default", "Очередь", "Q", 1);
        Visit queueTail = Visit.builder().id("queue-tail").build();
        queue.getVisits().add(queueTail);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint poolPoint = new ServicePoint("sp-default", "Пул по умолчанию");
        User poolStaff = new User("pool-staff-default", null);
        poolPoint.setUser(poolStaff);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        Visit visit = Visit.builder()
                .id("visit-default")
                .branchId(branch.getId())
                .queueId(queue.getId())
                .poolServicePointId(poolPoint.getId())
                .poolUserId(poolStaff.getId())
                .visitEvents(new ArrayList<>(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE)))
                .events(new ArrayList<>())
                .build();

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        log.info("Вызываем updateVisit без явного указания позиции.");
        branch.updateVisit(visit, eventService, VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, visitService);

        log.info("Убеждаемся, что визит добавлен в начало.");
        assertEquals(
                List.of("visit-default", "queue-tail"),
                queue.getVisits().stream().map(Visit::getId).toList());
        assertEquals(
                List.of("visit-default"),
                poolPoint.getVisits().stream().map(Visit::getId).toList());
        assertEquals(
                List.of("visit-default"),
                poolStaff.getVisits().stream().map(Visit::getId).toList());
        assertEquals(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getState().name(), visit.getStatus());
        assertEquals(poolStaff, branch.getUsers().get(poolStaff.getName()));

        verify(visitService).addEvent(visit, VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, eventService);
        verify(branchService).add(branch.getId(), branch);
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(eventService).send(eq("stat"), eq(false), any(Event.class));
        verify(eventService).send(eq("frontend"), eq(false), any(Event.class));
    }
}
