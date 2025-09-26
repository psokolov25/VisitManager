package ru.aritmos.events.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.test.LoggingAssertions;

class ChangedObjectTest {

    @DisplayName("Построитель заполняет все поля изменённого объекта")
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
