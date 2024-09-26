package ru.aritmos.service.rules;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.SegmentationRuleData;
import ru.aritmos.model.visit.Visit;

@Requires(property = "micronaut.application.rules.segmentation", value = "simple")
@Singleton
public class SimpleSegmentationRule implements SegmentationRule {
  /**
   * Возвращает очередь согласно текущей услуги визита, если у услуги предусмотрено правило -
   * выполняется оно, если нет - берется очередь с указанной для очереди услугой
   *
   * @param visit визитё
   * @param branch отделение
   * @return Очередь
   */
  @Override
  public Optional<Queue> getQueue(Visit visit, Branch branch) {
    if (visit.getCurrentService() != null) {
      Optional<String> queueId =
          checkSegmentationRules(visit, branch.getSegmentationRules().values().stream().toList());
      if (queueId.isPresent()) {
        return Optional.of(branch.getQueues().get(queueId.get()));
      }
      if (branch.getQueues().containsKey(visit.getCurrentService().getLinkedQueueId())) {
        return Optional.of(branch.getQueues().get(visit.getCurrentService().getLinkedQueueId()));
      }
    }

    return Optional.empty();
  }

  /**
   * Поиск идентификатора очереди в правиле сегментации
   *
   * @param visit визит
   * @param rules правила сегментации
   * @return идентификатор очереди
   */
  private Optional<String> checkSegmentationRules(Visit visit, List<SegmentationRuleData> rules) {
    if (!visit.getParameterMap().isEmpty()) {

      Optional<SegmentationRuleData> result =
          rules.stream()
              .filter(
                  f ->
                      f.getServiceGroupId() == null
                          || f.getServiceGroupId()
                              .equals(visit.getCurrentService().getServiceGroupId()))
              .filter(
                  f ->
                      visit
                          .getParameterMap()
                          .entrySet()
                          .containsAll(f.getVisitProperty().entrySet()))
              .findFirst();

      return result.map(SegmentationRuleData::getQueueId);

    } else {
      return Optional.empty();
    }
  }
}
