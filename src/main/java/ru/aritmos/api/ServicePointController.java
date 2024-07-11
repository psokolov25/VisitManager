package ru.aritmos.api;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.tiny.TinyVisit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * @author Pavel Sokolov
 * REST API управления зоной ожидания
 */

@Controller("/servicepoint")
public class ServicePointController {
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
     * Возвращает список визитов из очереди
     * @param branchId идентификатор отделения
     * @param queueId идентификатор очереди
     * @param limit количество последних возвращаемых талонов
     * @return список визитов
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/queues/{queueId}/visits/limit/{limit}", consumes = "application/json", produces = "application/json")
    public List<TinyVisit> getVisits(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId, @PathVariable Long limit) {

        List<TinyVisit> result = new ArrayList<>();

        visitService.getVisits(branchId, queueId,limit).forEach(f -> {
            TinyVisit visit =
                    TinyVisit.builder()
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
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/queues/{queueId}/visits/{visitId}", consumes = "application/json", produces = "application/json")
    public Visit getVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId, @PathVariable String visitId) {


        return visitService.getVisits(branchId, queueId).stream()
                .filter(f -> f.getId()
                        .equals(visitId)).findFirst().orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Visit not found!"));

    }





    /**
     * Вызов визита
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}/call", consumes = "application/json", produces = "application/json")
    public Visit callVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId, @Body Visit visit) {


        return visitService.visitCall(branchId, servicePointId, visit);


    }

    /**
     * Вызов наиболее ожидающего визита c ожиданием подтверждения
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}/confirmed/call", consumes = "application/json", produces = "application/json")
    public Optional<Visit> visitCallForConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {


        return visitService.visitCallForConfirm(branchId, servicePointId);


    }

    /**
     * Вызов визита c ожиданием подтверждения
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}/confirmed/call", consumes = "application/json", produces = "application/json")
    public Visit visitCallForConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId, @Body Visit visit) {


        return visitService.visitCallForConfirm(branchId, servicePointId, visit);


    }
    /**
     * Отмена вызова из за того, что сотрудник не пришел
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}/confirmed/noshow", consumes = "application/json", produces = "application/json")
    public Visit visitCallNoShow(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId, @Body Visit visit) {


        return visitService.visitCallNoShow(branchId, servicePointId, visit);


    }
    /**
     * Вызов визита c ожиданием подтверждения
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}/confirmed/recall", consumes = "application/json", produces = "application/json")
    public Visit visitReCallForConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId, @Body Visit visit) {


        return visitService.visitReCallForConfirm(branchId, servicePointId, visit);


    }

    /**
     * Подтверждение прихода клиента
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}/confirmed/confirm", consumes = "application/json", produces = "application/json")
    public Visit visitCallConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId, @Body Visit visit) {


        return visitService.visitCallConfirm(branchId, servicePointId, visit);


    }

    /**
     * Вызов визита с наибольшим временем ожидания
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     *
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/servicepoints/{servicePointId}/call", consumes = "application/json", produces = "application/json")
    public Optional<Visit> visitCall(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {


        return visitService.visitCall(branchId, servicePointId);


    }
    /**
     * Возвращает список доступных точке обслуживания очередей (в зависимости от профиля залогиненного сотрудника)
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     *
     * @return доступные очереди
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/servicepoints/{servicePointId}/queues", consumes = "application/json", produces = "application/json")
    public Optional<List<Queue>> getQueues(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {


        return visitService.getQueues(branchId, servicePointId);


    }

    /**
     * Удаление визита из очереди
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit визит
     */
    @Tag(name = "Зона обслуживания")
    @Delete(uri = "/branches/{branchId}/visits/servicepoints/{servicePointId}", consumes = "application/json", produces = "application/json")
    public void deleteVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId, @Body Visit visit) {


        visitService.deleteVisit(branchId, servicePointId, visit);


    }

    /**
     * Перевод визита в очередь из точки обслуживания
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param queueId идентификатор очереди     *
     * @return визит после перевода
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/serrvicepoints/{servicePointId}/queue/{queueId}/visit/transferFromServicePoint", consumes = "application/json", produces = "application/json")
    public Visit transferVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId, @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId) {
        Branch branch;

        try {
            branch = branchService.getBranch(branchId);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        if (!branch.getQueues().containsKey(queueId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
        }


        return visitService.visitTransfer(branchId, servicePointId, queueId);


    }
    /**
     * Возвращение визита в очередь
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания     *
     * @return визит после перевода
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/serrvicepoints/{servicePointId}/visit/return", consumes = "application/json", produces = "application/json")
    public Visit returnVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {


        return visitService.returnVisit(branchId, servicePointId);





    }


    /**
     * Перевод визита из очереди в очередь
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания     *
     * @param queueId идентификатор очереди
     * @param visit переводимый визит
     * @return итоговый визит
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/serrvicepoints/{servicePointId}/queue/{queueId}/visit/transferFromQueue", consumes = "application/json", produces = "application/json")
    public Visit visitTransferFromQueue(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId, @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId, @Body Visit visit) {
        Branch branch;

        try {
            branch = branchService.getBranch(branchId);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        if (!branch.getQueues().containsKey(queueId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
        }


        Visit result = visitService.visitTransferFromQueue(branchId, servicePointId, queueId, visit);

        return result;


    }
    /**
     * Завершение визита
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @return визит после перевода
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/serrvicepoints/{servicePointId}/visit/end", consumes = "application/json", produces = "application/json")
    public Visit visitEnd(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId, @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {





        Visit visit = visitService.visitEnd(branchId, servicePointId);
        eventService.send("*", true, Event.builder()
                .body(visit)
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_ENDED")
                .senderService(applicationName)
                .build());
        return visit;


    }

}