package ru.aritmos;

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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

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

    @Post(uri = "/branches/{id}/entrypoints/{entryPointId}/visit", consumes = "application/json", produces = "application/json")
    public Visit creeateVisit(@PathVariable String id, @PathVariable String entryPointId, @Body ArrayList<String> services, Boolean printTicket) {
        Branch branch;
        try {
            branch = branchService.getBranch(id);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        if (new HashSet<>(branch.getServices().stream().map(BranchEntity::getId).toList()).containsAll(services)) {

            Visit visit = visitService.createVisit(id, entryPointId, branchService.getBranch(id).getServices().stream().filter(f -> services.contains(f.getId())).toList(), printTicket);
            eventService.send("*", true, Event.builder()
                    .body(visit)
                    .eventDate(new Date())
                    .eventType("VISIT_CREATED")
                    .senderService(applicationName)
                    .build());
            return visit;
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Services not found!");
        }

    }

    @Post(uri = "/branches/visits/call", consumes = "application/json", produces = "application/json")
    public Visit callVisit(@Body Visit visit) {
        Visit result = visitService.visitCall(visit);

        eventService.send("*", true, Event.builder()
                .body(result)
                .eventDate(new Date())
                .eventType("VISIT_CALLED")
                .senderService(applicationName)
                .build());
        return visit;


    }

    @Put(uri = "/branches/{id}/queue/{queueId}", consumes = "application/json", produces = "application/json")
    public Visit transferVisit(@PathVariable String id, @PathVariable String queueId, @Body Visit visit) {
        Branch branch;
        Queue queue;
        try {
            branch = branchService.getBranch(id);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        if (!branch.getQueues().containsKey(queueId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
        }
        queue = branch.getQueues().get(queueId);


        Visit result = visitService.visitTransfer(visit, queue);
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(new Date())
                .eventType("VISIT_TRANSFERRED")
                .senderService(applicationName)
                .build());
        return result;


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

}