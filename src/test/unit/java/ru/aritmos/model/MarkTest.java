package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class MarkTest {

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

    @Test
    void settersShouldAllowUpdates() {
        Mark mark = new Mark();
        mark.setId("m2");
        mark.setValue("Loyal");

        assertEquals("m2", mark.getId());
        assertEquals("Loyal", mark.getValue());
    }
}
