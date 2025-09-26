package ru.aritmos.keycloack.service;

import static ru.aritmos.test.LoggingAssertions.*;

import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import com.nimbusds.jwt.JWTClaimsSet;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.oauth2.endpoint.token.response.JWTOpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class UserMapperTest {

    @DisplayName("Извлекает роли пользователя из клеймов")
    @Test
    void extractsRolesFromClaims() {
        UserMapper mapper = new UserMapper(new KeyCloackClient());

        LinkedTreeMap<String, Object> client = new LinkedTreeMap<>();
        client.put("roles", List.of("r1", "r2"));
        LinkedTreeMap<String, LinkedTreeMap<String, Object>> resourceAccess =
            new LinkedTreeMap<>();
        resourceAccess.put("client", client);
        LinkedTreeMap<String, Object> realmAccess = new LinkedTreeMap<>();
        realmAccess.put("roles", List.of("realm"));

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject("user123")
            .claim("resource_access", resourceAccess)
            .claim("realm_access", realmAccess)
            .build();

        OpenIdClaims claims = new JWTOpenIdClaims(claimsSet);

        AuthenticationResponse response =
            Mono.from(
                mapper.createAuthenticationResponse(
                    "keycloak", new OpenIdTokenResponse(), claims, null))
                .block();

        Authentication auth = response.getAuthentication().orElseThrow();
        assertEquals("user123", auth.getName());
        assertTrue(auth.getRoles().containsAll(List.of("r1", "r2", "realm")));
    }

    @DisplayName("Игнорирует некорректные структуры клеймов")
    @Test
    void ignoresInvalidClaimsStructures() {
        UserMapper mapper = new UserMapper(new KeyCloackClient());

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject("user123")
            .claim("resource_access", "bad")
            .claim("realm_access", "bad")
            .build();

        OpenIdClaims claims = new JWTOpenIdClaims(claimsSet);

        AuthenticationResponse response =
            Mono.from(
                mapper.createAuthenticationResponse(
                    "keycloak", new OpenIdTokenResponse(), claims, null))
                .block();

        Authentication auth = response.getAuthentication().orElseThrow();
        assertTrue(auth.getRoles().isEmpty());
    }
}

