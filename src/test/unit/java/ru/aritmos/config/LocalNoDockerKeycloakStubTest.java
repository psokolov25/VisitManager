package ru.aritmos.config;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

class LocalNoDockerKeycloakStubTest {

    @DisplayName("Возвращаются предсказуемые данные пользователя")
    @Test
    void returnsPredictableUserData() {
        LocalNoDockerKeycloakStub stub = new LocalNoDockerKeycloakStub();

        Optional<UserRepresentation> user = stub.getUserInfo("tester");

        assertTrue(user.isPresent());
        UserRepresentation u = user.get();
        assertEquals("tester", u.getUsername());
        assertEquals("Local", u.getFirstName());
        assertEquals("User", u.getLastName());
        assertEquals("tester@local", u.getEmail());
    }

    @DisplayName("Остальные методы возвращают заглушки")
    @Test
    void otherMethodsReturnStubs() {
        LocalNoDockerKeycloakStub stub = new LocalNoDockerKeycloakStub();

        List<GroupRepresentation> groups = stub.getAllBranchesOfUser("tester");
        assertTrue(groups.isEmpty());

        assertTrue(stub.isUserModuleTypeByUserName("tester", "type"));

        String path = stub.getBranchPathByBranchPrefix("region", "001");
        assertEquals("/region/001", path);
    }
}

