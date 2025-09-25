package ru.aritmos.keycloack.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeyCloackUserTest {

    @Test
    void fieldsCanBeSetAndRead() {
        KeyCloackUser user = new KeyCloackUser();
        user.setEmail("user@example.com");
        user.setName("User");
        HashMap<String, HashMap<String, List<String>>> resourceAccess = new HashMap<>();
        resourceAccess.put("client", new HashMap<>());
        resourceAccess.get("client").put("roles", List.of("read"));
        user.setResource_access(resourceAccess);
        user.setPreferred_username("user");
        user.setRoles(List.of("admin"));
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("department", "sales");
        user.setAttributes(attributes);
        user.setGroups(List.of("group1"));

        assertEquals("user@example.com", user.getEmail());
        assertEquals("User", user.getName());
        assertEquals(List.of("read"), user.getResource_access().get("client").get("roles"));
        assertEquals("user", user.getPreferred_username());
        assertEquals(List.of("admin"), user.getRoles());
        assertEquals("sales", user.getAttributes().get("department"));
        assertEquals(List.of("group1"), user.getGroups());
        assertTrue(user.toString().contains("User"));
    }
}
