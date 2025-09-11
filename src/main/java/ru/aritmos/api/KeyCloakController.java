package ru.aritmos.api;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import ru.aritmos.keycloack.model.Credentials;
import ru.aritmos.keycloack.service.KeyCloackClient;

/** REST API для операций авторизации в Keycloak. */
@Slf4j
@Controller
@SerdeImport(AuthorizationResponse.class)
@SuppressWarnings("unused")
public class KeyCloakController {
  /** Технический логин Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.techlogin")
  String techlogin;

  /** Технический пароль Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.techpassword")
  String techpassword;

  /** Идентификатор клиента Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-id")
  String clientId;

  /** Realm Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.realm")
  String realm;

  /** URL сервера Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.keycloakurl")
  String keycloakUrl;

  /** Клиент для взаимодействия с Keycloak. */
  @Inject KeyCloackClient keyCloackClient;

  /**
   * Авторизация пользователя в Keycloak
   *
   * @param credentials логин и пароль пользователя
   * @return - данные авторизации (токен, токен обновления и т д )
   */
  @Tag(name = "Полный список")
  @Tag(name = "Взаимодействие с Keycloak")
  @Post(uri = "/keycloak", consumes = "application/json", produces = "application/json")
  Optional<AuthorizationResponse> Auth(@Body Credentials credentials) {

    return keyCloackClient.Auth(credentials);
  }

  /**
   * Удаление сеанса сотрудника.
   *
   * @param login логин сотрудника
   * @param isForced принудительно завершить сессию
   * @param reason причина завершения
   */
  @Tag(name = "Полный список")
  @Tag(name = "Взаимодействие с Keycloak")
  @Post(
      uri = "/keycloak/users/{login}",
      consumes = "application/json",
      produces = "application/json")
  @SuppressWarnings("all")
  void DeleteSession(
      @PathVariable String login,
      @QueryValue(defaultValue = "false") Boolean isForced,
      @QueryValue(defaultValue = "") String reason) {
    keyCloackClient.userLogout(login, isForced, reason);
  }
}
