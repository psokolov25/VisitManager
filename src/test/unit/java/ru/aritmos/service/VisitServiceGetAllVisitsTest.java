package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;

/**
 * Дополнительные юнит-тесты для методов {@link VisitService}.
 */
class VisitServiceGetAllVisitsTest {

    @DisplayName("Получение доступных визитов оставляет только ожидающих клиентов и визиты с истёкшей задержкой возврата")
    @Test
    void getAvailableVisitsFiltersOnlyWaitingAndTimedOut() {
        VisitService service = new VisitService();

        Visit valid = Visit.builder()
                .id("v1")
                .status("WAITING")
                .build();
        Visit waitingReturn = Visit.builder()
                .id("v2")
                .status("WAITING")
                .returnDateTime(ZonedDateTime.now())
                .returnTimeDelay(60L)
                .build();
        Visit served = Visit.builder()
                .id("v3")
                .status("SERVED")
                .build();

        List<Visit> result = service.getAvailableVisits(List.of(valid, waitingReturn, served));
        assertEquals(List.of(valid), result);
    }

    @DisplayName("Получение всех визитов возвращает посещения отделения")
    @Test
    void getAllVisitsReturnsBranchVisits() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q1", "A", 30);
        Visit visit = Visit.builder().id("v1").build();
        queue.getVisits().add(visit);
        branch.getQueues().put(queue.getId(), queue);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        HashMap<String, Visit> visits = service.getAllVisits("b1");
        assertEquals(1, visits.size());
        assertSame(visit, visits.get("v1"));
    }

    @DisplayName("Фильтрация по статусам оставляет визиты с указанными состояниями")
    @Test
    void getVisitsByStatusesFiltersByStatus() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q1", "A", 30);
        Visit waiting = Visit.builder().id("v1").status("WAITING").build();
        Visit served = Visit.builder().id("v2").status("SERVED").build();
        queue.getVisits().addAll(List.of(waiting, served));
        branch.getQueues().put(queue.getId(), queue);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        HashMap<String, Visit> visits = service.getVisitsByStatuses("b1", List.of("WAITING"));
        assertEquals(1, visits.size());
        assertSame(waiting, visits.get("v1"));
    }
}

