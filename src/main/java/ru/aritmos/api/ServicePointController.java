package ru.aritmos.api;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.keycloak.representations.idm.GroupRepresentation;
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
 * REST API для работы с точками обслуживания, очередями и пулами сотрудников.
 *
 * <p>Контроллер поддерживает выдачу талонов, перевод визитов между очередями и управление
 * занятостью рабочих мест в отделении.
 */
@SuppressWarnings({"unused", "RedundantSuppression", "RedundantDefaultParameter"})
@SerdeImport(GroupRepresentation.class)
@Controller("/servicepoint")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
    @ApiResponse(responseCode = "401", description = "Не авторизован"),
    @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
    @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
    @ApiResponse(responseCode = "405", description = "Метод не поддерживается"),
    @ApiResponse(responseCode = "415", description = "Неподдерживаемый тип данных"),
    @ApiResponse(responseCode = "500", description = "Ошибка сервера")
})
public class ServicePointController {
  /** Сервис для выборки услуг. */
  @Inject Services services;
  /** Сервис отделений. */
  @Inject BranchService branchService;
  /** Сервис визитов. */
  @Inject VisitService visitService;
  /** Сервис событий. */
  @Inject EventService eventService;
  /** Клиент Keycloak. */
  @Inject KeyCloackClient keyCloackClient;

  /** Имя этого приложения (Micronaut). */
  @Value("${micronaut.application.name}")
  String applicationName;

  /**
   * Возвращает список незанятых точек обслуживания отделения.
   *
   * @param branchId идентификатор отделения
   * @return карта свободных точек обслуживания
   */
  @Operation(
      summary = "Свободные точки обслуживания отделения",
      description = "Возвращает карту незанятых точек обслуживания для указанного отделения",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Список свободных точек обслуживания",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ServicePoint.class))),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Возвращает список принтеров, зарегистрированных в отделении.
   *
   * @param branchId идентификатор отделения
   * @return список принтеров отделения
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о принтерах")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Принтеры отделения",
      description = "Возвращает список принтеров отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список принтеров"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get("/branches/{branchId}/printers")
  @ExecuteOn(TaskExecutors.IO)
  public List<Entity> getPrinters(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getPrinters(branchId);
  }

  /**
   * Возвращает краткий список очередей отделения.
   *
   * @param branchId идентификатор отделения
   * @return список очередей с идентификаторами и названиями
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные об очередях")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Очереди отделения",
      description = "Возвращает список очередей отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список очередей"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get("/branches/{branchId}/queues")
  @ExecuteOn(TaskExecutors.IO)
  public List<Entity> getQueues(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getQueus(branchId);
  }

  /**
   * Возвращает подробную информацию об очередях отделения.
   *
   * @param branchId идентификатор отделения
   * @return список очередей с расширенными данными
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные об очередях")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Очереди отделения (полные данные)",
      description = "Возвращает подробную информацию об очередях отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список очередей"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get("/branches/{branchId}/queues/full")
  @ExecuteOn(TaskExecutors.IO)
  public List<Queue> getFullQueues(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getFullQueus(branchId);
  }

  /**
   * Возвращает список точек обслуживания отделения с признаком занятости.
   *
   * @param branchId идентификатор отделения
   * @return список точек обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Tag(name = "Данные о пулах")
  @Operation(
      summary = "Все точки обслуживания",
      description = "Возвращает список точек обслуживания отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список точек обслуживания"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Подробные точки обслуживания",
      description = "Возвращает подробную информацию о точках обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список точек обслуживания"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * @param userName логин пользователя
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Get("/branches/{branchId}/servicePoints/user/{userName}")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Точка обслуживания по логину",
      description = "Возвращает точку обслуживания, где работает указанный сотрудник",
      responses = {
        @ApiResponse(responseCode = "200", description = "Точка обслуживания"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Optional<ServicePoint> getServicePointsByUserName(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String userName) {
    Optional<ServicePoint> result =
        visitService.getServicePointHashMap(branchId).values().stream()
            .filter(f -> f.getUser() != null && f.getUser().getName().equals(userName))
            .findFirst();
    if (result.isEmpty()) {
      Optional<User> user =
          visitService.getUsers(branchId).stream()
              .filter(f -> f.getName().equals(userName))
              .findFirst();
      if (user.isPresent() && user.get().isOnBreak()) {
        String servicePointId = user.get().getLastServicePointId();
        if (visitService.getServicePointHashMap(branchId).containsKey(servicePointId)) {
          ServicePoint servicePoint =
              visitService.getServicePointHashMap(branchId).get(servicePointId);
          if (servicePoint.getUser() == null) {
            servicePoint.setUser(user.get());
            return Optional.of(servicePoint);
          }
        }
      }
    }
    return result;
  }

  /**
   * Возвращает список сотрудников отделения
   *
   * @param branchId идентификатор отделения
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Данные о сотрудниках")
  @Tag(name = "Полный список")
  @Tag(name = "Данные о пулах")
  @Operation(
      summary = "Сотрудники отделения",
      description = "Возвращает список сотрудников отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список сотрудников"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get("/branches/{branchId}/users")
  @ExecuteOn(TaskExecutors.IO)
  public List<User> getUsersOfBranch(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return branchService.getUsers(branchId).values().stream().toList();
  }

  /**
   * Возвращает список всех сотрудников, на данный момент работающих в отделении
   *
   * @param branchId идентификатор отделения
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Данные о сотрудниках")
  @Tag(name = "Полный список")
  @Tag(name = "Данные о пулах")
  @Operation(
      summary = "Работающие сотрудники отделения",
      description = "Возвращает список сотрудников, находящихся на рабочем месте",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список сотрудников"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get("/branches/{branchId}/workingusers")
  @ExecuteOn(TaskExecutors.IO)
  public List<User> getAllWorkingUsersOfBranch(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getAllWorkingUsers(branchId).values().stream().toList();
  }

  /**
   * Возвращает точку обслуживания по логину сотрудника
   *
   * @param userName логин пользователя
   * @return свободные точки обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Get("/servicePoints/user/{userName}")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Поиск точки обслуживания по логину",
      description = "Возвращает точку обслуживания по логину сотрудника среди всех отделений",
      responses = {
        @ApiResponse(responseCode = "200", description = "Точка обслуживания"),
        @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * @param userName логин пользователя
   * @return пользователь занимающий рабочее место
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Сотрудник по логину",
      description = "Возвращает информацию о сотруднике по его логину",
      responses = {
        @ApiResponse(responseCode = "200", description = "Сотрудник найден"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Рабочие профили отделения",
      description = "Возвращает список рабочих профилей отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список профилей"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get("/branches/{branchId}/workProfiles")
  @ExecuteOn(TaskExecutors.IO)
  public List<TinyClass> getWorkProfiles(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.getWorkProfiles(branchId);
  }

  /**
   * Смена рабочего профиля сотрудника работающего в точке обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param workProfileId идентификатор рабочего профиля
   * @return сотрудник
   */
  @Operation(
      operationId = "openServicePoint",
      summary = "Смена рабочего профиля сотрудника работающего в точки обслуживания",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Смена рабочего профиля произошла успешно"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Рабочий профиль не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "404",
            description = "Сотрудник на точке обслуживания не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Работа сотрудников")
  @Tag(name = "Полный список")
  @Put("/branches/{branchId}/servicePoints/{servicePointId}/workProfiles/{workProfileId}")
  @ExecuteOn(TaskExecutors.IO)
  public User changeUserWorkprofile(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "d5a84e60-e605-4527-b065-f4bd7a385790") String workProfileId)
      throws BusinessException {

    return branchService.changeUserWorkProfileInServicePoint(
        branchId, servicePointId, workProfileId);
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
   * @throws BusinessException бизнес-ошибка
   * @throws java.io.IOException ошибка взаимодействия с внешними сервисами
   */
  @Operation(
      operationId = "openServicePoint",
      summary = "Открытие точки обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Открытие произошло успешно"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Рабочий профиль не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "409",
            description =
                "В данной точке обслуживания уже сидит сотрудник, и если возвращается не пустой ticketId - идет обслуживание",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = {
                      @ExampleObject(
                          name = "Сотрудник не обслуживает",
                          value =
                              """
                                      {
                                        "servicePointId": "a66ff6f4-4f4a-4009-8602-0dc278024cf2",
                                        "message": "The service point is already busy",
                                        "ticket": "",
                                        "userName": "psokolov",
                                        "servicePointName": "Каб. 121"
                                      }"""),
                      @ExampleObject(
                          name = "Сотрудник обслуживает",
                          value =
                              """
                                            {
                                              "servicePointId": "a66ff6f4-4f4a-4009-8602-0dc278024cf2",
                                              "message": "The service point is already busy",
                                              "ticket": "F001",
                                              "userName": "psokolov",
                                              "servicePointName": "Каб. 121"
                                            }""")
                    },
                    schema = @Schema(implementation = HashMap.class))),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @PathVariable(defaultValue = "d5a84e60-e605-4527-b065-f4bd7a385790") String workProfileId)
      throws BusinessException, IOException {

    return branchService.openServicePoint(
        branchId, userName, servicePointId, workProfileId, visitService);
  }

  /**
   * Закрытие рабочей станции сотрудником Если рабочая станция уже закрыта выдается 409 ошибка
   * (конфликт)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param isBreak флаг указывающий, что точка обслуживания закрывается из-за ухода сотрудника на
   *     перерыв
  * @param isForced флаг "принудительного" завершения обслуживания
  * @param breakReason причина перерыва
  * @param reason причина принудительного завершения обслуживания
  */
  @SuppressWarnings("all")
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Работа сотрудников")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Закрытие точки обслуживания",
      description =
          "Завершает работу точки обслуживания. При повторном запросе возвращает конфликт",
      responses = {
        @ApiResponse(responseCode = "200", description = "Точка обслуживания закрыта"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "409", description = "Точка уже закрыта"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post("/branches/{branchId}/servicePoints/{servicePointId}/close")
  @ExecuteOn(TaskExecutors.IO)
  public void closeServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @QueryValue(defaultValue = "false") Boolean isBreak,
      @Nullable @QueryValue String breakReason,
      @QueryValue(defaultValue = "false") Boolean isForced,
      @QueryValue(defaultValue = "") String reason) {

    branchService.closeServicePoint(
        branchId, servicePointId, visitService, false, isBreak, breakReason, isForced, reason);
  }

  /**
   * Закрытие рабочей станции сотрудником и выход из системы Если рабочая станция уже закрыта
   * выдается 409 ошибка (конфликт)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param isBreak флаг указывающий, что точка обслуживания закрывается из-за ухода сотрудника на
   *     перерыв
   * @param isForced флаг "принудительного" завершения обслуживания
  * @param reason причина принудительного завершения обслуживания
  * @param breakReason причина перерыва
  */
  @SuppressWarnings("all")
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Работа сотрудников")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Закрытие точки и выход сотрудника",
      description = "Закрывает точку обслуживания и завершает сессию сотрудника",
      responses = {
        @ApiResponse(responseCode = "200", description = "Точка обслуживания закрыта"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "409", description = "Точка уже закрыта"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post("/branches/{branchId}/servicePoints/{servicePointId}/logout")
  @ExecuteOn(TaskExecutors.IO)
  public void logoutUser(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @QueryValue(defaultValue = "false") Boolean isBreak,
      @Nullable @QueryValue String breakReason,
      @QueryValue(defaultValue = "false") Boolean isForced,
      @QueryValue(defaultValue = "") String reason) {

    branchService.closeServicePoint(
        branchId, servicePointId, visitService, true, isBreak, breakReason, isForced, reason);
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
  @Operation(
      summary = "Визиты очереди с ограничением",
      description =
          "Возвращает последние визиты указанной очереди, количество ограничено параметром",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список визитов"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Все визиты очереди",
      description = "Возвращает все визиты указанной очереди",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список визитов"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Все визиты отделения",
      description =
          "Возвращает все визиты отделения, включая находящиеся в очередях и пулах",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список визитов"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Визит по идентификатору",
      description = "Возвращает визит по его идентификатору",
      responses = {
        @ApiResponse(responseCode = "200", description = "Данные визита"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Визиты по статусам",
      description = "Возвращает визиты с указанными статусами",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список визитов"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Получение данных о визите",
      description = "Возвращает информацию о визите по идентификатору",
      responses = {
        @ApiResponse(responseCode = "200", description = "Данные о визите"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
        .orElseThrow(() -> new BusinessException("visit_not_found", eventService, HttpStatus.NOT_FOUND));
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
  @Operation(
      summary = "Вызов визита по идентификатору",
      description = "Переводит визит в статус CALLED",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "409", description = "Визит уже вызван"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Вызов визита с подтверждением",
      description = "Визит вызывается и ожидает подтверждения клиента",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "403", description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/confirmed/call/visit",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirm(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body Visit visit) {

    return visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointId, visit);
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
  @Operation(
      summary = "Вызов визита по идентификатору с подтверждением",
      description = "Визит вызывается по ID и ожидает подтверждения клиента",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "403", description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
    return visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointId, visit);
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
  @Tag(name = "Вызов визита c наибольшим временем ожидания")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Вызов визита с максимальным ожиданием",
      description = "Вызывает визит с наибольшим временем ожидания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Вызов визита с наибольшим временем ожидания с ожиданием подтверждения
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Вызов визита c наибольшим временем ожидания")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Вызов визита с подтверждением",
      description =
          "Вызывает визит с максимальным временем ожидания и ожидает подтверждения клиента",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/call",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirmMaxWaitingTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    return visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointId);
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
  @Tag(name = "Вызов визита c наибольшим временем ожидания")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Вызов из перечня очередей")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Вызов из указанных очередей",
      description =
          "Вызывает визит с максимальным временем ожидания из переданных очередей",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Tag(name = "Вызов визита c наибольшим временем ожидания")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Вызов из очередей с подтверждением",
      description =
          "Вызывает визит с максимальным временем ожидания из переданных очередей с ожиданием подтверждения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/callfromQueues",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<Visit> visitCallForConfirmMaxWaitingTime(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body List<String> queueIds) {

    return visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointId, queueIds);
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
  @Tag(name = "Вызов визита c максимальным временем жизни")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Вызов визита с максимальным временем жизни",
      description = "Вызывает визит, дольше всего ожидающий обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Tag(name = "Вызов визита c максимальным временем жизни")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Вызов из перечня очередей")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Вызов из очередей с максимальным временем жизни",
      description =
          "Вызывает визит с наибольшим временем жизни из указанных очередей",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Tag(name = "Вызов визита c максимальным временем жизни")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Вызов визита с подтверждением по времени жизни",
      description =
          "Вызывает визит с максимальным временем жизни и ожидает подтверждения клиента",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Tag(name = "Вызов визита c максимальным временем жизни")
  @Tag(name = "Вызов из перечня очередей")
  @Tag(name = "Ожидание подтверждения прихода")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Вызов из очередей по времени жизни с подтверждением",
      description =
          "Вызывает визит с максимальным временем жизни из указанных очередей с ожиданием подтверждения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "207", description = "Режим автоматического вызова активен"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Отмена вызова: клиент не пришёл",
      description = "Переводит визит в статус NO_SHOW",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит отменён"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Отмена вызова по идентификатору",
      description = "Переводит визит в статус NO_SHOW по его идентификатору",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит отменён"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Повторный вызов визита с ожиданием подтверждения
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
  @Operation(
      summary = "Повторный вызов визита",
      description = "Повторно вызывает визит с ожиданием подтверждения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван повторно"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "409", description = "Визит уже вызван"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Повторный вызов визита с ожиданием подтверждения по идентификатору
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
  @Operation(
      summary = "Повторный вызов визита по идентификатору",
      description = "Повторно вызывает визит по его идентификатору",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит вызван повторно"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "409", description = "Визит уже вызван"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Подтверждение прихода",
      description = "Переводит визит в статус CONFIRMED",
      responses = {
        @ApiResponse(responseCode = "200", description = "Приход подтвержден"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "409", description = "Визит уже подтвержден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Подтверждение прихода по идентификатору",
      description = "Подтверждает приход визита по его идентификатору",
      responses = {
        @ApiResponse(responseCode = "200", description = "Приход подтвержден"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "409", description = "Визит уже подтвержден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Отмена режима автоматического вызова для точки обслуживания
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
  @Operation(
      summary = "Отмена автоматического вызова",
      description = "Отключает режим автоматического вызова в точке обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Режим отключен"),
        @ApiResponse(responseCode = "207", description = "Режим уже отключён"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put("/branches/{branchId}/servicePoins/{servicePointId}/cancelAutoCall")
  public Optional<ServicePoint> cancelAutoCallModeOfServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {
    return visitService.cancelAutoCallModeOfServicePoint(branchId, servicePointId);
  }

  /**
   * Запуск режима автоматического вызова для точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return точка обслуживания
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Автоматический вызов")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Запуск автоматического вызова",
      description = "Включает режим автоматического вызова в точке обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Режим включен"),
        @ApiResponse(responseCode = "207", description = "Режим уже включён"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put("/branches/{branchId}/servicePoins/{servicePointId}/startAutoCall")
  public Optional<ServicePoint> startAutoCallModeOfServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {
    visitService.setAutoCallModeOfBranch(branchId, true);
    return visitService.startAutoCallModeOfServicePoint(branchId, servicePointId);
  }

  /**
   * Получение возможных фактических услуг
   *
   * @param branchId идентификатор отделения
   * @param serviceId идентификатор услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные об услугах")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Возможные фактические услуги",
      description = "Возвращает перечень доступных фактических услуг для указанной услуги",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список фактических услуг"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
          .filter(f -> f.getValue().getServiceIds().contains(serviceId))
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue));
    } else {
      throw new BusinessException(
          String.format("Service %s not found", serviceId),
          String.format("Услуга %s не найдена", serviceId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Получение списка предоставленных фактических услуг у текущей услугу текущего визита в указанной
   * точке обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные об услугах")
  @Tag(name = "Фактические услуги")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Фактические услуги текущей услуги", 
      description = "Возвращает список предоставленных фактических услуг текущего визита в точке обслуживания", 
      responses = {
        @ApiResponse(responseCode = "200", description = "Список фактических услуг"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(
      uri = "/branches/{branchId}/servicePoins/{servicePointId}/deliveredServices",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Map<String, DeliveredService> getDeliveredServiceOfCurrentService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {

    return visitService.getDeliveredServices(branchId, servicePointId);
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
  @Operation(
      summary = "Получение услуг рабочего профиля",
      description =
          "Возвращает список услуг, доступных для указанного рабочего профиля отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список услуг"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Рабочий профиль не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Получение услуг очереди",
      description =
          "Возвращает список услуг, связанных с указанной очередью отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список услуг"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Получение возможных фактических услуг в отделении
   *
   * @param branchId идентификатор отделения *
   * @return список услуг
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные об услугах")
  @Tag(name = "Фактические услуги")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Получение возможных фактических услуг",
      description =
          "Возвращает перечень фактических услуг, которые может оказать отделение",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список фактических услуг"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(
      uri = "/branches/{branchId}/possibleDeliveredServices",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public List<DeliveredService> getDeliveredServicesByBranchId(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {

    return branchService.getDeliveredServicesByBranchId(branchId);
  }

  /**
   * Получение возможных итогов для услуги
   *
   * @param branchId идентификатор отделения
   * @param serviceId идентификатор услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные об итогах")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Возможные итоги услуги",
      description = "Возвращает список возможных итогов для указанной услуги",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список итогов"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
          String.format("Service %s not found", serviceId),
          String.format("Услуга %s не найдена", serviceId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление фактической услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор фактической услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Фактические услуги")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление фактической услуги",
      description = "Добавляет фактическую услугу к текущему визиту",
      responses = {
        @ApiResponse(responseCode = "200", description = "Фактическая услуга добавлена"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "404",
            description = "Текущая услуга визита отсутствует"),
        @ApiResponse(
            responseCode = "404",
            description = "Фактическая услуга не найдена в конфигурации отделения"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Получение марков в визите
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор точки обслуживания *
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Марки")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Получение меток визита",
      description = "Возвращает список меток, установленных на визит",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список меток"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(uri = "/branches/{branchId}/visits/{visitId}/marks", produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public List<Mark> getMarks(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String visitId) {

    return visitService.getMarks(branchId, visitId);
  }

  /**
   * Удаление марки в визите
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param markId идентификатор метки
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Марки")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Удаление метки визита",
      description = "Удаляет выбранную метку из визита",
      responses = {
        @ApiResponse(responseCode = "200", description = "Метка удалена"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Метка не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Возвращение списка возможных марков отделения
   *
   * @param branchId идентификатор отделения
   * @return список меток
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Марки")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Список возможных меток",
      description = "Возвращает перечень меток, доступных в отделении",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список меток"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(uri = "/branches/{branchId}/marks/", produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public HashMap<String, Mark> deleteMark(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {

    Branch branch = branchService.getBranch(branchId);
    return branch.getMarks();
  }

  /**
   * Добавление марки в формате объекта
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param markId идентификатор метки
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Марки")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление метки визиту",
      description = "Присваивает визиту выбранную метку",
      responses = {
        @ApiResponse(responseCode = "200", description = "Метка добавлена"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Метка не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Добавление заметки в виде текста
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param noteText текст заметки
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Заметки")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление текстовой заметки",
      description = "Создает текстовую заметку для визита",
      responses = {
        @ApiResponse(responseCode = "200", description = "Заметка добавлена"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/notes",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit addNoteAsText(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body String noteText) {

    return visitService.addNote(branchId, servicePointId, noteText);
  }

  /**
   * Получение текстовых заметок в визите
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор точки обслуживания *
   * @return визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Заметки")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Получение заметок визита",
      description = "Возвращает текстовые заметки, добавленные к визиту",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список заметок"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(uri = "/branches/{branchId}/visits/{visitId}/notes", produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public List<Mark> getNotes(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String visitId) {

    return visitService.getNotes(branchId, visitId);
  }

  /**
   * Добавление итога текущей услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param outcomeId идентификатор итога оказания услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Итоги услуги")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление итога услуги",
      description = "Фиксирует итог оказания текущей услуги визиту",
      responses = {
        @ApiResponse(responseCode = "200", description = "Итог установлен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "404",
            description = "Текущая услуга визита отсутствует"),
        @ApiResponse(responseCode = "409", description = "Итог недоступен для текущей услуги"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Изменение визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление услуги визиту",
      description = "Добавляет новую услугу в список услуг визита",
      responses = {
        @ApiResponse(responseCode = "200", description = "Услуга добавлена"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Добавление услуг в визит
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param serviceIds идентификатор услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Изменение визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление нескольких услуг визиту",
      description = "Добавляет набор услуг в визит",
      responses = {
        @ApiResponse(responseCode = "200", description = "Услуги добавлены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/services",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit addServices(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body List<String> serviceIds) {
    Visit visit = null;
    for (String serviceId : serviceIds) {

      visit = visitService.addService(branchId, servicePointId, serviceId);
    }
    return visit;
  }

  /**
   * Добавление итога фактической услуги текущей услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор фактической услуги
   * @param outcomeId идентификатор итога оказания услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Итоги услуги")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление итога фактической услуги",
      description = "Устанавливает итог для фактической услуги визита",
      responses = {
        @ApiResponse(responseCode = "200", description = "Итог установлен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "404",
            description = "Текущая услуга визита отсутствует"),
        @ApiResponse(
            responseCode = "404",
            description = "Фактическая услуга не найдена у визита"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(
            responseCode = "404",
            description = "Итог для фактической услуги не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredService/{deliveredServiceId}/outcome/{outcomeId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit addOutcomeOfDeliveredService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "35d73fdd-1597-4d94-a087-fd8a99c9d1ed")
          String deliveredServiceId,
      @PathVariable(defaultValue = "8dc29622-cd87-4384-85a7-04b66b28dd0f") String outcomeId) {

    return visitService.addOutcomeOfDeliveredService(
        branchId, servicePointId, deliveredServiceId, outcomeId);
  }

  /**
   * Удаление итога фактической услуги текущей услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор фактической услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Итоги услуги")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Удаление итога фактической услуги",
      description = "Удаляет установленный итог для фактической услуги визита",
      responses = {
        @ApiResponse(responseCode = "200", description = "Итог удален"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(
            responseCode = "404",
            description = "Текущая услуга визита отсутствует"),
        @ApiResponse(
            responseCode = "404",
            description = "Фактическая услуга не найдена у визита"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Удаление итога фактической услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param serviceId идентификатор итога оказания услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Итоги услуги")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Удаление итога услуги",
      description = "Удаляет итог оказания услуги у визита",
      responses = {
        @ApiResponse(responseCode = "200", description = "Итог удален"),

        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),

        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Удаление фактической услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор фактической услуги
   * @return вызванный визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Обслуживание")
  @Tag(name = "Фактические услуги")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Удаление фактической услуги",
      description = "Удаляет фактическую услугу из визита",
      responses = {
        @ApiResponse(responseCode = "200", description = "Фактическая услуга удалена"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Услуга не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
   * Возвращает данные о точке обслуживания если в данный момент она не действует - ищется
   * сотрудник, который на перерыве и сидел за этой рабочей станцией и возвращаем данные с этим
   * сотрудником
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return доступные очереди
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Данные о точках обслуживания")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Данные точки обслуживания",
      description = "Возвращает информацию о точке обслуживания, учитывая сотрудника на перерыве",
      responses = {
        @ApiResponse(responseCode = "200", description = "Точка обслуживания"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(
      uri = "/branches/{branchId}/servicePoints/{servicePointId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Optional<ServicePoint> getServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {
    Optional<ServicePoint> servicePoint =
        visitService.getServicePointHashMap(branchId).containsKey(servicePointId)
            ? Optional.of(visitService.getServicePointHashMap(branchId).get(servicePointId))
            : Optional.empty();
    if (servicePoint.isPresent()) {
      Optional<User> user =
          visitService.getUsers(branchId).stream()
              .filter(f -> f.isOnBreak() && f.getLastServicePointId().equals(servicePointId))
              .findFirst();
      if (servicePoint.get().getUser() == null && user.isPresent()) {
        servicePoint.get().setUser(user.get());
      }
    }
    return servicePoint;
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
  @Operation(
      summary = "Доступные очереди точки обслуживания",
      description = "Возвращает список очередей, доступных для точки обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список очередей"),
        @ApiResponse(
            responseCode = "403",
            description = "Сотрудник не авторизован или точка обслуживания недоступна"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
  @Operation(
      summary = "Удаление визита из очереди",
      description = "Удаляет визит, находящийся в точке обслуживания",
      responses = {
        @ApiResponse(responseCode = "204", description = "Визит удален"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "409", description = "Визит нельзя удалить"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Delete(
      uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}",
      consumes = "application/json",
      produces = "application/json")
  @Status(HttpStatus.NO_CONTENT)
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
  @Operation(
      summary = "Удаление визита по идентификатору",
      description = "Удаляет визит по его идентификатору",
      responses = {
        @ApiResponse(responseCode = "204", description = "Визит удален"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "409", description = "Визит нельзя удалить"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Delete(
      uri = "/branches/{branchId}/visits/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @Status(HttpStatus.NO_CONTENT)
  @ExecuteOn(TaskExecutors.IO)
  public void deleteVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable String visitId) {
    if (!visitService.getAllVisits(branchId).containsKey(visitId)) {
      throw new BusinessException(
          String.format("Visit with id %s not found", visitId),
          String.format("Визит с идентификатором %s не найден", visitId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
    Visit visit = visitService.getAllVisits(branchId).get(visitId);

    visitService.deleteVisit(visit);
  }

  /**
   * Перевод визита в очередь из точки обслуживания.
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueId идентификатор очереди
   * @param isAppend вставка в конец (true) или начало (false)
   * @param transferTimeDelay задержка после перевода в секундах
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита в очередь",
      description = "Переводит визит из точки обслуживания в указанную очередь",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(
      uri =
          "/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromServicePoint",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransfer(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new BusinessException("queue_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitTransfer(
        branchId, servicePointId, queueId, isAppend, transferTimeDelay);
  }

  /**
   * Возвращение визита в пул точки обслуживания из точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания, которой принадлежит пул
   * @param returnTimeDelay задержка возвращения в секундах
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Возвращение визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Возврат визита в пул",
      description = "Возвращает визит в пул указанной точки обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит возвращен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @QueryValue(defaultValue = "60") Long returnTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new BusinessException("service_point_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitBackToServicePointPool(
        branchId, servicePointId, poolServicePointId, returnTimeDelay);
  }

  /**
   * Перевод визита в пул точки обслуживания из точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания, которой принадлежит пул
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Завершение вызова")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита в пул",
      description = "Переводит визит в пул указанной точки обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
          String poolServicePointId,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new BusinessException("service_point_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitTransferToServicePointPool(
        branchId, servicePointId, poolServicePointId, transferTimeDelay);
  }

  /**
   * Перевод визита в пул точки обслуживания из точки обслуживания внешней службой.
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания, которой принадлежит пул
   * @param serviceInfo данные внешней службы
   * @param transferTimeDelay задержка перевода в секундах
   * @return визит после перевода
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита внешней службой (Ресепшен, MI и т д)")
  @Tag(name = "Обслуживание")
  @Tag(name = "Полный список")
  //  @Put(
  //      uri =
  //
  // "/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/service/transfer",
  //      consumes = "application/json",
  //      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2")
          String poolServicePointId,
      HashMap<String, String> serviceInfo,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new BusinessException("service_point_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitTransferToServicePointPool(
        branchId, servicePointId, poolServicePointId, serviceInfo, transferTimeDelay);
  }

  /**
   * Возвращение визита в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param returnTimeDelay задержка визита в секундах
   * @return визит после перевода
   */
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Обслуживание")
    @Tag(name = "Возвращение визита")
    @Tag(name = "Изменение визита")
    @Tag(name = "Полный список")
    @Tag(name = "Завершение вызова")
    @Operation(
        summary = "Возврат визита в очередь",
        description = "Возвращает визит из точки обслуживания обратно в очередь",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит возвращен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(
        uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/put_back",
        consumes = "application/json",
        produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
  public Visit returnVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @QueryValue(defaultValue = "60") Long returnTimeDelay) {

    return visitService.stopServingAndBackToQueue(branchId, servicePointId, returnTimeDelay);
  }

  /**
   * Перевод визита из очереди в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueId идентификатор очереди
   * @param visitId идентификатор визита
   * @param index позиция визита в списке
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита по позиции",
      description = "Переводит визит из очереди в очередь на указанную позицию",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @QueryValue(defaultValue = "0") Integer index,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new BusinessException("queue_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransfer(
        branchId, servicePointId, queueId, visit, index, transferTimeDelay);
  }

  /**
   * Перевод визита из очереди в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param queueId идентификатор очереди
   * @param visitId идентификатор визита
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита в начало или конец очереди",
      description = "Переводит визит из очереди в очередь с размещением в начало или конец списка",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new BusinessException("queue_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransfer(
        branchId, servicePointId, queueId, visit, !isAppend, transferTimeDelay);
  }

  /**
   * Перевод визита из очереди в очередь с помощью внешней службы (Ресепшен, MI и т д)
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @param visitId идентификатор визита
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param serviceInfo данные о внешней службе
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @param sid идентификатор сессии сотрудника (cookie sid)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита внешней службой (Ресепшен, MI и т д)")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита внешней службой",
      description = "Переводит визит в другую очередь с указанием внешней службы",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(
      uri = "/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueue(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "c211ae6b-de7b-4350-8a4c-cff7ff98104e") String queueId,
      @PathVariable String visitId,
      @Body HashMap<String, String> serviceInfo,
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay,
      @Nullable @CookieValue("sid") String sid) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new BusinessException("queue_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransfer(
        branchId, queueId, visit, isAppend, serviceInfo, transferTimeDelay, sid);
  }

  /**
   * Перевод визита из очереди в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueId идентификатор очереди
   * @param visit переводимый визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита в другую очередь",
      description = "Переводит переданный визит из одной очереди в другую",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new BusinessException("queue_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitTransfer(
        branchId, servicePointId, queueId, visit, !isAppend, transferTimeDelay);
  }

  /**
   * Перевод визита из очереди в очередь в указанную позицию
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания *
   * @param queueId идентификатор очереди
   * @param visit переводимый визит
   * @param index позиция визита в списке
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита на позицию очереди",
      description = "Переводит визит из очереди в очередь на указанную позицию",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очередь не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @PathVariable(defaultValue = "0") Integer index,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getQueues().containsKey(queueId)) {
      throw new BusinessException("queue_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitTransfer(
        branchId, servicePointId, queueId, visit, index, transferTimeDelay);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания в указанную позицию
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания, которой принадлежит пул
   * @param visit переводимый визит
   * @param index позиция визита в списке
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита в пул на позицию",
      description = "Переводит визит из очереди в пул точки обслуживания на указанную позицию",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @PathVariable(defaultValue = "0") Integer index,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new BusinessException("service_point_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, poolServicePointId, visit, index, transferTimeDelay);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания, которой принадлежит пул
   * @param visit переводимый визит
   * @param isAppend вставка в конец (true) или начало (false)
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита в пул точки обслуживания",
      description = "Переводит визит из очереди в пул указанной точки обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    Branch branch;

    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getServicePoints().containsKey(poolServicePointId)) {
      throw new BusinessException("service_point_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, poolServicePointId, visit, isAppend, transferTimeDelay);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания внешней службой (MI, ресепшен и т д)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visitId идентификатор визита
   * @param serviceInfo данные о внешней службе
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @param sid идентификатор сессии сотрудника (cookie sid)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита внешней службой (Ресепшен, MI и т д)")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита в пул внешней службой",
      description = "Переводит визит из очереди в пул точки обслуживания с указанием внешней службы",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(
      uri =
          "/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  public Visit visitTransferFromQueueToServicePointPool(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @PathVariable String visitId,
      HashMap<String, String> serviceInfo,
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay,
      @Nullable @CookieValue("sid") String sid) {
    Branch branch;
    Visit visit = visitService.getVisit(branchId, visitId);
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getServicePoints().containsKey(servicePointId)) {
      throw new BusinessException("service_point_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, visit, isAppend, serviceInfo, transferTimeDelay, sid);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания, которой принадлежит пул
   * @param visitId переводимый визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита по идентификатору в пул",
      description = "Переводит визит по идентификатору из очереди в пул точки обслуживания",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, poolServicePointId, visit, isAppend, transferTimeDelay);
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания в указанную позицию
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания, которой принадлежит пул
   * @param visitId переводимый визит
   * @param index позиция визита в списке
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @return итоговый визит
   */
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Изменение визита")
  @Tag(name = "Перевод визита")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Перевод визита по идентификатору на позицию в пуле",
      description = "Переводит визит по идентификатору из очереди в пул точки обслуживания на указанную позицию",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит переведен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
        @ApiResponse(responseCode = "404", description = "Визит не найден"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
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
      @QueryValue(defaultValue = "0") Integer index,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {

    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointId, poolServicePointId, visit, index, transferTimeDelay);
  }

  /**
   * Завершение обслуживания (нормальное)
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param isForced флаг "принудительного" завершения обслуживания
   * @param reason причина принудительного завершения обслуживания
   * @return визит после перевода
   */
    @SuppressWarnings("all")
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Обслуживание")
    @Tag(name = "Полный список")
    @Tag(name = "Завершение вызова")
    @Operation(
        summary = "Завершение обслуживания",
        description = "Завершает обслуживание визита в точке обслуживания",
        responses = {
          @ApiResponse(responseCode = "200", description = "Обслуживание завершено"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(
        uri = "/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end",
        consumes = "application/json",
        produces = "application/json")
    @ExecuteOn(TaskExecutors.IO)
  public Visit visitEnd(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @QueryValue(defaultValue = "false") Boolean isForced,
      @QueryValue(defaultValue = "") String reason) {

    return visitService.visitEnd(branchId, servicePointId, isForced, reason);
  }

  /**
   * Перевод визита из очереди в пул сотрудника в конец или начало
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visit переводимый визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @param sid идентификатор сессии сотрудника (cookie sid)
   * @return визит
   */
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Изменение визита")
    @Tag(name = "Перевод визита")
    @Tag(name = "Полный список")
    @Operation(
        summary = "Перевод визита в пул сотрудника",
        description = "Переводит визит в пул сотрудника в начало или конец списка",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит переведен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
          @ApiResponse(responseCode = "404", description = "Визит не найден"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/users/{userId}")
    public Visit visitTransferFromQueueToUserPool(
        @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
        @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
        @Body Visit visit,
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay,
      @Nullable @CookieValue("sid") String sid) {

    return visitService.visitTransferFromQueueToUserPool(
        branchId, userId, visit, isAppend, transferTimeDelay, sid);
  }

  /**
   * Перевод визита из очереди в пул сотрудника в определенную позицию в пуле
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visit переводимый визит
   * @param index позиция визита в списке
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @param sid идентификатор сессии сотрудника (cookie sid)
   * @return визит
   */
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Изменение визита")
    @Tag(name = "Перевод визита")
    @Tag(name = "Полный список")
    @Operation(
        summary = "Перевод визита в пул сотрудника на позицию",
        description = "Переводит визит из очереди в пул сотрудника на указанную позицию",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит переведен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
          @ApiResponse(responseCode = "404", description = "Визит не найден"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/users/{userId}/position/{index}")
    public Visit visitTransferFromQueueToUserPool(
        @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
        @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
        @Body Visit visit,
      @PathVariable(defaultValue = "0") Integer index,
      @QueryValue(defaultValue = "0") Long transferTimeDelay,
      @Nullable @CookieValue("sid") String sid) {
    return visitService.visitTransferFromQueueToUserPool(
        branchId, userId, visit, index, transferTimeDelay, sid);
  }

  /**
   * Перевод визита из очереди в пул сотрудника
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visitId идентификатор переводимого визита
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @param sid идентификатор сессии сотрудника (cookie sid)
   * @return визит
   */
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Изменение визита")
    @Tag(name = "Перевод визита")
    @Tag(name = "Полный список")
    @Operation(
        summary = "Перевод визита по идентификатору в пул сотрудника",
        description = "Переводит визит по идентификатору в пул сотрудника в начало или конец списка",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит переведен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
          @ApiResponse(responseCode = "404", description = "Визит не найден"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/users/{userId}/visits/{visitId}")
    public Visit visitTransferFromQueueToUserPool(
        @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
        @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
        @PathVariable String visitId,
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay,
      @Nullable @CookieValue("sid") String sid) {
    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToUserPool(
        branchId, userId, visit, isAppend, transferTimeDelay, sid);
  }

  /**
   * Перевод визита из очереди в пул сотрудника из внешней службы (MI, Ресепшен и т д)
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visitId идентификатор переводимого визита
   * @param serviceInfo данные о внешней службе
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @param sid идентификатор сессии сотрудника (cookie sid)
   * @return визит
   */
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Изменение визита")
    @Tag(name = "Перевод визита внешней службой (Ресепшен, MI и т д)")
    @Tag(name = "Полный список")
    @Operation(
        summary = "Перевод визита внешней службой в пул сотрудника",
        description = "Переводит визит в пул сотрудника по данным внешней службы",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит переведен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
          @ApiResponse(responseCode = "404", description = "Визит не найден"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer")
    public Visit visitTransferFromQueueToUserPool(
        @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
        @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
        @PathVariable String visitId,
      HashMap<String, String> serviceInfo,
      @QueryValue(defaultValue = "true") Boolean isAppend,
      @QueryValue(defaultValue = "0") Long transferTimeDelay,
      @Nullable @CookieValue("sid") String sid) {
    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToUserPool(
        branchId, userId, visit, isAppend, serviceInfo, transferTimeDelay, sid);
  }

  /**
   * Перевод визита из очереди в пул сотрудника
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор сотрудника
   * @param visitId идентификатор переводимого визита
   * @param index позиция визита в списке
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @param sid идентификатор сессии сотрудника (cookie sid)
   * @return визит
   */
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Изменение визита")
    @Tag(name = "Перевод визита")
    @Tag(name = "Полный список")
    @Operation(
        summary = "Перевод визита по идентификатору в пул сотрудника на позицию",
        description = "Переводит визит из очереди в пул сотрудника на указанную позицию",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит переведен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
          @ApiResponse(responseCode = "404", description = "Визит не найден"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/users/{userId}/visits/{visitId}/position/{index}")
    public Visit visitTransferFromQueueToUserPool(
        @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
        @PathVariable(defaultValue = "f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89") String userId,
        @PathVariable String visitId,
      @QueryValue(defaultValue = "0") Integer index,
      @QueryValue(defaultValue = "0") Long transferTimeDelay,
      @Nullable @CookieValue("sid") String sid) {
    Visit visit = visitService.getVisit(branchId, visitId);
    return visitService.visitTransferFromQueueToUserPool(
        branchId, userId, visit, index, transferTimeDelay, sid);
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
    @Operation(
        summary = "Возвращение визита в пул сотрудника",
        description = "Возвращает визит из точки обслуживания в пул сотрудника",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит возвращен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
          @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/users/{userId}/put_back")
    public Visit visitBackToUserPool(
        @PathVariable String branchId,
        @PathVariable String servicePointId,
        @PathVariable String userId,
      @QueryValue(defaultValue = "60") Long returnTimeDelay) {
    return visitService.visitBackToUserPool(branchId, servicePointId, userId, returnTimeDelay);
  }

  /**
   * Перевод визита из точки обслуживания в пул сотрудника
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param userId идентификатор сотрудника
   * @param transferTimeDelay задержка визита после перевода в секундах (период запрета на вызов
   *     после перевода)
   * @return визит
   */
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Обслуживание")
    @Tag(name = "Изменение визита")
    @Tag(name = "Завершение вызова")
    @Tag(name = "Перевод визита")
    @Tag(name = "Полный список")
    @Operation(
        summary = "Перевод визита в пул сотрудника",
        description = "Переводит визит из точки обслуживания в пул сотрудника",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит переведен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
          @ApiResponse(responseCode = "404", description = "Сотрудник не найден"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/users/{userId}/transfer")
    public Visit visitTransferToUserPool(
        @PathVariable String branchId,
        @PathVariable String servicePointId,
        @PathVariable String userId,
      @QueryValue(defaultValue = "0") Long transferTimeDelay) {
    return visitService.visitTransferToUserPool(
        branchId, servicePointId, userId, transferTimeDelay);
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
    @Operation(
        summary = "Отложить визит",
        description = "Отложить текущий визит в точке обслуживания",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит отложен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/postpone")
    public Visit visitPostPone(
        @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
        @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId) {
      return visitService.visitPostPone(branchId, servicePointId);
  }

  /**
   * Возвращение визита из точки обслуживания
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
    @Operation(
        summary = "Возвращение визита из точки обслуживания",
        description = "Возвращает обслуживаемый визит в очередь отделения",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит возвращен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Точка обслуживания не найдена"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/servicePoints/{servicePointId}/visit/put_back")
    public Visit visitPutBack(
        @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
        @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
        @QueryValue(defaultValue = "60") Long returnTimeDelay) {
    return visitService.visitPutBack(branchId, servicePointId, returnTimeDelay);
  }

  /**
   * Возвращение вызванного визита
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор визита
   * @param returnTimeDelay задержка возвращения в секундах
   * @return визит
   */
    @Tag(name = "Зона обслуживания")
    @Tag(name = "Обслуживание")
    @Tag(name = "Изменение визита")
    @Tag(name = "Завершение вызова")
    @Tag(name = "Возвращение визита")
    @Tag(name = "Полный список")
    @Operation(
        summary = "Возвращение вызванного визита",
        description = "Возвращает ранее вызванный визит в очередь",
        responses = {
          @ApiResponse(responseCode = "200", description = "Визит возвращен"),
          @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
          @ApiResponse(responseCode = "404", description = "Визит не найден"),
          @ApiResponse(responseCode = "500", description = "Ошибка сервера")
        })
    @Put(uri = "/branches/{branchId}/visits/{visitId}/put_back")
    public Visit calledVisitPutBack(
        @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
        @PathVariable String visitId,
        @QueryValue(defaultValue = "60") Long returnTimeDelay) {
      return visitService.backCalledVisit(branchId, visitId, returnTimeDelay);
  }
}
