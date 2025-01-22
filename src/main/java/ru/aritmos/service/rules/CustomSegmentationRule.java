package ru.aritmos.service.rules;

import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import lombok.*;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Entity;
import ru.aritmos.model.GroovyScript;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;

@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Singleton
@SuppressWarnings("unchecked")
public class CustomSegmentationRule extends Entity implements SegmentationRule {
  @Inject BranchService branchService;
  @Inject EventService eventService;
  GroovyScript groovyScript;

  CustomSegmentationRule(String branchId, String segmentationRuleId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getSegmentationRules().containsKey(segmentationRuleId)) {
      groovyScript = currentBranch.getCustomSegmentationRules().get(segmentationRuleId);
    } else {
      throw new BusinessException(
          "Segmentation rule not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  @Override
  public Optional<Queue> getQueue(Visit visit, Branch branch) {

    if (groovyScript.getInputParameters().containsKey("visit")
        && getGroovyScript().getInputParameters().containsKey("branch")) {
      groovyScript.getInputParameters().put("visit", visit);
      groovyScript.getInputParameters().put("branch", branch);
      groovyScript.Execute();
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
}
