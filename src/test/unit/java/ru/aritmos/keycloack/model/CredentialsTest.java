package ru.aritmos.keycloack.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class CredentialsTest {

    @DisplayName("Setters And Getters Are Accessible")
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
