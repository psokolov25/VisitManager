package ru.aritmos.keycloack.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.resource.AuthorizationResource;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.UserInfo;
import ru.aritmos.model.UserSession;
import ru.aritmos.model.UserToken;

/**
 * Расширенные сценарии для {@link KeyCloackClient}, покрывающие ранее непроверенные ветки.
 */
class KeyCloackClientCoverageTest {

    private static final Logger log = LoggerFactory.getLogger(KeyCloackClientCoverageTest.class);

    @DisplayName("проверяется сценарий «get user session by login returns session with max start time»")
    @Test
    void getUserSessionByLoginReturnsSessionWithMaxStartTime() {
        log.info("Готовим клиента Keycloak и пользователя для получения сессии");
        KeyCloackClient client = new KeyCloackClient();
        client.realm = "realm";
        Keycloak keycloak = mock(Keycloak.class);
        client.keycloak = keycloak;

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-id")).thenReturn(userResource);

        UserRepresentation user = new UserRepresentation();
        user.setId("user-id");
        user.setUsername("operator");
        user.setFirstName("Иван");
        user.setLastName("Иванов");
        user.setEmail("ivanov@example.com");

        UserSessionRepresentation oldSession = new UserSessionRepresentation();
        oldSession.setId("old-session");
        oldSession.setStart(100L);
        UserSessionRepresentation newSession = new UserSessionRepresentation();
        newSession.setId("new-session");
        newSession.setStart(200L);
        List<UserSessionRepresentation> sessions = List.of(oldSession, newSession);
        when(userResource.getUserSessions()).thenReturn(sessions);

        log.info("Вызываем getUserSessionByLogin для пользователя {}", user.getUsername());
        Optional<UserSession> result = client.getUserSessionByLogin(user);

        log.info("Проверяем, что возвращена последняя активная сессия");
        assertTrue(result.isPresent(), "Ожидаем непустой результат");
        UserSessionRepresentation expectedRepresentation =
                sessions.parallelStream()
                        .toList()
                        .parallelStream()
                        .max((m1, m2) -> Long.compare(m2.getStart(), m1.getStart()))
                        .orElseThrow();
        assertEquals(expectedRepresentation.getId(), result.get().getSid());
        assertEquals("operator", result.get().getLogin());
        assertEquals(
                URLEncoder.encode("Иван Иванов", StandardCharsets.UTF_8),
                result.get().getUserToken().getUser().getName());
    }

    @DisplayName("проверяется сценарий «get user session by login returns empty when user data invalid»")
    @Test
    void getUserSessionByLoginReturnsEmptyWhenUserDataInvalid() {
        log.info("Готовим клиента Keycloak и пользователя с некорректными данными");
        KeyCloackClient client = new KeyCloackClient();
        client.realm = "realm";
        Keycloak keycloak = mock(Keycloak.class);
        client.keycloak = keycloak;

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-id")).thenReturn(userResource);
        when(userResource.getUserSessions()).thenReturn(Collections.emptyList());

        UserRepresentation user = spy(new UserRepresentation());
        doReturn("user-id").when(user).getId();
        doReturn("operator").when(user).getUsername();
        doReturn("mail@example.com").when(user).getEmail();
        doThrow(new IllegalStateException("Некорректные ФИО")).when(user).getFirstName();

        log.info("Вызываем getUserSessionByLogin и ожидаем отлов исключения внутри метода");
        Optional<UserSession> result = client.getUserSessionByLogin(user);

        log.info("Проверяем, что при ошибке данных возвращается пустой Optional");
        assertTrue(result.isEmpty(), "Результат должен быть пустым");
    }

    @DisplayName("проверяется сценарий «is user module type by user name detects composite role type»")
    @Test
    void isUserModuleTypeByUserNameDetectsCompositeRoleType() {
        log.info("Готовим структуру ролей для проверки типа модуля пользователя");
        KeyCloackClient client = new KeyCloackClient();
        client.realm = "realm";
        Keycloak keycloak = mock(Keycloak.class);
        client.keycloak = keycloak;

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleByIdResource roleByIdResource = mock(RoleByIdResource.class);

        UserRepresentation user = new UserRepresentation();
        user.setId("user-id");

        RoleRepresentation role = new RoleRepresentation();
        role.setId("role-id");
        MappingsRepresentation mappings = new MappingsRepresentation();
        mappings.setRealmMappings(List.of(role));

        RoleRepresentation composite = new RoleRepresentation();
        composite.setId("composite-id");
        composite.setAttributes(Map.of("type", List.of("admin")));

        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByUsername("operator", true)).thenReturn(List.of(user));
        when(usersResource.get("user-id")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(mappings);
        when(realmResource.rolesById()).thenReturn(roleByIdResource);
        when(roleByIdResource.getRoleComposites("role-id")).thenReturn(Set.of(composite));
        when(roleByIdResource.getRole("composite-id")).thenReturn(composite);

        log.info("Вызываем проверку принадлежности пользователя к типу admin");
        Boolean result = client.isUserModuleTypeByUserName("operator", "admin");

        log.info("Убеждаемся, что тип распознан корректно");
        assertTrue(result, "Ожидаем, что пользователь принадлежит типу admin");
    }

    @DisplayName("проверяется сценарий «get user by sid returns user when session exists»")
    @Test
    void getUserBySidReturnsUserWhenSessionExists() {
        log.info("Готовим клиентов и сессии Keycloak для поиска пользователя по sid");
        KeyCloackClient client = new KeyCloackClient();
        client.realm = "realm";
        Keycloak keycloak = mock(Keycloak.class);
        client.keycloak = keycloak;

        RealmResource realmResource = mock(RealmResource.class);
        ClientsResource clientsResource = mock(ClientsResource.class);
        ClientResource clientResource = mock(ClientResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setId("client-id");
        UserSessionRepresentation session = new UserSessionRepresentation();
        session.setId("target-sid");
        session.setUserId("user-id");
        UserRepresentation user = new UserRepresentation();

        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findAll()).thenReturn(List.of(clientRepresentation));
        when(clientsResource.get("client-id")).thenReturn(clientResource);
        when(clientResource.getUserSessions(0, 1000000000)).thenReturn(List.of(session));
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-id")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(user);

        log.info("Запрашиваем пользователя по сессии target-sid");
        Optional<UserRepresentation> result = client.getUserBySid("target-sid");

        log.info("Проверяем, что пользователь найден");
        assertTrue(result.isPresent(), "Ожидаем найденного пользователя");
        assertSame(user, result.get());
    }

    @DisplayName("проверяется сценарий «user logout sends events and updates session params»")
    @Test
    void userLogoutSendsEventsAndUpdatesSessionParams() {
        log.info("Готовим KeyCloackClient для сценария выхода пользователя");
        KeyCloackClient client = spy(new KeyCloackClient());
        client.realm = "realm";
        client.techlogin = "tech";
        client.techpassword = "pass";
        client.clientId = "client";
        client.secret = "secret";
        client.keycloakUrl = "http://keycloak";

        EventService eventService = mock(EventService.class);
        client.eventService = eventService;

        Keycloak keycloak = mock(Keycloak.class);
        client.keycloak = keycloak;
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        ServerInfoResource serverInfoResource = mock(ServerInfoResource.class);
        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-id")).thenReturn(userResource);
        when(usersResource.list()).thenReturn(List.of(createUser("user-id", "operator")));
        when(keycloak.serverInfo()).thenReturn(serverInfoResource);
        ServerInfoRepresentation serverInfo = new ServerInfoRepresentation();
        when(serverInfoResource.getInfo()).thenReturn(serverInfo);

        UserSession session = UserSession.builder()
                .sid("sid")
                .userToken(UserToken.builder().user(UserInfo.builder().name("encoded").build()).build())
                .build();
        doReturn(Optional.of(session)).when(client).getUserSessionByLogin(any(UserRepresentation.class));

        AuthzClient authzClient = mock(AuthzClient.class);
        AuthorizationResource authorizationResource = mock(AuthorizationResource.class);
        when(authzClient.authorization("tech", "pass")).thenReturn(authorizationResource);
        when(authorizationResource.authorize()).thenReturn(new AuthorizationResponse());

        try (MockedStatic<KeyCloackClient> mocked = mockStatic(KeyCloackClient.class)) {
            mocked.when(() -> KeyCloackClient.getAuthzClient("secret", "http://keycloak", "realm", "client"))
                    .thenReturn(authzClient);
            log.info("Вызываем userLogout для принудительного выхода пользователя");
            client.userLogout("operator", true, "maintenance");
        }

        log.info("Проверяем, что параметры сессии дополнены информацией о выходе");
        assertEquals("true", session.getParams().get("isForced"));
        assertEquals("maintenance", session.getParams().get("reason"));

        log.info("Проверяем публикацию событий и завершение сессии в Keycloak");
        verify(eventService).send(eq("frontend"), eq(false), any(Event.class));
        verify(eventService).send(eq("stat"), eq(false), any(Event.class));
        verify(userResource).logout();
    }

    @DisplayName("проверяется сценарий «get all branches by region name returns recursive branches»")
    @Test
    void getAllBranchesByRegionNameReturnsRecursiveBranches() {
        log.info("Готовим KeyCloackClient и структуру регионов для поиска отделений");
        KeyCloackClient client = spy(new KeyCloackClient());
        client.realm = "realm";
        client.eventService = mock(EventService.class);

        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        GroupsResource groupsResource = mock(GroupsResource.class);
        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.groups()).thenReturn(groupsResource);

        GroupRepresentation region = new GroupRepresentation();
        region.setId("region-1");
        region.setName("Центр");
        when(groupsResource.groups(0, 1000000000)).thenReturn(List.of(region));

        List<GroupRepresentation> expectedBranches = List.of(new GroupRepresentation(), new GroupRepresentation());
        doReturn(expectedBranches).when(client).getAllBranchesByRegionId("region-1", keycloak);

        log.info("Вызываем getAllBranchesByRegionName для региона Центр");
        List<GroupRepresentation> result = client.getAllBranchesByRegionName("Центр", keycloak);

        log.info("Проверяем, что найденный регион передан в рекурсивный метод и возвращены ожидаемые отделения");
        assertEquals(expectedBranches, result, "Список отделений должен совпадать с подготовленным");
        verify(client).getAllBranchesByRegionId("region-1", keycloak);
    }

    @DisplayName("проверяется сценарий «get all branches by region name throws when region missing»")
    @Test
    void getAllBranchesByRegionNameThrowsWhenRegionMissing() {
        log.info("Готовим KeyCloackClient и список регионов без совпадений");
        KeyCloackClient client = new KeyCloackClient();
        client.realm = "realm";
        EventService eventService = mock(EventService.class);
        client.eventService = eventService;

        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        GroupsResource groupsResource = mock(GroupsResource.class);
        when(keycloak.realm("realm")).thenReturn(realmResource);
        when(realmResource.groups()).thenReturn(groupsResource);

        GroupRepresentation anotherRegion = new GroupRepresentation();
        anotherRegion.setId("region-2");
        anotherRegion.setName("Север");
        when(groupsResource.groups(0, 1000000000)).thenReturn(List.of(anotherRegion));

        log.info("Вызываем getAllBranchesByRegionName и ожидаем HttpStatusException");
        HttpStatusException thrown = assertThrows(
            HttpStatusException.class,
            () -> client.getAllBranchesByRegionName("Центр", keycloak)
        );

        log.info("Проверяем, что возврат произошел с кодом 404 и отправкой события");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Region Центр not found", thrown.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
    }

    private UserRepresentation createUser(String id, String username) {
        UserRepresentation user = new UserRepresentation();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}