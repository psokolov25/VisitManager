package ru.aritmos.api;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.tiny.TinyVisit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

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
     * Возвращает все не занятые точки обслуживания
     *
     * @param branchId идентификатор отделения
     * @return свободные точки обслуживания
     */
    @Tag(name = "Зона обслуживания")
    @Get("/branches/{branchId}/servicePoints/getFree")
    @ExecuteOn(TaskExecutors.IO)
    public HashMap<String, ServicePoint> getFreeServicePoints(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
        return visitService.getStringServicePointHashMap(branchId);
    }

    /**
     * Открытие рабочей станции сотрудником
     *
     * @param user сотрудник
     * @return сотрудник
     */
    @Tag(name = "Зона обслуживания")
    @Post("/branches/user/login")
    @ExecuteOn(TaskExecutors.IO)
    public User loginUser(@Body User user) {


        branchService.loginUser(user);
        return user;

    }

    /**
     * Закрытие рабочей станции
     *
     * @param user сотрудник
     * @return сотрудник
     */
    @Tag(name = "Зона обслуживания")
    @Post("/branches/user/logout")
    @ExecuteOn(TaskExecutors.IO)
    public User logoutUser(@Body User user) {
        if (user.getServicePoinrtId() != null) {

            branchService.logoutUser(user);
            return user;
        } else {
            throw new BusinessException(String.format("User %s already logged out", user.getName()), eventService, HttpStatus.CONFLICT);
        }
    }


    /**
     * Возвращает список визитов из очереди
     *
     * @param branchId идентификатор отделения
     * @param queueId  идентификатор очереди
     * @param limit    количество последних возвращаемых талонов
     * @return список визитов
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/queues/{queueId}/visits/limit/{limit}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public List<TinyVisit> getVisits(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                     @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
                                     @PathVariable Long limit) {

        List<TinyVisit> result = new ArrayList<>();

        visitService.getVisits(branchId, queueId, limit).forEach(f -> {
            TinyVisit visit =
                    TinyVisit.builder()
                            .id(f.getId())
                            .ticketId(f.getTicket())
                            .currentService(f.getCurrentService())
                            .createDate(f.getCreateDateTime())
                            .transferDate(f.getTransferDateTime()).build();
            result.add(visit);
        });
        return result;

    }

    /**
     * Возвращает полный список визитов в отделении
     *
     * @param branchId идентификатор отделения
     * @return список визитов
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/visits/all", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public HashMap<String, Visit> getAllVisits(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {


        return visitService.getAllVisits(branchId);

    }
    /**
     * Возвращает визит по его идентификатору
     *
     * @param branchId идентификатор отделения
     * @param visitId идентификатор визита
     * @return визит
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/visits/{visitId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit getVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,@PathVariable String visitId) {


        return visitService.getVisit(branchId,visitId);

    }

    /**
     * Возвращает  список визитов в отделении с фильтрацией по статусу
     *
     * @param branchId идентификатор отделения
     * @param statuses массив статусов визита
     * @return список визитов
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/statuses", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public HashMap<String, Visit> getVisitsByStatuses(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                                      @Body List<String> statuses) {


        return visitService.getVisitsByStatuses(branchId, statuses);

    }

    /**
     * Получает данные о визите
     *
     * @param branchId идентификатор отделения
     * @param queueId  идентификатор очереди
     * @param visitId  идентификатор визита
     * @return данные о визите
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/queues/{queueId}/visits/{visitId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit getVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                          @PathVariable(defaultValue = "55da9b66-c928-4d47-9811-dbbab20d3780") String queueId,
                          @PathVariable String visitId) {


        return visitService.getVisits(branchId, queueId).stream()
                .filter(f -> f.getId()
                        .equals(visitId)).findFirst().orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Visit not found!"));

    }


    /**
     * Вызов визита
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visitId        идентификатор визита
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/visits/{visitId}/call", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit callVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                           @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                           @PathVariable String visitId) {


        return visitService.visitCall(branchId, servicePointId, visitId);


    }

    /**
     * Вызов наиболее ожидающего визита с ожиданием подтверждения
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/call", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Optional<Visit> visitCallForConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                               @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {


        return visitService.visitCallForConfirm(branchId, servicePointId);


    }

    /**
     * Вызов визита с ожиданием подтверждения
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/call", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitCallForConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                     @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                     @Body Visit visit) {


        return visitService.visitCallForConfirm(branchId, servicePointId, visit);


    }

    /**
     * Вызов визита с ожиданием подтверждения
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visitId  идентификатор визита
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/call/{visitId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitCallForConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                     @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                     @PathVariable String visitId) {

            Visit visit = visitService.getVisit(branchId, visitId);
            return visitService.visitCallForConfirm(branchId, servicePointId, visit);


    }

    /**
     * Отмена вызова из-за того, что клиент не пришел
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/noshow", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitCallNoShow(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                 @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                 @Body Visit visit) {


        return visitService.visitNoShow(branchId, servicePointId, visit);


    }
    /**
     * Отмена вызова из-за того, что клиент не пришел
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visitId идентификатор визита
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/noshow/{visitId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitCallNoShow(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                 @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                 @PathVariable String visitId) {

        Visit visit=visitService.getVisit(branchId,visitId);
        return visitService.visitNoShow(branchId, servicePointId, visit);


    }

    /**
     * Повторный вызов визита c ожиданием подтверждения
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/recall", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitReCallForConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                       @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                       @Body Visit visit) {


        return visitService.visitReCallForConfirm(branchId, servicePointId, visit);


    }
    /**
     * Повторный вызов визита c ожиданием подтверждения
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visitId идентификатор визита
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/recall/{visitId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitReCallForConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                       @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                       @PathVariable String visitId) {

        Visit visit=visitService.getVisit(branchId,visitId);
        return visitService.visitReCallForConfirm(branchId, servicePointId, visit);


    }

    /**
     * Подтверждение прихода клиента
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/confirm", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitCallConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                  @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                  @Body Visit visit) {


        return visitService.visitConfirm(branchId, servicePointId, visit);


    }


    /**
     * Подтверждение прихода клиента
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visitId идентификатор визита
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/confirm/{visitId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitCallConfirm(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                  @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                  @PathVariable String visitId) {

        Visit visit=visitService.getVisit(branchId,visitId);
        return visitService.visitConfirm(branchId, servicePointId, visit);


    }

    /**
     * Добавление предоставленной услуги
     *
     * @param branchId           идентификатор отделения
     * @param servicePointId     идентификатор точки обслуживания
     * @param deliveredServiceId идентификатор предоставленной услуги
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredservice/{deliveredServiceId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitAddDeliveredService(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                          @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                          @PathVariable(defaultValue = "35d73fdd-1597-4d94-a087-fd8a99c9d1ed") String deliveredServiceId) {


        return visitService.addDeliveredService(branchId, servicePointId, deliveredServiceId);


    }

    /**
     * Добавление итога текущей услуги
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param outcomeId      идентификатор итога оказания услуги
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/outcome/{outcomeId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitAddOutcomeService(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                        @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                        @PathVariable(defaultValue = "462bac1a-568a-4f1f-9548-1c7b61792b4b") String outcomeId) {


        return visitService.addOutcomeService(branchId, servicePointId, outcomeId);


    }

    /**
     * Добавление услуги в визит
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param serviceId      идентификатор итога оказания услуги
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/services/{serviceId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit addOutcomeDeliveredService(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                            @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                            @PathVariable(defaultValue = "c3916e7f-7bea-4490-b9d1-0d4064adbe8b") String serviceId) {


        return visitService.addService(branchId, servicePointId, serviceId);


    }

    /**
     * Добавление итога предоставленной услуги текущей услуги
     *
     * @param branchId           идентификатор отделения
     * @param servicePointId     идентификатор точки обслуживания
     * @param deliveredServiceId идентификатор предоставленной услуги
     * @param outcomeId          идентификатор итога оказания услуги
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredService/{deliveredServiceId}/outcome/{outcomeId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit addOutcomeDeliveredService(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                            @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                            @PathVariable() String deliveredServiceId,
                                            @PathVariable(defaultValue = "462bac1a-568a-4f1f-9548-1c7b61792b4b") String outcomeId) {


        return visitService.addOutcomeDeliveredService(branchId, servicePointId, deliveredServiceId, outcomeId);


    }

    /**
     * Удаление итога предоставленной услуги текущей услуги
     *
     * @param branchId           идентификатор отделения
     * @param servicePointId     идентификатор точки обслуживания
     * @param deliveredServiceId идентификатор предоставленной услуги
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Delete(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredServices/{deliveredServiceId}/outcome", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit deleteOutcomeDeliveredService(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                               @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                               @PathVariable() String deliveredServiceId) {


        return visitService.deleteOutcomeDeliveredService(branchId, servicePointId, deliveredServiceId);


    }

    /**
     * Удаление итога предоставленной услуги
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param serviceId      идентификатор итога оказания услуги
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Delete(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/service/{serviceId}/outcome", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitDeleteOutcomeService(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                           @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                           @PathVariable(defaultValue = "c3916e7f-7bea-4490-b9d1-0d4064adbe8b") String serviceId) {


        return visitService.deleteOutcomeService(branchId, servicePointId, serviceId);


    }

    /**
     * Удаление предоставленной услуги
     *
     * @param branchId           идентификатор отделения
     * @param servicePointId     идентификатор точки обслуживания
     * @param deliveredServiceId идентификатор предоставленной услуги
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Delete(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredServices/{deliveredServiceId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitDeleteDeliveredService(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                             @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                             @PathVariable(defaultValue = "35d73fdd-1597-4d94-a087-fd8a99c9d1ed") String deliveredServiceId) {


        return visitService.addDeliveredService(branchId, servicePointId, deliveredServiceId);


    }

    /**
     * Вызов визита с наибольшим временем ожидания
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @return вызванный визит
     */
    @Tag(name = "Зона обслуживания")
    @Post(uri = "/branches/{branchId}/servicePoints/{servicePointId}/call", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Optional<Visit> visitCall(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                     @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {


        return visitService.visitCall(branchId, servicePointId);


    }

    /**
     * Возвращает список доступных точке обслуживания очередей (в зависимости от профиля залогиненного сотрудника)
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @return доступные очереди
     */
    @Tag(name = "Зона обслуживания")
    @Get(uri = "/branches/{branchId}/servicePoints/{servicePointId}/queues", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Optional<List<Queue>> getQueues(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                           @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {


        return visitService.getQueues(branchId, servicePointId);


    }

    /**
     * Удаление визита из очереди
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     */
    @Tag(name = "Зона обслуживания")
    @Delete(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public void deleteVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                            @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                            @Body Visit visit) {


        visitService.deleteVisit(visit);


    }

    /**
     * Удаление визита из очереди по идентификатору визита
     *
     * @param branchId идентификатор отделения
     * @param visitId  идентификатор визита
     */
    @Tag(name = "Зона обслуживания")
    @Delete(uri = "/branches/{branchId}/visits/{visitId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public void deleteVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                            @PathVariable String visitId) {
        if (!visitService.getAllVisits(branchId).containsKey(visitId)) {
            throw new BusinessException(String.format("Visit with id %s not found!", visitId), eventService, HttpStatus.NOT_FOUND);
        }
        Visit visit = visitService.getAllVisits(branchId).get(visitId);

        visitService.deleteVisit(visit);


    }

    /**
     * Перевод визита в очередь из точки обслуживания
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param queueId        идентификатор очереди
     * @return визит после перевода
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromServicePoint", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit transferVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                               @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                               @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId) {
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
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания     *
     * @return визит после перевода
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/return", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit returnVisit(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                             @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                             @QueryValue(defaultValue = "3000") Long returnTimeDelay) {


        return visitService.returnVisit(branchId, servicePointId, returnTimeDelay);


    }


    /**
     * Перевод визита из очереди в очередь
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания     *
     * @param queueId        идентификатор очереди
     * @param visitId        идентификатор визита
     * @return итоговый визит
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromQueue/{visitId}", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitTransferFromQueue(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                        @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                        @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId, @PathVariable String visitId) {
        Branch branch;

        try {
            branch = branchService.getBranch(branchId);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        if (!branch.getQueues().containsKey(queueId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
        }

        Visit visit=visitService.getVisit(branchId,visitId);
        return visitService.visitTransferFromQueue(branchId, servicePointId, queueId, visit);


    }

    /**
     * Перевод визита из очереди в очередь
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания     *
     * @param queueId        идентификатор очереди
     * @param visit          переводимый визит
     * @return итоговый визит
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromQueue", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitTransferFromQueue(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                                        @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
                                        @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId, @Body Visit visit) {
        Branch branch;

        try {
            branch = branchService.getBranch(branchId);
        } catch (Exception ex) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");

        }
        if (!branch.getQueues().containsKey(queueId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
        }


        return visitService.visitTransferFromQueue(branchId, servicePointId, queueId, visit);


    }

    /**
     * Завершение обслуживания (нормальное)
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @return визит после перевода
     */
    @Tag(name = "Зона обслуживания")
    @Put(uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end", consumes = "application/json", produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public Visit visitEnd(@PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
                          @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {


        return visitService.visitEnd(branchId, servicePointId);


    }

}