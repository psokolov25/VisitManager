package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class MarkTest {

    @DisplayName("проверяется сценарий «builder should populate fields»")
    @Test
    void builderShouldPopulateFields() {
        ZonedDateTime now = ZonedDateTime.now();
        User author = new User();
        author.setName("Ivan");

        Mark mark = Mark.builder().id("m1").value("VIP").markDate(now).author(author).build();

        assertEquals("m1", mark.getId());
        assertEquals("VIP", mark.getValue());
        assertEquals(now, mark.getMarkDate());
        assertSame(author, mark.getAuthor());
    }

    @DisplayName("проверяется сценарий «setters should allow updates»")
    @Test
    void settersShouldAllowUpdates() {
        Mark mark = new Mark();
        mark.setId("m2");
        mark.setValue("Loyal");

        assertEquals("m2", mark.getId());
        assertEquals("Loyal", mark.getValue());
    }
}