package ru.aritmos.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Queue;
import ru.aritmos.model.*;

import java.util.*;

@Singleton
public class VisitService {
    @Inject
    BranchService branchService;
    @Inject
    EventService eventService;
    @Inject
    PrinterService printerService;

    @Value("${micronaut.application.name}")
    String applicationName;

    @ExecuteOn(TaskExecutors.IO)

    public Visit createVisit(String branchId, String entryPointId, List<Service> services, Boolean printTicket) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (!services.isEmpty()) {
            EntryPoint entryPoint;

            if (currentBranch.getEntryPoints().stream().noneMatch(f -> f.getId().equals(entryPointId))) {
                throw new BusinessException("EntryPoint not found in branch configuration!", eventService, applicationName);
            } else {
                entryPoint = currentBranch.getEntryPoints().stream().filter(f -> f.getId().equals(entryPointId)).findFirst().get();
            }
            Queue serviceQueue = currentBranch.getServices().get(0).linkedQueue;
            Integer ticketCounter = serviceQueue.getTicketCounter();
            serviceQueue.setTicketCounter(++ticketCounter);
            Visit result = Visit.builder()
                    .id(UUID.randomUUID().toString())
                    .entryPoint(entryPoint)
                    .printTicket(printTicket)
                    .branchId(branchId)
                    .currentService(services.get(0))
                    .queue(serviceQueue)
                    .createData(new Date())
                    .ticket(serviceQueue.getTicketPrefix() + serviceQueue.getTicketCounter().toString())
                    .servedServices(new ArrayList<>())
                    .unservedServices(services.size() > 1 ? services.subList(1, services.size() - 1) : new ArrayList<>())
                    .build();

            if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
                currentBranch.getQueues().get(serviceQueue.getId()).getVisits().add(result);
                branchService.add(currentBranch.getId(), currentBranch);
                if (printTicket && entryPoint.getPrinterId() != null) {
                    printerService.print(entryPoint.getPrinterId(), result);
                }
                return result;
            } else {
                throw new BusinessException("Queue not found in branch configuration!", eventService, applicationName);
            }

        } else {
            throw new BusinessException("Services can not be empty!", eventService, applicationName);
        }
    }

    public Visit visitTransfer(Visit visit, Queue queue) {
        visit.setQueue(queue);

        return visit;
    }

    public Visit visitCall(Visit visit) {
        Branch currentBranch = branchService.getBranch(visit.getBranchId());
        Optional<Queue> queue;

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueue().getId())).findFirst();
        if ((queue.isPresent())) {
            queue.get().getVisits().remove(visit);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService, applicationName);
        }

        visit.setQueue(null);

        return visit;
    }


}
