package ru.aritmos.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.aritmos.test.LoggingAssertions.assertEquals;
import static ru.aritmos.test.LoggingAssertions.assertSame;
import static ru.aritmos.test.LoggingAssertions.assertThrows;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/** Тесты переводов визитов в {@link ServicePointController} с подробным логированием шагов. */
@ExtendWith(TestLoggingExtension.class)
class ServicePointControllerVisitTransferTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePointControllerVisitTransferTest.class);

    private ControllerContext controllerContext() {
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);

        ServicePointController controller = new ServicePointController();
        controller.services = mock(Services.class);
        controller.branchService = branchService;
        controller.visitService = visitService;
        controller.eventService = eventService;
        controller.keyCloackClient = mock(KeyCloackClient.class);
        return new ControllerContext(controller, visitService, branchService, eventService);
    }

    private record ControllerContext(
            ServicePointController controller,
            VisitService visitService,
            BranchService branchService,
            EventService eventService) {}

    @Test
    void visitTransferSucceedsWhenQueueExists() {
        LOG.info("Шаг 1: создаём контроллер и отделение с целевой очередью.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-transfer-ok", "Отделение с очередью");
        Queue queue = new Queue("queue-destination", "Очередь", "Q", 1);
        branch.getQueues().put(queue.getId(), queue);
        when(context.branchService().getBranch("branch-transfer-ok")).thenReturn(branch);

        Visit visit = Visit.builder().id("visit-to-transfer").build();
        when(context.visitService().visitTransfer("branch-transfer-ok", "sp-1", "queue-destination", true, 15L))
                .thenReturn(visit);

        LOG.info("Шаг 2: переводим визит из точки обслуживания в очередь.");
        Visit result = context.controller().visitTransfer("branch-transfer-ok", "sp-1", "queue-destination", true, 15L);

        LOG.info("Шаг 3: проверяем возвращаемый визит и делегирование в сервис визитов.");
        assertSame(visit, result);
        verify(context.visitService()).visitTransfer("branch-transfer-ok", "sp-1", "queue-destination", true, 15L);
    }

    @Test
    void visitTransferThrowsWhenBranchMissing() {
        LOG.info("Шаг 1: настраиваем контроллер, чтобы сервис отделений выбрасывал исключение.");
        ControllerContext context = controllerContext();
        when(context.branchService().getBranch("branch-missing"))
                .thenThrow(new IllegalStateException("branch lookup failed"));

        LOG.info("Шаг 2: вызываем перевод визита и ожидаем BusinessException с HTTP 404.");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                context.controller().visitTransfer("branch-missing", "sp-1", "queue-1", false, 0L));

        LOG.info("Шаг 3: убеждаемся, что ошибка транслируется в статус 404 и публикуется событие.");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(context.eventService()).send(eq("*"), eq(false), any());
        verify(context.visitService(), never()).visitTransfer(anyString(), anyString(), anyString(), anyBoolean(), anyLong());
    }

    @Test
    void visitTransferThrowsWhenQueueMissing() {
        LOG.info("Шаг 1: возвращаем отделение без нужной очереди.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-transfer-missing-queue", "Отделение без очереди");
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        LOG.info("Шаг 2: пытаемся перевести визит и фиксируем исключение.");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                context.controller().visitTransfer(branch.getId(), "sp-1", "queue-missing", true, 0L));

        LOG.info("Шаг 3: проверяем публикацию бизнес-события и отсутствие делегирования.");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(context.eventService()).send(eq("*"), eq(false), any());
        verify(context.visitService(), never()).visitTransfer(anyString(), anyString(), anyString(), anyBoolean(), anyLong());
    }

    @Test
    void visitBackToServicePointPoolSucceedsWhenPoolExists() {
        LOG.info("Шаг 1: подготавливаем отделение с пулом точки обслуживания.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-pool-return", "Отделение с пулом");
        ServicePoint poolOwner = new ServicePoint("pool-owner", "Оператор");
        branch.getServicePoints().put(poolOwner.getId(), poolOwner);
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        Visit visit = Visit.builder().id("visit-to-pool").build();
        when(context.visitService().visitBackToServicePointPool(
                        branch.getId(), "sp-1", poolOwner.getId(), 90L))
                .thenReturn(visit);

        LOG.info("Шаг 2: возвращаем визит в пул и проверяем результат.");
        Visit result = context.controller().visitBackToServicePointPool(
                branch.getId(), "sp-1", poolOwner.getId(), 90L);

        LOG.info("Шаг 3: убеждаемся, что контроллер делегировал операцию сервису визитов.");
        assertSame(visit, result);
        verify(context.visitService())
                .visitBackToServicePointPool(branch.getId(), "sp-1", poolOwner.getId(), 90L);
    }

    @Test
    void visitBackToServicePointPoolThrowsWhenServicePointMissing() {
        LOG.info("Шаг 1: создаём отделение без искомого пула точки обслуживания.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-pool-missing", "Отделение без пула");
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        LOG.info("Шаг 2: вызываем возвращение визита и ожидаем бизнес-исключение.");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                context.controller().visitBackToServicePointPool(branch.getId(), "sp-1", "pool-missing", 30L));

        LOG.info("Шаг 3: проверяем, что статус 404 и события опубликованы, а сервис визитов не вызван.");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(context.eventService()).send(eq("*"), eq(false), any());
        verify(context.visitService(), never())
                .visitBackToServicePointPool(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void visitTransferToServicePointPoolDelegates() {
        LOG.info("Шаг 1: готовим отделение и целевой пул точки обслуживания.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-transfer-pool", "Отделение с пулом");
        ServicePoint poolOwner = new ServicePoint("pool-owner-transfer", "Оператор");
        branch.getServicePoints().put(poolOwner.getId(), poolOwner);
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        Visit visit = Visit.builder().id("visit-to-pool").build();
        when(context.visitService().visitTransferToServicePointPool(
                        branch.getId(), "sp-1", poolOwner.getId(), 45L))
                .thenReturn(visit);

        LOG.info("Шаг 2: выполняем перевод визита в пул и анализируем ответ.");
        Visit result = context.controller().visitTransferToServicePointPool(
                branch.getId(), "sp-1", poolOwner.getId(), 45L);

        LOG.info("Шаг 3: убеждаемся в корректной делегации сервису визитов.");
        assertSame(visit, result);
        verify(context.visitService())
                .visitTransferToServicePointPool(branch.getId(), "sp-1", poolOwner.getId(), 45L);
    }

    @Test
    void visitTransferToServicePointPoolWithServiceInfoDelegates() {
        LOG.info("Шаг 1: подготавливаем отделение, пул точки и данные внешней службы.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-transfer-pool-service", "Отделение с внешним переводом");
        ServicePoint poolOwner = new ServicePoint("pool-owner-service", "Оператор");
        branch.getServicePoints().put(poolOwner.getId(), poolOwner);
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("source", "reception");
        Visit visit = Visit.builder().id("visit-from-external").build();
        when(context.visitService().visitTransferToServicePointPool(
                        branch.getId(), "sp-1", poolOwner.getId(), serviceInfo, 12L))
                .thenReturn(visit);

        LOG.info("Шаг 2: переводим визит внешней службой и фиксируем результат.");
        Visit result = context.controller().visitTransferToServicePointPool(
                branch.getId(), "sp-1", poolOwner.getId(), serviceInfo, 12L);

        LOG.info("Шаг 3: проверяем передачу параметров в сервис визитов.");
        assertSame(visit, result);
        verify(context.visitService())
                .visitTransferToServicePointPool(branch.getId(), "sp-1", poolOwner.getId(), serviceInfo, 12L);
    }

    @Test
    void visitTransferFromQueueWithServiceInfoDelegatesAndFetchesVisit() {
        LOG.info("Шаг 1: подготавливаем отделение с очередью и визит.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-from-queue", "Отделение источника");
        Queue queue = new Queue("queue-source", "Очередь источника", "Q", 1);
        branch.getQueues().put(queue.getId(), queue);
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        Visit visit = Visit.builder().id("visit-external").branchId(branch.getId()).build();
        when(context.visitService().getVisit(branch.getId(), visit.getId())).thenReturn(visit);

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("initiator", "mi");
        when(context.visitService().visitTransfer(
                        branch.getId(), queue.getId(), visit, true, serviceInfo, 20L, "sid-123"))
                .thenReturn(visit);

        LOG.info("Шаг 2: переводим визит из очереди внешней службой.");
        Visit result = context.controller().visitTransferFromQueue(
                branch.getId(), queue.getId(), visit.getId(), serviceInfo, true, 20L, "sid-123");

        LOG.info("Шаг 3: убеждаемся в получении визита и делегировании перевода.");
        assertSame(visit, result);
        verify(context.visitService()).getVisit(branch.getId(), visit.getId());
        verify(context.visitService())
                .visitTransfer(branch.getId(), queue.getId(), visit, true, serviceInfo, 20L, "sid-123");
    }

    @Test
    void visitTransferFromQueueDelegatesAndInvertsAppendFlag() {
        LOG.info("Шаг 1: создаём отделение и целевую очередь.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-queue-direct", "Отделение очереди");
        Queue queue = new Queue("queue-direct", "Очередь", "Q", 1);
        branch.getQueues().put(queue.getId(), queue);
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        Visit visit = Visit.builder().id("visit-direct").build();
        when(context.visitService().visitTransfer(
                        branch.getId(), "sp-target", queue.getId(), visit, false, 5L))
                .thenReturn(visit);

        LOG.info("Шаг 2: переводим визит между очередями через точку обслуживания.");
        Visit result = context.controller().visitTransferFromQueue(
                branch.getId(), "sp-target", queue.getId(), visit, true, 5L);

        LOG.info("Шаг 3: проверяем инверсию признака вставки и делегирование визиту.");
        assertSame(visit, result);
        verify(context.visitService())
                .visitTransfer(branch.getId(), "sp-target", queue.getId(), visit, false, 5L);
    }

    @Test
    void visitTransferFromQueueThrowsWhenQueueMissing() {
        LOG.info("Шаг 1: возвращаем отделение без нужной очереди.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-queue-missing", "Отделение без очереди");
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        LOG.info("Шаг 2: вызываем перевод из очереди и ожидаем ошибку 404.");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                context.controller().visitTransferFromQueue(branch.getId(), "sp-target", "queue-missing", Visit.builder().id("v").build(), true, 0L));

        LOG.info("Шаг 3: проверяем публикацию события об ошибке и отсутствие вызова сервиса визитов.");
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(context.eventService()).send(eq("*"), eq(false), any());
        verify(context.visitService(), never())
                .visitTransfer(anyString(), anyString(), anyString(), any(Visit.class), anyBoolean(), anyLong());
    }
}
