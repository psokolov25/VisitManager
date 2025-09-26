package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;

class RealmAccessTest {

    @DisplayName("Строитель и сеттеры формируют полный доступ")
    @Test
    void builderAndSettersConstructFullAccess() {
        GroupRepresentation branch = new GroupRepresentation();
        branch.setName("branch-1");
        GroupRepresentation group = new GroupRepresentation();
        group.setName("group-1");

        RealmAccess viaBuilder = RealmAccess.builder()
                .roles(List.of("admin"))
                .rolesWithModules(Map.of("admin", List.of("monitoring")))
                .branches(List.of(branch))
                .groups(List.of(group))
                .modules(List.of("reports"))
                .build();

        RealmAccess viaSetters = new RealmAccess();
        viaSetters.setRoles(List.of("admin"));
        viaSetters.setRolesWithModules(Map.of("admin", List.of("monitoring")));
        viaSetters.setBranches(List.of(branch));
        viaSetters.setGroups(List.of(group));
        viaSetters.setModules(List.of("reports"));

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("reports"));
    }
}
