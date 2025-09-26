package ru.aritmos.service.rules;

import java.util.List;
import java.util.Optional;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

/** Правило вызова клиента на обслуживание. */
public interface CallRule extends Rule {
  /**
   * Вызвать следующего клиента на указанном окне.
   *
   * @param branch филиал
   * @param servicePoint окно обслуживания
   * @return визит, если найден
   */
  Optional<Visit> call(Branch branch, ServicePoint servicePoint);

  /**
   * Вызвать из ограниченного списка очередей.
   *
   * @param branch филиал
   * @param servicePoint окно обслуживания
   * @param queueIds список очередей
   * @return визит, если найден
   */
  Optional<Visit> call(Branch branch, ServicePoint servicePoint, List<String> queueIds);

  /**
   * Получить доступные окна обслуживания для визита.
   *
   * @param currentBranch филиал
   * @param visit визит
   * @return список доступных окон
   */
  List<ServicePoint> getAvailiableServicePoints(Branch currentBranch, Visit visit);
}
