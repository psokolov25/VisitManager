package ru.aritmos.clients;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;
import ru.aritmos.model.visit.Visit;

/** HTTP‑клиент печати талонов. */
@Client(value = "${micronaut.application.printerServiceURL}")
public interface PrinterClient {
  /**
   * Напечатать талон по визиту.
   *
   * @param charset кодировка контента
   * @param responseRequired требуется ли получить ответ
   * @param visit визит
   * @return реактивный ответ печати
   */
  @Retryable(
      delay = "${micronaut.application.printerRetryPeriod:5s}",
      maxDelay = "${micronaut.application.printerRetryMaxPeriod:45m}",
      attempts = "${micronaut.application.printerUrlRetryRepeat:30}")
  @Post(uri = "/printer/visit", produces = "application/json", consumes = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  Publisher<HttpResponse<?>> print(
      @QueryValue(defaultValue = "UTF-8") String charset,
      @QueryValue Boolean responseRequired,
      @Body Visit visit);
}
