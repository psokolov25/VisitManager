package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты для {@link VisitService#deleteVisit(Visit)} c подробным логированием шагов и проверок.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceDeleteVisitTest {

    private static final Logger LOG = LoggerFactory.getLogger(VisitServiceDeleteVisitTest.class);

    @BeforeEach
    void resetVisitEventBefore() {
        resetVisitEvent();
    }

    @AfterEach
    void resetVisitEventAfter() {
        resetVisitEvent();
    }

    @DisplayName("deleteVisit очищает привязки визита и уведомляет отделение")
    @Test
    void deleteVisitClearsAssignmentsAndNotifiesBranch() {
        LOG.info("Шаг 1: создаём визит с привязкой к очереди и точке обслуживания");
        Visit visit = Visit.builder()
                .id("visit-success")
                .queueId("queue-1")
                .servicePointId("sp-1")
                .returnTimeDelay(0L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>())
                .build();

        LOG.info("Шаг 2: настраиваем сервис и зависимости");
        BranchService branchService = mock(BranchService.class);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        LOG.info("Шаг 3: вызываем deleteVisit и ожидаем очистку состояния визита");
        service.deleteVisit(visit);

        LOG.info("Шаг 4: проверяем, что визит отвязан от очереди и точки обслуживания");
        assertNull(visit.getQueueId());
        assertNull(visit.getServicePointId());

        LOG.info("Шаг 5: убеждаемся, что branchService получил событие удаления");
        verify(branchService).updateVisit(same(visit), same(VisitEvent.DELETED), same(service));
        assertNotNull(VisitEvent.DELETED.dateTime);
        assertTrue(VisitEvent.DELETED.getParameters().isEmpty());
        verifyNoInteractions(service.eventService);
    }

    @DisplayName("deleteVisit выбрасывает исключение, если задержка возврата не истекла")
    @Test
    void deleteVisitThrowsWhenReturnDelayNotElapsed() {
        LOG.info("Шаг 1: готовим визит, недавно вернувшийся в очередь");
        Visit visit = Visit.builder()
                .id("visit-return")
                .queueId("queue-1")
                .servicePointId("sp-1")
                .returnDateTime(ZonedDateTime.now().minusSeconds(5))
                .returnTimeDelay(3600L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>())
                .build();

        LOG.info("Шаг 2: инициализируем сервис с моками зависимостей");
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        LOG.info("Шаг 3: ожидаем ошибку при попытке удалить визит до истечения задержки возврата");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () -> service.deleteVisit(visit));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any());
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), any(VisitService.class));
        assertEquals("queue-1", visit.getQueueId());
        assertEquals("sp-1", visit.getServicePointId());
    }

    @DisplayName("deleteVisit выбрасывает исключение, если задержка перевода не истекла")
    @Test
    void deleteVisitThrowsWhenTransferDelayNotElapsed() {
        LOG.info("Шаг 1: формируем визит, недавно переведённый из очереди");
        Visit visit = Visit.builder()
                .id("visit-transfer")
                .queueId("queue-2")
                .servicePointId("sp-2")
                .transferDateTime(ZonedDateTime.now().minusSeconds(5))
                .transferTimeDelay(3600L)
                .returnTimeDelay(0L)
                .parameterMap(new HashMap<>())
                .build();

        LOG.info("Шаг 2: создаём сервис с моками BranchService и EventService");
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        LOG.info("Шаг 3: проверяем, что метод запрещает удаление визита до истечения задержки перевода");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () -> service.deleteVisit(visit));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any());
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), any(VisitService.class));
        assertEquals("queue-2", visit.getQueueId());
        assertEquals("sp-2", visit.getServicePointId());
    }

    private void resetVisitEvent() {
        VisitEvent.DELETED.getParameters().clear();
        VisitEvent.DELETED.dateTime = null;
    }
}
