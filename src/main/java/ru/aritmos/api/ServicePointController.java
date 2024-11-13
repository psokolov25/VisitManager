package ru.aritmos.api;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.*;
import ru.aritmos.model.Queue;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.model.tiny.TinyServicePoint;
import ru.aritmos.model.tiny.TinyVisit;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

/**
 * @author Pavel Sokolov REST API управления зоной ожидания
 */
@Controller("/servicepoint")
public class ServicePointController {
  @Inject Services services;
  @Inject BranchService branchService;
  @Inject VisitService visitService;
  @Inject EventService eventService;
  @Inject KeyCloackClient keyCloackClient;

  @Value("${micronaut.application.name}")
  String applicationName;

  /**
   * Возвращает все не занятые сотрудниками точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Get("/branches/{branchId}/servicePoints/getFree")
  @ExecuteOn(TaskExecutors.IO)
  public HashMap<String, ServicePoint> getFreeServicePoints(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getStringServicePointHashMap(branchId);
  }

  /**
   * Возвращает все точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @return точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Tag(name = "Данные о пулах")
  @Get("/branches/{branchId}/servicePoints")
  @ExecuteOn(TaskExecutors.IO)
  public List<TinyServicePoint> getServicePoints(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getServicePointHashMap(branchId).values().stream()
        .map(m -> new TinyServicePoint(m.getId(), m.getName(), m.getUser() == null))
        .toList();
  }

  /**
   * Возвращает все точки обслуживания (с данными пулов)
   *
   * @param branchId идентификатор отделения
   * @return свободные обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Tag(name = "Данные о пулах")
  @Get("/branches/{branchId}/servicePoints/detailed")
  @ExecuteOn(TaskExecutors.IO)
  public List<ServicePoint> getDetailedServicePoints(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getServicePointHashMap(branchId).values().stream().toList();
  }

  /**
   * Возвращает точку обслуживания по логину сотрудника
   *
   * @param branchId идентификатор отделения
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Get("/branches/{branchId}/servicePoints/user/{userName}")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<ServicePoint> getServicePointsByUserName(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String userName) {
    return visitService.getServicePointHashMap(branchId).values().stream()
        .filter(f -> f.getUser() != null && f.getUser().getName().equals(userName))
        .findFirst();
  }

  /**
   * Возвращает список сотрудников отделения
   *
   * @param branchId идентификатор отделения
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Данные о сртрудника")
  @Tag(name = "Полный список")
  @Tag(name = "Данные о пулах")
  @Get("/branches/{branchId}/users")
  @ExecuteOn(TaskExecutors.IO)
  public HashMap<String, User> getUsersOfBranch(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return branchService.getUsers(branchId);
  }

  /**
   * Возвращает точку обслуживания по логину сотрудника
   *
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Get("/servicePoints/user/{userName}")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<ServicePoint> getServicePointsByUserName(@PathVariable String userName) {

    return branchService.getDetailedBranches().values().stream()
        .flatMap(
            fm ->
                fm.getServicePoints().values().stream()
                    .filter(f -> f.getUser() != null && f.getUser().getName().equals(userName)))
        .findFirst();
  }

  /**
   * Возвращает сотрудника по логину
   *
   * @param branchId идентификатор отделения
   * @return пользователь занимающий рабочее место
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Get("/branches/{branchId}/users/user/{userName}")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<User> getUserByUserName(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String userName) {
    Optional<ServicePoint> servicePoint =
        visitService.getServicePointHashMap(branchId).values().stream()
            .filter(f -> f.getUser() != null && f.getUser().getName().equals(userName))
            .findFirst();
    return servicePoint.map(ServicePoint::getUser);
  }

  /**
   * Возвращает все рабочие профили
   *
   * @param branchId идентификатор отделения
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о рабочих профилях")
  @Tag(name = "Полный список")
  @Get("/branches/{branchId}/workProfiles")
  @ExecuteOn(TaskExecutors.IO)
  public List<TinyClass> getWorkProfiles(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getWorkProfiles(branchId);
  }

  /**
   * Открытие рабочей станции сотрудником Если рабочая станция уже открыта - выдается 409 ошибка
   * (конфликт)
   *
   * @param branchId идентификатор отделения
   * @param userName имя пользователя
   * @param servicePointId идентификатор точки обслуживания
   * @param workProfileId идентификатор рабочего профиля
   * @return сотрудник
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Работа сотрудников")
  @Tag(name = "Полный список")
  @Post(
      "/branches/{branchId}/servicePoints/{servicePointId}/workProfiles/{workProfileId}/users/{userName}/open")
  @ExecuteOn(TaskExecutors.IO)
  public User openServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String userName,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "d5a84e60-e605-4527-b065-f4bd7a385790") String workProfileId) {

    return branchService.openServicePoint(branchId, userName, servicePointId, workProfileId);
  }

  /**
   * Закрытие рабочей станции сотрудником Если рабочая станция уже закрыта выдается 409 ошибка
   * (конфликт)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Работа сотрудников")
  @Tag(name = "Полный список")
  @Post("/branches/{branchId}/servicePoints/{servicePointId}/close")
  @ExecuteOn(TaskExecutors.IO)
  public void closeUser(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    branchService.closeServicePoint(branchId, servicePointId, visitService, false);
  }

  /**
   * Закрытие рабочей станции сотрудником и выход из системы Если рабочая станция уже закрыта
   * выдается 409 ошибка (конфликт)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Работа сотрудников")
  @Tag(name = "Полный список")
  @Post("/branches/{branchId}/servicePoints/{servicePointId}/logout")
  @ExecuteOn(TaskExecutors.IO)
  public void logoutUser(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    branchService.closeServicePoint(branchId, servicePointId, visitService, true);
  }

  /**
   * Получение списка визитов в указанной очереди указанного отделения с ограничением выдачи
   * элементов Максимальное количество визитов указывается в параметре limit, если количество
   * визитов меньше - выводятся все визиты. Визиты сортируются по времени ожидания, от большего к
   * меньшему
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @param limit количество последних возвращаемых талонов
   * @return список визитов
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о визитах")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/queues/{queueId}/visits/limit/{limit}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public List<TinyVisit> getVisits(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
      @PathVariable Long limit) {

    List<TinyVisit> result = new ArrayList<>();

    visitService
        .getVisits(branchId, queueId, limit)
        .forEach(
            f -> {
              TinyVisit visit =
                  TinyVisit.builder()
                      .id(f.getId())
                      .ticketId(f.getTicket())
                      .currentService(f.getCurrentService())
                      .createDate(f.getCreateDateTime())
                      .transferDate(f.getTransferDateTime())
                      .build();
              result.add(visit);
            });
    return result;
  }

  /**
   * Получение списка визитов в указанной очереди указанного отделения. Визиты сортируются по
   * времени ожидания, от большего к меньшему.
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @return список визитов
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о визитах")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/queues/{queueId}/visits/",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public List<Visit> getVisits(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId) {

    return visitService.getVisits(branchId, queueId);
  }

  /**
   * Возвращает полный список визитов в отделении учитываются визиты расположенные в очередях, пулах
   * рабочих станций и пулах сотрудников, а так же визиты обслуживаемые в данный момент
   *
   * @param branchId идентификатор отделения
   * @return список визитов
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о визитах")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/visits/all",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public HashMap<String, Visit> getAllVisits(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {

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
  @Tag(name = "Данные о визитах")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/visits/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit getVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String visitId) {

    return visitService.getVisit(branchId, visitId);
  }

  /**
   * Возвращает список визитов в отделении с фильтрацией по статусу выводятся визиты, чей статус
   * входит в передаваемым в теле запроса списком статусов.
   *
   * @param branchId идентификатор отделения
   * @param statuses массив статусов визита
   * @return список визитов
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о визитах")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/statuses",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public HashMap<String, Visit> getVisitsByStatuses(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body List<String> statuses) {

    return visitService.getVisitsByStatuses(branchId, statuses);
  }

  /**
   * Получает данные о визите
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @param visitId идентификатор визита
   * @return данные о визите
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о визитах")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/queues/{queueId}/visits/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit getVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "55da9b66-c928-4d47-9811-dbbab20d3780") String queueId,
      @PathVariable String visitId) {

    return visitService.getVisits(branchId, queueId).stream()
        .filter(f -> f.getId().equals(visitId))
        .findFirst()
        .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Visit not found!"));
  }

  /**
   * Вызов визита по идентификатору
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visitId идентификатор визита
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов определенного визита (cherry-peak)")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/visits/{visitId}/call",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> callVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable String visitId) {

    return visitService.visitCall(branchId, servicePointId, visitId);
  }

  /**
   * Вызов наиболее ожидающего визита с ожиданием подтверждения
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов c наибольшим временем ожидания")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/call",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirmMaxWaitingTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    return visitService.visitCallForConfirm(branchId, servicePointId);
  }

  /**
   * Вызов наиболее ожидающего визита с ожиданием подтверждения из списка очередей, чьи
   * идентификаторы перечислены в теле запроса
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueIds идентификаторы очередей
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов из перечня очередей")
  @Tag(name = "Вызов c наибольшим временем ожидания")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/callfromQueues",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirmMaxWaitingTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body List<String> queueIds) {

    return visitService.visitCallForConfirm(branchId, servicePointId, queueIds);
  }

  /**
   * Вызов визита с наибольшим временем ожидания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Вызов c наибольшим временем ожидания")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/call",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallWithMaximalWaitingTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    return visitService.visitCallWithMaximalWaitingTime(branchId, servicePointId);
  }

  /**
   * Вызов визита с наибольшим временем ожидания из списка очередей, чьи идентификаторы в теле
   * запроса
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueIds идентификаторы очередей
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Вызов c наибольшим временем ожидания")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Вызов из перечня очередей")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/callfromQueues",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCall(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body List<String> queueIds) {

    return visitService.visitCallWithMaximalWaitingTime(branchId, servicePointId, queueIds);
  }

  /**
   * Вызов визита с наибольшим временем жизни визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Вызов c максимальным временем жизни визита")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/call/maxLifeTime",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallMaxLifeTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    return visitService.visitCallWithMaxLifeTime(branchId, servicePointId);
  }

  /**
   * Вызов визита с наибольшим временем жизни визита из указанных очередей, чьи идентификаторы
   * указаны в теле запроса
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueIds идентификаторы очередей из которых производится вызов
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Вызов c максимальным временем жизни визита")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Вызов из перечня очередей")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/callfromQueues/maxLifeTime",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallMaxLifeTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body List<String> queueIds) {

    return visitService.visitCallWithMaxLifeTime(branchId, servicePointId, queueIds);
  }

  /**
   * Вызов с максимальным временем жизни визита с ожиданием подтверждения
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов c максимальным временем жизни визита")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/call/maxLifeTime",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirmMaxLifeTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    return visitService.visitCallForConfirmWithMaxLifeTime(branchId, servicePointId);
  }

  /**
   * Вызов с максимальным временем жизни визита с ожиданием подтверждения из списка очередей, чьи
   * идентификаторы в теле запроса
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueIds идентификаторы очередей
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов c максимальным временем жизни визита")
  @Tag(name = "Вызов из перечня очередей")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Post(
      uri =
          "/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/callfromQueues/maxLifeTime",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirmMaxLifeTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body List<String> queueIds) {

    return visitService.visitCallForConfirmWithMaxLifeTime(branchId, servicePointId, queueIds);
  }

  /**
   * Вызов визита с ожиданием подтверждения
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов определенного визита (cherry-peak)")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/confirmed/call/visit",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirm(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body Visit visit) {

    return visitService.visitCallForConfirm(branchId, servicePointId, visit);
  }

  /**
   * Вызов визита с ожиданием подтверждения по идентификатору
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visitId идентификатор визита
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов определенного визита (cherry-peak)")
  @Tag(name = "Ожидание подтверждения прихода")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/call/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirmByVisitId(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable String visitId) {

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitCallForConfirm(branchId, servicePointId, visit);
  }

  /**
   * Отмена вызова из-за того, что клиент не пришел
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Результат вызова")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/noshow",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitNoShow(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body Visit visit) {

    return visitService.visitNoShow(branchId, servicePointId, visit);
  }

  /**
   * Отмена вызова из-за того, что клиент не пришел по идентификатору
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visitId идентификатор визита
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Результат вызова")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/noshow/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallNoShow(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable String visitId) {

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitNoShow(branchId, servicePointId, visit);
  }

  /**
   * Повторный вызов визита c ожиданием подтверждения
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов определенного визита (cherry-peak)")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/recall",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitReCallForConfirm(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body Visit visit) {

    return visitService.visitReCallForConfirm(branchId, servicePointId, visit);
  }

  /**
   * Повторный вызов визита c ожиданием подтверждения по идентификатору
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visitId идентификатор визита
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов определенного визита (cherry-peak)")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/recall/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitReCallForConfirm(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable String visitId) {

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitReCallForConfirm(branchId, servicePointId, visit);
  }

  /**
   * Подтверждение прихода клиента
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Результат вызова")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/confirm",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitConfirm(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body Visit visit) {

    return visitService.visitConfirm(branchId, servicePointId, visit);
  }

  /**
   * Подтверждение прихода клиента
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visitId идентификатор визита
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Результат вызова")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Post(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/confirm/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitConfirm(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable String visitId) {

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitConfirm(branchId, servicePointId, visit);
  }

  /**
   * Отмена режима автовызова для точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return точка обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Настройка точки обслуживания")
  @Tag(name = "Автоматический вызов")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Put("/branches/{branchId}/servicePoins/{servicePointId}/cancelAutoCall")
  public Optional<ServicePoint> cancelAutoCallModeOfServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {
    return visitService.cancelAutoCallModeOfServicePoint(branchId, servicePointId);
  }

  /**
   * Запуск режима автовызова для точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return точка обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Автоматический вызов")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Put("/branches/{branchId}/servicePoins/{servicePointId}/startAutoCall")
  public Optional<ServicePoint> startAutoCallModeOfServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {
    return visitService.startAutoCallModeOfServicePoint(branchId, servicePointId);
  }

  /**
   * Получение возможный предоставленных услуг
   *
   * @param branchId идентификатор отделения
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Данные об услугах (в разработке!)")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/services/{serviceId}/deliveredServices",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Map<String, DeliveredService> getDeliveredService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "c3916e7f-7bea-4490-b9d1-0d4064adbe8b") String serviceId) {

    Branch branch = branchService.getBranch(branchId);
    if (branch.getServices().containsKey(serviceId)) {
      return branch.getPossibleDeliveredServices().entrySet().stream()
          .filter(f -> f.getValue().getServviceIds().contains(serviceId))
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue));
    } else {
      throw new BusinessException(
          String.format("Service %s not found!", serviceId), eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Получение услуг соответствующего рабочего профиля
   *
   * @param branchId идентификатор отделения
   * @param workProfileId идентификатор рабочего профиля
   * @return список услуг
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные об услугах")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/workProfile/{workProfileId}/services",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public List<Service> getServicesByWorkProfileId(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "d5a84e60-e605-4527-b065-f4bd7a385790") String workProfileId) {

    return branchService.getServicesByWorkProfileId(branchId, workProfileId);
  }

  /**
   * Получение услуг соответствующей очереди
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @return список услуг
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные об услугах")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/queue/{queueId}/services",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public List<Service> getServicesByQueueId(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "55da9b66-c928-4d47-9811-dbbab20d3780") String queueId) {

    return branchService.getServicesByQueueId(branchId, queueId);
  }

  /**
   * Получение возможных итогов для услуги
   *
   * @param branchId идентификатор отделения
   * @param serviceId идентификатор услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Данные об итогах (в разработке!)")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/services/{serviceId}/outcomes",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public HashMap<String, Outcome> getOutcomes(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "c3916e7f-7bea-4490-b9d1-0d4064adbe8b") String serviceId) {

    Branch branch = branchService.getBranch(branchId);
    if (branch.getServices().containsKey(serviceId)) {
      return branch.getServices().get(serviceId).getPossibleOutcomes();
    } else {
      throw new BusinessException(
          String.format("Service %s not found!", serviceId), eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление предоставленной услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор предоставленной услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Обслуживание (в разработке!)")
  @Tag(name = "Предоставленные услуги (в разработке!)")
  @Tag(name = "Полный список")
  @Post(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredservice/{deliveredServiceId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit addDeliveredService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "35d73fdd-1597-4d94-a087-fd8a99c9d1ed")
          String deliveredServiceId) {

    return visitService.addDeliveredService(branchId, servicePointId, deliveredServiceId);
  }

  /**
   * Удаление текстовой пометки в визит
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param markId идентификатор метки
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Пометки")
  @Tag(name = "Полный список")
  @Delete(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/mark/{markId}",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit deleteMark(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "04992364-9e96-4ec9-8a05-923766aa57e7") String markId) {

    return visitService.deleteMark(branchId, servicePointId, markId);
  }

  /**
   * Возвращение списка возможных меток отделения
   *
   * @param branchId идентификатор отделения
   * @return список меток
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Пометки")
  @Tag(name = "Полный список")
  @Get(uri = "/branches/{branchId}/marks/", produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public HashMap<String, Mark> deleteMark(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {

    Branch branch = branchService.getBranch(branchId);
    return branch.getMarks();
  }

  /**
   * Добавление пометки в формате объекта
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param markId идентификатор метки
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Пометки")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/mark/{markId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit addMark(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "04992364-9e96-4ec9-8a05-923766aa57e7") String markId) {

    return visitService.addMark(branchId, servicePointId, markId);
  }

  /**
   * Добавление итога текущей услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param outcomeId идентификатор итога оказания услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Обслуживание (в разработке!)")
  @Tag(name = "Итоги услуги (в разработке!)")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/outcome/{outcomeId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit addOutcomeService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "462bac1a-568a-4f1f-9548-1c7b61792b4b") String outcomeId) {

    return visitService.addOutcomeService(branchId, servicePointId, outcomeId);
  }

  /**
   * Добавление услуги в визит
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param serviceId идентификатор услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Обслуживание (в разработке!)")
  @Tag(name = "Изменение визита (в разработке!)")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/services/{serviceId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit addService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c3916e7f-7bea-4490-b9d1-0d4064adbe8b") String serviceId) {

    return visitService.addService(branchId, servicePointId, serviceId);
  }

  /**
   * Добавление итога предоставленной услуги текущей услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор предоставленной услуги
   * @param outcomeId идентификатор итога оказания услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Обслуживание (в разработке!)")
  @Tag(name = "Итоги услуги (в разработке!)")
  @Tag(name = "Полный список")
  @Post(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredService/{deliveredServiceId}/outcome/{outcomeId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit addOutcomeDeliveredService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable() String deliveredServiceId,
      @PathVariable(defaultValue = "462bac1a-568a-4f1f-9548-1c7b61792b4b") String outcomeId) {

    return visitService.addOutcomeDeliveredService(
        branchId, servicePointId, deliveredServiceId, outcomeId);
  }

  /**
   * Удаление итога предоставленной услуги текущей услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор предоставленной услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Обслуживание (в разработке!)")
  @Tag(name = "Итоги услуги (в разработке!)")
  @Tag(name = "Полный список")
  @Delete(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredServices/{deliveredServiceId}/outcome",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit deleteOutcomeDeliveredService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable() String deliveredServiceId) {

    return visitService.deleteOutcomeDeliveredService(branchId, servicePointId, deliveredServiceId);
  }

  /**
   * Удаление итога предоставленной услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param serviceId идентификатор итога оказания услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Обслуживание (в разработке!)")
  @Tag(name = "Итоги услуги (в разработке!)")
  @Tag(name = "Полный список")
  @Delete(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/service/{serviceId}/outcome",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitDeleteOutcomeService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c3916e7f-7bea-4490-b9d1-0d4064adbe8b") String serviceId) {

    return visitService.deleteOutcomeService(branchId, servicePointId, serviceId);
  }

  /**
   * Удаление предоставленной услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор предоставленной услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания (в разработке!)")
  @Tag(name = "Обслуживание (в разработке!)")
  @Tag(name = "Предоставленные услуги (в разработке!)")
  @Tag(name = "Полный список")
  @Delete(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredServices/{deliveredServiceId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit deleteDeliveredService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "35d73fdd-1597-4d94-a087-fd8a99c9d1ed")
          String deliveredServiceId) {

    return visitService.deleteDeliveredService(branchId, servicePointId, deliveredServiceId);
  }

  /**
   * Возвращает список доступных точек обслуживания очередей (в зависимости от сотрудников,
   * расположенных в точке обслуживания и имеющих соответсвующий рабочий профиль) (то есть имеющего
   * возможность вызывать из выводимой очереди)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return доступные очереди
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Get(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/queues",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<List<Queue>> getQueues(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    return visitService.getQueues(branchId, servicePointId);
  }

  /**
   * Удаление визита из очереди
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Полный список")
  @Delete(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public void deleteVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body Visit visit) {

    visitService.deleteVisit(visit);
  }

  /**
   * Удаление визита из очереди по идентификатору визита
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор визита
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Полный список")
  @Delete(
      uri = "/branches/{branchId}/visits/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public void deleteVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String visitId) {
    if (!visitService.getAllVisits(branchId).containsKey(visitId)) {
      throw new BusinessException(
          String.format("Visit with id %s not found!", visitId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
    Visit visit = visitService.getAllVisits(branchId).get(visitId);

    visitService.deleteVisit(visit);
  }

  /**
   * Перевод визита в очередь из точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueId идентификатор очереди
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromServicePoint",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransfer(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
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
   * Возвращение визита в пул точки обслуживания из точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания пула
   * @param returnTimeDelay задержка возвращения в секундах
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Возвращение визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/put_back",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitBackToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2")
          String poolServicePointId,
      @QueryValue(defaultValue = "0") Long returnTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Service point not found!");
    }

    return visitService.visitBackToServicePointPool(
        branchId, servicePointId, poolServicePointId, returnTimeDelay);
  }

  /**
   * Перевод визита в пул точки обслуживания из точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания пула *
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/transfer",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2")
          String poolServicePointId) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Service point not found!");
    }

    return visitService.visitTransferToServicePointPool(
        branchId, servicePointId, poolServicePointId);
  }

  /**
   * Перевод визита в пул точки обслуживания из точки обслуживания внешней службой
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания пула *
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита внешней службой (Ресепшен, MI и т д)")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/service/transfer",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2")
          String poolServicePointId,
      HashMap<String, String> serviceInfo) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Service point not found!");
    }

    return visitService.visitTransferToServicePointPool(
        branchId, servicePointId, poolServicePointId, serviceInfo);
  }

  /**
   * Возвращение визита в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param returnTimeDelay задержка визита в секундах
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Возвращение визита")
  @Tag(name = "Изменение визита")
  @Tag(name = "Полный список")
  @Tag(name = "Завершение вызова")
  @Put(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/put_back",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit returnVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @QueryValue(defaultValue = "0") Long returnTimeDelay) {

    return visitService.visitBackToQueue(branchId, servicePointId, returnTimeDelay);
  }

  /**
   * Перевод визита из очереди в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param queueId идентификатор очереди
   * @param visitId идентификатор визита
   * @param index позиция визита в списке
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromQueue/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueue(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
      @PathVariable String visitId,
      @QueryValue(defaultValue = "0") Integer index) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
    }

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransfer(branchId, servicePointId, queueId, visit, index);
  }

  /**
   * Перевод визита из очереди в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param queueId идентификатор очереди
   * @param visitId идентификатор визита
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromQueueToStartOrToEnd/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueue(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
      @PathVariable String visitId,
      @QueryValue(defaultValue = "true") Boolean isAppend) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
    }

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransfer(branchId, servicePointId, queueId, visit, isAppend);
  }

  /**
   * Перевод визита из очереди в очередь с помощью внешней службы (Ресепшен ,MI и т д)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param queueId идентификатор очереди
   * @param visitId идентификатор визита
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param serviceInfo данные о внешней службе
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита внешней службой (Ресепшен, MI и т д)")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromQueueToStartOrToEnd/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueue(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
      @PathVariable String visitId,
      @Body HashMap<String, String> serviceInfo,
      @QueryValue(defaultValue = "true") Boolean isAppend) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
    }

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransfer(
        branchId, servicePointId, queueId, visit, isAppend, serviceInfo);
  }

  /**
   * Перевод визита из очереди в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param queueId идентификатор очереди
   * @param visit переводимый визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromQueue",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueue(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
      @Body Visit visit,
      @QueryValue(defaultValue = "true") Boolean isAppend) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
    }

    return visitService.visitTransfer(branchId, servicePointId, queueId, visit, isAppend);
  }

  /**
   * Перевод визита из очереди в очередь в указанную позцицию
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param queueId идентификатор очереди
   * @param visit переводимый визит
   * @param index позиция визита в списке
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromQueue/position/{index}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueue(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
      @Body Visit visit,
      @PathVariable(defaultValue = "0") Integer index) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Queue not found!");
    }

    return visitService.visitTransfer(branchId, servicePointId, queueId, visit, index);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания в указанную позицию
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания пула
   * @param visit переводимый визит
   * @param index позиция визита в списке
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/transferFromQueue/position/{index}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueueToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e")
          String poolServicePointId,
      @Body Visit visit,
      @PathVariable(defaultValue = "0") Integer index) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Service point not found!");
    }

    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, poolServicePointId, visit, index);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания пула
   * @param visit переводимый визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/transferFromQueue",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueueToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e")
          String poolServicePointId,
      @Body Visit visit,
      @QueryValue(defaultValue = "true") Boolean isAppend) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Service point not found!");
    }

    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, poolServicePointId, visit, isAppend);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания пула
   * @param visitId переводимый визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visits/{visitId}/transferFromQueue",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueueToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e")
          String poolServicePointId,
      @PathVariable String visitId,
      @QueryValue(defaultValue = "true") Boolean isAppend) {

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, poolServicePointId, visit, isAppend);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания в указанную позицию
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания пула
   * @param visitId переводимый визит
   * @param index позиция визита в списке
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visits/{visitId}/transferFromQueueWithIndex",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueueToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e")
          String poolServicePointId,
      @PathVariable String visitId,
      @QueryValue(defaultValue = "0") Integer index) {

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, poolServicePointId, visit, index);
  }

  /**
   * Завершение обслуживания (нормальное)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Tag(name = "Завершение вызова")
  @Put(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitEnd(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    return visitService.visitEnd(branchId, servicePointId);
  }

  /**
   * Перевод визита из очереди в юзерпул
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visit переводимый визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/users/{userId}")
  public Visit visitTransferFromQueueToUserPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
      @Body Visit visit,
      @QueryValue(defaultValue = "true") Boolean isAppend) {
    return visitService.visitTransferFromQueueToUserPool(branchId, userId, visit, isAppend);
  }

  /**
   * Перевод визита из очереди в юзерпул
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visit переводимый визит
   * @param index позиция визита в списке
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/users/{userId}/position/{index}")
  public Visit visitTransferFromQueueToUserPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
      @Body Visit visit,
      @PathVariable(defaultValue = "0") Integer index) {
    return visitService.visitTransferFromQueueToUserPool(branchId, userId, visit, index);
  }

  /**
   * Перевод визита из очереди в юзерпул
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visitId идентификатор переводимого визита
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/users/{userId}/visits/{visitId}")
  public Visit visitTransferFromQueueToUserPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
      @PathVariable String visitId,
      @QueryValue(defaultValue = "true") Boolean isAppend) {
    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToUserPool(branchId, userId, visit, isAppend);
  }

  /**
   * Перевод визита из очереди в юзерпул
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visitId идентификатор переводимого визита
   * @param index флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/users/{userId}/visits/{visitId}/position/{index}")
  public Visit visitTransferFromQueueToUserPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
      @PathVariable String visitId,
      @QueryValue(defaultValue = "0") Integer index) {
    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToUserPool(branchId, userId, visit, index);
  }

  /**
   * Возвращение визита из точки обслуживания в пул сотрудника
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param userId идентификатор сотрудника
   * @param returnTimeDelay задержка возвращения в секундах
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Изменение визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Возвращение визита")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/users/{userId}/put_back")
  public Visit visitBackToUserPool(
      @PathVariable String branchId,
      @PathVariable String servicePointId,
      @PathVariable String userId,
      @QueryValue(defaultValue = "0") Long returnTimeDelay) {
    return visitService.visitBackToUserPool(branchId, servicePointId, userId, returnTimeDelay);
  }

  /**
   * Перевод визита из точки обслуживания в пул сотрудника
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param userId идентификатор сотрудника *
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Изменение визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/users/{userId}/transfer")
  public Visit visitTransferToUserPool(
      @PathVariable String branchId,
      @PathVariable String servicePointId,
      @PathVariable String userId) {
    return visitService.visitTransferToUserPool(branchId, servicePointId, userId);
  }

  /**
   * Перевод визита из точки обслуживания в пул сотрудника внешней службы
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param userId идентификатор сотрудника *
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Изменение визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Перевод визита внешней службой (Ресепшен, MI и т д)")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/users/{userId}/service/transfer")
  public Visit visitTransferToUserPool(
      @PathVariable String branchId,
      @PathVariable String servicePointId,
      @PathVariable String userId,
      @Body HashMap<String, String> serviceInfo) {
    return visitService.visitTransferToUserPool(branchId, servicePointId, userId, serviceInfo);
  }

  /**
   * Отложить визит
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Изменение визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Возвращение визита")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/postpone")
  public Visit visitPostPone(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {
    return visitService.visitPostPone(branchId, servicePointId);
  }

  /**
   * Возвращение визита из сервиса поинта
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param returnTimeDelay задержка возвращения в секундах
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Изменение визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Возвращение визита")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/visit/put_back")
  public Visit visitPutBack(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @QueryValue(defaultValue = "0") Long returnTimeDelay) {
    return visitService.visitPutBack(branchId, servicePointId, returnTimeDelay);
  }
}
