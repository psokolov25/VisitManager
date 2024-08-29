package ru.aritmos.keycloack.service;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.oauth2.endpoint.authorization.response.DefaultOauthAuthorizationResponse;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.OauthAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.TokenResponse;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Slf4j
@Named("keycloak")
@Singleton
@Replaces(DefaultOauthAuthorizationResponse.class)
public class KeyCloackAuthenticationMapper implements OauthAuthenticationMapper {
    final KeyCloackClient keyCloackClient;

    public KeyCloackAuthenticationMapper(KeyCloackClient keyCloackClient) {
        this.keyCloackClient = keyCloackClient;
    }
    private static String basicAuthToken(String token) {
        return "Bearer " + token;
    }
    @Override
    public Publisher<AuthenticationResponse> createAuthenticationResponse(TokenResponse tokenResponse, @Nullable State state) {
        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("token", tokenResponse.getAccessToken());
        return Mono.from(keyCloackClient.getUserInfo("Aritmos",stringObjectHashMap,  basicAuthToken(tokenResponse.getAccessToken()))).map(m -> m)
                .map(user -> {


                    return AuthenticationResponse.success(user.getName(),user.getRoles()); // (4)
                });
    }
}
