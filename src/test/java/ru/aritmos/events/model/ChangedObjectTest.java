package ru.aritmos.events.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

class ChangedObjectTest {

    @Test
    void builderSetsFields() {
        ChangedObject obj =
                ChangedObject.builder()
                        .oldValue("old")
                        .newValue("new")
                        .className("MyClass")
                        .action("UPDATE")
                        .build();

        Assertions.assertEquals("old", obj.getOldValue());
        Assertions.assertEquals("new", obj.getNewValue());
        Assertions.assertEquals("MyClass", obj.getClassName());
        Assertions.assertEquals("UPDATE", obj.getAction());
    }
}
