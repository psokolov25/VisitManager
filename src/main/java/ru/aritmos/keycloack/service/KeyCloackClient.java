package ru.aritmos.keycloack.service;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.keycloack.model.Credentials;
import ru.aritmos.model.UserInfo;
import ru.aritmos.model.UserSession;
import ru.aritmos.model.UserToken;

/**
 * Клиент для взаимодействия с Keycloak: пользователи, группы, сессии.
 */
@Slf4j
@Singleton
@Requires(notEnv = "local-no-docker")
@SuppressWarnings("all")
public class KeyCloackClient {
  /** Сервис отправки событий. */
  @Inject EventService eventService;

  /** Технический логин для доступа к Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.techlogin")
  String techlogin;

  /** Технический пароль для доступа к Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.techpassword")
  String techpassword;

  /** Идентификатор клиента Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-id")
  String clientId;

  /** Секрет клиента Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-secret")
  String secret;

  @Getter
  /** Realm Keycloak, в котором выполняются операции. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.realm")
  String realm;

  /** URL сервера Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.keycloakurl")
  String keycloakUrl;

  /** Экземпляр клиента Keycloak (ленивая инициализация). */
  Keycloak keycloak;

  //  private static void keycloakLogout(Keycloak keycloak) {
  //    if (keycloak.tokenManager() != null) {
  //      try {
  //        keycloak.tokenManager().logout();
  //      } catch (Exception e) {
  //        log.warn(e.getMessage());
  //      }
  //    }
  //    keycloak.close();
  //  }

  /**
   * Создать клиент авторизации Keycloak.
   *
   * @param secret секрет клиента
   * @param keycloakUrl URL сервера Keycloak
   * @param realm realm
   * @param clientId идентификатор клиента
   * @return клиент авторизации
   */
  public static AuthzClient getAuthzClient(
      String secret, String keycloakUrl, String realm, String clientId) {
    Map<String, Object> clientCredentials = new HashMap<>();
    clientCredentials.put("secret", secret);
    clientCredentials.put("provider", "secret");

    Configuration configuration =
        new Configuration(keycloakUrl, realm, clientId, clientCredentials, null);

    return AuthzClient.create(configuration);
  }

  /**
   * Получить все отделения по имени региона.
   *
   * @param regionName имя региона
   * @param keycloak клиент Keycloak
   * @return список групп
   */
  public List<GroupRepresentation> getAllBranchesByRegionName(
      String regionName, Keycloak keycloak) {

    RealmResource resource = keycloak.realm(getRealm());
    String regionId =
        resource.groups().groups(0, 1000000000).stream()
            .filter(f -> f.getName().equals(regionName))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        String.format("Region %s not found", regionName),
                        eventService,
                        HttpStatus.NOT_FOUND))
            .getId();
    return getAllBranchesByRegionId(regionId, keycloak);
  }

  /**
   * Получить путь группы отделения по префиксу.
   *
   * @param regionName имя региона
   * @param prefix префикс отделения
   * @return путь группы
   */
  public String getBranchPathByBranchPrefix(String regionName, String prefix) {

    String result =
        getAllBranchesByRegionName(regionName, getKeycloak()).stream()
            .filter(
                f ->
                    f.getAttributes().containsKey("branchPrefix")
                        && f.getAttributes().get("branchPrefix").contains(prefix))
            .findFirst()
            .orElse(new GroupRepresentation())
            .getPath();
    // keycloakLogout(keycloak);
    return result;
  }

  /**
   * Получить все отделения рекурсивно по идентификатору региона.
   *
   * @param regionId идентификатор региона
   * @param keycloak клиент Keycloak
   * @return список групп
   */
  public List<GroupRepresentation> getAllBranchesByRegionId(String regionId, Keycloak keycloak) {

    RealmResource resource = keycloak.realm(getRealm());
    List<GroupRepresentation> result = new ArrayList<>();

    resource
        .groups()
        .group(regionId)
        .getSubGroups(0, 1000000000, false)
        .forEach(
            subGroup -> {
              if (subGroup.getAttributes() != null
                  && subGroup.getAttributes().containsKey("type")
                  && subGroup.getAttributes().get("type").contains("branch")) {
                result.add(subGroup);
              }
              if (subGroup.getAttributes() != null
                  && subGroup.getAttributes().containsKey("type")
                  && subGroup.getAttributes().get("type").contains("region")
                  && subGroup.getSubGroupCount() > 0) {
                result.addAll(getAllBranchesByRegionId(subGroup.getId(), keycloak));
              }
            });

    return result;
  }

  /**
   * Получение всех отделений, к которым пользователь имеет доступ
   *
   * @param username имя пользователя
   * @return список групп формате keycloak GroupRepresentation
   */
  public List<GroupRepresentation> getAllBranchesOfUser(String username) {

    List<GroupRepresentation> result = new ArrayList<>();
    getUserInfo(username)
        .ifPresent(
            p -> {
              getKeycloak()
                  .realm(getRealm())
                  .users()
                  .get(p.getId())
                  .groups(0, 1000000000)
                  .forEach(f -> result.addAll(getAllBranchesByRegionId(f.getId(), keycloak)));
            });
    // keycloakLogout(keycloak);
    return result;
  }

  /**
   * Найти пользователя по идентификатору сессии.
   *
   * @param sid идентификатор сессии
   * @return пользователь, если найден
   */
  public Optional<UserRepresentation> getUserBySid(String sid) {

    RealmResource realmResource = getKeycloak().realm(realm);
    for (ClientRepresentation f : realmResource.clients().findAll()) {

      List<UserSessionRepresentation> t =
          realmResource.clients().get(f.getId()).getUserSessions(0, 1000000000).stream().toList();

      if (t.stream().filter(session -> session.getId().equals(sid)).count() > 0) {
        UserRepresentation user =
            realmResource
                .users()
                .get(
                    t.stream()
                        .filter(session -> session.getId().equals(sid))
                        .toList()
                        .get(0)
                        .getUserId())
                .toRepresentation();
        return Optional.of(user);
      }
    }
    return Optional.empty();
  }

  /**
   * Проверить принадлежность пользователя к типу модуля.
   *
   * @param userName логин пользователя
   * @param type тип модуля (например, admin)
   * @return признак принадлежности
   */
  public Boolean isUserModuleTypeByUserName(String userName, String type) {

    RealmResource realmResource = getKeycloak().realm(realm);
    for (UserRepresentation f : realmResource.users().searchByUsername(userName, true)) {
      if (realmResource.users().get(f.getId()).roles().getAll().getRealmMappings() != null) {
        for (RoleRepresentation role :
            realmResource.users().get(f.getId()).roles().getAll().getRealmMappings()) {
          for (String compositeId :
              realmResource.rolesById().getRoleComposites(role.getId()).stream()
                  .map(RoleRepresentation::getId)
                  .toList()) {
            if (realmResource.rolesById().getRole(compositeId).getAttributes().containsKey("type")
                && realmResource
                    .rolesById()
                    .getRole(compositeId)
                    .getAttributes()
                    .get("type")
                    .contains(type)) {
              // keycloakLogout(keycloak);
              return true;
            }
          }
        }
      }
    }
    // keycloakLogout(keycloak);
    return false;
  }

  /**
   * Авторизация пользователя в Keycloak
   *
   * @param credentials логин и пароль пользователя
   * @return - данные авторизации (токен, токен обновления и т д)
   */
  public Optional<AuthorizationResponse> Auth(@Body Credentials credentials) {

    AuthzClient authzClient = getAuthzClient(secret, keycloakUrl, realm, clientId);

    return Optional.of(
        authzClient
            .authorization(credentials.getUsername(), credentials.getPassword())
            .authorize());
  }

  /**
   * Получить информацию о пользователе.
   *
   * @param userName логин
   * @return пользователь, если найден
   */
  public Optional<UserRepresentation> getUserInfo(String userName) {

    List<UserRepresentation> userRepresentationList =
        getKeycloak().realm(realm).users().search(userName, true);
    if (!userRepresentationList.isEmpty()) {

      // keycloak.close();
      return Optional.of(userRepresentationList.get(0));
    }

    // keycloak.close();
    return Optional.empty();
  }

  /**
   * Получить информацию о сессии пользователя.
   *
   * @param user пользователь Keycloak
   * @return сессия пользователя, если есть
   */
  public Optional<UserSession> getUserSessionByLogin(UserRepresentation user) {
    RealmResource realmResource = getKeycloak().realm(realm);
    Optional<UserSessionRepresentation> userSessionRepresentation =
        realmResource.users().get(user.getId()).getUserSessions().parallelStream()
            .toList()
            .parallelStream()
            .max((m1, m2) -> Long.compare(m2.getStart(), m1.getStart()));
    try {
      UserSession userSession =
          UserSession.builder()
              .sid(
                  userSessionRepresentation.isPresent()
                      ? userSessionRepresentation.get().getId()
                      : UUID.randomUUID().toString())
              .create_session(Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime()).getTime())
              .login(user.getUsername())
              .last_update(Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime()).getTime())
              .userToken(
                  UserToken.builder()
                      .user(
                          UserInfo.builder()
                              .email(user.getEmail())
                              .sub(user.getId())
                              .name(
                                  URLEncoder.encode(
                                      user.getFirstName() + " " + user.getLastName(),
                                      StandardCharsets.UTF_8))
                              .description("")
                              .build())
                      .build())
              .build();
      return Optional.of(userSession);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Выход сотрудника.
   *
   * @param login логин сотрудника
   * @param isForced принудительный выход
   * @param reason причина
   */
  public void userLogout(@PathVariable String login, Boolean isForced, String reason) {
    AuthzClient authzClient = getAuthzClient(secret, keycloakUrl, realm, clientId);

    AuthorizationResponse t = authzClient.authorization(techlogin, techpassword).authorize();

    getKeycloak().realm(realm).users().list().stream()
        .filter(f -> f.getUsername().equals(login))
        .forEach(
            f -> {
              Optional<UserSession> userSession = getUserSessionByLogin(f);
              if (userSession.isPresent()) {
                userSession
                    .get()
                    .setParams(
                        new HashMap<>(Map.of("isForced", isForced.toString(), "reason", reason)));
              }
              eventService.send(
                  "frontend",
                  false,
                  Event.builder()
                      .senderService("visitmanager")
                      .eventType(
                          !isForced
                              ? "PROCESSING_USER_LOGOUT_NOT_FORCE"
                              : "PROCESSING_USER_LOGOUT_FORCE")
                      .body(userSession)
                      .build());
              eventService.send(
                  "stat",
                  false,
                  Event.builder()
                      .senderService("visitmanager")
                      .eventType("KEYCLOACK_USER_LOGOUT")
                      .params(Map.of("isForced", isForced.toString(), "reason", reason))
                      .body(userSession)
                      .build());
              keycloak.realm(realm).users().get(f.getId()).logout();
              log.info("{}", keycloak.serverInfo().getInfo());
            });
    /*if (keycloak.tokenManager() != null) {
      keycloak.tokenManager().logout();
    }*/
    // keycloak.close();
  }

  /**
   * Получить/инициализировать клиент Keycloak.
   *
   * @return клиент Keycloak
   */
  public Keycloak getKeycloak() {
    if (keycloak == null || keycloak.isClosed()) {
      keycloak =
          KeycloakBuilder.builder()
              .clientId(clientId)
              .clientSecret(secret)
              .realm(realm)
              .username(techlogin)
              .password(techpassword)
              .serverUrl(keycloakUrl)
              .build();
      return keycloak;
    } else {

      return keycloak;
    }
  }
}
