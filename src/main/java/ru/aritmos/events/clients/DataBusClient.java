package ru.aritmos.events.clients;

import io.micronaut.context.annotation.Requires;
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
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * HTTP‑клиент для отправки событий на DataBus.
 */
@Requires(notEnv = "local-no-docker")
@Client(value = "${micronaut.application.dataBusUrl}")
public interface DataBusClient {
  /**
   * Отправить событие на DataBus.
   *
   * @param destinationServices получатели (через запятую)
   * @param sendToOtherBus отправлять на общий bus
   * @param sendDate дата отправки в формате RFC‑1123
   * @param senderService имя отправителя
   * @param type тип события
   * @param body тело события
   * @return карта с результатом публикации
   */
  @Retryable(
      delay = "${micronaut.application.dataBusUrlRetryPeriod:30s}",
      maxDelay = "${micronaut.application.dataBusUrlRetryMaxPeriod:45m}",
      attempts = "${micronaut.application.dataBusUrlRetryRepeat:30}")
  @SingleResult
  @Post(
      uri = "/databus/events/types/{type}",
      produces = "application/json",
      consumes = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Async
  Publisher<Map<String, String>> send(
      @Header("Service-Destination") String destinationServices,
      @Header("Send-To-OtherBus") Boolean sendToOtherBus,
      @Header("Send-Date") @Parameter(example = "Wed, 09 Apr 2008 23:55:38 GMT") String sendDate,
      @Header("Service-Sender") String senderService,
      @PathVariable String type,
      @Body Object body);
}
