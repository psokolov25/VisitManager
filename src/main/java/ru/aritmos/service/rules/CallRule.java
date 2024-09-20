package ru.aritmos.service.rules;

import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

import java.util.List;
import java.util.Optional;

public interface CallRule extends Rule{
    Optional<Visit> call(Branch branch,ServicePoint servicePoint );
    List<ServicePoint> getAvaliableServicePoints(Branch currentBranch,Visit visit);
}
