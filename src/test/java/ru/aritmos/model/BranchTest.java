package ru.aritmos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.visit.Visit;

/**
 * Модульные проверки для {@link Branch}.
 */
class BranchTest {

    @Test
    void incrementTicketCounterReturnsNewValue() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        branch.getQueues().put("q1", queue);

        int result = branch.incrementTicketCounter(queue);

        assertEquals(1, result);
        assertEquals(1, queue.getTicketCounter());
    }

    @Test
    void incrementTicketCounterReturnsMinusOneForForeignQueue() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "Q", 1);

        int result = branch.incrementTicketCounter(queue);

        assertEquals(-1, result);
    }

    @Test
    void getAllVisitsCollectsFromUsersServicePointsAndQueues() {
        Branch branch = new Branch("b1", "Branch");

        // Визит пользователя
        Visit visit1 = Visit.builder().id("v1").status("NEW").build();
        User user = new User("u1", null);
        user.getVisits().add(visit1);
        ServicePoint sp1 = new ServicePoint("sp1", "SP1");
        sp1.setUser(user);

        // Текущий визит точки обслуживания
        Visit visit2 = Visit.builder().id("v2").status("NEW").build();
        ServicePoint sp2 = new ServicePoint("sp2", "SP2");
        sp2.setVisit(visit2);

        // Визиты точки обслуживания
        Visit visit3 = Visit.builder().id("v3").status("DONE").build();
        ServicePoint sp3 = new ServicePoint("sp3", "SP3");
        sp3.setVisits(List.of(visit3));

        // Визит очереди
        Visit visit4 = Visit.builder().id("v4").status("NEW").build();
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        queue.getVisits().add(visit4);

        branch.getServicePoints().put("sp1", sp1);
        branch.getServicePoints().put("sp2", sp2);
        branch.getServicePoints().put("sp3", sp3);
        branch.getQueues().put("q1", queue);

        Map<String, Visit> all = branch.getAllVisits();

        assertEquals(4, all.size());
        assertTrue(all.containsKey("v1"));
        assertTrue(all.containsKey("v2"));
        assertTrue(all.containsKey("v3"));
        assertTrue(all.containsKey("v4"));
    }

    @Test
    void getVisitsByStatusFiltersVisits() {
        Branch branch = new Branch("b1", "Branch");
        Visit v1 = Visit.builder().id("v1").status("NEW").build();
        Visit v2 = Visit.builder().id("v2").status("DONE").build();
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setVisits(List.of(v1, v2));
        branch.getServicePoints().put("sp1", sp);

        Map<String, Visit> filtered = branch.getVisitsByStatus(List.of("NEW"));

        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("v1"));
        assertFalse(filtered.containsKey("v2"));
    }
    @Test
    void getAllVisitsList() {
        // TODO: implement test
    }

    @Test
    void openServicePoint() {
        // TODO: implement test
    }

    @Test
    void closeServicePoint() {
        // TODO: implement test
    }

    @Test
    void updateVisit() {
        // TODO: implement test
    }

    @Test
    void addUpdateService() {
        // TODO: implement test
    }

    @Test
    void deleteServices() {
        // TODO: implement test
    }

    @Test
    void adUpdateServiceGroups() {
        // TODO: implement test
    }

    @Test
    void addUpdateServicePoint() {
        // TODO: implement test
    }

    @Test
    void deleteServicePoints() {
        // TODO: implement test
    }

    @Test
    void addUpdateQueues() {
        // TODO: implement test
    }

    @Test
    void deleteQueues() {
        // TODO: implement test
    }

    @Test
    void adUpdateSegmentRules() {
        // TODO: implement test
    }

}
