package ru.aritmos.keycloack.service;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.security.config.SecurityConfiguration;
import io.micronaut.security.oauth2.client.OpenIdProviderMetadata;
import io.micronaut.security.oauth2.configuration.OauthClientConfiguration;
import io.micronaut.security.oauth2.endpoint.endsession.request.EndSessionEndpoint;
import io.micronaut.security.oauth2.endpoint.endsession.request.EndSessionEndpointResolver;
import io.micronaut.security.oauth2.endpoint.endsession.request.OktaEndSessionEndpoint;
import io.micronaut.security.oauth2.endpoint.endsession.response.EndSessionCallbackUrlBuilder;
import io.micronaut.security.token.reader.TokenResolver;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;

/** Реализация резолвера URL завершения сессии с учётом Okta/Keycloak. */
@Singleton
@Replaces(EndSessionEndpointResolver.class)
@Getter
public class EndSessionEndpointResolverReplacement extends EndSessionEndpointResolver {
  /** Контекст бинов Micronaut. */
  private final BeanContext beanContext;

  /** Резолвер токена в запросе. */
  @SuppressWarnings("rawtypes")
  private final TokenResolver tokenResolver;

  /** Конфигурация безопасности. */
  private final SecurityConfiguration securityConfiguration;

  /**
   * Конструктор.
   *
   * @param beanContext контекст бинов Micronaut
   * @param securityConfiguration конфигурация безопасности
   * @param tokenResolver резолвер токена
   */
  @SuppressWarnings("all")
  public EndSessionEndpointResolverReplacement(
      BeanContext beanContext,
      SecurityConfiguration securityConfiguration,
      TokenResolver tokenResolver) {
    super(beanContext);
    this.beanContext = beanContext;
    this.tokenResolver = tokenResolver;
    this.securityConfiguration = securityConfiguration;
  }

  /**
   * Формирует end-session URL, совместимый с Okta/Keycloak, вместо стандартного резолвера.
   *
   * @param oauthClientConfiguration конфигурация OAuth2-клиента
   * @param openIdProviderMetadata ленивый провайдер метаданных OpenID
   * @param endSessionCallbackUrlBuilder построитель callback-URL завершения сессии
   * @return опционально сформированный эндпойнт завершения сессии
   */
  @SuppressWarnings(value = "unchecked")
  @Override
  public Optional<EndSessionEndpoint> resolve(
      OauthClientConfiguration oauthClientConfiguration,
      Supplier<OpenIdProviderMetadata> openIdProviderMetadata,
      EndSessionCallbackUrlBuilder endSessionCallbackUrlBuilder) {
    return Optional.of(
        new OktaEndSessionEndpoint(
            endSessionCallbackUrlBuilder,
            oauthClientConfiguration,
            openIdProviderMetadata,
            securityConfiguration,
            tokenResolver));
  }
}
