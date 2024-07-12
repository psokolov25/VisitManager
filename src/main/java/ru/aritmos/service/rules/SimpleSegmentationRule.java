package ru.aritmos.service.rules;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;

import java.util.Optional;
@Requires(property = "micronaut.application.rules.segmentation", value = "simple")
@Singleton
public class SimpleSegmentationRule implements SegmentationRule {

    @Override
    public Optional<Queue> getQueue(Visit visit, Branch branch) {
        if(visit.getCurrentService()!=null)
        {
            if(branch.getQueues().containsKey(visit.getCurrentService().getLinkedQueueId()))
            {
                return Optional.of(branch.getQueues().get(visit.getCurrentService().getLinkedQueueId()));
            }
        }

        return Optional.empty();
    }
}
