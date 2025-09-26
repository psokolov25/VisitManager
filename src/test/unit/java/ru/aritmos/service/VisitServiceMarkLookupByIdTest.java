package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Mark;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты строковых перегрузок {@link VisitService#addMark(String, String, String)} и
 * {@link VisitService#deleteMark(String, String, String)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceMarkLookupByIdTest {

    private static final Logger LOG = LoggerFactory.getLogger(VisitServiceMarkLookupByIdTest.class);

    @DisplayName("Добавление отметки по идентификатору делегирует существующей записи")
    @Test
    void addMarkByIdDelegatesToExistingMark() {
        LOG.info("Шаг 1: создаём отделение с преднастроенной заметкой");
        Branch branch = new Branch("b1", "Branch");
        Mark mark = Mark.builder().id("mark-1").value("Важно").build();
        branch.setMarks(new HashMap<>());
        branch.getMarks().put(mark.getId(), mark);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = spy(new VisitService());
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        Visit expectedVisit = Visit.builder().id("visit-1").build();
        doReturn(expectedVisit).when(service).addMark("b1", "sp1", mark);

        LOG.info("Шаг 2: вызываем метод добавления отметки по идентификатору");
        Visit result = service.addMark("b1", "sp1", mark.getId());

        LOG.info("Шаг 3: убеждаемся, что делегирующий вызов выполнен и результат возвращён без публикации ошибок");
        assertSame(expectedVisit, result);
        verify(service).addMark("b1", "sp1", mark);
        verify(branchService).getBranch("b1");
        verifyNoInteractions(eventService);
    }

    @DisplayName("Добавление отметки по идентификатору выбрасывает исключение при отсутствии записи")
    @Test
    void addMarkByIdThrowsWhenMarkMissing() {
        LOG.info("Шаг 1: создаём отделение без требуемой заметки");
        Branch branch = new Branch("b1", "Branch");
        branch.setMarks(new HashMap<>());

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        LOG.info("Шаг 2: вызываем метод и ожидаем ошибку 404");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () -> service.addMark("b1", "sp1", "missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(branchService).getBranch("b1");
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @DisplayName("Удаление отметки по идентификатору делегирует существующей записи")
    @Test
    void deleteMarkByIdDelegatesToExistingMark() {
        LOG.info("Шаг 1: создаём отделение с заметкой для удаления");
        Branch branch = new Branch("b1", "Branch");
        Mark mark = Mark.builder().id("mark-2").value("Удалить").build();
        branch.setMarks(new HashMap<>());
        branch.getMarks().put(mark.getId(), mark);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = spy(new VisitService());
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        Visit expectedVisit = Visit.builder().id("visit-2").build();
        doReturn(expectedVisit).when(service).deleteMark("b1", "sp1", mark);

        LOG.info("Шаг 2: вызываем удаление по идентификатору");
        Visit result = service.deleteMark("b1", "sp1", mark.getId());

        LOG.info("Шаг 3: проверяем делегирование и отсутствие публикации ошибок");
        assertSame(expectedVisit, result);
        verify(service).deleteMark("b1", "sp1", mark);
        verify(branchService).getBranch("b1");
        verifyNoInteractions(eventService);
    }

    @DisplayName("Удаление отметки по идентификатору выбрасывает исключение при отсутствии записи")
    @Test
    void deleteMarkByIdThrowsWhenMarkMissing() {
        LOG.info("Шаг 1: подготавливаем отделение без искомой заметки");
        Branch branch = new Branch("b1", "Branch");
        branch.setMarks(new HashMap<>());

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        LOG.info("Шаг 2: вызываем удаление по несуществующему идентификатору и ожидаем HTTP 404");
        HttpStatusException exception = assertThrows(HttpStatusException.class, () -> service.deleteMark("b1", "sp1", "missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(branchService).getBranch("b1");
        verify(eventService).send(eq("*"), eq(false), any());
    }
}
