package ru.aritmos.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.DelayedEvents;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.model.visit.VisitEventInformation;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Набор тестов для {@link VisitService#visitTransferToUserPool(String, String, String, Long)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceTransferToUserPoolTest {

    private static final Logger LOG = LoggerFactory.getLogger(VisitServiceTransferToUserPoolTest.class);

    @BeforeEach
    void resetEventParameters() {
        LOG.info("Подготовка: очищаем параметры событий");
        VisitEvent.STOP_SERVING.getParameters().clear();
        VisitEvent.TRANSFER_TO_USER_POOL.getParameters().clear();
    }

    @Test
    void visitTransferToUserPoolMovesVisitAndPublishesNotifications() {
        LOG.info("Шаг 1: формируем отделение, оператора и визит");
        Branch branch = new Branch("branch-1", "Отделение №1");
        ServicePoint servicePoint = new ServicePoint("sp-main", "Основная точка");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-1");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getUsers().put(operator.getId(), operator);

        LOG.info("Шаг 2: создаём визит с историей очередей");
        VisitEventInformation previousEvent = VisitEventInformation.builder()
                .visitEvent(VisitEvent.TRANSFER_TO_QUEUE)
                .eventDateTime(ZonedDateTime.now().minusMinutes(5))
                .parameters(Map.of("oldQueueId", "queue-history"))
                .build();

        Visit visit = Visit.builder()
                .id("visit-1")
                .branchId(branch.getId())
                .ticket("A-001")
                .servicePointId(servicePoint.getId())
                .queueId("queue-active")
                .parameterMap(new HashMap<>(Map.of(
                        "LastPoolUserId", "old-user",
                        "LastQueueId", "queue-last")))
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>(List.of(previousEvent)))
                .build();
        servicePoint.setVisit(visit);

        LOG.info("Шаг 3: настраиваем зависимости сервиса");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;

        LOG.info("Шаг 4: переводим визит в пул пользователя");
        Long transferDelay = 45L;
        Visit result = service.visitTransferToUserPool(branch.getId(), servicePoint.getId(), operator.getId(), transferDelay);

        LOG.info("Шаг 5: проверяем изменение состояния визита");
        assertSame(visit, result);
        assertNull(result.getQueueId());
        assertNull(result.getPoolServicePointId());
        assertNull(result.getServicePointId());
        assertNull(result.getStartServingDateTime());
        assertEquals(operator.getId(), result.getPoolUserId());
        assertNotNull(result.getTransferDateTime());
        assertEquals(transferDelay, result.getTransferTimeDelay());
        assertFalse(result.getParameterMap().containsKey("LastPoolUserId"));
        assertEquals("queue-last", result.getParameterMap().get("LastQueueId"));

        LOG.info("Шаг 6: анализируем отправленные события обновления визита");
        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(eq(visit), eventCaptor.capture(), eq(service));
        List<VisitEvent> capturedEvents = eventCaptor.getAllValues();
        assertEquals(2, capturedEvents.size());

        VisitEvent stopEvent = capturedEvents.get(0);
        assertSame(VisitEvent.STOP_SERVING, stopEvent);
        assertEquals("false", stopEvent.getParameters().get("isForced"));
        assertEquals(branch.getId(), stopEvent.getParameters().get("branchId"));
        assertEquals(servicePoint.getId(), stopEvent.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), stopEvent.getParameters().get("servicePointName"));
        assertEquals(operator.getId(), stopEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), stopEvent.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), stopEvent.getParameters().get("workProfileId"));

        VisitEvent transferEvent = capturedEvents.get(1);
        assertSame(VisitEvent.TRANSFER_TO_USER_POOL, transferEvent);
        assertEquals(branch.getId(), transferEvent.getParameters().get("branchId"));
        assertEquals(operator.getId(), transferEvent.getParameters().get("userId"));
        assertEquals(operator.getId(), transferEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), transferEvent.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), transferEvent.getParameters().get("workProfileId"));
        assertEquals(servicePoint.getId(), transferEvent.getParameters().get("servicePointId"));
        assertEquals("queue-history", transferEvent.getParameters().get("queueId"));

        LOG.info("Шаг 7: проверяем публикацию отложенного события для фронтенда");
        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(transferDelay), eq(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("USER_POOL_REFRESHED", delayedEvent.getEventType());
        assertEquals(Map.of("userId", operator.getId(), "branchId", branch.getId()), delayedEvent.getParams());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) delayedEvent.getBody();
        assertEquals(operator.getId(), body.get("id"));
        assertEquals(operator.getName(), body.get("name"));
        assertEquals(branch.getId(), body.get("branchId"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitTransferToUserPoolFailsWhenServicePointMissing() {
        LOG.info("Шаг 1: настраиваем отделение без нужной точки обслуживания");
        Branch branch = new Branch("branch-2", "Отделение №2");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;

        LOG.info("Шаг 2: выполняем перевод и фиксируем исключение");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                service.visitTransferToUserPool(branch.getId(), "missing-sp", "user-2", 30L));

        LOG.info("Шаг 3: проверяем параметры ошибки и отсутствие побочных эффектов");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("ServicePoint missing-sp! not exist!", exception.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), eq(service));
        verifyNoInteractions(delayedEvents);
    }

    @Test
    void visitTransferToUserPoolFailsWhenVisitMissing() {
        LOG.info("Шаг 1: создаём отделение с точкой без активного визита");
        Branch branch = new Branch("branch-3", "Отделение №3");
        ServicePoint servicePoint = new ServicePoint("sp-empty", "Свободная ТО");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;

        LOG.info("Шаг 2: ожидаем сообщение об отсутствии визита на точке");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                service.visitTransferToUserPool(branch.getId(), servicePoint.getId(), "user-3", 15L));

        LOG.info("Шаг 3: удостоверяемся, что вызовы обновления и отложенные события не выполнялись");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Visit in ServicePoint sp-empty! not exist!", exception.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), eq(service));
        verifyNoInteractions(delayedEvents);
    }

    @Test
    void visitTransferToUserPoolFailsWhenUserNotFound() {
        LOG.info("Шаг 1: готовим отделение с визитом, но без работающих операторов");
        Branch branch = new Branch("branch-4", "Отделение №4");
        ServicePoint servicePoint = new ServicePoint("sp-main", "Основная ТО");
        Visit visit = Visit.builder()
                .id("visit-4")
                .branchId(branch.getId())
                .ticket("A-404")
                .parameterMap(new HashMap<>())
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        servicePoint.setVisit(visit);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;

        LOG.info("Шаг 2: пытаемся перевести визит к несуществующему пользователю");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                service.visitTransferToUserPool(branch.getId(), servicePoint.getId(), "user-missing", 20L));

        LOG.info("Шаг 3: проверяем, что обработка ошибки не создала побочных действий");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found in branch configuration!", exception.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), eq(service));
        verifyNoInteractions(delayedEvents);
    }
}
