package ru.aritmos.keycloack.service;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.annotation.*;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import ru.aritmos.keycloack.model.Credentials;

@Slf4j
@Singleton
public class KeyCloackClient {
  @Property(name = "micronaut.security.oauth2.clients.keycloak.techlogin")
  String techlogin;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.techpassword")
  String techpassword;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-id")
  String clientId;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-secret")
  String secret;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.realm")
  String realm;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.keycloakurl")
  String keycloakUrl;

  Keycloak keycloak;

  /**
   * Авторизация пользователя в Keycloak
   *
   * @param credentials логин и пароль пользователя
   * @return - данные авторизации (токен, токен обновления и т д )
   */
  public Optional<AuthorizationResponse> Auth(@Body Credentials credentials) {

    AuthzClient authzClient= getAuthzClient(secret, keycloakUrl, realm, clientId);


    return Optional.of(
        authzClient
            .authorization(credentials.getUsername(), credentials.getPassword())
            .authorize());
  }

  public static AuthzClient getAuthzClient(String secret, String keycloakUrl, String realm, String clientId) {
    Map<String, Object> clientCredentials = new HashMap<>();
    clientCredentials.put("secret", secret);
    clientCredentials.put("provider", "secret");

    Configuration configuration =
        new Configuration(keycloakUrl, realm, clientId, clientCredentials, null);

      return AuthzClient.create(configuration);
  }

  public Optional<UserRepresentation> getUserInfo(String userName) {

    AuthzClient authzClient= getAuthzClient(secret, keycloakUrl, realm, clientId);
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
      keycloak.close();
      return Optional.of(userRepresentationList.get(0));
    }
    if (keycloak.tokenManager() != null) {
      keycloak.tokenManager().logout();
    }
    keycloak.close();
    return Optional.empty();
  }

  /**
   * Выход сотрудника
   *
   * @param login логин сотрудника
   */
  public void userLogout(@PathVariable String login) {
    AuthzClient authzClient= getAuthzClient(secret, keycloakUrl, realm, clientId);

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
    keycloak.close();
  }
}
