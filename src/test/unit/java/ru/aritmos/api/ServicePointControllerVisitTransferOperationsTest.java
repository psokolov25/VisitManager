package ru.aritmos.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты сценариев перевода визитов, покрывающие наименее проверенные методы
 * {@link ServicePointController}.
 */
@ExtendWith(TestLoggingExtension.class)
class ServicePointControllerVisitTransferOperationsTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePointControllerVisitTransferOperationsTest.class);

    private ServicePointController controllerWith(
        BranchService branchService, VisitService visitService, EventService eventService) {
        ServicePointController controller = new ServicePointController();
        controller.services = mock(Services.class);
        controller.branchService = branchService;
        controller.visitService = visitService;
        controller.eventService = eventService;
        controller.keyCloackClient = mock(KeyCloackClient.class);
        return controller;
    }

    @Test
    void visitTransferDelegatesToServiceWhenQueueExists() {
        LOG.info("Шаг 1: настраиваем отделение с очередью и подготовим заглушки сервисов");
        BranchService branchService = mock(BranchService.class);
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-1", "Отделение");
        branch.setQueues(new HashMap<>());
        branch.getQueues().put("queue-1", new Queue("queue-1", "Основная", "A", 60));
        when(branchService.getBranch("branch-1")).thenReturn(branch);
        Visit expected = Visit.builder().id("visit-1").build();
        when(visitService.visitTransfer("branch-1", "sp-1", "queue-1", true, 15L)).thenReturn(expected);

        LOG.info("Шаг 2: вызываем контроллер для перевода визита");
        ServicePointController controller = controllerWith(branchService, visitService, eventService);
        Visit actual = controller.visitTransfer("branch-1", "sp-1", "queue-1", true, 15L);

        LOG.info("Шаг 3: убеждаемся, что результат возвращён сервисом и события не публиковались");
        assertSame(expected, actual);
        verify(branchService).getBranch("branch-1");
        verify(visitService).visitTransfer("branch-1", "sp-1", "queue-1", true, 15L);
        verifyNoInteractions(eventService);
    }

    @Test
    void visitTransferThrowsNotFoundWhenQueueMissing() {
        LOG.info("Шаг 1: создаём отделение без нужной очереди");
        BranchService branchService = mock(BranchService.class);
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-1", "Отделение");
        branch.setQueues(new HashMap<>());
        when(branchService.getBranch("branch-1")).thenReturn(branch);

        LOG.info("Шаг 2: выполняем перевод визита и ожидаем ошибку 404");
        ServicePointController controller = controllerWith(branchService, visitService, eventService);
        HttpStatusException exception =
            assertThrows(HttpStatusException.class, () -> controller.visitTransfer("branch-1", "sp-1", "queue-404", true, 0L));

        LOG.info("Шаг 3: проверяем статус и публикацию бизнес-события");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }

    @Test
    void visitBackToServicePointPoolDelegatesWhenPoolExists() {
        LOG.info("Шаг 1: настраиваем отделение с пулом точки обслуживания");
        BranchService branchService = mock(BranchService.class);
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-2", "Отделение");
        branch.setServicePoints(new HashMap<>());
        branch.getServicePoints().put("pool-1", new ServicePoint("pool-1", "Окно 1"));
        when(branchService.getBranch("branch-2")).thenReturn(branch);
        Visit expected = Visit.builder().id("visit-2").build();
        when(visitService.visitBackToServicePointPool("branch-2", "sp-1", "pool-1", 30L)).thenReturn(expected);

        LOG.info("Шаг 2: возвращаем визит в пул через контроллер");
        ServicePointController controller = controllerWith(branchService, visitService, eventService);
        Visit actual = controller.visitBackToServicePointPool("branch-2", "sp-1", "pool-1", 30L);

        LOG.info("Шаг 3: подтверждаем делегирование и отсутствие ошибок");
        assertSame(expected, actual);
        verify(branchService).getBranch("branch-2");
        verify(visitService).visitBackToServicePointPool("branch-2", "sp-1", "pool-1", 30L);
        verifyNoInteractions(eventService);
    }

    @Test
    void visitBackToServicePointPoolFailsWhenPoolMissing() {
        LOG.info("Шаг 1: готовим отделение без подходящего пула");
        BranchService branchService = mock(BranchService.class);
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-2", "Отделение");
        branch.setServicePoints(new HashMap<>());
        when(branchService.getBranch("branch-2")).thenReturn(branch);

        LOG.info("Шаг 2: вызываем метод и ловим исключение 404");
        ServicePointController controller = controllerWith(branchService, visitService, eventService);
        HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.visitBackToServicePointPool("branch-2", "sp-1", "pool-missing", 10L));

        LOG.info("Шаг 3: проверяем статус и отправку события об ошибке");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }

    @Test
    void visitTransferToServicePointPoolDelegatesWhenPoolExists() {
        LOG.info("Шаг 1: подготавливаем отделение с доступным пулом");
        BranchService branchService = mock(BranchService.class);
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-3", "Отделение");
        branch.setServicePoints(new HashMap<>());
        branch.getServicePoints().put("pool-2", new ServicePoint("pool-2", "Окно 2"));
        when(branchService.getBranch("branch-3")).thenReturn(branch);
        Visit expected = Visit.builder().id("visit-3").build();
        when(visitService.visitTransferToServicePointPool("branch-3", "sp-2", "pool-2", 0L)).thenReturn(expected);

        LOG.info("Шаг 2: переводим визит в пул через контроллер");
        ServicePointController controller = controllerWith(branchService, visitService, eventService);
        Visit actual = controller.visitTransferToServicePointPool("branch-3", "sp-2", "pool-2", 0L);

        LOG.info("Шаг 3: удостоверяемся в корректной работе");
        assertSame(expected, actual);
        verify(branchService).getBranch("branch-3");
        verify(visitService).visitTransferToServicePointPool("branch-3", "sp-2", "pool-2", 0L);
        verifyNoInteractions(eventService);
    }

    @Test
    void visitTransferToServicePointPoolFailsWhenPoolMissing() {
        LOG.info("Шаг 1: формируем отделение без требуемого пула");
        BranchService branchService = mock(BranchService.class);
        VisitService visitService = mock(VisitService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-3", "Отделение");
        branch.setServicePoints(new HashMap<>());
        when(branchService.getBranch("branch-3")).thenReturn(branch);

        LOG.info("Шаг 2: выполняем перевод и фиксируем ошибку 404");
        ServicePointController controller = controllerWith(branchService, visitService, eventService);
        HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransferToServicePointPool("branch-3", "sp-2", "pool-missing", 5L));

        LOG.info("Шаг 3: проверяем публикацию события ошибки и отсутствие вызова сервиса визитов");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }
}
