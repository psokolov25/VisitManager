package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Набор тестов для {@link VisitService#visitPostPone(String, String)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceVisitPostPoneTest {

    private static final Logger LOG = LoggerFactory.getLogger(VisitServiceVisitPostPoneTest.class);

    @DisplayName("проверяется сценарий «visit post pone returns visit to user pool»")
    @Test
    void visitPostPoneReturnsVisitToUserPool() {
        LOG.info("Шаг 1: подготавливаем отделение с оператором и активным визитом");
        Branch branch = new Branch("b1", "Центральное отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно №1");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Анна Оператор");
        servicePoint.setUser(operator);
        Visit visit = Visit.builder().id("visit-1").build();
        servicePoint.setVisit(visit);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        LOG.info("Шаг 2: настраиваем зависимости VisitService через мок BranchService");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        LOG.info("Шаг 3: создаём spy VisitService и подменяем возврат в пул пользователя");
        VisitService service = spy(new VisitService());
        service.branchService = branchService;
        service.eventService = eventService;
        Visit expected = Visit.builder().id("visit-1").build();
        doReturn(expected)
                .when(service)
                .visitBackToUserPool("b1", "sp1", "user-1", 0L);

        LOG.info("Шаг 4: вызываем visitPostPone и убеждаемся, что визит возвращён через visitBackToUserPool");
        Visit result = service.visitPostPone("b1", "sp1");
        assertSame(expected, result);
        verify(service).visitBackToUserPool("b1", "sp1", "user-1", 0L);
        verify(branchService).getBranch("b1");
        verifyNoInteractions(eventService);
    }

    @DisplayName("проверяется сценарий «visit post pone throws when visit missing»")
    @Test
    void visitPostPoneThrowsWhenVisitMissing() {
        LOG.info("Шаг 1: формируем отделение с точкой обслуживания без назначенного визита");
        Branch branch = new Branch("b1", "Центральное отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно №1");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Анна Оператор");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        LOG.info("Шаг 2: инициализируем сервис с моками зависимостей");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        LOG.info("Шаг 3: ожидаем HttpStatusException при отсутствии визита");
        HttpStatusException exception =
                assertThrows(HttpStatusException.class, () -> service.visitPostPone("b1", "sp1"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        LOG.info("Шаг 4: проверяем публикацию события BUSINESS_ERROR через EventService");
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), eventCaptor.capture());
        assertEquals("BUSINESS_ERROR", eventCaptor.getValue().getEventType());
    }

    @DisplayName("проверяется сценарий «visit post pone throws when user missing»")
    @Test
    void visitPostPoneThrowsWhenUserMissing() {
        LOG.info("Шаг 1: создаём отделение с визитом, но без назначенного оператора");
        Branch branch = new Branch("b1", "Центральное отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно №1");
        Visit visit = Visit.builder().id("visit-1").build();
        servicePoint.setVisit(visit);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        LOG.info("Шаг 2: конфигурируем сервис и ожидаем ошибку BusinessException");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        LOG.info("Шаг 3: проверяем, что метод сообщает об отсутствии оператора");
        HttpStatusException exception =
                assertThrows(HttpStatusException.class, () -> service.visitPostPone("b1", "sp1"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User does not exist", exception.getMessage());

        LOG.info("Шаг 4: убеждаемся, что событие ошибки отправлено");
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), eventCaptor.capture());
        assertEquals("BUSINESS_ERROR", eventCaptor.getValue().getEventType());
    }
}