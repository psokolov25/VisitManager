package ru.aritmos.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.aritmos.test.LoggingAssertions.assertEquals;
import static ru.aritmos.test.LoggingAssertions.assertSame;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Mark;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;
import ru.aritmos.test.TestLoggingExtension;

/** Логирующие тесты для марок, заметок и исходов визита в {@link ServicePointController}. */
@ExtendWith(TestLoggingExtension.class)
class ServicePointControllerVisitDetailsTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePointControllerVisitDetailsTest.class);

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
        return new ControllerContext(controller, visitService, branchService);
    }

    private record ControllerContext(
            ServicePointController controller,
            VisitService visitService,
            BranchService branchService) {}

    @Test
    void getMarksDelegatesToVisitService() {
        LOG.info("Шаг 1: настраиваем контроллер и мок сервиса визитов.");
        ControllerContext context = controllerContext();
        List<Mark> marks = List.of(Mark.builder().id("mark-1").value("Приоритет").markDate(ZonedDateTime.now()).build());
        when(context.visitService().getMarks("branch-1", "visit-1")).thenReturn(marks);

        LOG.info("Шаг 2: запрашиваем метки визита через контроллер.");
        List<Mark> result = context.controller().getMarks("branch-1", "visit-1");

        LOG.info("Шаг 3: убеждаемся, что данные получены напрямую из сервиса визитов.");
        assertSame(marks, result);
        verify(context.visitService()).getMarks("branch-1", "visit-1");
    }

    @Test
    void deleteMarkFromServicePointDelegatesToVisitService() {
        LOG.info("Шаг 1: подготавливаем визит для удаления метки.");
        ControllerContext context = controllerContext();
        Visit visit = Visit.builder().id("visit-mark-delete").build();
        when(context.visitService().deleteMark("branch-1", "sp-1", "mark-1")).thenReturn(visit);

        LOG.info("Шаг 2: вызываем контроллер для удаления метки у визита.");
        Visit result = context.controller().deleteMark("branch-1", "sp-1", "mark-1");

        LOG.info("Шаг 3: проверяем делегирование визиту.");
        assertSame(visit, result);
        verify(context.visitService()).deleteMark("branch-1", "sp-1", "mark-1");
    }

    @Test
    void deleteMarkReturnsBranchMarks() {
        LOG.info("Шаг 1: формируем отделение с доступными метками.");
        ControllerContext context = controllerContext();
        Branch branch = new Branch("branch-with-marks", "Отделение с метками");
        Mark mark = Mark.builder().id("mark-available").value("VIP").markDate(ZonedDateTime.now()).build();
        branch.getMarks().put(mark.getId(), mark);
        when(context.branchService().getBranch(branch.getId())).thenReturn(branch);

        LOG.info("Шаг 2: запрашиваем перечень меток через контроллер.");
        HashMap<String, Mark> result = context.controller().deleteMark(branch.getId());

        LOG.info("Шаг 3: убеждаемся, что возвращена карта меток из доменной модели.");
        assertEquals(branch.getMarks(), result);
        verify(context.branchService()).getBranch(branch.getId());
    }

    @Test
    void addMarkDelegatesToVisitService() {
        LOG.info("Шаг 1: настраиваем мок добавления метки.");
        ControllerContext context = controllerContext();
        Visit visit = Visit.builder().id("visit-add-mark").build();
        when(context.visitService().addMark("branch-2", "sp-2", "mark-2")).thenReturn(visit);

        LOG.info("Шаг 2: добавляем метку через контроллер.");
        Visit result = context.controller().addMark("branch-2", "sp-2", "mark-2");

        LOG.info("Шаг 3: проверяем результат и вызов сервиса визитов.");
        assertSame(visit, result);
        verify(context.visitService()).addMark("branch-2", "sp-2", "mark-2");
    }

    @Test
    void addNoteAsTextDelegatesToVisitService() {
        LOG.info("Шаг 1: настраиваем сервис визитов на добавление заметки.");
        ControllerContext context = controllerContext();
        Visit visit = Visit.builder().id("visit-note").build();
        when(context.visitService().addNote("branch-3", "sp-3", "Отложить")).thenReturn(visit);

        LOG.info("Шаг 2: добавляем заметку в визит через контроллер.");
        Visit result = context.controller().addNoteAsText("branch-3", "sp-3", "Отложить");

        LOG.info("Шаг 3: проверяем делегирование на сервис визитов.");
        assertSame(visit, result);
        verify(context.visitService()).addNote("branch-3", "sp-3", "Отложить");
    }

    @Test
    void getNotesDelegatesToVisitService() {
        LOG.info("Шаг 1: готовим заметки визита.");
        ControllerContext context = controllerContext();
        List<Mark> notes = List.of(Mark.builder().id("note-1").value("Напечатать договор").markDate(ZonedDateTime.now()).build());
        when(context.visitService().getNotes("branch-4", "visit-4")).thenReturn(notes);

        LOG.info("Шаг 2: читаем заметки через контроллер.");
        List<Mark> result = context.controller().getNotes("branch-4", "visit-4");

        LOG.info("Шаг 3: убеждаемся, что данные пришли из сервиса визитов.");
        assertSame(notes, result);
        verify(context.visitService()).getNotes("branch-4", "visit-4");
    }

    @Test
    void addOutcomeServiceDelegatesToVisitService() {
        LOG.info("Шаг 1: имитируем добавление исхода услуги.");
        ControllerContext context = controllerContext();
        Visit visit = Visit.builder().id("visit-outcome").build();
        when(context.visitService().addOutcomeService("branch-5", "sp-5", "outcome-5")).thenReturn(visit);

        LOG.info("Шаг 2: добавляем исход через контроллер.");
        Visit result = context.controller().addOutcomeService("branch-5", "sp-5", "outcome-5");

        LOG.info("Шаг 3: фиксируем делегирование в сервис визитов.");
        assertSame(visit, result);
        verify(context.visitService()).addOutcomeService("branch-5", "sp-5", "outcome-5");
    }

    @Test
    void addServiceDelegatesToVisitService() {
        LOG.info("Шаг 1: настраиваем мок добавления услуги.");
        ControllerContext context = controllerContext();
        Visit visit = Visit.builder().id("visit-service").build();
        when(context.visitService().addService("branch-6", "sp-6", "service-6")).thenReturn(visit);

        LOG.info("Шаг 2: добавляем услугу через контроллер.");
        Visit result = context.controller().addService("branch-6", "sp-6", "service-6");

        LOG.info("Шаг 3: убеждаемся в делегировании и корректном ответе.");
        assertSame(visit, result);
        verify(context.visitService()).addService("branch-6", "sp-6", "service-6");
    }

    @Test
    void addOutcomeOfDeliveredServiceDelegatesToVisitService() {
        LOG.info("Шаг 1: подготавливаем визит и фактическую услугу.");
        ControllerContext context = controllerContext();
        Visit visit = Visit.builder().id("visit-delivered-outcome").build();
        when(context.visitService().addOutcomeOfDeliveredService("branch-7", "sp-7", "delivered-7", "outcome-7"))
                .thenReturn(visit);

        LOG.info("Шаг 2: добавляем исход фактической услуги через контроллер.");
        Visit result = context.controller().addOutcomeOfDeliveredService("branch-7", "sp-7", "delivered-7", "outcome-7");

        LOG.info("Шаг 3: проверяем делегирование.");
        assertSame(visit, result);
        verify(context.visitService()).addOutcomeOfDeliveredService("branch-7", "sp-7", "delivered-7", "outcome-7");
    }

    @Test
    void deleteOutcomeDeliveredServiceDelegatesToVisitService() {
        LOG.info("Шаг 1: подготавливаем визит с удалением исхода фактической услуги.");
        ControllerContext context = controllerContext();
        Visit visit = Visit.builder().id("visit-delivered-delete").build();
        when(context.visitService().deleteOutcomeDeliveredService("branch-8", "sp-8", "delivered-8"))
                .thenReturn(visit);

        LOG.info("Шаг 2: удаляем исход через контроллер.");
        Visit result = context.controller().deleteOutcomeDeliveredService("branch-8", "sp-8", "delivered-8");

        LOG.info("Шаг 3: убеждаемся в делегировании.");
        assertSame(visit, result);
        verify(context.visitService()).deleteOutcomeDeliveredService("branch-8", "sp-8", "delivered-8");
    }
}
