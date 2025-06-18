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

@Slf4j
@Controller
@SerdeImport(AuthorizationResponse.class)
@SuppressWarnings("unused")
public class KeyCloakController {
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
   * Удаления сеанса сотрудника
   *
   * @param login логин сотрудника
   */
  @Tag(name = "Полный список")
  @Tag(name = "Взаимодействие с Keycloak")
  @Post(
      uri = "/keycloak/users/{login}",
      consumes = "application/json",
      produces = "application/json")
  void DeleteSession(@PathVariable String login, @QueryValue(defaultValue = "false") Boolean isForced) {
    keyCloackClient.userLogout(login,isForced);
  }
}
