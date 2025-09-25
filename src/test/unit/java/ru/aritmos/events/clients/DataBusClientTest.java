package ru.aritmos.events.clients;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.core.async.annotation.SingleResult;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Рефлексивные проверки конфигурации {@link DataBusClient}.
 */
class DataBusClientTest {

  /** Проверяет наличие аннотации клиента с ожидаемым значением URL. */
  @DisplayName("Client Is Annotated With Service Url")
  @Test
  void clientIsAnnotatedWithServiceUrl() {
    Client client = DataBusClient.class.getAnnotation(Client.class);

    assertNotNull(client, "Аннотация @Client отсутствует");
    assertEquals("${micronaut.application.dataBusUrl}", client.value());
  }

  /** Убеждаемся, что метод send настроен на POST с ретраями и асинхронным исполнением. */
  @DisplayName("Send Method Contains Http And Async Annotations")
  @Test
  void sendMethodContainsHttpAndAsyncAnnotations() throws NoSuchMethodException {
    Method send =
        DataBusClient.class.getMethod(
            "send",
            String.class,
            Boolean.class,
            String.class,
            String.class,
            String.class,
            Object.class);

    Post post = send.getAnnotation(Post.class);
    assertNotNull(post, "Отсутствует @Post");
    assertEquals("/databus/events/types/{type}", post.uri());
    assertArrayEquals(new String[] {"application/json"}, post.produces());
    assertArrayEquals(new String[] {"application/json"}, post.consumes());

    Retryable retryable = send.getAnnotation(Retryable.class);
    assertNotNull(retryable, "Отсутствует @Retryable");
    assertEquals("${micronaut.application.dataBusUrlRetryPeriod:30s}", retryable.delay());
    assertEquals("${micronaut.application.dataBusUrlRetryMaxPeriod:45m}", retryable.maxDelay());
    assertEquals("${micronaut.application.dataBusUrlRetryRepeat:30}", retryable.attempts());

    assertNotNull(send.getAnnotation(SingleResult.class), "Отсутствует @SingleResult");
    assertNotNull(send.getAnnotation(Async.class), "Отсутствует @Async");
    ExecuteOn executeOn = send.getAnnotation(ExecuteOn.class);
    assertNotNull(executeOn, "Отсутствует @ExecuteOn");
    assertEquals(TaskExecutors.IO, executeOn.value());
  }

  /** Проверяем ожидаемые аннотации на параметрах метода send. */
  @DisplayName("Send Method Has All Headers And Parameters")
  @Test
  void sendMethodHasAllHeadersAndParameters() throws NoSuchMethodException {
    Method send =
        DataBusClient.class.getMethod(
            "send",
            String.class,
            Boolean.class,
            String.class,
            String.class,
            String.class,
            Object.class);

    Annotation[][] annotations = send.getParameterAnnotations();

    Header destinationHeader =
        (Header)
            Arrays.stream(annotations[0])
                .filter(Header.class::isInstance)
                .findFirst()
                .orElseThrow();
    assertEquals("Service-Destination", destinationHeader.value());

    Header sendToOtherBusHeader =
        (Header)
            Arrays.stream(annotations[1])
                .filter(Header.class::isInstance)
                .findFirst()
                .orElseThrow();
    assertEquals("Send-To-OtherBus", sendToOtherBusHeader.value());

    Header sendDateHeader =
        (Header)
            Arrays.stream(annotations[2])
                .filter(Header.class::isInstance)
                .findFirst()
                .orElseThrow();
    assertEquals("Send-Date", sendDateHeader.value());
    assertTrue(
        Arrays.stream(annotations[2]).anyMatch(Parameter.class::isInstance),
        "Ожидается аннотация @Parameter");

    Header senderHeader =
        (Header)
            Arrays.stream(annotations[3])
                .filter(Header.class::isInstance)
                .findFirst()
                .orElseThrow();
    assertEquals("Service-Sender", senderHeader.value());

    assertTrue(
        Arrays.stream(annotations[4]).anyMatch(PathVariable.class::isInstance),
        "Идентификатор типа должен быть @PathVariable");
    assertTrue(
        Arrays.stream(annotations[5]).anyMatch(Body.class::isInstance),
        "Тело запроса должно быть помечено @Body");
  }
}
