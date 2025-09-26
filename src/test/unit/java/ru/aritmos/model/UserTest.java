package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import ru.aritmos.keycloack.service.KeyCloackClient;

class UserTest {
    @DisplayName("Конструктор заполняет поля при переданном клиенте Keycloak")
    @Test
    void constructorInitializesFieldsWithKeycloakClient() {
        KeyCloackClient client = mock(KeyCloackClient.class);
        when(client.isUserModuleTypeByUserName("user", "admin")).thenReturn(true);
        List<GroupRepresentation> branches = List.of(new GroupRepresentation());
        when(client.getAllBranchesOfUser("user")).thenReturn(branches);

        User user = new User("1", "user", client);

        assertTrue(user.getIsAdmin());
        assertEquals(branches, user.getAllBranches());
        assertEquals("user", user.getName());
        assertSame(client, user.getKeyCloackClient());
    }

    @DisplayName("Статус перерыва корректно влияет на расчёт длительности")
    @Test
    void breakStatusAndDurationCalculation() {
        User user = new User("2", "worker", null);
        assertFalse(user.isOnBreak());
        assertEquals(0L, user.getLastBreakDuration());

        user.setLastBreakStartTime(ZonedDateTime.now());
        assertTrue(user.isOnBreak());
        assertTrue(user.getLastBreakDuration() >= 0);

        user.setLastBreakEndTime(user.getLastBreakStartTime().plusSeconds(45));
        assertFalse(user.isOnBreak());
        assertEquals(45L, user.getLastBreakDuration());
    }

    @DisplayName("Конструктор генерирует идентификатор при его отсутствии")
    @Test
    void constructorGeneratesIdWhenNotProvided() {
        KeyCloackClient client = mock(KeyCloackClient.class);
        when(client.isUserModuleTypeByUserName("name", "admin")).thenReturn(false);
        when(client.getAllBranchesOfUser("name")).thenReturn(List.of());

        User user = new User("name", client);

        assertNotNull(user.getId());
        assertEquals("name", user.getName());
        assertFalse(Boolean.TRUE.equals(user.getIsAdmin()));
    }
}

