package ru.aritmos.keycloack.service;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.keycloack.model.Credentials;

@Slf4j
@Singleton
public class KeyCloackClient {
  @Inject EventService eventService;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.techlogin")
  String techlogin;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.techpassword")
  String techpassword;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-id")
  String clientId;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-secret")
  String secret;

  @Getter
  @Property(name = "micronaut.security.oauth2.clients.keycloak.realm")
  String realm;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.keycloakurl")
  String keycloakUrl;

  Keycloak keycloak;

  private static void keycloakLogout(Keycloak keycloak) {
    if (keycloak.tokenManager() != null) {
      try {
        keycloak.tokenManager().logout();
      } catch (Exception e) {
        log.warn(e.getMessage());
      }
    }
    keycloak.close();
  }

  public static AuthzClient getAuthzClient(
      String secret, String keycloakUrl, String realm, String clientId) {
    Map<String, Object> clientCredentials = new HashMap<>();
    clientCredentials.put("secret", secret);
    clientCredentials.put("provider", "secret");

    Configuration configuration =
        new Configuration(keycloakUrl, realm, clientId, clientCredentials, null);

    return AuthzClient.create(configuration);
  }

  public List<GroupRepresentation> getAllBranchesByRegionName(String regionName,Keycloak keycloak) {

    RealmResource resource = keycloak.realm(getRealm());
    String regionId =
        resource.groups().groups(0, 1000000000).stream()
            .filter(f -> f.getName().equals(regionName))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        String.format("Region %s not found!", regionName),
                        eventService,
                        HttpStatus.NOT_FOUND))
            .getId();
    return getAllBranchesByRegionId(regionId,keycloak);
  }

  public String getBranchPathByBranchPrefix(String regionName, String prefix) {
    AuthzClient authzClient = getAuthzClient(secret, keycloakUrl, realm, clientId);
    AuthorizationResponse t = authzClient.authorization(techlogin, techpassword).authorize();
    if (keycloak == null || keycloak.isClosed()) {
      keycloak = Keycloak.getInstance(keycloakUrl, realm, clientId, t.getToken());
    }
    String result= getAllBranchesByRegionName(regionName,keycloak).stream()
        .filter(
            f ->
                f.getAttributes().containsKey("branchPrefix")
                    && f.getAttributes().get("branchPrefix").contains(prefix))
        .findFirst()
        .orElse(new GroupRepresentation())
        .getPath();
    keycloakLogout(keycloak);
    return result;
  }


  public List<GroupRepresentation> getAllBranchesByRegionId(String regionId,Keycloak keycloak) {

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
                        result.addAll(getAllBranchesByRegionId(subGroup.getId(),keycloak));
                      }
                    });
    //keycloakLogout(keycloak);
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
              AuthzClient authzClient = getAuthzClient(secret, keycloakUrl, realm, clientId);
              AuthorizationResponse t =
                  authzClient.authorization(techlogin, techpassword).authorize();
              if (keycloak == null || keycloak.isClosed()) {
                keycloak = Keycloak.getInstance(keycloakUrl, realm, clientId, t.getToken());
              }
              keycloak
                  .realm(getRealm())
                  .users()
                  .get(p.getId())
                  .groups(0, 1000000000)
                  .forEach(f -> result.addAll(getAllBranchesByRegionId(f.getId(),keycloak)));
            });
    return result;
  }

  public Boolean isUserModuleTypeByUserName(String userName, String type) {

    AuthzClient authzClient = getAuthzClient(secret, keycloakUrl, realm, clientId);
    AuthorizationResponse t = authzClient.authorization(techlogin, techpassword).authorize();
    if (keycloak == null || keycloak.isClosed()) {
      keycloak = Keycloak.getInstance(keycloakUrl, realm, clientId, t.getToken());
    }
    RealmResource realmResource = keycloak.realm(realm);
    for (UserRepresentation f : realmResource.users().searchByUsername(userName, true)) {
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
            keycloakLogout(keycloak);
            return true;
          }
        }
      }
    }
    keycloakLogout(keycloak);
    return false;
  }

  /**
   * Авторизация пользователя в Keycloak
   *
   * @param credentials логин и пароль пользователя
   * @return - данные авторизации (токен, токен обновления и т д )
   */
  public Optional<AuthorizationResponse> Auth(@Body Credentials credentials) {

    AuthzClient authzClient = getAuthzClient(secret, keycloakUrl, realm, clientId);

    return Optional.of(
        authzClient
            .authorization(credentials.getUsername(), credentials.getPassword())
            .authorize());
  }

  public Optional<UserRepresentation> getUserInfo(String userName) {

    AuthzClient authzClient = getAuthzClient(secret, keycloakUrl, realm, clientId);
    AuthorizationResponse t = authzClient.authorization(techlogin, techpassword).authorize();
    if (keycloak == null || keycloak.isClosed()) {
      keycloak = Keycloak.getInstance(keycloakUrl, realm, clientId, t.getToken());
    }
    List<UserRepresentation> userRepresentationList =
        keycloak.realm(realm).users().search(userName, true);
    if (!userRepresentationList.isEmpty()) {
      if (keycloak.tokenManager() != null) {
        keycloak.tokenManager().logout();
      }
      //keycloak.close();
      return Optional.of(userRepresentationList.get(0));
    }
    if (keycloak.tokenManager() != null) {
      keycloak.tokenManager().logout();
    }
    //keycloak.close();
    return Optional.empty();
  }

  /**
   * Выход сотрудника
   *
   * @param login логин сотрудника
   */
  public void userLogout(@PathVariable String login) {
    AuthzClient authzClient = getAuthzClient(secret, keycloakUrl, realm, clientId);

    AuthorizationResponse t = authzClient.authorization(techlogin, techpassword).authorize();
    if (keycloak == null || keycloak.isClosed()) {
      keycloak = Keycloak.getInstance(keycloakUrl, realm, clientId, t.getToken());
    }
    keycloak
        .realm(realm)
        .users()
        .search(login, true)
        .forEach(
            f -> {
              keycloak.realm(realm).users().get(f.getId()).logout();
              log.info("{}", keycloak.serverInfo().getInfo());
            });
    if (keycloak.tokenManager() != null) {
      keycloak.tokenManager().logout();
    }
    //keycloak.close();
  }
}
