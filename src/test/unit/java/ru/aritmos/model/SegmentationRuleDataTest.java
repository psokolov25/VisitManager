package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class SegmentationRuleDataTest {

    @DisplayName("Метод построения заполняет все поля модели")
    @Test
    void builderShouldPopulateAllFields() {
        HashMap<String, String> visitProperty = new HashMap<>();
        visitProperty.put("service", "s1");

        SegmentationRuleData data =
                SegmentationRuleData.builder()
                        .id("rule1")
                        .name("Important rule")
                        .branchId("branch42")
                        .visitProperty(visitProperty)
                        .serviceGroupId("group7")
                        .queueId("queue11")
                        .build();

        assertEquals("rule1", data.getId());
        assertEquals("Important rule", data.getName());
        assertEquals("branch42", data.getBranchId());
        assertEquals(visitProperty, data.getVisitProperty());
        assertEquals("group7", data.getServiceGroupId());
        assertEquals("queue11", data.getQueueId());
    }

    @DisplayName("Свойство визита можно задать позднее")
    @Test
    void visitPropertyCanBeAssignedLater() {
        SegmentationRuleData data = SegmentationRuleData.builder().id("rule2").build();

        assertNull(data.getVisitProperty());

        data.setVisitProperty(new HashMap<>());

        assertNotNull(data.getVisitProperty());
        assertTrue(data.getVisitProperty().isEmpty());
    }
}
