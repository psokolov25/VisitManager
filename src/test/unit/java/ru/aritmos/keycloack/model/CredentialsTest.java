package ru.aritmos.keycloack.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;

class CredentialsTest {

    @Test
    void сеттерыИГеттерыДоступны() {
        Credentials credentials = new Credentials();
        credentials.setUsername("user");
        credentials.setPassword("secret");

        assertEquals("user", credentials.getUsername());
        assertEquals("secret", credentials.getPassword());
        assertTrue(credentials.toString().contains("user"));
    }
}
