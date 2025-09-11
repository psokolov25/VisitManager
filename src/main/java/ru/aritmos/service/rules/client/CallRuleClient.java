package ru.aritmos.service.rules.client;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import java.util.Optional;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

/**
 * HTTP‑клиент для вызова правила определения следующего визита/обслуживания.
 */
@Client(value = "${micronaut.application.rules.callRuleApiUrl}")
public interface CallRuleClient {
  /**
   * Выполнить вызов правила для точки обслуживания.
   *
   * @param branch отделение
   * @param servicePoint точка обслуживания
   * @return визит (если найден) или пусто
   */
  @Post()
  Optional<Visit> callRule(@Body Branch branch, @Body ServicePoint servicePoint);
}
