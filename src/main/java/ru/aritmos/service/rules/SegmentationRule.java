package ru.aritmos.service.rules;

import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;

import java.util.Optional;

public interface SegmentationRule  extends Rule {
    Optional<Queue> getQueue(Visit visit, Branch branch);
}
