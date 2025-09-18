package ru.aritmos.config;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ru.aritmos.events.clients.DataBusClient;

/**
 * Стаб HTTP‑клиента шины данных для профиля local-no-docker.
 */
@Singleton
@Requires(env = "local-no-docker")
public class LocalNoDockerDataBusClientStub implements DataBusClient {

  /** Хранит историю вызовов для проверки в тестах. */
  private final List<InvocationRecord> invocations =
      Collections.synchronizedList(new ArrayList<>());

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
    invocations.add(
        new InvocationRecord(
            destinationServices, sendToOtherBus, sendDate, senderService, type, body));
    // Просто подтверждаем отправку без реального HTTP вызова
    return Mono.just(Map.of("status", "stubbed", "type", type));
  }

  /** Возвращает копию записанных вызовов. */
  public List<InvocationRecord> getInvocations() {
    return List.copyOf(invocations);
  }

  /** Очищает историю вызовов. */
  public void clearInvocations() {
    invocations.clear();
  }

  /**
   * Запись одного вызова {@link #send(String, Boolean, String, String, String, Object)}.
   */
  public record InvocationRecord(
      String destinationServices,
      Boolean sendToOtherBus,
      String sendDate,
      String senderService,
      String type,
      Object body) {}
}
