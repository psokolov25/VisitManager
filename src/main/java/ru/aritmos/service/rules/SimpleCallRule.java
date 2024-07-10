package ru.aritmos.service.rules;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpStatus;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@Named("SimpleCallRule")
@Requires(property = "micronaut.application.rules.callVisit", value = "simple")
public class SimpleCallRule implements CallRule {
    @Inject
    EventService eventService;

    @Override
    public Optional<Visit> call(Branch branch, ServicePoint servicePoint) {

        if (servicePoint.getUser() != null) {
            String workprofileId = servicePoint.getUser().getCurrentWorkProfileId();
            if (branch.getWorkProfiles().containsKey(workprofileId)) {
                List<String> queueIds = branch.getWorkProfiles().get(workprofileId).getQueueIds();
                List<Queue> availableQueues = branch
                        .getQueues().
                        entrySet().
                        stream().
                        filter(f -> queueIds.contains(f.getKey())).
                        map(Map.Entry::getValue).toList();
                return availableQueues.stream().map(Queue::getVisits).flatMap(List::stream).toList().stream().max(Comparator.comparing(Visit::getWaitingTime));
            }


        } else {
            throw new BusinessException("User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);

        }
        return Optional.empty();


    }
}
