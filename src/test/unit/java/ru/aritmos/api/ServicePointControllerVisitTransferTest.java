package ru.aritmos.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Набор тестов, покрывающих наименее тестируемые ветки {@link ServicePointController}.
 */
@ExtendWith(TestLoggingExtension.class)
class ServicePointControllerVisitTransferTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePointControllerVisitTransferTest.class);

    private ServicePointController controllerWith(
        VisitService visitService,
        BranchService branchService,
        EventService eventService
    ) {
        ServicePointController controller = new ServicePointController();
        controller.services = mock(Services.class);
        controller.branchService = branchService;
        controller.visitService = visitService;
        controller.eventService = eventService;
        controller.keyCloackClient = mock(KeyCloackClient.class);
        controller.applicationName = "visitmanager-test";
        return controller;
    }

    private Branch branchWithQueue(String branchId, String queueId) {
        LOG.info("Готовим отделение {} с очередью {}", branchId, queueId);
        Branch branch = new Branch(branchId, "Тестовое отделение");
        branch.getQueues().put(queueId, new Queue(queueId, "Очередь", "A", 30));
        return branch;
    }

    private Branch branchWithServicePoint(String branchId, String servicePointId) {
        LOG.info("Готовим отделение {} с точкой обслуживания {}", branchId, servicePointId);
        Branch branch = new Branch(branchId, "Отделение с точкой");
        branch.getServicePoints().put(servicePointId, new ServicePoint(servicePointId, "Окно №1"));
        return branch;
    }

    @DisplayName("Перенос визита по идентификатору инвертирует признак вставки в очередь")
    @Test
    void visitTransferFromQueueByIdInvertsAppendFlag() {
        LOG.info("Шаг 1: настраиваем окружение для перевода визита по идентификатору");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = branchWithQueue("branch-1", "queue-1");
        when(branchService.getBranch("branch-1")).thenReturn(branch);

        Visit storedVisit = Visit.builder().id("visit-7").build();
        when(visitService.getVisit("branch-1", "visit-7")).thenReturn(storedVisit);
        Visit expected = Visit.builder().id("visit-7").status("transferred").build();
        when(
            visitService.visitTransfer(
                anyString(),
                anyString(),
                anyString(),
                any(Visit.class),
                anyBoolean(),
                anyLong()
            )
        ).thenReturn(expected);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: вызываем контроллер с параметром isAppend=true");
        Visit actual = controller.visitTransferFromQueue(
            "branch-1",
            "service-point-1",
            "queue-1",
            "visit-7",
            true,
            15L
        );

        LOG.info("Шаг 3: проверяем, что флаг был инвертирован перед передачей в сервис");
        ArgumentCaptor<Boolean> appendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(visitService).visitTransfer(
            eq("branch-1"),
            eq("service-point-1"),
            eq("queue-1"),
            same(storedVisit),
            appendCaptor.capture(),
            eq(15L)
        );
        assertSame(expected, actual);
        assertFalse(appendCaptor.getValue(), "Флаг вставки должен инвертироваться для сервиса");
        verify(branchService).getBranch("branch-1");
        verify(visitService).getVisit("branch-1", "visit-7");
    }

    @DisplayName("Перенос визита по идентификатору возвращает ошибку 404 при отсутствии очереди")
    @Test
    void visitTransferFromQueueByIdFailsWhenQueueMissing() {
        LOG.info("Шаг 1: создаем отделение без целевой очереди");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-2", "Отделение без очереди");
        when(branchService.getBranch("branch-2")).thenReturn(branch);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: ожидаем HttpStatusException со статусом 404");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransferFromQueue("branch-2", "sp-404", "queue-missing", "visit-1", true, 0L)
        );

        LOG.info("Шаг 3: проверяем детали ошибки и публикацию события");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Queue not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(visitService, never()).getVisit(anyString(), anyString());
    }

    @DisplayName("Перенос визита по телу запроса инвертирует признак вставки")
    @Test
    void visitTransferFromQueueWithBodyRespectsAppendInversion() {
        LOG.info("Шаг 1: настраиваем отделение и подготовленный визит");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = branchWithQueue("branch-3", "queue-body");
        when(branchService.getBranch("branch-3")).thenReturn(branch);

        Visit request = Visit.builder().id("visit-body").build();
        Visit response = Visit.builder().id("visit-body").status("moved").build();
        when(
            visitService.visitTransfer(
                anyString(),
                anyString(),
                anyString(),
                same(request),
                anyBoolean(),
                anyLong()
            )
        ).thenReturn(response);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: вызываем метод с параметром isAppend=false");
        Visit actual = controller.visitTransferFromQueue(
            "branch-3",
            "sp-body",
            "queue-body",
            request,
            false,
            3L
        );

        LOG.info("Шаг 3: убеждаемся, что сервис получил противоположный флаг");
        ArgumentCaptor<Boolean> appendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(visitService).visitTransfer(
            eq("branch-3"),
            eq("sp-body"),
            eq("queue-body"),
            same(request),
            appendCaptor.capture(),
            eq(3L)
        );
        assertSame(response, actual);
        assertTrue(appendCaptor.getValue(), "Флаг должен быть развернут контроллером");
    }

    @DisplayName("Перенос визита с индексом передаёт позицию без искажений")
    @Test
    void visitTransferFromQueueWithIndexPassesExactPosition() {
        LOG.info("Шаг 1: подготавливаем отделение, визит и индекс");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = branchWithQueue("branch-4", "queue-index");
        when(branchService.getBranch("branch-4")).thenReturn(branch);

        Visit request = Visit.builder().id("visit-index").build();
        Visit response = Visit.builder().id("visit-index").status("positioned").build();
        when(
            visitService.visitTransfer(
                anyString(),
                anyString(),
                anyString(),
                same(request),
                anyInt(),
                anyLong()
            )
        ).thenReturn(response);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: вызываем перевод на позицию 5");
        Visit actual = controller.visitTransferFromQueue(
            "branch-4",
            "sp-index",
            "queue-index",
            request,
            5,
            8L
        );

        LOG.info("Шаг 3: контролируем, что индекс передан корректно");
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(visitService).visitTransfer(
            eq("branch-4"),
            eq("sp-index"),
            eq("queue-index"),
            same(request),
            indexCaptor.capture(),
            eq(8L)
        );
        assertSame(response, actual);
        assertEquals(5, indexCaptor.getValue());
    }

    @DisplayName("Перенос визита через внешнюю службу сохраняет метаданные без изменений")
    @Test
    void visitTransferFromQueueExternalServicePropagatesMetadata() {
        LOG.info("Шаг 1: готовим отделение и данные внешней службы");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = branchWithQueue("branch-5", "queue-external");
        when(branchService.getBranch("branch-5")).thenReturn(branch);

        Visit visit = Visit.builder().id("visit-external").build();
        when(visitService.getVisit("branch-5", "visit-external")).thenReturn(visit);
        Visit response = Visit.builder().id("visit-external").status("queued").build();
        when(
            visitService.visitTransfer(
                anyString(),
                anyString(),
                any(Visit.class),
                anyBoolean(),
                any(HashMap.class),
                anyLong(),
                any()
            )
        ).thenReturn(response);

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("source", "reception");

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: выполняем перевод через внешнюю службу");
        Visit actual = controller.visitTransferFromQueue(
            "branch-5",
            "queue-external",
            "visit-external",
            serviceInfo,
            false,
            20L,
            "sid-1"
        );

        LOG.info("Шаг 3: убеждаемся, что метаданные переданы без изменений");
        verify(visitService).visitTransfer(
            eq("branch-5"),
            eq("queue-external"),
            same(visit),
            eq(false),
            same(serviceInfo),
            eq(20L),
            eq("sid-1")
        );
        assertSame(response, actual);
    }

    @DisplayName("Перевод визита в пул через внешнюю службу проверяет наличие точки обслуживания")
    @Test
    void visitTransferFromQueueToServicePointPoolExternalServiceValidatesServicePoint() {
        LOG.info("Шаг 1: готовим отделение и сервисную точку");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-6", "Отделение с пулом");
        branch.getServicePoints().put("sp-pool", new ServicePoint("sp-pool", "Окно 7"));
        when(branchService.getBranch("branch-6")).thenReturn(branch);

        Visit visit = Visit.builder().id("visit-pool").build();
        when(visitService.getVisit("branch-6", "visit-pool")).thenReturn(visit);
        Visit response = Visit.builder().id("visit-pool").status("pool").build();
        when(
            visitService.visitTransferFromQueueToServicePointPool(
                anyString(),
                anyString(),
                any(Visit.class),
                anyBoolean(),
                any(HashMap.class),
                anyLong(),
                any()
            )
        ).thenReturn(response);

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("source", "mi");

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: выполняем перевод визита в пул через внешнюю службу");
        Visit actual = controller.visitTransferFromQueueToServicePointPool(
            "branch-6",
            "sp-pool",
            "visit-pool",
            serviceInfo,
            true,
            40L,
            "cookie"
        );

        LOG.info("Шаг 3: проверяем корректность делегирования в сервис");
        verify(visitService).visitTransferFromQueueToServicePointPool(
            eq("branch-6"),
            eq("sp-pool"),
            same(visit),
            eq(true),
            same(serviceInfo),
            eq(40L),
            eq("cookie")
        );
        assertSame(response, actual);
    }

    @DisplayName("Перевод визита в пул через внешнюю службу возвращает 404 без точки обслуживания")
    @Test
    void visitTransferFromQueueToServicePointPoolExternalServiceFailsWithoutServicePoint() {
        LOG.info("Шаг 1: создаем отделение без зарегистрированной точки обслуживания");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-7", "Отделение без точек");
        when(branchService.getBranch("branch-7")).thenReturn(branch);

        when(visitService.getVisit("branch-7", "visit-missing")).thenReturn(Visit.builder().id("visit-missing").build());

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: ожидаем ошибку с кодом 404 из-за отсутствия точки");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransferFromQueueToServicePointPool(
                "branch-7",
                "sp-absent",
                "visit-missing",
                new HashMap<>(),
                false,
                10L,
                null
            )
        );

        LOG.info("Шаг 3: подтверждаем публикацию события и статус ответа");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Service point not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
    }

    @DisplayName("Перенос визита по идентификатору делегирует поиск визита и позиции сервису")
    @Test
    void visitTransferFromQueueByVisitIdDelegatesIndexAndVisitLookup() {
        LOG.info("Шаг 1: готовим отделение с очередью и визит в хранилище");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = branchWithQueue("branch-idx", "queue-target");
        when(branchService.getBranch("branch-idx")).thenReturn(branch);

        Visit storedVisit = Visit.builder().id("visit-idx").build();
        when(visitService.getVisit("branch-idx", "visit-idx")).thenReturn(storedVisit);
        Visit expected = Visit.builder().id("visit-idx").status("reordered").build();
        when(
            visitService.visitTransfer(
                anyString(),
                anyString(),
                anyString(),
                same(storedVisit),
                anyInt(),
                anyLong()
            )
        ).thenReturn(expected);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: переводим визит на позицию с идентификатором");
        Visit actual = controller.visitTransferFromQueue(
            "branch-idx",
            "sp-main",
            "queue-target",
            "visit-idx",
            4,
            12L
        );

        LOG.info("Шаг 3: убеждаемся, что индекс и визит переданы корректно");
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(visitService).visitTransfer(
            eq("branch-idx"),
            eq("sp-main"),
            eq("queue-target"),
            same(storedVisit),
            indexCaptor.capture(),
            eq(12L)
        );
        assertSame(expected, actual);
        assertEquals(4, indexCaptor.getValue());
        verify(visitService).getVisit("branch-idx", "visit-idx");
    }

    @DisplayName("Перенос визита по идентификатору возвращает 404 при отсутствии очереди")
    @Test
    void visitTransferFromQueueByVisitIdFailsWhenQueueMissing() {
        LOG.info("Шаг 1: создаем отделение без требуемой очереди");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-no-queue", "Отделение без очередей");
        when(branchService.getBranch("branch-no-queue")).thenReturn(branch);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: ожидаем ошибку 404 из-за отсутствующей очереди");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransferFromQueue(
                "branch-no-queue",
                "sp-missing",
                "queue-unknown",
                "visit-x",
                0,
                0L
            )
        );

        LOG.info("Шаг 3: проверяем статус и факт отправки события");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Queue not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(visitService, never()).getVisit(anyString(), anyString());
    }

    @DisplayName("Перенос визита по идентификатору возвращает 404 при отсутствии отделения")
    @Test
    void visitTransferFromQueueByVisitIdFailsWhenBranchMissing() {
        LOG.info("Шаг 1: настраиваем сервис отделений на выброс исключения");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        when(branchService.getBranch("branch-error")).thenThrow(new IllegalStateException("branch missing"));

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: ожидаем BusinessException, оборачивающуюся в HttpStatusException");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransferFromQueue(
                "branch-error",
                "sp-any",
                "queue-any",
                "visit-any",
                1,
                5L
            )
        );

        LOG.info("Шаг 3: убеждаемся, что ошибка преобразована в 404 и событие отправлено");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Branch not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }

    @DisplayName("Перевод визита из точки обслуживания передаёт признак вставки сервису")
    @Test
    void visitTransferFromServicePointDelegatesAppendFlag() {
        LOG.info("Шаг 1: подготавливаем отделение с очередью для перевода из сервиса");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = branchWithQueue("branch-transfer", "queue-main");
        when(branchService.getBranch("branch-transfer")).thenReturn(branch);

        Visit expected = Visit.builder().id("visit-transfer").status("delegated").build();
        when(
            visitService.visitTransfer(
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyLong()
            )
        ).thenReturn(expected);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: выполняем перевод визита с параметром вставки");
        Visit actual = controller.visitTransfer("branch-transfer", "sp-1", "queue-main", false, 7L);

        LOG.info("Шаг 3: проверяем делегирование параметров");
        verify(visitService).visitTransfer("branch-transfer", "sp-1", "queue-main", false, 7L);
        assertSame(expected, actual);
    }

    @DisplayName("Перевод визита из точки обслуживания возвращает 404 при отсутствии очереди")
    @Test
    void visitTransferFromServicePointFailsWhenQueueMissing() {
        LOG.info("Шаг 1: создаем отделение без требуемой очереди для перевода");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-transfer-missing", "Отделение без очередей");
        when(branchService.getBranch("branch-transfer-missing")).thenReturn(branch);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: ожидаем исключение при попытке перевода");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransfer("branch-transfer-missing", "sp-1", "queue-absent", true, 0L)
        );

        LOG.info("Шаг 3: проверяем статус ошибки и публикацию события");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Queue not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }

    @DisplayName("Перевод визита из точки обслуживания возвращает 404 при отсутствии отделения")
    @Test
    void visitTransferFromServicePointFailsWhenBranchUnavailable() {
        LOG.info("Шаг 1: подготавливаем сервис отделений к ошибке получения данных");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        when(branchService.getBranch("branch-transfer-error"))
            .thenThrow(new IllegalArgumentException("not found"));

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: вызываем метод и фиксируем исключение");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransfer("branch-transfer-error", "sp-err", "queue", true, 9L)
        );

        LOG.info("Шаг 3: убеждаемся в корректности статуса и сообщения");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Branch not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }

    @DisplayName("Возврат визита в пул точки обслуживания делегируется сервису визитов")
    @Test
    void visitBackToServicePointPoolDelegatesToService() {
        LOG.info("Шаг 1: формируем отделение с пулом точки обслуживания");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = branchWithServicePoint("branch-back", "sp-pool");
        when(branchService.getBranch("branch-back")).thenReturn(branch);

        Visit expected = Visit.builder().id("visit-back").status("pool").build();
        when(
            visitService.visitBackToServicePointPool(
                anyString(),
                anyString(),
                anyString(),
                anyLong()
            )
        ).thenReturn(expected);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: возвращаем визит в пул");
        Visit actual = controller.visitBackToServicePointPool("branch-back", "sp-main", "sp-pool", 30L);

        LOG.info("Шаг 3: проверяем делегирование параметров и результат");
        verify(visitService).visitBackToServicePointPool("branch-back", "sp-main", "sp-pool", 30L);
        assertSame(expected, actual);
    }

    @DisplayName("Возврат визита в пул точки обслуживания возвращает 404 при отсутствии пула")
    @Test
    void visitBackToServicePointPoolFailsWhenPoolMissing() {
        LOG.info("Шаг 1: создаем отделение без пула точек обслуживания");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-no-pool", "Отделение без пула");
        when(branchService.getBranch("branch-no-pool")).thenReturn(branch);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: ожидаем BusinessException при отсутствии точки пула");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitBackToServicePointPool("branch-no-pool", "sp-main", "sp-missing", 15L)
        );

        LOG.info("Шаг 3: убеждаемся, что событие отправлено, а статус равен 404");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Service point not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }

    @DisplayName("Возврат визита в пул точки обслуживания возвращает 404 при отсутствии отделения")
    @Test
    void visitBackToServicePointPoolFailsWhenBranchUnavailable() {
        LOG.info("Шаг 1: конфигурируем сервис отделений на выброс исключения при получении отдела");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        when(branchService.getBranch("branch-back-error"))
            .thenThrow(new IllegalStateException("branch missing"));

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: вызываем метод и ловим HttpStatusException");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitBackToServicePointPool("branch-back-error", "sp", "pool", 20L)
        );

        LOG.info("Шаг 3: проверяем статус и текст ошибки");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Branch not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }

    @DisplayName("Перевод визита в пул точки обслуживания делегируется сервису визитов")
    @Test
    void visitTransferToServicePointPoolDelegatesToService() {
        LOG.info("Шаг 1: подготавливаем отделение с доступным пулом обслуживания");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = branchWithServicePoint("branch-transfer-pool", "sp-pool");
        when(branchService.getBranch("branch-transfer-pool")).thenReturn(branch);

        Visit expected = Visit.builder().id("visit-pool").status("pool").build();
        when(
            visitService.visitTransferToServicePointPool(
                anyString(),
                anyString(),
                anyString(),
                anyLong()
            )
        ).thenReturn(expected);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: выполняем перевод визита в пул");
        Visit actual = controller.visitTransferToServicePointPool("branch-transfer-pool", "sp-main", "sp-pool", 18L);

        LOG.info("Шаг 3: проверяем, что параметры переданы сервису без искажений");
        verify(visitService).visitTransferToServicePointPool("branch-transfer-pool", "sp-main", "sp-pool", 18L);
        assertSame(expected, actual);
    }

    @DisplayName("Перевод визита в пул точки обслуживания возвращает 404 при отсутствии целевой точки")
    @Test
    void visitTransferToServicePointPoolFailsWhenPoolMissing() {
        LOG.info("Шаг 1: создаем отделение без зарегистрированных пулов");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        Branch branch = new Branch("branch-transfer-no-pool", "Отделение без пула");
        when(branchService.getBranch("branch-transfer-no-pool")).thenReturn(branch);

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: ожидаем исключение из-за отсутствующей точки пула");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransferToServicePointPool(
                "branch-transfer-no-pool",
                "sp-main",
                "sp-missing",
                25L
            )
        );

        LOG.info("Шаг 3: убеждаемся, что событие отправлено и статус соответствует 404");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Service point not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }

    @DisplayName("Перевод визита в пул точки обслуживания возвращает 404 при отсутствии отделения")
    @Test
    void visitTransferToServicePointPoolFailsWhenBranchUnavailable() {
        LOG.info("Шаг 1: конфигурируем сервис отделений на ошибку при поиске отделения");
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        when(branchService.getBranch("branch-transfer-pool-error"))
            .thenThrow(new RuntimeException("not found"));

        ServicePointController controller = controllerWith(visitService, branchService, eventService);

        LOG.info("Шаг 2: вызываем перевод в пул и фиксируем ошибку");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> controller.visitTransferToServicePointPool("branch-transfer-pool-error", "sp", "pool", 5L)
        );

        LOG.info("Шаг 3: проверяем, что статус соответствует 404 и сообщение понятное");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Branch not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verifyNoInteractions(visitService);
    }
}
