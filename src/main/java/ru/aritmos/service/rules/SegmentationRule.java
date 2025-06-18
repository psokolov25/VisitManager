package ru.aritmos.service.rules;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpStatus;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.GroovyScriptService;

@Introspected(classes = GroovyScript.class)
@Singleton
@SuppressWarnings("unchecked")
public class SegmentationRule {
  @Inject BranchService branchService;
  @Inject EventService eventService;
  GroovyScript groovyScript;

  /**
   * Возвращает очередь согласно текущей услуги визита, если у услуги предусмотрено правило -
   * выполняется оно, если нет - берется очередь с указанной для очереди услугой
   *
   * @param visit визитё
   * @param branch отделение
   * @return Очередь
   */

  public Optional<Queue> getQueue(Visit visit, Branch branch) throws SystemException {
    if (visit.getCurrentService() != null
        && visit.getCurrentService().getServiceGroupId() != null) {
      Service service = visit.getCurrentService();
      if (branch.getServiceGroups().containsKey(service.getServiceGroupId())) {
        ServiceGroup serviceGroup = branch.getServiceGroups().get(service.getServiceGroupId());
        try {

          if (serviceGroup.getServiceIds().contains(visit.getCurrentService().getId())) {
            Optional<String> queueId =
                checkSegmentationRules(
                    visit,
                    branch.getSegmentationRules().values().stream()
                        .filter(
                            segmentationRuleData ->
                                segmentationRuleData
                                    .getServiceGroupId()
                                    .equals(serviceGroup.getId()))
                        .toList());
            if (queueId.isPresent()) {
              return Optional.of(branch.getQueues().get(queueId.get()));
            }
            Optional<Queue> queueByGroovy =
                getQueue(visit, branch, serviceGroup.getSegmentationRuleId());
            if (queueByGroovy.isPresent()) {
              return queueByGroovy;
            }
          }
        } catch (Exception ex) {
          throw new SystemException(ex.getMessage(), eventService);
        }
      }
    }
    assert visit.getCurrentService() != null;
    if (branch.getQueues().containsKey(visit.getCurrentService().getLinkedQueueId())) {
      return Optional.of(branch.getQueues().get(visit.getCurrentService().getLinkedQueueId()));
    }

    return Optional.empty();
  }


  public Optional<Queue> getQueue(Visit visit, Branch branch, String segmentationRuleId) {
    Branch currentBranch = branchService.getBranch(branch.getId());
    if (segmentationRuleId == null || segmentationRuleId.isEmpty()) {
      return Optional.empty();
    }
    if (currentBranch.getSegmentationRules().containsKey(segmentationRuleId)) {
      groovyScript =
          currentBranch.getCustomSegmentationRules().get(segmentationRuleId).toBuilder().build();
    } else {
      throw new BusinessException(
          "Segmentation rule not found!", eventService, HttpStatus.NOT_FOUND);
    }
    Map<String, Object> inputParameters = groovyScript.getInputParameters();
    if (inputParameters.containsKey("visit") && inputParameters.containsKey("branch")) {
      inputParameters.put("visit", visit);
      inputParameters.put("branch", branch);
      GroovyScriptService groovyScriptService = new GroovyScriptService();
      groovyScriptService.Execute(groovyScript);
      if (groovyScript.getOutputParameters().containsKey("queue")) {
        Optional<Queue> queue;
        queue = (Optional<Queue>) groovyScript.getOutputParameters().get("queue");
        return queue;
      } else return Optional.empty();
    } else {
      throw new BusinessException(
          "Input parameter visit or branch not found!", eventService, HttpStatus.NOT_FOUND);
    }
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
