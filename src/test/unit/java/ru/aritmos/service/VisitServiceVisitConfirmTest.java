package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Юнит-тесты для {@link VisitService#visitConfirm(String, String, Visit)}.
 */
@Slf4j
@ExtendWith(TestLoggingExtension.class)
class VisitServiceVisitConfirmTest {

    @BeforeEach
    void clearEventParametersBefore() {
        VisitEvent.START_SERVING.getParameters().clear();
    }

    @AfterEach
    void clearEventParametersAfter() {
        VisitEvent.START_SERVING.getParameters().clear();
    }

    @DisplayName("Подтверждение визита переводит клиента к точке обслуживания и публикует событие начала обслуживания")
    @Test
    void visitConfirmMovesVisitToServicePointAndPublishesStartServingEvent() {
        log.info("Готовим отделение и точку обслуживания для позитивного сценария подтверждения визита");
        Branch branch = new Branch("b1", "Главное отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно №1");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-1");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Service service = new Service();
        service.setId("srv-1");
        service.setName("Консультация");

        Visit visit = Visit.builder()
                .id("visit-1")
                .branchId("b1")
                .queueId("queue-42")
                .poolServicePointId("pool-sp-7")
                .poolUserId("pool-user-5")
                .userId("legacy-user")
                .parameterMap(new HashMap<>())
                .currentService(service)
                .build();
        visit.setTransferDateTime(ZonedDateTime.now().minusMinutes(5));
        visit.setReturnDateTime(ZonedDateTime.now().minusMinutes(2));

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService serviceUnderTest = new VisitService();
        serviceUnderTest.branchService = branchService;
        serviceUnderTest.eventService = eventService;

        log.info("Вызываем visitConfirm для визита {} и точки обслуживания {}", visit.getId(), servicePoint.getId());
        Visit result = serviceUnderTest.visitConfirm("b1", "sp1", visit);

        log.info("Проверяем, что визит обновлён и передан на обслуживание");
        assertSame(visit, result);
        assertEquals("START_SERVING", visit.getStatus());
        assertNotNull(visit.getStartServingDateTime());
        assertEquals("sp1", visit.getServicePointId());
        assertEquals("queue-42", visit.getParameterMap().get("LastQueueId"));
        assertNull(visit.getQueueId());
        assertEquals("pool-sp-7", visit.getParameterMap().get("LastPoolServicePointId"));
        assertEquals("pool-user-5", visit.getParameterMap().get("LastPoolUserId"));
        assertTrue(visit.getParameterMap().containsKey("LastUserId"));
        assertNull(visit.getParameterMap().get("LastUserId"));
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertNull(visit.getTransferDateTime());
        assertNull(visit.getReturnDateTime());
        assertEquals("user-1", visit.getUserId());
        assertEquals("Иван Оператор", visit.getUserName());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest));
        VisitEvent startServingEvent = eventCaptor.getValue();
        assertSame(VisitEvent.START_SERVING, startServingEvent);
        assertNotNull(startServingEvent.dateTime);

        log.info("Сравниваем параметры события START_SERVING");
        Map<String, String> params = new HashMap<>(startServingEvent.getParameters());
        assertEquals("sp1", params.get("servicePointId"));
        assertEquals("b1", params.get("branchId"));
        assertEquals("srv-1", params.get("serviceId"));
        assertEquals("Консультация", params.get("serviceName"));
        assertEquals("user-1", params.get("staffId"));
        assertEquals("Иван Оператор", params.get("staffName"));
        assertEquals("wp-1", params.get("workProfileId"));
    }

    @DisplayName("Подтверждение визита отклоняется, когда точка обслуживания уже занята")
    @Test
    void visitConfirmFailsWhenServicePointAlreadyHasVisit() {
        log.info("Готовим отделение с занятой точкой обслуживания");
        Branch branch = new Branch("b1", "Главное отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно №1");
        servicePoint.setVisit(Visit.builder().id("busy").parameterMap(new HashMap<>()).build());
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService serviceUnderTest = new VisitService();
        serviceUnderTest.branchService = branchService;
        serviceUnderTest.eventService = eventService;

        Visit visit = Visit.builder().id("visit-1").branchId("b1").parameterMap(new HashMap<>()).build();

        log.info("Ожидаем конфликт при подтверждении визита, когда точка уже занята");
        HttpStatusException exception = assertThrows(
                HttpStatusException.class, () -> serviceUnderTest.visitConfirm("b1", "sp1", visit));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), same(serviceUnderTest));
    }

    @DisplayName("Подтверждение визита завершается ошибкой 404, если точка обслуживания отсутствует")
    @Test
    void visitConfirmFailsWhenServicePointMissing() {
        log.info("Готовим отделение без нужной точки обслуживания");
        Branch branch = new Branch("b1", "Главное отделение");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService serviceUnderTest = new VisitService();
        serviceUnderTest.branchService = branchService;
        serviceUnderTest.eventService = eventService;

        Visit visit = Visit.builder().id("visit-1").branchId("b1").parameterMap(new HashMap<>()).build();

        log.info("Ожидаем ошибку 404 при попытке подтвердить визит на отсутствующую точку");
        HttpStatusException exception = assertThrows(
                HttpStatusException.class, () -> serviceUnderTest.visitConfirm("b1", "missing", visit));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), same(serviceUnderTest));
    }
}
