package ru.aritmos.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Queue;
import ru.aritmos.model.*;
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.rules.SegmentationRule;

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
    @Inject
    CallRule callRule;
    @Inject
    SegmentationRule segmentationRule;
    @Value("${micronaut.application.name}")
    String applicationName;

    public @NotNull HashMap<String, ServicePoint> getStringServicePointHashMap(String branchId) {
        Branch currentBranch = branchService.getBranch(branchId);
        HashMap<String, ServicePoint> freeServicePoints = new HashMap<>();
        currentBranch.getServicePoints().entrySet().stream().filter(f -> f.getValue().getUser() == null).forEach(fe -> freeServicePoints.put(fe.getKey(), fe.getValue()));
        return freeServicePoints;
    }

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

    public Optional<List<Queue>> getQueues(String branchId, String servicePointId) {
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
                return Optional.of(avaibleQueues);

            } else {
                throw new BusinessException("User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);

            }


        } else {
            throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);

        }

    }

    public HashMap<String, Visit> getAllVisits(String brainchId) {
        return branchService.getBranch(brainchId).getAllVisits();
    }

    public HashMap<String, Visit> getVisitsByStatuses(String brainchId, List<String> statuses) {
        return branchService.getBranch(brainchId).getVisitsByStatus(statuses);
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


                Visit visit = Visit.builder()
                        .id(UUID.randomUUID().toString())
                        .status("WAITING")
                        .entryPoint(entryPoint)
                        .printTicket(printTicket)
                        .branchId(branchId)
                        .branchName(currentBranch.getName())
                        .currentService(currentService)
                        .unservedServices(unServedServices)
                        .createDateTime(ZonedDateTime.now())
                        //.updateDateTime(ZonedDateTime.now())
                        //.transferDateTime(ZonedDateTime.now())
                        // .endDateTime(ZonedDateTime.now())
                        .servicePointId(null)

                        .servedServices(new ArrayList<>())
                        .transactions(new ArrayList<>())
                        .parameterMap(new HashMap<>())
                        .build();
                Queue serviceQueue;
                if (segmentationRule.getQueue(visit, currentBranch).isPresent()) {
                    serviceQueue = segmentationRule.getQueue(visit, currentBranch).get();

                    serviceQueue.setTicketCounter(branchService.incrementTicetCounter(branchId, serviceQueue));
                    visit.setQueueId(serviceQueue.getId());
                    visit.setTicketId((serviceQueue.getTicketPrefix() + String.format("%03d", serviceQueue.getTicketCounter())));
                    VisitEvent event = VisitEvent.CREATED;
                    event.dateTime = ZonedDateTime.now();
                    visit.setTransaction(event, eventService);
                    branchService.updateVisit(visit);
                    if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
                        VisitEvent queueEvent = VisitEvent.PLACED_IN_QUEUE;
                        queueEvent.dateTime = ZonedDateTime.now();
                        visit.setQueueId(serviceQueue.getId());
                        visit.updateTransaction(queueEvent, eventService);


                        if (printTicket && entryPoint.getPrinterId() != null) {
                            printerService.print(entryPoint.getPrinterId(), visit);
                        }


                        changedVisitEventSend("CREATED", null, visit, new HashMap<>());
                        branchService.updateVisit(visit);
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

        throw new BusinessException("Queue  not found in branch configuration!", eventService);
    }

    public Visit returnVisit(String branchId, String servicePointId, Long returnTimeDelay) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                visit.setReturnDateTime(ZonedDateTime.now());
                visit.setReturnTimeDelay(returnTimeDelay);
                branchService.updateVisit(visit);
                if (visit.getParameterMap().containsKey("LastQueueId")) {
                    return visitTransfer(branchId, servicePointId, visit.getParameterMap().get("LastQueueId").toString());
                } else {
                    throw new BusinessException("Visit cant be transfer!", eventService);
                }
            } else {
                throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    public Visit visitTransfer(String branchId, String servicePointId, String queueID) {

        Branch currentBranch = branchService.getBranch(branchId);

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                Visit oldVisit = visit.toBuilder().build();
                Queue queue;
                if (currentBranch.getQueues().containsKey(queueID)) {
                    queue = currentBranch.getQueues().get(queueID);
                } else {

                    throw new BusinessException("Queue not found in branch configuration!", eventService);
                }
                visit.setServicePointId(null);
                currentBranch.getServicePoints().get(servicePointId);
                visit.setServicePointId(null);

                assert queue != null;
                visit.setQueueId(queue.getId());
                visit.setServicePointId(null);

                visit.setTransferDateTime(ZonedDateTime.now());
                queue.getVisits().add(visit);
                currentBranch.getQueues().put(queue.getId(), queue);
                VisitEvent event = VisitEvent.BACK_TO_QUEUE;
                event.dateTime = ZonedDateTime.now();
                visit.updateTransaction(event, eventService);
                branchService.updateVisit(visit);
                changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
                log.info("Visit {} transfered!", visit);
                return visit;
            } else {
                throw new BusinessException(String.format("Visit in ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    public Visit visitTransferFromQueue(String branchId, String servicePointId, String queueID, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (visit.getQueueId().isBlank()) {
            throw new BusinessException("Visit not in a queue!", eventService);
        }
        Visit oldVisit = visit.toBuilder().build();
        Queue queue;
        if (currentBranch.getQueues().containsKey(queueID)) {
            queue = currentBranch.getQueues().get(queueID);
        } else {

            throw new BusinessException("Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        }

        currentBranch.getServicePoints().get(servicePointId);
        visit.setServicePointId(null);

        assert queue != null;
        visit.setQueueId(queue.getId());



        queue.getVisits().add(visit);
        currentBranch.getQueues().put(queue.getId(), queue);
        VisitEvent event = VisitEvent.BACK_TO_QUEUE;
        event.dateTime = ZonedDateTime.now();
        visit.setTransaction(event, eventService);
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
                visit.setTransferDateTime(ZonedDateTime.now());
                servicePoint.setVisit(null);
                currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
                currentBranch.getServicePoints().get(servicePointId);
                visit.setServicePointId(null);

                VisitEvent event;

                if (visit.getUnservedServices() != null && !visit.getUnservedServices().isEmpty()) {
                    visit.getServedServices().add(visit.toBuilder().build().getCurrentService());
                    visit.setCurrentService(visit.toBuilder().build().getUnservedServices().get(0));
                    visit.getUnservedServices().remove(0);
                    String queueIdToReturn = visit.getCurrentService().getLinkedQueueId();
                    visit.setQueueId(queueIdToReturn);


                    visit.setServedDateTime(ZonedDateTime.now());
                    event = VisitEvent.STOP_SERVING;
                    event.dateTime = ZonedDateTime.now();

                    visit.updateTransaction(event, eventService);
                    event = VisitEvent.BACK_TO_QUEUE;
                    event.dateTime = ZonedDateTime.now();
                    visit.setReturnDateTime(ZonedDateTime.now());
                    visit.setCallDateTime(null);

                    visit.setStartServingDateTime(null);

                    visit.setTransaction(event, eventService);
                    visit.setServicePointId(null);
                    //Queue queue = currentBranch.getQueues().get(queueIdToReturn);
                    //queue.getVisits().add(visit);
                    //currentBranch.getQueues().put(queue.getId(), queue);

                } else {
                    visit.getServedServices().add(visit.getCurrentService());
                    visit.setCurrentService(null);
                    visit.setServedDateTime(ZonedDateTime.now());
                    visit.setQueueId(null);
                    event = VisitEvent.END;
                    event.dateTime = ZonedDateTime.now();
                    visit.updateTransaction(event, eventService);
                    visit.setServicePointId(null);

                }


                branchService.updateVisit(visit);
                changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
                log.info("Visit {} ended", visit);
                return visit;

            } else {
                throw new BusinessException("Visit not found in ServicePoint ", eventService, HttpStatus.NOT_FOUND);
            }

        } else {

            throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);

        }

    }


    public Visit visitCall(String branchId, String servicePointId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        Visit oldVisit = visit.toBuilder().build();
        Optional<Queue> queue;

        visit.setStatus("CALLED");
        visit.setCallDateTime(ZonedDateTime.now());

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                throw new BusinessException("Visit alredey called in the ServicePoint! ", eventService, HttpStatus.CONFLICT);
            }
            visit.setServicePointId(servicePointId);
            visit.setUserName(servicePoint.getUser() != null ? servicePoint.getUser().getName() : null);
            servicePoint.setVisit(visit);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueueId())).findFirst();
        if ((queue.isPresent())) {
            List<Visit> visits = queue.get().getVisits();
            visits.removeIf(f -> f.getId().equals(visit.getId()));
            queue.get().setVisits(visits);
            currentBranch.getQueues().put(queue.get().getId(), queue.get());
        } else {
            throw new BusinessException("Queue not found in branch configuration or not available for current workprofile!", eventService, HttpStatus.NOT_FOUND);
        }
        visit.getParameterMap().put("LastQueueId", visit.getQueueId());
        visit.setQueueId(null);
        VisitEvent event = VisitEvent.CALLED;
        event.dateTime = ZonedDateTime.now();
        visit.updateTransaction(event, eventService);
        VisitEvent servingEvent = VisitEvent.START_SERVING;
        servingEvent.dateTime = ZonedDateTime.now();
        visit.setStartServingDateTime(ZonedDateTime.now());
        visit.updateTransaction(servingEvent, eventService);
        branchService.add(currentBranch.getId(), currentBranch);

        log.info("Visit {} called!", visit);
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }
    public Visit visitCall(String branchId, String servicePointId, String visitId) {
        if(this.getAllVisits(branchId).containsKey(visitId)) {
            Visit visit = this.getAllVisits(branchId).get(visitId);
            return this.visitCall(branchId, servicePointId, visit);
        }
        throw new BusinessException(String.format("Visit %s not found!",visitId), eventService, HttpStatus.NOT_FOUND);


    }

    public Visit visitCallForConfirm(String branchId, String servicePointId, Visit visit) {

        Visit oldVisit = visit.toBuilder().build();

        visit.setStatus("CALLED");
        visit.setCallDateTime(ZonedDateTime.now());


        VisitEvent event = VisitEvent.CALLED;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("ServicePointId", servicePointId);
        event.getParameters().put("branchID", branchId);
        visit.updateTransaction(event, eventService);
        branchService.updateVisit(visit);


        log.info("Visit {} called!", visit);
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    public Visit visitReCallForConfirm(String branchId, String servicePointId, Visit visit) {

        Visit oldVisit = visit.toBuilder().build();

        visit.setStatus("RECALLED");
        visit.setCallDateTime(ZonedDateTime.now());


        VisitEvent event = VisitEvent.RECALLED;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("ServicePointId", servicePointId);
        event.getParameters().put("branchID", branchId);
        visit.updateTransaction(event, eventService);
        branchService.updateVisit(visit);


        log.info("Visit {} called!", visit);
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    public Visit visitCallConfirm(String branchId, String servicePointId, Visit visit) {

        Visit oldVisit = visit.toBuilder().build();


        visit.setStatus("START_SERVING");
        visit.setStartServingDateTime(ZonedDateTime.now());
        visit.getParameterMap().put("LastQueueId", visit.getQueueId());
        visit.setQueueId(null);
        visit.setServicePointId(servicePointId);


        VisitEvent event = VisitEvent.START_SERVING;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("ServicePointId", servicePointId);
        event.getParameters().put("branchID", branchId);
        visit.updateTransaction(event, eventService);
        branchService.updateVisit(visit);


        log.info("Visit {} statted serving!", visit);
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    public Visit visitCallNoShow(String branchId, String servicePointId, Visit visit) {

        Visit oldVisit = visit.toBuilder().build();


        visit.setStatus("NO_SHOW");
        visit.setStartServingDateTime(null);
        visit.setQueueId(null);
        visit.setServicePointId(null);


        VisitEvent event = VisitEvent.NO_SHOW;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("ServicePointId", servicePointId);
        event.getParameters().put("branchID", branchId);
        visit.updateTransaction(event, eventService);
        branchService.updateVisit(visit);


        log.info("Visit {} statted serving!", visit);
        changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    public Optional<Visit> visitCallForConfirm(String branchId, String servicePointId) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);

            Optional<Visit> visit = callRule.call(currentBranch, servicePoint);
            if (visit.isPresent()) {

                return Optional.of(visitCallForConfirm(branchId, servicePointId, visit.get()));
            }

        } else {
            throw new BusinessException("User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);

        }


        return Optional.empty();

    }


    public Optional<Visit> visitCall(String branchId, String servicePointId) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getUser() != null) {

                Optional<Visit> visit = callRule.call(currentBranch, servicePoint);
                if (visit.isPresent()) {

                    return visit.map(value -> this.visitCall(branchId, servicePointId, value));
                }

            } else {
                throw new BusinessException("User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);

            }


        } else {
            throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);

        }
        return Optional.empty();
    }


    public void deleteVisit(String branchId, String servicePointId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (visit.getReturningTime() > 0 && visit.getReturningTime() < visit.getReturnTimeDelay()) {
            throw new BusinessException("You cant delete just returned visit!", eventService, HttpStatus.NOT_FOUND);
        }
        Optional<Queue> queue;

        visit.setStatus("CALLED");

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            visit.setServicePointId(servicePointId);
            servicePoint.setVisit(visit);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueueId())).findFirst();
        if ((queue.isPresent())) {
            if (!queue.get().getVisits().stream().map(Visit::getId).toList().contains(visit.getId())) {
                throw new BusinessException("Visit not found or already deleted!", eventService, HttpStatus.NOT_FOUND);
            }
            queue.get().getVisits().removeIf(f -> f.getId().equals(visit.getId()));
            currentBranch.getQueues().put(queue.get().getId(), queue.get());
            branchService.add(currentBranch.getId(), currentBranch);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        }
        VisitEvent event = VisitEvent.NO_SHOW;
        event.dateTime = ZonedDateTime.now();
        visit.updateTransaction(event, eventService);
        log.info("Visit {} deleted!", visit);
        changedVisitEventSend("DELETED", visit, null, new HashMap<>());
    }

}
