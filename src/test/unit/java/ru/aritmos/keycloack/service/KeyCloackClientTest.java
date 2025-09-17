package ru.aritmos.keycloack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Тесты для {@link KeyCloackClient}.
 */
class KeyCloackClientTest {

    @Test
    void getBranchPathByBranchPrefixReturnsPath() {
        KeyCloackClient client = spy(new KeyCloackClient());
        client.keycloak = mock(Keycloak.class);
        GroupRepresentation group = new GroupRepresentation();
        group.setPath("/r/b");
        Map<String, List<String>> attrs = new HashMap<>();
        attrs.put("branchPrefix", List.of("PR"));
        group.setAttributes(attrs);
        doReturn(List.of(group)).when(client).getAllBranchesByRegionName(eq("region"), any());

        String path = client.getBranchPathByBranchPrefix("region", "PR");
        assertEquals("/r/b", path);
    }

    @Test
    void getBranchPathByBranchPrefixReturnsNullWhenNotFound() {
        KeyCloackClient client = spy(new KeyCloackClient());
        client.keycloak = mock(Keycloak.class);
        doReturn(Collections.emptyList()).when(client).getAllBranchesByRegionName(eq("region"), any());

        assertNull(client.getBranchPathByBranchPrefix("region", "PR"));
    }

    @Test
    void getAllBranchesByRegionIdCollectsBranchesRecursively() {
        KeyCloackClient client = new KeyCloackClient();
        client.realm = "realm";

        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        GroupsResource groupsResource = mock(GroupsResource.class);
        GroupResource rootGroup = mock(GroupResource.class);
        GroupResource subRegionGroup = mock(GroupResource.class);

        GroupRepresentation branch = new GroupRepresentation();
        branch.setAttributes(Map.of("type", List.of("branch")));
        GroupRepresentation region = new GroupRepresentation();
        region.setId("region2");
        region.setSubGroupCount(1L);
        region.setAttributes(Map.of("type", List.of("region")));
        GroupRepresentation nestedBranch = new GroupRepresentation();
        nestedBranch.setAttributes(Map.of("type", List.of("branch")));

        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.group("region1")).thenReturn(rootGroup);
        when(rootGroup.getSubGroups(0, 1000000000, false)).thenReturn(List.of(branch, region));
        when(groupsResource.group("region2")).thenReturn(subRegionGroup);
        when(subRegionGroup.getSubGroups(0, 1000000000, false)).thenReturn(List.of(nestedBranch));

        List<GroupRepresentation> result = client.getAllBranchesByRegionId("region1", keycloak);
        assertEquals(2, result.size());
        assertTrue(result.contains(branch));
        assertTrue(result.contains(nestedBranch));
    }

    @Test
    void getKeycloakReturnsExistingInstance() {
        KeyCloackClient client = new KeyCloackClient();
        Keycloak existing = mock(Keycloak.class);
        when(existing.isClosed()).thenReturn(false);
        client.keycloak = existing;

        Keycloak result = client.getKeycloak();
        assertSame(existing, result);
    }

    @Test
    void getUserInfoReturnsUserWhenFound() {
        KeyCloackClient client = new KeyCloackClient();
        client.realm = "realm";
        Keycloak keycloak = mock(Keycloak.class);
        client.keycloak = keycloak;

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserRepresentation user = new UserRepresentation();

        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search("user", true)).thenReturn(List.of(user));

        Optional<UserRepresentation> result = client.getUserInfo("user");
        assertTrue(result.isPresent());
        assertSame(user, result.get());
    }

    @Test
    void getUserInfoReturnsEmptyWhenUserMissing() {
        KeyCloackClient client = new KeyCloackClient();
        client.realm = "realm";
        Keycloak keycloak = mock(Keycloak.class);
        client.keycloak = keycloak;

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);

        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search("user", true)).thenReturn(Collections.emptyList());

        Optional<UserRepresentation> result = client.getUserInfo("user");
        assertTrue(result.isEmpty());
    }
}
