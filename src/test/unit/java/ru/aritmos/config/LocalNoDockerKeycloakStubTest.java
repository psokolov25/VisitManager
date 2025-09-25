package ru.aritmos.config;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

class LocalNoDockerKeycloakStubTest {

    @DisplayName("проверяется сценарий «returns predictable user data»")
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

    @DisplayName("проверяется сценарий «other methods return stubs»")
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
