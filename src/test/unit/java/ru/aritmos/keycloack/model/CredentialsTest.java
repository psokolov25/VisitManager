package ru.aritmos.keycloack.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class CredentialsTest {

    @DisplayName("проверяется сценарий «setters and getters are accessible»")
    @Test
    void settersAndGettersAreAccessible() {
        Credentials credentials = new Credentials();
        credentials.setUsername("user");
        credentials.setPassword("secret");

        assertEquals("user", credentials.getUsername());
        assertEquals("secret", credentials.getPassword());
        assertTrue(credentials.toString().contains("user"));
    }
}