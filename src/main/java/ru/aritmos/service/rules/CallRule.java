package ru.aritmos.service.rules;

import java.util.List;
import java.util.Optional;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

public interface CallRule extends Rule {
  Optional<Visit> call(Branch branch, ServicePoint servicePoint);

  Optional<Visit> call(Branch branch, ServicePoint servicePoint, List<String> queueIds);

  List<ServicePoint> getAvailiableServicePoints(Branch currentBranch, Visit visit);
}
