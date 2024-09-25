package ru.aritmos.service.rules;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.SegmentationRuleData;
import ru.aritmos.model.visit.Visit;

import java.util.List;
import java.util.Optional;
@Requires(property = "micronaut.application.rules.segmentation", value = "simple")
@Singleton
public class SimpleSegmentationRule implements SegmentationRule {

    @Override
    public Optional<Queue> getQueue(Visit visit, Branch branch) {
        if(visit.getCurrentService()!=null)
        {
            Optional<String> queueId=checkSegmentationRules(visit,branch.getSegmentationRules());
            if(queueId.isPresent()){
                return Optional.of(branch.getQueues().get(queueId.get()));
            }
            if(branch.getQueues().containsKey(visit.getCurrentService().getLinkedQueueId()))
            {
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
        if(!visit.getParameterMap().isEmpty()) {
            Optional<SegmentationRuleData> result = rules.stream().filter(f -> f.getKeyProperty().entrySet().containsAll(visit.getParameterMap().entrySet())).findFirst();
            return result.map(SegmentationRuleData::getQueueId);
        }
        else {
            return Optional.empty();
        }
    }
}
