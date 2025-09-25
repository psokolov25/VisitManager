package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReceptionTest {

    @DisplayName("Builder Should Initialize Sessions List")
    @Test
    void builderShouldInitializeSessionsList() {
        Reception reception = Reception.builder().branchId("branch1").build();

        assertEquals("branch1", reception.getBranchId());
        assertNotNull(reception.getReceptionSessions());
        assertTrue(reception.getReceptionSessions().isEmpty());
    }

    @DisplayName("Builder Allows Setting Printers And Sessions")
    @Test
    void builderAllowsSettingPrintersAndSessions() {
        Entity printer = Entity.builder().id("printer1").name("Main").build();
        ReceptionSession session =
                ReceptionSession.builder().user(new User()).build();

        Reception reception =
                Reception.builder()
                        .branchId("branch2")
                        .printers(List.of(printer))
                        .receptionSessions(List.of(session))
                        .build();

        assertEquals(List.of(printer), reception.getPrinters());
        assertEquals(List.of(session), reception.getReceptionSessions());
    }
}
