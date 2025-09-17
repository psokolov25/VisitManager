package ru.aritmos.model.tiny;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;

class TinyClassTest {

    @Test
    void builderShouldPopulateFields() {
        TinyClass tiny = TinyClass.builder().id("id1").name("Queue").build();

        assertEquals("id1", tiny.getId());
        assertEquals("Queue", tiny.getName());
    }

    @Test
    void settersShouldAllowChanges() {
        TinyClass tiny = new TinyClass();
        tiny.setId("id2");
        tiny.setName("Service");

        assertEquals("id2", tiny.getId());
        assertEquals("Service", tiny.getName());
    }
}
