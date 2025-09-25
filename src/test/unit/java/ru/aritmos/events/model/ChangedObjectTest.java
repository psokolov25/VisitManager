package ru.aritmos.events.model;

import ru.aritmos.test.LoggingAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class ChangedObjectTest {

    @DisplayName("Билдер заполняет все поля")
    @Test
    void builderSetsFields() {
        ChangedObject obj =
                ChangedObject.builder()
                        .oldValue("old")
                        .newValue("new")
                        .className("MyClass")
                        .action("UPDATE")
                        .build();

        LoggingAssertions.assertEquals("old", obj.getOldValue());
        LoggingAssertions.assertEquals("new", obj.getNewValue());
        LoggingAssertions.assertEquals("MyClass", obj.getClassName());
        LoggingAssertions.assertEquals("UPDATE", obj.getAction());
    }
}
