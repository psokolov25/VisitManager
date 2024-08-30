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
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Named("keycloak")
@Singleton
@Replaces(DefaultOpenIdAuthenticationMapper.class)
public class UserMapper implements OpenIdAuthenticationMapper {
    //public class UserMapper {
    @Property(name = "micronaut.security.oauth2.clients.keycloak.client-id")
    String clientId;
    @Property(name = "micronaut.security.oauth2.clients.keycloak.client-secret")
    String secret;
    final KeyCloackClient keyCloackClient;

    public UserMapper(KeyCloackClient keyCloackClient) {
        this.keyCloackClient = keyCloackClient;
    }

    //    @Override
//    public Publisher<AuthenticationResponse> createAuthenticationResponse(@NotNull TokenResponse tokenResponse, @Nullable State state) { // (3)
//        log.error(tokenResponse.getAccessToken());
//        Object o = tokenResponse;
//        return Flux.from(keyCloackClient.get(tokenResponse.getAccessToken())).map(m -> m)
//                .map(user -> {
//
//                    return AuthenticationResponse.success(user.toString()); // (4)
//                });
//    }
//    private static String basicAuth(String username, String password) {
//        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
//    }

    private static String basicAuthToken(String token) {
        return "Bearer " + token;
    }

    //@Override
    public @NonNull Publisher<AuthenticationResponse> createAuthenticationResponse(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims, @io.micronaut.core.annotation.Nullable State state) {


        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("token", tokenResponse.getAccessToken());
        List<String> roles = new ArrayList<String>();
        if (openIdClaims.contains("resource_access")) {
            LinkedTreeMap<String, LinkedTreeMap<String, Object>> rolesHashMap = (LinkedTreeMap<String, LinkedTreeMap<String, Object>>) openIdClaims.get("resource_access");
            rolesHashMap.entrySet().forEach(k ->
            {
                        if(k.getValue().containsKey("roles"))
                        {
                            roles.addAll((List<String>) k.getValue().get("roles"));
                        }
            });

        }
        if (openIdClaims.contains("realm_access")) {
            LinkedTreeMap<String, Object> rolesHashMap = (LinkedTreeMap<String, Object>) openIdClaims.get("realm_access");
            rolesHashMap.entrySet().forEach(k ->
            {
                if(k.getKey().equals("roles"))
                {
                    roles.addAll((List<String>) k.getValue());
                }
            });

        }

        //return AuthenticationResponse.success(user.getPreferred_username(),user.getRoles().stream().distinct().toList()); // (4)
        return Mono.just(openIdClaims).map(m -> m)
                .map(user -> {
                            return AuthenticationResponse.success(user.getSubject(), roles,user.getClaims()); // (4)
                        }
                );

    }
}
