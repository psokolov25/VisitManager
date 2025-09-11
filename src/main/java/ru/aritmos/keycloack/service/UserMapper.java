package ru.aritmos.keycloack.service;

import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.*;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Класс отвечает за маппинг свойств пользователя в Keycloak и стандартных свойств OpenId
 * авторизации
 */
@Slf4j
@Named("keycloak")
@Singleton
@Replaces(DefaultOpenIdAuthenticationMapper.class)
@SuppressWarnings("unused")
public class UserMapper implements OpenIdAuthenticationMapper {
  /** Клиент Keycloak для получения данных пользователя. */
  final KeyCloackClient keyCloackClient;

  // public class UserMapper {
  /** Идентификатор клиента Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-id")
  String clientId;

  /** Секрет клиента Keycloak. */
  @Property(name = "micronaut.security.oauth2.clients.keycloak.client-secret")
  String secret;

  /**
   * Конструктор маппера пользователя.
   *
   * @param keyCloackClient клиент Keycloak
   */
  public UserMapper(KeyCloackClient keyCloackClient) {
    this.keyCloackClient = keyCloackClient;
  }

  /**
   * Берет данные пользователя из openIdClaims с возвращенными данными из Keycloak, извлекает
   * свойства resource_access и realm_access и извлекает из них список ролей в виде списка строк и
   * формирует ответ Авторизации передавая параметром ид пользователя, роли пользователя, набор
   * аттрибутов пользователя
   *
   * @param providerName название провайдера данных пользователя (Keycloak)
   * @param tokenResponse набор токенов (токен доступа и токен обновления)
   * @param openIdClaims набор данных пользователя, передаваемые Keycloak
   * @param state состояние
   * @return данные авторизации
   */
  @SuppressWarnings("unchecked")
  @Override
  public @NonNull Publisher<AuthenticationResponse> createAuthenticationResponse(
      String providerName,
      OpenIdTokenResponse tokenResponse,
      OpenIdClaims openIdClaims,
      @io.micronaut.core.annotation.Nullable State state) {

    List<String> roles = new ArrayList<>();
    /*
     Извлечение из openIsClaims данных о клиентских ролях пользователя и заполнение ролями списка roles
    */
    if (openIdClaims.contains("resource_access")) {
      try {

        LinkedTreeMap<String, LinkedTreeMap<String, Object>> rolesHashMap =
            (LinkedTreeMap<String, LinkedTreeMap<String, Object>>)
                openIdClaims.get("resource_access");
        rolesHashMap.forEach(
            (key, value) -> {
              if (value.containsKey("roles")) {
                roles.addAll((List<String>) value.get("roles"));
              }
            });
      } catch (ClassCastException ignored) {

      }
    }
    /*
     Извлечение из openIsClaims данных о ролях Keycloak реалма пользователя и заполнение ролями списка roles
    */
    if (openIdClaims.contains("realm_access")) {
      try {
        LinkedTreeMap<String, Object> rolesHashMap =
            (LinkedTreeMap<String, Object>) openIdClaims.get("realm_access");
        rolesHashMap.forEach(
            (key, value) -> {
              if (key.equals("roles")) {
                roles.addAll((List<String>) value);
              }
            });
      } catch (Exception ignored) {

      }
    }

    /*
    Асинхронное успешное возвращение данных пользователя, с передачей идентификатора пользователя (возможно в будущем заменим на логин
    ,список ролей, и список аттрибутов пользователя
     */
    return Mono.just(openIdClaims)
        .map(m -> m)
        .map(
            user -> {
              return AuthenticationResponse.success(
                  user.getSubject(), roles, user.getClaims()); // (4)
            });
  }
}
