package ru.aritmos.test;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;
import java.util.Map;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ru.aritmos.events.clients.DataBusClient;

/**
 * Тестовый стаб клиента DataBus: отключает реальные HTTP вызовы.
 */
@Singleton
@Requires(env = Environment.TEST)
@Replaces(DataBusClient.class)
public class DataBusClientStub implements DataBusClient {

  @Override
  public Publisher<Map<String, String>> send(
      String destinationServices,
      Boolean sendToOtherBus,
      String sendDate,
      String senderService,
      String type,
      Object body) {
    return Mono.just(Map.of("status", "stubbed", "type", type));
  }
}
