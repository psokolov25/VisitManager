package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisitParametersTest {

    @DisplayName("Билдер создаёт пустые коллекции по умолчанию")
    @Test
    void builderShouldCreateEmptyCollectionsByDefault() {
        VisitParameters parameters = VisitParameters.builder().build();

        assertNotNull(parameters.getServiceIds());
        assertTrue(parameters.getServiceIds().isEmpty());
        assertNotNull(parameters.getParameters());
        assertTrue(parameters.getParameters().isEmpty());
    }

    @DisplayName("Билдер принимает переданные значения")
    @Test
    void builderShouldAcceptCustomValues() {
        ArrayList<String> serviceIds = new ArrayList<>(List.of("s1"));
        HashMap<String, String> extra = new HashMap<>();
        extra.put("priority", "high");

        VisitParameters parameters =
                VisitParameters.builder().serviceIds(serviceIds).parameters(extra).build();

        assertEquals(serviceIds, parameters.getServiceIds());
        assertEquals(extra, parameters.getParameters());
    }
}
