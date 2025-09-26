package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;

/**
 * Юнит-тест для {@link VisitService#getVisits(String, String)}.
 */
class VisitServiceGetVisitsTest {

    /** Проверяет фильтрацию визитов и сортировку по времени ожидания. */
    @DisplayName("Получение визитов фильтрует и сортирует список")
    @Test
    void filtersAndSortsVisits() {
        VisitService service = new VisitService();
        BranchService branchService = new BranchService();
        service.branchService = branchService;

        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 30);

        ZonedDateTime now = ZonedDateTime.now();
        Visit valid1 = Visit.builder()
                .id("v1")
                .createDateTime(now.minusSeconds(120))
                .build();
        Visit valid2 = Visit.builder()
                .id("v2")
                .createDateTime(now.minusSeconds(60))
                .build();
        Visit invalid = Visit.builder()
                .id("v3")
                .returnDateTime(now.minusSeconds(10))
                .returnTimeDelay(30L)
                .build();

        queue.getVisits().addAll(List.of(valid1, valid2, invalid));
        branch.getQueues().put(queue.getId(), queue);
        branchService.branches.put(branch.getId(), branch);

        List<Visit> visits = service.getVisits("b1", "q1");
        assertEquals(2, visits.size());
        assertEquals("v1", visits.get(0).getId());
        assertEquals("v2", visits.get(1).getId());
    }

    /** Проверяет фильтрацию по ограничению времени перевода визита. */
    @DisplayName("Получение визитов учитывает ограничение по времени перевода")
    @Test
    void filtersVisitsByTransferDelay() {
        VisitService service = new VisitService();
        BranchService branchService = new BranchService();
        service.branchService = branchService;

        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 30);
        ZonedDateTime now = ZonedDateTime.now();

        Visit allowed = Visit.builder()
                .id("allowed")
                .createDateTime(now.minusMinutes(5))
                .transferDateTime(now.minusMinutes(2))
                .transferTimeDelay(30L)
                .build();
        Visit alsoAllowed = Visit.builder()
                .id("also")
                .createDateTime(now.minusMinutes(3))
                .build();
        Visit filtered = Visit.builder()
                .id("filtered")
                .createDateTime(now.minusMinutes(4))
                .transferDateTime(now.minusSeconds(10))
                .transferTimeDelay(600L)
                .build();

        queue.getVisits().addAll(List.of(allowed, alsoAllowed, filtered));
        branch.getQueues().put(queue.getId(), queue);
        branchService.branches.put(branch.getId(), branch);

        List<Visit> visits = service.getVisits("b1", "q1");
        assertEquals(2, visits.size());
        assertEquals("also", visits.get(0).getId());
        assertEquals("allowed", visits.get(1).getId());
    }

    /** Ограничивает количество возвращаемых визитов. */
    @DisplayName("Получение визитов ограничивает количество результатов")
    @Test
    void limitsNumberOfVisits() {
        VisitService service = new VisitService();
        BranchService branchService = new BranchService();
        service.branchService = branchService;

        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 30);
        ZonedDateTime now = ZonedDateTime.now();

        Visit v1 = Visit.builder().id("v1").createDateTime(now.minusSeconds(100)).build();
        Visit v2 = Visit.builder().id("v2").createDateTime(now.minusSeconds(50)).build();
        Visit v3 = Visit.builder().id("v3").createDateTime(now.minusSeconds(10)).build();

        queue.getVisits().addAll(List.of(v1, v2, v3));
        branch.getQueues().put(queue.getId(), queue);
        branchService.branches.put(branch.getId(), branch);

        List<Visit> visits = service.getVisits("b1", "q1", 2L);
        assertEquals(2, visits.size());
        assertEquals("v1", visits.get(0).getId());
        assertEquals("v2", visits.get(1).getId());
    }

    /** Бросает HTTP-исключение, если очередь не найдена. */
    @DisplayName("Получение визитов выбрасывает исключение при отсутствии очереди")
    @Test
    void throwsWhenQueueMissing() {
        VisitService service = new VisitService();
        BranchService branchService = new BranchService();
        branchService.branches.put("b1", new Branch("b1", "Branch"));
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        HttpStatusException exception = assertThrows(HttpStatusException.class, () -> service.getVisits("b1", "missing"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());
    }
}
