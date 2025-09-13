package ru.aritmos.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.service.BranchService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import org.junit.jupiter.api.Disabled;

class ManagementControllerTest {

    @Test
    void getBranchReturnsBranch() {
        BranchService branchService = mock(BranchService.class);
        Branch branch = new Branch("b1", "Branch");
        when(branchService.getBranch("b1")).thenReturn(branch);
        ManagementController controller = new ManagementController();
        controller.branchService = branchService;
        controller.keyCloakClient = mock(KeyCloackClient.class);
        assertSame(branch, controller.getBranch("b1"));
    }

    @Test
    void getBranchThrowsNotFound() {
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1"))
            .thenThrow(new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!"));
        ManagementController controller = new ManagementController();
        controller.branchService = branchService;
        controller.keyCloakClient = mock(KeyCloackClient.class);
        assertThrows(HttpStatusException.class, () -> controller.getBranch("b1"));
    }

    @Test
    void getBranchesWithoutUserReturnsAll() {
        BranchService branchService = mock(BranchService.class);
        HashMap<String, Branch> branches = new HashMap<>();
        when(branchService.getBranches()).thenReturn(branches);
        ManagementController controller = new ManagementController();
        controller.branchService = branchService;
        controller.keyCloakClient = mock(KeyCloackClient.class);
        assertSame(branches, controller.getBranches(null));
        verify(branchService).getBranches();
    }

    @Test
    void getBranchesWithUnknownUserFallsBackToAll() {
        BranchService branchService = mock(BranchService.class);
        HashMap<String, Branch> branches = new HashMap<>();
        when(branchService.getBranches()).thenReturn(branches);
        KeyCloackClient client = mock(KeyCloackClient.class);
        when(client.getUserInfo("u1")).thenReturn(Optional.empty());
        ManagementController controller = new ManagementController();
        controller.branchService = branchService;
        controller.keyCloakClient = client;
        assertSame(branches, controller.getBranches("u1"));
    }

    @Test
    void getTinyBranchesReturnsMappedList() {
        BranchService branchService = mock(BranchService.class);
        HashMap<String, Branch> branches = new HashMap<>();
        Branch b1 = new Branch("b1", "Branch");
        branches.put("b1", b1);
        when(branchService.getBranches()).thenReturn(branches);
        ManagementController controller = new ManagementController();
        controller.branchService = branchService;
        controller.keyCloakClient = mock(KeyCloackClient.class);
        List<TinyClass> tiny = controller.getTinyBranches();
        assertEquals(1, tiny.size());
        assertEquals("b1", tiny.get(0).getId());
    }
    @Disabled("Not yet implemented")
    @Test
    void getBranchTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void getBranchesTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void getTinyBranchesTest() {
        // TODO implement
    }

}
