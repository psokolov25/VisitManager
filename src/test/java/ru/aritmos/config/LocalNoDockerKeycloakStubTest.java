package ru.aritmos.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;

class LocalNoDockerKeycloakStubTest {

    LocalNoDockerKeycloakStub stub = new LocalNoDockerKeycloakStub();

    @Test
    void getUserInfoReturnsFilledUser() {
        Optional<UserRepresentation> user = stub.getUserInfo("tester");
        assertTrue(user.isPresent(), "должен вернуть пользователя");
        assertEquals("tester", user.get().getUsername());
        assertEquals("Local", user.get().getFirstName());
        assertEquals("User", user.get().getLastName());
        assertEquals("tester@local", user.get().getEmail());
    }

    @Test
    void getBranchPathByBranchPrefixBuildsPath() {
        String path = stub.getBranchPathByBranchPrefix("region", "001");
        assertEquals("/region/001", path);
    }
}

