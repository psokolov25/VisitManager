package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.serde.annotation.Serdeable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityTest {

    @Test
    @DisplayName("Построитель создаёт сущность с указанными значениями")
    void builderCreatesEntity() {
        Entity entity = Entity.builder()
                .id("123")
                .name("касса")
                .build();

        assertEquals("123", entity.getId());
        assertEquals("касса", entity.getName());
    }

    @Test
    @DisplayName("геттеры, сеттеры и equals/hashCode работают корректно")
    void gettersSettersAndEqualityWorkCorrectly() {
        Entity первый = new Entity();
        первый.setId("id-1");
        первый.setName("отделение");

        Entity второй = new Entity("id-1", "отделение");

        assertEquals(первый, второй);
        assertEquals(первый.hashCode(), второй.hashCode());

        второй.setName("другой офис");
        assertNotEquals(первый, второй);
    }

    @Test
    @DisplayName("класс Entity аннотирован Serdeable")
    void verifySerdeableAnnotation() {
        assertTrue(Entity.class.isAnnotationPresent(Serdeable.class));
    }
}
