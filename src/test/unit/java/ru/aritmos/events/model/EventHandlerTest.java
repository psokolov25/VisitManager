package ru.aritmos.events.model;

import static ru.aritmos.test.LoggingAssertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.exceptions.SystemException;

class EventHandlerTest {

    @DisplayName("проверяется сценарий «handle method should be annotated for io executor»")
    @Test
    void handleMethodShouldBeAnnotatedForIoExecutor() throws NoSuchMethodException {
        Method handle = EventHandler.class.getMethod("Handle", Event.class);
        ExecuteOn executeOn = handle.getAnnotation(ExecuteOn.class);

        assertNotNull(executeOn, "аннотация ExecuteOn должна присутствовать");
        assertEquals(TaskExecutors.IO, executeOn.value());
    }

    @DisplayName("проверяется сценарий «handle method should declare expected exceptions»")
    @Test
    void handleMethodShouldDeclareExpectedExceptions() throws NoSuchMethodException {
        Method handle = EventHandler.class.getMethod("Handle", Event.class);
        Set<Class<?>> declared = Set.of(handle.getExceptionTypes());

        assertTrue(
                declared.contains(JsonProcessingException.class),
                "должна декларироваться JsonProcessingException");
        assertTrue(
                declared.contains(SystemException.class),
                "должна декларироваться SystemException");
        assertTrue(
                declared.contains(IllegalAccessException.class),
                "должна декларироваться IllegalAccessException");
        assertEquals(3, declared.size());
    }
}