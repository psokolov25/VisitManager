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

@Singleton
@Named("SimpleCallRule")
@Requires(property = "micronaut.application.rules.callVisit", value = "custom")
public class CustomCallRule implements CallRule {
  @Inject CallRuleClient callRuleClient;

  @Override
  public Optional<Visit> call(Branch branch, ServicePoint servicePoint) {
    return callRuleClient.callRule(branch, servicePoint);
  }

  @Override
  public Optional<Visit> call(Branch branch, ServicePoint servicePoint, List<String> queueIds) {
    return Optional.empty();
  }

  @Override
  public List<ServicePoint> getAvailiableServicePoints(Branch currentBranch, Visit visit) {
    return List.of();
  }
}
