package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.serde.annotation.Serdeable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityTest {

    @DisplayName("билдер создает сущность с указанными значениями")
    @Test
    void builderCreatesEntity() {
        Entity entity = Entity.builder()
                .id("123")
                .name("касса")
                .build();

        assertEquals("123", entity.getId());
        assertEquals("касса", entity.getName());
    }

    @DisplayName("геттеры, сеттеры и equals/hashCode работают корректно")
    @Test
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

    @DisplayName("класс Entity аннотирован Serdeable")
    @Test
    void verifySerdeableAnnotation() {
        assertTrue(Entity.class.isAnnotationPresent(Serdeable.class));
    }
}
