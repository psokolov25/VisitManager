package ru.aritmos.clients;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.visit.Visit;

class PrinterClientTest {

    @DisplayName("Интерфейс объявлен клиентом Micronaut")
    @Test
    void interfaceIsAnnotatedAsMicronautClient() {
        Client annotation = PrinterClient.class.getAnnotation(Client.class);
        assertNotNull(annotation, "Ожидался @Client на интерфейсе");
        assertEquals("${micronaut.application.printerServiceURL}", annotation.value());
    }

    @DisplayName("Метод печати содержит аннотации повторных попыток и очереди исполнения")
    @Test
    void printMethodHasRetryAnnotations() throws NoSuchMethodException {
        Method method = PrinterClient.class.getMethod("print", String.class, Boolean.class, Visit.class);

        assertTrue(method.isAnnotationPresent(Post.class));

        Retryable retryable = method.getAnnotation(Retryable.class);
        assertNotNull(retryable);
        assertEquals("${micronaut.application.printerUrlRetryRepeat:30}", retryable.attempts());

        ExecuteOn executeOn = method.getAnnotation(ExecuteOn.class);
        assertNotNull(executeOn);
        assertTrue(java.util.Arrays.asList(executeOn.value()).contains(TaskExecutors.IO));
    }
}
