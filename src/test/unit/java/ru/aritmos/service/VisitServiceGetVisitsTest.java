package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;

/**
 * Юнит-тест для {@link VisitService#getVisits(String, String)}.
 */
class VisitServiceGetVisitsTest {

    /** Проверяет фильтрацию визитов и сортировку по времени ожидания. */
    @Test
    void filtersAndSortsVisits() {
        VisitService service = new VisitService();
        BranchService branchService = new BranchService();
        service.branchService = branchService;

        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 30);

        Visit valid1 = Visit.builder()
                .id("v1")
                .createDateTime(ZonedDateTime.now().minusSeconds(100))
                .build();
        Visit valid2 = Visit.builder()
                .id("v2")
                .createDateTime(ZonedDateTime.now().minusSeconds(50))
                .build();
        Visit invalid = Visit.builder()
                .id("v3")
                .returnDateTime(ZonedDateTime.now().minusSeconds(10))
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
    @Test
    void limitsNumberOfVisits() {
        VisitService service = new VisitService();
        BranchService branchService = new BranchService();
        service.branchService = branchService;

        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 30);

        Visit v1 = Visit.builder().id("v1").createDateTime(ZonedDateTime.now().minusSeconds(100)).build();
        Visit v2 = Visit.builder().id("v2").createDateTime(ZonedDateTime.now().minusSeconds(50)).build();
        Visit v3 = Visit.builder().id("v3").createDateTime(ZonedDateTime.now().minusSeconds(10)).build();

        queue.getVisits().addAll(List.of(v1, v2, v3));
        branch.getQueues().put(queue.getId(), queue);
        branchService.branches.put(branch.getId(), branch);

        List<Visit> visits = service.getVisits("b1", "q1", 2L);
        assertEquals(2, visits.size());
    }
}
