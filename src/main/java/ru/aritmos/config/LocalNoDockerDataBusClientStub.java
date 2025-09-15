package ru.aritmos.config;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.util.Map;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ru.aritmos.events.clients.DataBusClient;

/**
 * Стаб HTTP‑клиента шины данных для профиля local-no-docker.
 */
@Singleton
@Requires(env = "local-no-docker")
@Replaces(DataBusClient.class)
public class LocalNoDockerDataBusClientStub implements DataBusClient {

  /**
   * Эмулирует отправку события без фактического HTTP-запроса.
   *
   * @param destinationServices целевые сервисы
   * @param sendToOtherBus пересылать ли в дополнительные шины
   * @param sendDate метка времени отправки
   * @param senderService имя сервиса-отправителя
   * @param type тип события
   * @param body тело события
   * @return реактивный ответ с информацией о заглушке
   */
  @Override
  public Publisher<Map<String, String>> send(
      String destinationServices,
      Boolean sendToOtherBus,
      String sendDate,
      String senderService,
      String type,
      Object body) {
    // Просто подтверждаем отправку без реального HTTP вызова
    return Mono.just(Map.of("status", "stubbed", "type", type));
  }
}

