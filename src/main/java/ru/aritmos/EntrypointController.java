package ru.aritmos;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.BranchEntity;
import ru.aritmos.model.Service;
import ru.aritmos.model.Visit;
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

    @Post(uri = "/branches/{id}/visit", consumes = "application/json", produces = "application/json")
    public Visit creeateVisit(@PathVariable String id, @Body ArrayList<String> services)  {
        Branch branch;
        try {
            branch = branchService.getBranch(id);
        } catch (Exception ex) {
            throw new HttpClientResponseException("Branch not found!", HttpResponse.notFound());
        }
        if (new HashSet<>(branch.getServices().stream().map(BranchEntity::getId).toList()).containsAll(services)) {

            Visit visit = visitService.createVisit(id, branchService.getBranch(id).getServices().stream().filter(f -> services.contains(f.getId())).toList());
            eventService.send("*", true, Event.builder()
                    .body(visit)
                    .eventDate(new Date())
                    .eventType("VISIT_CREATED")
                    .senderService(applicationName)
                    .build());
            return visit;
        } else {
            throw new HttpClientResponseException("Services not found!", HttpResponse.notFound());
        }

    }


    @Get(uri = "/branches/{id}/services", produces = "application/json")
    public List<Service> GetAllServices(@PathVariable String id) {
        try {
            return services.getAllAvilableServies(id);

        } catch (BusinessException ex) {
            if (ex.getMessage().contains("not found")) {
                throw new HttpClientResponseException(ex.getMessage(), HttpResponse.notFound());
            } else {
                throw new HttpClientResponseException(ex.getMessage(), HttpResponse.serverError());
            }
        }
    }

}