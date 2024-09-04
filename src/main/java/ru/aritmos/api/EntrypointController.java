package ru.aritmos.api;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.utils.SecurityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;
import io.micronaut.security.authentication.Authentication;
import java.util.*;

/**
 * @author Pavel Sokolov
 * REST API управления зоной ожидания
 */


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
    @Inject
    SecurityService securityService;


    @Value("${micronaut.application.name}")
    String applicationName;


    /**
     * Создание визита
     *
     * @param branchId     идентификатор отделения
     * @param entryPointId идентификатор точки создания визита
     * @param serviceIds   массив идентификаторов услуг (на пример [
     *                     "c3916e7f-7bea-4490-b9d1-0d4064adbe8b","9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"
     *                     ]
     * @param printTicket  флаг печати талона
     * @return созданный визит
     */

    @Tag(name = "Зона ожидания")
    @Tag(name = "Полный список")
    @Post(uri = "/branches/{branchId}/entryPoints/{entryPointId}/visit", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit createVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "2") String entryPointId, @Body ArrayList<String> serviceIds, @QueryValue Boolean printTicket) {
        Branch branch;
        try {
            branch = branchService.getBranch(branchId);
        } catch (Exception ex) {
            throw new BusinessException("Branch not found!", eventService, HttpStatus.NOT_FOUND);


        }
        if (new HashSet<>(branch.getServices().values().stream().map(BranchEntity::getId).toList()).containsAll(serviceIds)) {

            ArrayList<String> services = new ArrayList<>();
            branchService.getBranch(branchId).getServices().values().stream().map(BranchEntity::getId).filter(serviceIds::contains).forEach(services::add);

            return visitService.createVisit(branchId, entryPointId, services, printTicket);

        } else {
            throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);

        }

    }

    /**
     * Получение списка доступных услуг
     *
     * @param branchId идентификатор отделения
     * @return список услуг
     */

    @Tag(name = "Зона ожидания")
    @Tag(name = "Полный список")
    @Get(uri = "/branches/{branchId}/services", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public List<Service> getAllAvilableServies(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,Authentication authentication) {
        try {

            return services.getAllAvilableServies(branchId);

        } catch (BusinessException ex) {
            if (ex.getMessage().contains("not found")) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
            } else {
                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
            }
        }
    }


}

