package ru.aritmos.keycloack.service;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.context.BeanContext;
import io.micronaut.security.config.SecurityConfiguration;
import io.micronaut.security.oauth2.client.OpenIdProviderMetadata;
import io.micronaut.security.oauth2.configuration.OauthClientConfiguration;
import io.micronaut.security.oauth2.endpoint.endsession.response.EndSessionCallbackUrlBuilder;
import io.micronaut.security.oauth2.endpoint.endsession.request.EndSessionEndpoint;
import io.micronaut.security.oauth2.endpoint.endsession.request.OktaEndSessionEndpoint;
import io.micronaut.security.token.reader.TokenResolver;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EndSessionEndpointResolverReplacementTest {

    @DisplayName("Resolve Returns Okta Endpoint")
    @Test
    void resolveReturnsOktaEndpoint() {
        BeanContext beanContext = Mockito.mock(BeanContext.class);
        SecurityConfiguration securityConfiguration = Mockito.mock(SecurityConfiguration.class);
        TokenResolver tokenResolver = Mockito.mock(TokenResolver.class);
        EndSessionEndpointResolverReplacement resolver = new EndSessionEndpointResolverReplacement(beanContext, securityConfiguration, tokenResolver);

        OauthClientConfiguration clientConfig = Mockito.mock(OauthClientConfiguration.class);
        Supplier<OpenIdProviderMetadata> metadata = Mockito.mock(Supplier.class);
        EndSessionCallbackUrlBuilder callback = Mockito.mock(EndSessionCallbackUrlBuilder.class);

        Optional<EndSessionEndpoint> endpoint = resolver.resolve(clientConfig, metadata, callback);

        assertTrue(endpoint.isPresent(), "ожидается непустой результат");
        assertTrue(endpoint.get() instanceof OktaEndSessionEndpoint, "должен быть OktaEndSessionEndpoint");
    }
}

