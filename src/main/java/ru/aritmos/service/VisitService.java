package ru.aritmos.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Queue;
import ru.aritmos.model.*;

import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
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
        return visits.stream().sorted((f1, f2) -> Long.compare(f2.getWaitingTime(), f1.getWaitingTime())).toList();

    }

    public List<Visit> getVisits(String branchId, String queueId, Long limit) {
        return getVisits(branchId, queueId).stream().limit(limit).toList();
    }

    public Visit createVisit(String branchId, String entryPointId, ArrayList<String> servicesIds, Boolean printTicket) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServices().keySet().stream().anyMatch(servicesIds::contains)) {
            ArrayList<Service> services = new ArrayList<>();
            servicesIds.forEach(f -> services.add(currentBranch.getServices().get(f)));


            return createVisit2(branchId, entryPointId, services, printTicket);


        } else {
            throw new BusinessException("Services not found!", eventService);
        }
    }

    public Visit createVisit2(String branchId, String entryPointId, ArrayList<Service> services, Boolean printTicket) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (!services.isEmpty()) {
            if (currentBranch.getServices().containsKey(services.get(0).getId())) {
                Service currentService = currentBranch.getServices().get(services.get(0).getId());
                List<Service> unServedServices = new ArrayList<>();
                services.stream().skip(1).forEach(f -> unServedServices.add(currentBranch.getServices().get(f.getId())));

                EntryPoint entryPoint;

                if (!currentBranch.getEntryPoints().containsKey(entryPointId)) {
                    throw new BusinessException("EntryPoint not found in branch configuration!", eventService);
                } else {
                    entryPoint = currentBranch.getEntryPoints().get(entryPointId);
                }
                Queue serviceQueue = currentBranch.getQueues().get(currentService.getLinkedQueueId());
                Integer ticketCounter = serviceQueue.getTicketCounter();
                serviceQueue.setTicketCounter(++ticketCounter);


                Visit visit = Visit.builder()
                        .id(UUID.randomUUID().toString())
                        .status("WAITING")
                        .entryPoint(entryPoint)
                        .printTicket(printTicket)
                        .branchId(branchId)
                        .currentService(currentService)
                        .unservedServices(unServedServices)
                        .queueId(serviceQueue.getId())
                        .createDate(ZonedDateTime.now())
                        .updateDate(ZonedDateTime.now())
                        .transferDate(ZonedDateTime.now())
                        .servicePointId(null)
                        .version(1)
                        .ticketId(serviceQueue.getTicketPrefix() + String.format("%03d", serviceQueue.getTicketCounter()))
                        .servedServices(new ArrayList<>())

                        .build();
                if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
                    serviceQueue.getVisits().add(visit);


                    branchService.add(currentBranch.getId(), currentBranch);

                    if (printTicket && entryPoint.getPrinterId() != null) {
                        printerService.print(entryPoint.getPrinterId(), visit);
                    }
                    changedVisitEventSend("CREATED", null, visit, new HashMap<>());
                    log.info("Visit {} created!", visit);
                    return visit;
                } else {
                    throw new BusinessException("Queue not found in branch configuration!", eventService);
                }

            } else {
                throw new BusinessException("Services can not be empty!", eventService);
            }
        } else {
            throw new BusinessException("Service  not found in branch configuration!", eventService);
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
            currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);


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
        currentBranch.getQueues().put(queue.getId(), queue);
        branchService.add(currentBranch.getId(), currentBranch);
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        log.info("Visit {} transfered!", visit);
        return visit;
    }

    public Visit visitEnd(String branchId, String servicePointId) {
        Branch currentBranch = branchService.getBranch(branchId);
        Visit visit;


        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                visit = servicePoint.getVisit();
                Visit oldVisit = visit.toBuilder().build();
                visit.setServicePointId(servicePoint.getId());
                visit.setTransferDate(ZonedDateTime.now());
                servicePoint.setVisit(null);
                currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
                currentBranch.getServicePoints().get(servicePointId);
                visit.setServicePointId(null);


                if (visit.getUnservedServices() != null && !visit.getUnservedServices().isEmpty()) {
                    visit.getServedServices().add(visit.toBuilder().build().getCurrentService());
                    visit.setCurrentService(visit.toBuilder().build().getUnservedServices().get(0));
                    visit.getUnservedServices().remove(0);
                    String queueIdToReturn = visit.getCurrentService().getLinkedQueueId();
                    visit.setQueueId(queueIdToReturn);
                    Queue queue = currentBranch.getQueues().get(queueIdToReturn);
                    queue.getVisits().add(visit);
                    currentBranch.getQueues().put(queue.getId(), queue);
                    visit.setStatus("WAITING");

                } else {
                    visit.getServedServices().add(visit.getCurrentService());
                    visit.setCurrentService(null);


                    visit.setQueueId(null);
                    visit.setStatus("ENDED");

                }
                visit.setServicePointId(null);
                visit.setUpdateDate(ZonedDateTime.now());
                visit.setVersion(visit.getVersion() + 1);



                branchService.add(currentBranch.getId(), currentBranch);
                changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
                log.info("Visit {} ended", visit);
                return visit;

            } else {
                throw new BusinessException("Visit not found in ServicePoint ", eventService, HttpStatus.NOT_FOUND);
            }

        } else {

            throw new BusinessException("ServicePoint not found in branch configuration!", eventService);

        }

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
            if(servicePoint.getVisit()!=null)
            {
                throw new BusinessException("Visit alredey called in the ServicePoint! ", eventService, HttpStatus.CONFLICT);
            }
            visit.setServicePointId(servicePointId);
            visit.setUserName(servicePoint.getUser() != null ? servicePoint.getUser().getName() : null);
            servicePoint.setVisit(visit);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService,HttpStatus.NOT_FOUND);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueueId())).findFirst();
        if ((queue.isPresent())) {
            List<Visit> visits = queue.get().getVisits();
            visits.removeIf(f -> f.getId().equals(visit.getId()));
            queue.get().setVisits(visits);
            currentBranch.getQueues().put(queue.get().getId(), queue.get());
        } else {
            throw new BusinessException("Queue not found in branch configuration or not available for current workprofile!", eventService,HttpStatus.NOT_FOUND);
        }

        visit.setQueueId(null);

        branchService.add(currentBranch.getId(), currentBranch);
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_CALLED")
                .senderService(applicationName)
                .build());
        log.info("Visit {} called!", visit);
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    public Optional<Visit> visitCall(String branchId, String servicePointId) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getUser() != null) {
                String workprofileId = servicePoint.getUser().getCurrentWorkProfileId();
                List<String> queueIds = currentBranch.getWorkProfiles().get(workprofileId).getQueueIds();
                List<Queue> avaibleQueues = currentBranch
                        .getQueues().
                        entrySet().
                        stream().
                        filter(f -> queueIds.contains(f.getKey())).
                        map(Map.Entry::getValue).toList();
                Optional<Visit> visit = avaibleQueues.stream().map(Queue::getVisits).flatMap(List::stream).toList().stream().max(Comparator.comparing(Visit::getWaitingTime));
                if (visit.isPresent()) {
                    return visit.map(value -> this.visitCall(branchId, servicePointId, value));
                }

            } else {
                throw new BusinessException("User not logged in in service point!", eventService,HttpStatus.FORBIDDEN);

            }


        } else {
            throw new BusinessException("ServicePoint not found in branch configuration!", eventService,HttpStatus.NOT_FOUND);

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
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService,HttpStatus.NOT_FOUND);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueueId())).findFirst();
        if ((queue.isPresent())) {
            if(!queue.get().getVisits().stream().map(Visit::getId).toList().contains(visit.getId()))
            {
                throw new BusinessException("Visit not found or already deleted!", eventService,HttpStatus.NOT_FOUND);
            }
            queue.get().getVisits().removeIf(f -> f.getId().equals(visit.getId()));
            currentBranch.getQueues().put(queue.get().getId(), queue.get());
            branchService.add(currentBranch.getId(), currentBranch);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService,HttpStatus.NOT_FOUND);
        }
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_DELETED")
                .senderService(applicationName)
                .build());
        log.info("Visit {} deleted!", visit);
        changedVisitEventSend("DELETED", visit, null, new HashMap<>());
    }

}
