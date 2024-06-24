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
    @Value("${micronaut.application.name}")
    String applicationName;

    /**
     * Возвращает данные об отделении
     * @param id идентификатор отделения
     * @return состояние отделения
     */
    @Get(uri = "/branches/{id}")
    public BranchEntity getBranch(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4")  String id) {
        Branch branch;
        try {
            branch = branchService.getBranch(id);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        return branch;
    }

    /**
     * Получение массива идентификаторов и названий отделений
     * @return массив идентификаторов и названий отделений
     */
    @Get(uri = "/branches")
    public HashMap<String,BranchEntity> getBranches() {
       return branchService.getBranches();
    }

    /**
     * Возвращает список визитов из очереди
     * @param branchId идентификатор отделения
     * @param queueId идентификатор очереди
     * @return список визитов
     */
    @Get(uri = "/branches/{branchId}/queues/{queueId}/visits", consumes = "application/json", produces = "application/json")
    public List<ru.aritmos.model.tiny.Visit> getVisits(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId) {

        List<ru.aritmos.model.tiny.Visit> result = new ArrayList<>();

        visitService.getVisits(branchId, queueId).forEach(f -> {
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
    /**
     * Возвращает список визитов из очереди
     * @param branchId идентификатор отделения
     * @param queueId идентификатор очереди
     * @param limit количество последних возвращаемых талонов
     * @return список визитов
     */
    @Get(uri = "/branches/{branchId}/queues/{queueId}/visits/limit/{limit}", consumes = "application/json", produces = "application/json")
    public List<ru.aritmos.model.tiny.Visit> getVisits(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,@PathVariable Long limit) {

        List<ru.aritmos.model.tiny.Visit> result = new ArrayList<>();

        visitService.getVisits(branchId, queueId,limit).forEach(f -> {
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

    /**
     * Получает данные о визите
     * @param branchId идентификатор отделения
     * @param queueId идентификатор очереди
     * @param visitId идентификатор визита
     * @return данные о визите
     */
    @Get(uri = "/branches/{branchId}/queues/{queueId}/visits/{visitId}", consumes = "application/json", produces = "application/json")
    public Visit getVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId, @PathVariable String visitId) {


        return visitService.getVisits(branchId, queueId).stream()
                .filter(f -> f.getId()
                        .equals(visitId)).findFirst().orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Visit not found!"));

    }

    /**
     *Создание визита
     * @param branchId идентификатор отделения
     * @param entryPointId идентификатор точки создания визита
     * @param serviceIds массив идентификаторов услуг
     * @param printTicket флаг печати талона
     * @return созданный визит
     */
    @Post(uri = "/branches/{branchId}/entrypoints/{entryPointId}/visit", consumes = "application/json", produces = "application/json")
    public Visit creeateVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "2") String entryPointId, @Body ArrayList<String> serviceIds, @QueryValue Boolean printTicket) {
        Branch branch;
        try {
            branch = branchService.getBranch(branchId);
        } catch (Exception ex) {
            throw new BusinessException("Branch not found!", eventService, HttpStatus.NOT_FOUND);


        }
        if (new HashSet<>(branch.getServices().stream().map(BranchEntity::getId).toList()).containsAll(serviceIds)) {

            Visit visit = visitService.createVisit(branchId, entryPointId, branchService.getBranch(branchId).getServices().stream().filter(f -> serviceIds.contains(f.getId())).toList(), printTicket);
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

    /**
     * Получение списка доступных услуг
     * @param branchId идентификатор отделения
     * @return список услуг
     */
    @Get(uri = "/branches/{branchId}/services", produces = "application/json")
    public List<Service> GetAllServices(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
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

    /**
     * Вызов визита
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit визит
     * @return вызванный визит
     */
    @Post(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}/call", consumes = "application/json", produces = "application/json")
    public Visit callVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "099c43c1-40b5-4b80-928a-1d4b363152a8") String servicePointId, @Body Visit visit) {


        return visitService.visitCall(branchId, servicePointId, visit);


    }
    /**
     * Вызов визита с наибольшим временем ожидания
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     *
     * @return вызванный визит
     */
    @Get(uri = "/branches/{branchId}/servicepoints/{servicePointId}/call", consumes = "application/json", produces = "application/json")
    public Optional<Visit> callVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "099c43c1-40b5-4b80-928a-1d4b363152a8") String servicePointId) {


        return visitService.visitCall(branchId, servicePointId);


    }

    /**
     * Удаление визита из очереди
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit визит
     */
    @Delete(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}", consumes = "application/json", produces = "application/json")
    public void deleteVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "099c43c1-40b5-4b80-928a-1d4b363152a8") String servicePointId, @Body Visit visit) {


        visitService.deleteVisit(branchId, servicePointId, visit);


    }

    /**
     *
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param queueId идентификатор очереди
     * @param visit визит
     * @return визит после перевода
     */
    @Put(uri = "/branches/{branchId}/visits/serrvicepoints/{servicePointId}/queue/{queueId}", consumes = "application/json", produces = "application/json")
    public Visit transferVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "099c43c1-40b5-4b80-928a-1d4b363152a8") String servicePointId, @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId, @Body Visit visit) {
        Branch branch;

        try {
            branch = branchService.getBranch(branchId);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        if (!branch.getQueues().containsKey(queueId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
        }


        Visit result = visitService.visitTransfer(branchId, servicePointId, queueId, visit);
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_TRANSFERRED")
                .senderService(applicationName)
                .build());
        return result;


    }


}