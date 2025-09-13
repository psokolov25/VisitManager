package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.*;

class ServicesTest {

    private Branch prepareBranch() {
        Branch branch = new Branch("b1", "Branch");
        Queue q1 = new Queue("q1", "Q1", "A", 1);
        Queue q2 = new Queue("q2", "Q2", "A", 1);
        branch.getQueues().put(q1.getId(), q1);
        branch.getQueues().put(q2.getId(), q2);

        Service s1 = new Service("s1", "S1", 10, "q1");
        Service s2 = new Service("s2", "S2", 10, "q2");
        branch.getServices().put(s1.getId(), s1);
        branch.getServices().put(s2.getId(), s2);

        WorkProfile wp1 = new WorkProfile("wp1", "WP1");
        wp1.getQueueIds().add("q1");
        branch.getWorkProfiles().put(wp1.getId(), wp1);

        User user = User.builder().currentWorkProfileId("wp1").build();
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setUser(user);
        branch.getServicePoints().put(sp.getId(), sp);

        return branch;
    }

    @Test
    void getAllServicesMarksAvailability() {
        Branch branch = prepareBranch();
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        Services services = new Services();
        services.branchService = branchService;

        List<Service> result = services.getAllServices("b1");
        assertEquals(2, result.size());
        assertTrue(branch.getServices().get("s1").getIsAvailable());
        assertFalse(branch.getServices().get("s2").getIsAvailable());
    }

    @Test
    void getAllAvailableServicesReturnsOnlyAvailable() {
        Branch branch = prepareBranch();
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        Services services = new Services();
        services.branchService = branchService;

        List<Service> result = services.getAllAvailableServices("b1");
        assertEquals(1, result.size());
        assertEquals("s1", result.get(0).getId());
    }
}

