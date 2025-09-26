package ru.aritmos.api;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import ru.aritmos.keycloack.model.Credentials;
import ru.aritmos.keycloack.service.KeyCloackClient;

/**
 * REST API для операций авторизации в Keycloak.
 *
 * <p>Контроллер предоставляет методы для получения токенов и завершения пользовательских сессий в
 * Keycloak.
 */
@Slf4j
@Controller
@SerdeImport(AuthorizationResponse.class)
@SuppressWarnings("unused")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
    @ApiResponse(responseCode = "401", description = "Не авторизован"),
    @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
    @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
    @ApiResponse(responseCode = "405", description = "Метод не поддерживается"),
    @ApiResponse(responseCode = "415", description = "Неподдерживаемый тип данных"),
    @ApiResponse(responseCode = "500", description = "Ошибка сервера")
})
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
   * Авторизует пользователя в Keycloak и возвращает набор токенов.
   *
   * @param credentials логин и пароль пользователя
   * @return ответ авторизации с набором токенов
   */
  @Operation(
      summary = "Авторизация пользователя в Keycloak",
      description = "Возвращает токены авторизации при корректных учётных данных",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешная авторизация",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthorizationResponse.class))),
        @ApiResponse(responseCode = "401", description = "Неверные учётные данные"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера Keycloak")
      })
  @Tag(name = "Полный список")
  @Tag(name = "Взаимодействие с Keycloak")
  @Post(uri = "/keycloak", consumes = "application/json", produces = "application/json")
  Optional<AuthorizationResponse> Auth(@Body Credentials credentials) {
    return keyCloackClient.Auth(credentials);
  }

  /**
   * Завершает активную сессию сотрудника в Keycloak.
   *
   * @param login логин сотрудника
   * @param isForced признак принудительного завершения сессии
   * @param reason причина завершения
   */
  @Operation(
      summary = "Завершение пользовательской сессии",
      description = "Удаляет активную сессию пользователя в Keycloak",
      responses = {
        @ApiResponse(responseCode = "200", description = "Сессия успешно завершена"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера Keycloak")
      })
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
