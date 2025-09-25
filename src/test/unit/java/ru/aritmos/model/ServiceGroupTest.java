package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class ServiceGroupTest {

    @DisplayName("проверяется сценарий «custom constructor should fill hierarchy fields»")
    @Test
    void customConstructorShouldFillHierarchyFields() {
        List<String> services = List.of("s1", "s2");

        ServiceGroup group = new ServiceGroup("g1", "Group", services, "branch1");

        assertEquals("g1", group.getId());
        assertEquals("Group", group.getName());
        assertEquals("branch1", group.getBranchId());
        assertEquals(services, group.getServiceIds());
    }

    @DisplayName("проверяется сценарий «segmentation identifiers should be mutable»")
    @Test
    void segmentationIdentifiersShouldBeMutable() {
        ServiceGroup group = new ServiceGroup();

        group.setSegmentationRuleId("rule42");
        group.setSegmentationParameterRuleId("param24");

        assertEquals("rule42", group.getSegmentationRuleId());
        assertEquals("param24", group.getSegmentationParameterRuleId());
    }
}