package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class MarkTest {

    @DisplayName("Builder Should Populate Fields")
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

    @DisplayName("Setters Should Allow Updates")
    @Test
    void settersShouldAllowUpdates() {
        Mark mark = new Mark();
        mark.setId("m2");
        mark.setValue("Loyal");

        assertEquals("m2", mark.getId());
        assertEquals("Loyal", mark.getValue());
    }
}
