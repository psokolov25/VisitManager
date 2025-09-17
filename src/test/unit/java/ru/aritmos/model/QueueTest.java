package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;

/**
 * Тесты для {@link Queue}.
 */
class QueueTest {

    @Test
    void constructorGeneratesIdAndSetsFields() {
        Queue queue = new Queue("Main", "A", 60);
        assertNotNull(queue.getId(), "id should be generated");
        assertEquals("Main", queue.getName());
        assertEquals("A", queue.getTicketPrefix());
        assertEquals(60, queue.getWaitingSL());
        assertEquals(0, queue.getTicketCounter());
    }

    @Test
    void constructorWithExplicitIdSetsAllFields() {
        Queue queue = new Queue("q1", "Secondary", "B", 30);
        assertEquals("q1", queue.getId());
        assertEquals("Secondary", queue.getName());
        assertEquals("B", queue.getTicketPrefix());
        assertEquals(30, queue.getWaitingSL());
        assertEquals(0, queue.getTicketCounter());
    }
}
