package ru.aritmos.api;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.*;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

import java.time.ZonedDateTime;
import java.util.*;

@Controller("/entrypoint")
public class EntrypointController {
    @Inject
    Services services;
    @Inject
    BranchService branchService;
    @Inject
    VisitService visitService;
    @Inject
    EventService eventService;
    @Value("${micronaut.application.name}")
    String applicationName;

    @Get(uri = "/branches/{id}")
    public Branch getBranch(String id) {
        Branch branch;
        try {
            branch = branchService.getBranch(id);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        return branch;
    }

    @Get(uri = "/branches/{id}/queues/{queueId}/visits", consumes = "application/json", produces = "application/json")
    public List<ru.aritmos.model.tiny.Visit> getVisits(@PathVariable String id, @PathVariable String queueId) {

        List<ru.aritmos.model.tiny.Visit> result = new ArrayList<>();

        visitService.getVisits(id, queueId).forEach(f -> {
            ru.aritmos.model.tiny.Visit visit =
                    ru.aritmos.model.tiny.Visit.builder()
                            .id(f.getId())
                            .ticketId(f.getTicketId())
                            .currentService(f.getCurrentService())
                            .transferDate(f.getTransferDate()).build();
            result.add(visit);
        });
        return result;

    }

    @Get(uri = "/branches/{id}/queues/{queueId}/visits/{visitId}", consumes = "application/json", produces = "application/json")
    public Visit getVisit(@PathVariable String id, @PathVariable String queueId, @PathVariable String visitId) {


        return visitService.getVisits(id, queueId).stream()
                .filter(f -> f.getId()
                        .equals(visitId)).findFirst().orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Visit not found!"));

    }

    @Post(uri = "/branches/{id}/entrypoints/{entryPointId}/visit", consumes = "application/json", produces = "application/json")
    public Visit creeateVisit(@PathVariable String id, @PathVariable String entryPointId, @Body ArrayList<String> services, @QueryValue Boolean printTicket) {
        Branch branch;
        try {
            branch = branchService.getBranch(id);
        } catch (Exception ex) {
            throw new BusinessException("Branch not found!", eventService, HttpStatus.NOT_FOUND);


        }
        if (new HashSet<>(branch.getServices().stream().map(BranchEntity::getId).toList()).containsAll(services)) {

            Visit visit = visitService.createVisit(id, entryPointId, branchService.getBranch(id).getServices().stream().filter(f -> services.contains(f.getId())).toList(), printTicket);
            eventService.send("*", true, Event.builder()
                    .body(visit)
                    .eventDate(ZonedDateTime.now())
                    .eventType("VISIT_CREATED")
                    .senderService(applicationName)
                    .build());
            return visit;

        } else {
            throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);

        }

    }

    @Get(uri = "/branches/{id}/services", produces = "application/json")
    public List<Service> GetAllServices(@PathVariable String id) {
        try {
            return services.getAllAvilableServies(id);

        } catch (BusinessException ex) {
            if (ex.getMessage().contains("not found")) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
            } else {
                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
            }
        }
    }


    @Post(uri = "/branches/{branchId}/visits/servicepoints/{id}/call", consumes = "application/json", produces = "application/json")
    public Visit callVisit(@PathVariable String branchId, @PathVariable String id, @Body Visit visit) {


        return visitService.visitCall(branchId, id, visit);


    }
    @Get(uri = "/branches/{branchId}/queues/{queueId}/servicepoints/{id}/call", consumes = "application/json", produces = "application/json")
    public Optional<Visit> callVisit(@PathVariable String branchId, @PathVariable String id,@PathVariable String queueId) {


        return visitService.visitCall(branchId, id,queueId);


    }

    @Delete(uri = "/branches/{branchId}/visits/servicepoints/{servicepointId}", consumes = "application/json", produces = "application/json")
    public void deleteVisit(@PathVariable String branchId, @PathVariable String servicepointId, @Body Visit visit) {


        visitService.deleteVisit(branchId, servicepointId, visit);


    }


    @Put(uri = "/branches/{branchId}/visits/serrvicepoints/{servicePointid}/queue/{queueId}", consumes = "application/json", produces = "application/json")
    public Visit transferVisit(@PathVariable String branchId, @PathVariable String servicePointid, @PathVariable String queueId, @Body Visit visit) {
        Branch branch;

        try {
            branch = branchService.getBranch(branchId);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        if (!branch.getQueues().containsKey(queueId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
        }


        Visit result = visitService.visitTransfer(branchId, servicePointid, queueId, visit);
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_TRANSFERRED")
                .senderService(applicationName)
                .build());
        return result;


    }


}