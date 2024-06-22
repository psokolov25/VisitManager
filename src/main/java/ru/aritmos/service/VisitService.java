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

import java.time.ZonedDateTime;
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


    private void changedVisitEventSend(String visitState, Visit oldVisit, Visit newVisit, HashMap<String, String> params) {
        eventService.sendChangedEvent("*", false, oldVisit, newVisit, params, visitState);
    }

    @ExecuteOn(TaskExecutors.IO)

    public List<Visit> getVisits(String branchId, String queueId) {
        Branch currentBranch = branchService.getBranch(branchId);
        Queue queue;
        if (currentBranch.getQueues().containsKey(queueId)) {
            queue = currentBranch.getQueues().get(queueId);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }
        List<Visit> visits;
        visits = queue.getVisits();
        return visits;

    }

    public Visit createVisit(String branchId, String entryPointId, ArrayList<String> servicesIds, Boolean printTicket) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServices().stream().anyMatch(f -> servicesIds.contains(f.getId()))) {
            List<Service> services = currentBranch.getServices().stream().filter(f -> servicesIds.contains(f.getId())).toList();
            return createVisit(branchId, entryPointId, services, printTicket);


        } else {
            throw new BusinessException("Services not found!", eventService);
        }
    }

    public Visit createVisit(String branchId, String entryPointId, List<Service> services, Boolean printTicket) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (!services.isEmpty()) {
            Service currentService = currentBranch.getServices().stream().filter(f -> f.getId().equals(services.get(0).getId())).findFirst().orElseThrow(() -> new RuntimeException("Service not found in branch configuration!"));
            EntryPoint entryPoint;

            if (!currentBranch.getEntryPoints().containsKey(entryPointId)) {
                throw new BusinessException("EntryPoint not found in branch configuration!", eventService);
            } else {
                entryPoint = currentBranch.getEntryPoints().get(entryPointId);
            }
            Queue serviceQueue = currentBranch.getQueues().get(currentService.getLinkedQueueId());
            Integer ticketCounter = serviceQueue.getTicketCounter();
            serviceQueue.setTicketCounter(++ticketCounter);


            Visit result = Visit.builder()
                    .id(UUID.randomUUID().toString())
                    .status("CREATED")
                    .entryPoint(entryPoint)
                    .printTicket(printTicket)
                    .branchId(branchId)
                    .currentService(currentService)
                    .queueId(serviceQueue.getId())
                    .createDate(ZonedDateTime.now())
                    .updateDate(ZonedDateTime.now())
                    .transferDate(ZonedDateTime.now())
                    .servicePointId(null)
                    .version(1)
                    .ticketId(serviceQueue.getTicketPrefix() + String.format("%03d", serviceQueue.getTicketCounter()))
                    .servedServices(new ArrayList<>())
                    .unservedServices(services.size() > 1 ? services.subList(1, services.size() - 1) : new ArrayList<>())
                    .build();
            if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
                serviceQueue.getVisits().add(result);


                branchService.add(currentBranch.getId(), currentBranch);

                if (printTicket && entryPoint.getPrinterId() != null) {
                    printerService.print(entryPoint.getPrinterId(), result);
                }
                changedVisitEventSend("CREATED", null, result, new HashMap<>());
                return result;
            } else {
                throw new BusinessException("Queue not found in branch configuration!", eventService);
            }

        } else {
            throw new BusinessException("Services can not be empty!", eventService);
        }
    }

    public Visit visitTransfer(String branchId, String servicePointId, String queueID, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        Visit oldVisit = visit.toBuilder().build();
        Queue queue;
        if (currentBranch.getQueues().containsKey(queueID)) {
            queue = currentBranch.getQueues().get(queueID);
        } else {

            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            visit.setServicePointId(servicePoint.getId());
            visit.setTransferDate(ZonedDateTime.now());
            servicePoint.setVisit(null);
            currentBranch.getServicePoints().put(servicePoint.getId(),servicePoint);


        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService);
            }
        }
        currentBranch.getServicePoints().get(servicePointId);
        visit.setServicePointId(null);

        assert queue != null;
        visit.setQueueId(queue.getId());
        visit.setServicePointId(null);
        visit.setUpdateDate(ZonedDateTime.now());
        visit.setVersion(visit.getVersion() + 1);
        visit.setStatus("TRANSFERRED");
        queue.getVisits().add(visit);
        currentBranch.getQueues().put(queue.getId(),queue);
        branchService.add(currentBranch.getId(),currentBranch);
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;
    }

    public Visit visitCall(String branchId, String servicePointId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        Visit oldVisit = visit.toBuilder().build();
        Optional<Queue> queue;
        visit.setUpdateDate(ZonedDateTime.now());
        visit.setVersion(visit.getVersion() + 1);
        visit.setStatus("CALLED");

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            visit.setServicePointId(servicePointId);
            servicePoint.setVisit(visit);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueueId())).findFirst();
        if ((queue.isPresent())) {
            List<Visit> visits = queue.get().getVisits();
            visits.removeIf(f -> f.getId().equals(visit.getId()));
            queue.get().setVisits(visits);
            currentBranch.getQueues().put(queue.get().getId(), queue.get());
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }

        visit.setQueueId(null);

        branchService.add(currentBranch.getId(), currentBranch);
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_CALLED")
                .senderService(applicationName)
                .build());
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    public Optional<Visit> visitCall(String branchId, String servicePointId, String queueID) {
        Branch currentBranch = branchService.getBranch(branchId);
        Queue currentQueue;
        if (currentBranch.getQueues().containsKey(queueID)) {
            currentQueue = currentBranch.getQueues().get(queueID);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }
        Optional<Visit> visit = currentQueue.getVisits().stream().max(Comparator.comparing(Visit::getWaitingTime));
        if (visit.isPresent()) {
            Visit oldVisit = visit.get().toBuilder().build();

            Optional<Queue> queue;
            visit.get().setUpdateDate(ZonedDateTime.now());
            visit.get().setVersion(visit.get().getVersion() + 1);
            visit.get().setStatus("CALLED");

            if (currentBranch.getServicePoints().containsKey(servicePointId)) {
                ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
                visit.get().setServicePointId(servicePointId);
                servicePoint.setVisit(visit.get());

            } else {
                if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                    throw new BusinessException("ServicePoint not found in branch configuration!", eventService);
                }
            }

            queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.get().getQueueId())).findFirst();
            if ((queue.isPresent())) {
                queue.get().getVisits().remove(visit.get());
            } else {
                throw new BusinessException("Queue not found in branch configuration!", eventService);
            }

            visit.get().setQueueId(null);
            eventService.send("*", true, Event.builder()
                    .body(visit)
                    .eventDate(ZonedDateTime.now())
                    .eventType("VISIT_CALLED")
                    .senderService(applicationName)
                    .build());
            changedVisitEventSend("CHANGED", oldVisit, visit.get(), new HashMap<>());
            return visit;
        }
        return Optional.empty();
    }

    public void deleteVisit(String branchId, String servicePointId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);

        Optional<Queue> queue;
        visit.setUpdateDate(ZonedDateTime.now());
        visit.setVersion(visit.getVersion() + 1);
        visit.setStatus("CALLED");

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            visit.setServicePointId(servicePointId);
            servicePoint.setVisit(visit);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueueId())).findFirst();
        if ((queue.isPresent())) {
            queue.get().getVisits().removeIf(f->f.getId().equals(visit.getId()));
            currentBranch.getQueues().put(queue.get().getId(),queue.get());
            branchService.add(currentBranch.getId(),currentBranch);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_DELETED")
                .senderService(applicationName)
                .build());
        changedVisitEventSend("DELETED", visit, null, new HashMap<>());
    }

}
