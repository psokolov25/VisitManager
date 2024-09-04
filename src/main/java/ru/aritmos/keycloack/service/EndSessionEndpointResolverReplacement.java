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
import lombok.Getter;

import java.util.Optional;
import java.util.function.Supplier;

@Singleton
@Replaces(EndSessionEndpointResolver.class)
@Getter
public class EndSessionEndpointResolverReplacement extends EndSessionEndpointResolver {
    private final BeanContext beanContext;
    @SuppressWarnings("rawtypes")
    private final TokenResolver tokenResolver;

    private final SecurityConfiguration securityConfiguration;


    /**
     * @param beanContext The bean context
     */
    @SuppressWarnings("rawtypes")
    public EndSessionEndpointResolverReplacement(BeanContext beanContext,
                                                 SecurityConfiguration securityConfiguration,
                                                 TokenResolver tokenResolver) {
        super(beanContext);
        this.beanContext = beanContext;
        this.tokenResolver = tokenResolver;
        this.securityConfiguration = securityConfiguration;
    }
    @SuppressWarnings(value = "unchecked")
    @Override
    public Optional<EndSessionEndpoint> resolve(OauthClientConfiguration oauthClientConfiguration,
                                                Supplier<OpenIdProviderMetadata> openIdProviderMetadata,
                                                EndSessionCallbackUrlBuilder endSessionCallbackUrlBuilder) {
        return Optional.of(new OktaEndSessionEndpoint(endSessionCallbackUrlBuilder,
                oauthClientConfiguration,
                openIdProviderMetadata,
                securityConfiguration,
                tokenResolver));
    }

}