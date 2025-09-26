package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.service.BranchService;
import ru.aritmos.keycloack.service.KeyCloackClient;

class ManagementControllerTest {

    @DisplayName("Контроллер возвращает запись отделения по идентификатору")
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

    @DisplayName("Контроллер возвращает ошибку 404 при отсутствии записи отделения")
    @Test
    void getBranchThrowsNotFound() {
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1"))
            .thenThrow(new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        ManagementController controller = new ManagementController();
        controller.branchService = branchService;
        controller.keyCloakClient = mock(KeyCloackClient.class);
        assertThrows(HttpStatusException.class, () -> controller.getBranch("b1"));
    }

    @DisplayName("Контроллер без указания пользователя возвращает полный список отделений")
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

    @DisplayName("Контроллер для неизвестного пользователя возвращает полный список отделений")
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

    @DisplayName("Контроллер для администратора возвращает весь перечень отделений")
    @Test
    void getBranchesForAdminВозвращаетВсеОтделения() {
        BranchService branchService = mock(BranchService.class);
        HashMap<String, Branch> branches = new HashMap<>();
        Branch branch = new Branch("b1", "Branch");
        branches.put("b1", branch);
        when(branchService.getBranches()).thenReturn(branches);

        KeyCloackClient client = mock(KeyCloackClient.class);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("admin");
        when(client.getUserInfo("admin")).thenReturn(Optional.of(user));
        when(client.isUserModuleTypeByUserName("admin", "admin")).thenReturn(true);
        when(client.getAllBranchesOfUser("admin")).thenReturn(Collections.emptyList());

        ManagementController controller = new ManagementController();
        controller.branchService = branchService;
        controller.keyCloakClient = client;

        Map<String, Branch> result = controller.getBranches("admin");

        assertEquals(branches, result);
    }

    @DisplayName("Контроллер фильтрует список отделений по доступам пользователя")
    @Test
    void getBranchesФильтруетДоступныеОтделенияДляПользователя() {
        BranchService branchService = mock(BranchService.class);
        HashMap<String, Branch> branches = new HashMap<>();
        Branch allowed = new Branch("b1", "Branch1");
        allowed.setPrefix("BR1");
        Branch denied = new Branch("b2", "Branch2");
        denied.setPrefix("BR2");
        branches.put("b1", allowed);
        branches.put("b2", denied);
        when(branchService.getBranches()).thenReturn(branches);

        KeyCloackClient client = mock(KeyCloackClient.class);
        UserRepresentation userInfo = new UserRepresentation();
        userInfo.setUsername("user");
        when(client.getUserInfo("user")).thenReturn(Optional.of(userInfo));

        GroupRepresentation group = new GroupRepresentation();
        HashMap<String, List<String>> attrs = new HashMap<>();
        attrs.put("branchPrefix", List.of("BR1"));
        group.setAttributes(attrs);
        when(client.getAllBranchesOfUser("user")).thenReturn(List.of(group));
        when(client.isUserModuleTypeByUserName("user", "admin"))
                .thenReturn(false)
                .thenThrow(new RuntimeException("fail"));

        ManagementController controller = new ManagementController();
        controller.branchService = branchService;
        controller.keyCloakClient = client;

        Map<String, Branch> result = controller.getBranches("user");

        assertEquals(1, result.size());
        assertTrue(result.containsKey("b1"));
        assertFalse(result.containsKey("b2"));
    }

    @DisplayName("Контроллер возвращает компактный список отделений в сокращённом формате")
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
}
