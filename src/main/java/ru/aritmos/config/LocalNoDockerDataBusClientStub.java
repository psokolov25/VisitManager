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

