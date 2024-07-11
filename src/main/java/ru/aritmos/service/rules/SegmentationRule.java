package ru.aritmos.service.rules;

import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;

public interface SegmentationRule  extends Rule {
    Queue getQueue(Visit visit, Branch branch);
}
