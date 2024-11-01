package ru.aritmos.keycloack.service;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.http.annotation.*;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.client.AuthzClient;
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

  @Property(name = "micronaut.security.oauth2.clients.keycloak.realm")
  String realm;

  @Property(name = "micronaut.security.oauth2.clients.keycloak.keycloakurl")
  String keycloakUrl;

  DefaultClassPathResourceLoader resourceLoader =
      new DefaultClassPathResourceLoader(this.getClass().getClassLoader());

  /**
   * Авторизация пользователя в Keycloak
   *
   * @param credentials логин и пароль пользователя
   * @return - данные авторизации (токен, токен обновления и т д )
   */
  public Optional<AuthorizationResponse> Auth(@Body Credentials credentials) {
    Optional<InputStream> res = resourceLoader.getResourceAsStream("keycloak.json");
    if (res.isPresent()) {
      AuthzClient authzClient = AuthzClient.create(res.get());

      return Optional.of(
          authzClient.authorization(credentials.getLogin(), credentials.getPassword()).authorize());
    }
    return Optional.empty();
  }

  /**
   * Удаления сеанса сотрудника
   *
   * @param login логин сотрудника
   */
  public void DeleteSession(@PathVariable String login) {
    Optional<InputStream> res = resourceLoader.getResourceAsStream("keycloak.json");
    if (res.isPresent()) {
      AuthzClient authzClient = AuthzClient.create(res.get());
      AuthorizationResponse t = authzClient.authorization(techlogin, techpassword).authorize();
      Keycloak keycloak = Keycloak.getInstance(keycloakUrl, realm, clientId, t.getToken());

      List<UserRepresentation> users = keycloak.realm(realm).users().search(login);
      if (!users.isEmpty()) {
        keycloak
            .realm(realm)
            .users()
            .get(users.get(0).getId())
            .getUserSessions()
            .forEach(
                f -> {
                  log.info("{}", f);
                  keycloak.realm(realm).deleteSession(f.getId(), false);
                });

        log.info("{}", keycloak.serverInfo().getInfo());
      }
    }
  }
}
