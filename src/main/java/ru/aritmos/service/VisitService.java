package ru.aritmos.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ru.aritmos.events.model.Event;
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


    private void changedVisitEventSend(String visitState,Visit oldVisit,Visit newVisit,HashMap<String,String> params)
    {
        eventService.sendChangedEvent("*",false,oldVisit,newVisit,params,visitState);
    }
    @ExecuteOn(TaskExecutors.IO)
    public List<Visit> getVisits(String branchId,String queueId)
    {
        Branch currentBranch = branchService.getBranch(branchId);
        Queue queue ;
        if (currentBranch.getQueues().containsKey(queueId)) {
            queue = currentBranch.getQueues().get(queueId);
        }
        else
        {
            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }
        List<Visit> visits;
        visits=queue.getVisits();
        return visits;

    }
    public Visit createVisit(String branchId, String entryPointId, List<Service> services, Boolean printTicket) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (!services.isEmpty()) {
            EntryPoint entryPoint;

            if (currentBranch.getEntryPoints().containsKey(entryPointId)) {
                throw new BusinessException("EntryPoint not found in branch configuration!", eventService);
            } else {
                entryPoint = currentBranch.getEntryPoints().get(entryPointId);
            }
            Queue serviceQueue = currentBranch.getServices().get(0).linkedQueue;
            Integer ticketCounter = serviceQueue.getTicketCounter();
            serviceQueue.setTicketCounter(++ticketCounter);
            Visit result = Visit.builder()
                    .id(UUID.randomUUID().toString())
                    .status("CREATED")
                    .entryPoint(entryPoint)
                    .printTicket(printTicket)
                    .branchId(branchId)
                    .currentService(services.get(0))
                    .queue(serviceQueue)
                    .createDate(new Date())
                    .updateDate(new Date())
                    .servicePoint(null)
                    .ticketId(serviceQueue.getTicketPrefix() + serviceQueue.getTicketCounter().toString())
                    .servedServices(new ArrayList<>())
                    .unservedServices(services.size() > 1 ? services.subList(1, services.size() - 1) : new ArrayList<>())
                    .build();

            if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
                currentBranch.getQueues().get(serviceQueue.getId()).getVisits().add(result);
                branchService.add(currentBranch.getId(), currentBranch);
                if (printTicket && entryPoint.getPrinterId() != null) {
                    printerService.print(entryPoint.getPrinterId(), result);
                }
                changedVisitEventSend("CREATED",null,result,new HashMap<>());
                return result;
            } else {
                throw new BusinessException("Queue not found in branch configuration!", eventService);
            }

        } else {
            throw new BusinessException("Services can not be empty!", eventService);
        }
    }

    public Visit visitTransfer(String branchId,String servicePointId, String queueID, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        Visit oldVisit=visit.toBuilder().build();
        Queue queue = null;
        if (currentBranch.getQueues().containsKey(queueID)) {
            queue = currentBranch.getQueues().get(queueID);
        }
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            visit.setServicePoint(servicePoint);
            servicePoint.setVisit(null);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService);
            }
        }
        currentBranch.getServicePoints().get(servicePointId);
        visit.setServicePoint(null);

        visit.setQueue(queue);
        visit.setServicePoint(null);
        visit.setUpdateDate(new Date());
        visit.setVersion(visit.getVersion() + 1);
        visit.setStatus("TRANSFERRED");
        changedVisitEventSend("CHANGED",oldVisit,visit,new HashMap<>());
        return visit;
    }

    public Visit visitCall(String branchId,String servicePointId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        Visit oldVisit=visit.toBuilder().build();
        Optional<Queue> queue;
        visit.setUpdateDate(new Date());
        visit.setVersion(visit.getVersion() + 1);
        visit.setStatus("CALLED");

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            visit.setServicePoint(servicePoint);
            servicePoint.setVisit(visit);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueue().getId())).findFirst();
        if ((queue.isPresent())) {
            queue.get().getVisits().remove(visit);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }

        visit.setQueue(null);
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(new Date())
                .eventType("VISIT_CALLED")
                .senderService(applicationName)
                .build());
        changedVisitEventSend("CHANGED",oldVisit,visit,new HashMap<>());
        return visit;

    }

    public void deleteVisit(String branchId, String servicePointId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);

        Optional<Queue> queue;
        visit.setUpdateDate(new Date());
        visit.setVersion(visit.getVersion() + 1);
        visit.setStatus("CALLED");

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            visit.setServicePoint(servicePoint);
            servicePoint.setVisit(visit);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueue().getId())).findFirst();
        if ((queue.isPresent())) {
            queue.get().getVisits().remove(visit);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(new Date())
                .eventType("VISIT_DELETED")
                .senderService(applicationName)
                .build());
        changedVisitEventSend("DELETED",visit,null,new HashMap<>());
    }

}
