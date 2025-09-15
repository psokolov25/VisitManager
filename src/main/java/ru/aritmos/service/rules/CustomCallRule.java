package ru.aritmos.service.rules;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.rules.client.CallRuleClient;

/**
 * Пользовательское правило вызова визита.
 * Делегирует логику вызова внешнему клиенту правил.
 */

@Singleton
@Named("SimpleCallRule")
@Requires(property = "micronaut.application.rules.callVisit", value = "custom")
public class CustomCallRule implements CallRule {
  /** Клиент вызова пользовательских правил. */
  @Inject CallRuleClient callRuleClient;


  /**
   * Вызов визита по настроенному правилу.
   *
   * @param branch отделение
   * @param servicePoint точка обслуживания
   * @return опционально найденный визит
   */
  @Override
  public Optional<Visit> call(Branch branch, ServicePoint servicePoint) {
    return callRuleClient.callRule(branch, servicePoint);
  }


  /**
   * Вызов визита по набору очередей.
   *
   * @param branch отделение
   * @param servicePoint точка обслуживания
   * @param queueIds идентификаторы очередей
   * @return опционально найденный визит
   */
  @Override
  public Optional<Visit> call(Branch branch, ServicePoint servicePoint, List<String> queueIds) {
    return Optional.empty();
  }


  /**
   * Получение доступных точек обслуживания для визита.
   *
   * @param currentBranch текущее отделение
   * @param visit визит
   * @return список доступных точек обслуживания
   */
  @Override
  public List<ServicePoint> getAvailiableServicePoints(Branch currentBranch, Visit visit) {
    return List.of();
  }
}
