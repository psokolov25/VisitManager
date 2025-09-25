package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class EntryPointTest {

    @DisplayName("проверяется сценарий «should inherit branch entity fields»")
    @Test
    void shouldInheritBranchEntityFields() {
        EntryPoint entryPoint = new EntryPoint();
        entryPoint.setId("ep1");
        entryPoint.setName("Main Hall");
        entryPoint.setBranchId("b1");
        entryPoint.setPrinter(Entity.builder().id("printer1").name("Printer").build());

        assertEquals("ep1", entryPoint.getId());
        assertEquals("Main Hall", entryPoint.getName());
        assertEquals("b1", entryPoint.getBranchId());
        assertEquals("printer1", entryPoint.getPrinter().getId());
    }
}