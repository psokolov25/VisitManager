package ru.aritmos.service.rules;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpStatus;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.BranchEntity;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

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
                Optional<Visit> result = availableQueues.stream()
                        .map(Queue::getVisits).flatMap(List::stream).toList().stream().filter(f -> f.getReturnDateTime() == null || f.getReturningTime() >  f.getReturnTimeDelay()).max((o1, o2) ->
                                o1.getReturningTime().compareTo(o2.getReturningTime()) == 0 ?
                                        o1.getWaitingTime().compareTo(o2.getWaitingTime()) :
                                        o1.getReturningTime().compareTo(o2.getReturningTime()));


                if (result.isPresent()) {
                    result.get().setReturnDateTime(null);
                    return result;
                } else {
                    return Optional.empty();
                }


            }


        } else {
            throw new BusinessException("User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);

        }
        return Optional.empty();


    }
    /**
     * Возвращает список точек обслуживания, которые могут вызвать данный визит
     * @param currentBranch текущее отделение
     * @param visit визит
     * @return список точек обслуживания
     */
    @Override

    public List<ServicePoint> getAvaliableServicePoints(Branch currentBranch,Visit visit)   {

        List<String> workProfileIds=currentBranch.getWorkProfiles().values().stream().filter(f->f.getQueueIds().contains(visit.getCurrentService().getLinkedQueueId())).map(BranchEntity::getId).toList();
        return currentBranch.getServicePoints().values().stream().filter(f->f.getUser()!=null && workProfileIds.contains(f.getUser().getCurrentWorkProfileId())).toList();
    }
}
