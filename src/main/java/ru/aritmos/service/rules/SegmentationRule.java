package ru.aritmos.service.rules;

import java.util.Optional;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;

public interface SegmentationRule extends Rule {
  Optional<Queue> getQueue(Visit visit, Branch branch);
}
