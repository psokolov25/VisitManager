package ru.aritmos.service;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.Visit;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class VisitService {
    @Inject
    BranchService branchService;

    @ExecuteOn(TaskExecutors.IO)
    public Visit createVisit(String branchId, ArrayList<Service> services) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (!services.isEmpty()) {

            Queue serviceQueue = currentBranch.getServices().get(0).linkedQueue;
            Integer ticketCounter=serviceQueue.getTicketCounter();
            serviceQueue.setTicketCounter(++ticketCounter);
            Visit result = Visit.builder()
                    .id(UUID.randomUUID().toString())

                    .currentService(services.get(0))
                    .queue(serviceQueue)
                    .createData(new Date())
                    .ticket(serviceQueue.getTicketPrefix() + serviceQueue.getTicketCounter().toString())
                    .servedServices(new ArrayList<>())
                    .unservedServices(services.size() > 1 ? services.subList(1, services.size() - 1) : new ArrayList<>())
                    .build();

            if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
                currentBranch.getQueues().get(serviceQueue.getId()).getVisits().add(result);
                branchService.add(currentBranch.getId(),currentBranch);
                return result;
            } else {
                throw new BusinessException("Queue not found in branch configuration!");
            }

        } else {
            throw new BusinessException("Services can not be empty!");
        }
    }
}
