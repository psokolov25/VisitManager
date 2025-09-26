package ru.aritmos.api;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.inject.Inject;
import java.util.*;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

/**
 * REST API управления визитами и зоной ожидания.
 *
 * <p>Контроллер обрабатывает регистрацию и сопровождение посетителей: создаёт виртуальные визиты,
 * оформляет талоны, добавляет параметры и возвращает справочную информацию об услугах отделения.
 */
@Controller("/entrypoint")
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
  @ApiResponse(responseCode = "401", description = "Не авторизован"),
  @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
  @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
  @ApiResponse(responseCode = "405", description = "Метод не поддерживается"),
  @ApiResponse(responseCode = "415", description = "Неподдерживаемый тип данных"),
  @ApiResponse(responseCode = "500", description = "Ошибка сервера")
})
public class EntrypointController {

  private static final String TAG_VISIT_REGISTRATION = "Визиты · Регистрация";
  private static final String TAG_VISIT_PARAMETERS = "Визиты · Управление параметрами";
  private static final String TAG_SERVICE_AVAILABILITY = "Справочники · Доступные услуги";
  private static final String TAG_SERVICE_CATALOG = "Справочники · Услуги отделения";

  /** Сервис работы с услугами отделения. */
  @Inject Services services;

  /** Сервис управления отделениями. */
  @Inject BranchService branchService;

  /** Сервис управления визитами. */
  @Inject VisitService visitService;

  /** Сервис отправки событий во внешние системы. */
  @Inject EventService eventService;

  /**
   * Создаёт виртуальный визит без печати талона.
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param serviceIds список идентификаторов услуг (например, {@code
   *     ["c3916e7f-7bea-4490-b9d1-0d4064adbe8b", "9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"]})
   * @param sid идентификатор сессии сотрудника (cookie {@code sid})
   * @return созданный визит
   * @throws BusinessException бизнес-ошибка
   * @throws SystemException системная ошибка
   */
  @Post(
      uri = "/branches/{branchId}/servicePoint/{servicePointId}/virtualVisit",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Создание виртуального визита",
      description = "Создаёт визит без печати талона для указанной точки обслуживания",
      tags = {TAG_VISIT_REGISTRATION},
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит создан"),
        @ApiResponse(
            responseCode = "409",
            description = "Визит с такими параметрами уже существует"),
        @ApiResponse(
            responseCode = "404",
            description = "Отделение, услуга или очередь для указанного набора услуг не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit createVirtualVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body
          @RequestBody(
              description = "Список идентификаторов услуг",
              useParameterTypeSchema = true,
              content =
                  @Content(
                      schema =
                          @Schema(
                              example =
                                  // language=JSON
                                  """
                                              [
                                                "c3916e7f-7bea-4490-b9d1-0d4064adbe8b",
                                                "9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"
                                              ]
                                              """)))
          List<String> serviceIds,
      @Nullable @CookieValue("sid") String sid)
      throws BusinessException, SystemException {
    Branch branch;
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }

    if (new HashSet<>(branch.getServices().values().stream().map(BranchEntity::getId).toList())
        .containsAll(serviceIds)) {
      VisitParameters visitParameters =
          VisitParameters.builder()
              .serviceIds(new ArrayList<>(serviceIds))
              .parameters(new HashMap<>())
              .build();
      return visitService.createVirtualVisit(branchId, servicePointId, visitParameters, sid);
    }

    throw new BusinessException("services_not_found", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Создаёт визит в отделении с печатью талона при необходимости.
   *
   * @param branchId идентификатор отделения
   * @param entryPointId идентификатор точки создания визита
   * @param serviceIds список идентификаторов услуг (например, {@code
   *     ["c3916e7f-7bea-4490-b9d1-0d4064adbe8b", "9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"]})
   * @param printTicket необходимость печати талона
   * @param segmentationRuleId идентификатор правила сегментации (опционально)
   * @return созданный визит
   * @throws BusinessException бизнес-ошибка
   * @throws SystemException системная ошибка
   */
  @Post(
      uri = "/branches/{branchId}/entryPoints/{entryPointId}/visit",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Создание визита",
      description = "Создаёт визит в отделении и при необходимости печатает талон",
      tags = {TAG_VISIT_REGISTRATION},
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит создан"),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        @ApiResponse(
            responseCode = "404",
            description =
                "Отделение, точка создания визитов, услуга или очередь для указанного набора услуг не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit createVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "2") String entryPointId,
      @Body
          @RequestBody(
              description = "Список идентификаторов услуг",
              useParameterTypeSchema = true,
              content =
                  @Content(
                      schema =
                          @Schema(
                              example =
                                  // language=JSON
                                  """
                                              [
                                                "c3916e7f-7bea-4490-b9d1-0d4064adbe8b",
                                                "9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"
                                              ]
                                              """)))
          List<String> serviceIds,
      @QueryValue(defaultValue = "false") Boolean printTicket,
      @Nullable @QueryValue String segmentationRuleId)
      throws BusinessException, SystemException {
    Branch branch;
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (new HashSet<>(branch.getServices().values().stream().map(BranchEntity::getId).toList())
        .containsAll(serviceIds)) {

      VisitParameters visitParameters =
          VisitParameters.builder()
              .serviceIds(new ArrayList<>(serviceIds))
              .parameters(new HashMap<>())
              .build();
      if (segmentationRuleId == null || segmentationRuleId.isEmpty()) {
        return visitService.createVisit(branchId, entryPointId, visitParameters, printTicket);
      } else {
        return visitService.createVisit(
            branchId, entryPointId, visitParameters, printTicket, segmentationRuleId);
      }
    }
    throw new BusinessException("services_not_found", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Создаёт визит с дополнительными параметрами и перечнем услуг.
   *
   * @param branchId идентификатор отделения
   * @param entryPointId идентификатор точки создания визита
   * @param parameters параметры визита и список услуг (например, {@code
   *     {"serviceIds":["c3916e7f-7bea-4490-b9d1-0d4064adbe8b","9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"],
   *     "parameters":{"sex":"male","age":"33"}}})
   * @param printTicket необходимость печати талона
   * @param segmentationRuleId идентификатор правила сегментации (опционально)
   * @return визит
   * @throws BusinessException бизнес-ошибка
   * @throws SystemException системная ошибка
   */
  @Post(
      uri = "/branches/{branchId}/entryPoints/{entryPointId}/visitWithParameters",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Создание визита с параметрами",
      description = "Создаёт визит с дополнительными параметрами и перечнем услуг",
      tags = {TAG_VISIT_REGISTRATION},
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит создан"),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        @ApiResponse(
            responseCode = "404",
            description =
                "Отделение, точка создания визитов, услуга, очередь или данные для правила сегментации не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit createVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "2") String entryPointId,
      @Body
          @RequestBody(
              description = "Идентификаторы услуг и дополнительные параметры визита",
              useParameterTypeSchema = true,
              content =
                  @Content(
                      schema =
                          @Schema(
                              example =
                                  // language=JSON
                                  """
   {
     "serviceIds": [
       "c3916e7f-7bea-4490-b9d1-0d4064adbe8b",
       "9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"
     ],
     "parameters": {
       "sex": "male",
       "age": "33"
     }
   }
   """)))
          VisitParameters parameters,
      @QueryValue Boolean printTicket,
      @Nullable @QueryValue String segmentationRuleId)
      throws BusinessException, SystemException {
    Branch branch;
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (new HashSet<>(branch.getServices().values().stream().map(BranchEntity::getId).toList())
        .containsAll(parameters.getServiceIds())) {
      if (segmentationRuleId == null || segmentationRuleId.isEmpty()) {
        return visitService.createVisit(branchId, entryPointId, parameters, printTicket);
      } else {
        return visitService.createVisit(
            branchId, entryPointId, parameters, printTicket, segmentationRuleId);
      }
    }
    throw new BusinessException("services_not_found", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Создаёт визит из зоны ресепшен с учётом дополнительных параметров.
   *
   * @param branchId идентификатор отделения
   * @param printerId идентификатор принтера
   * @param parameters параметры визита и список услуг (например, {@code
   *     {"serviceIds":["c3916e7f-7bea-4490-b9d1-0d4064adbe8b","9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"],
   *     "parameters":{"description":"Визит на получение кредита","age":"48"}}})
   * @param printTicket необходимость печати талона
   * @param segmentationRuleId идентификатор правила сегментации (опционально)
   * @param staffId идентификатор сессии сотрудника (cookie {@code sid})
   * @return визит
   * @throws BusinessException бизнес-ошибка
   * @throws SystemException системная ошибка обработки визита
   */
  @Post(
      uri = "/branches/{branchId}/printer/{printerId}/visitWithParameters",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Создание визита из приёмной",
      description = "Создаёт визит с дополнительными параметрами из зоны ресепшен",
      tags = {TAG_VISIT_REGISTRATION},
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит создан"),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        @ApiResponse(
            responseCode = "404",
            description =
                "Отделение, услуга, очередь или данные для правила сегментации не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit createVisitFromReception(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "eb7ea46d-c995-4ca0-ba92-c92151473614") String printerId,
      @Body
          @RequestBody(
              description = "Идентификаторы услуг и дополнительные параметры визита",
              useParameterTypeSchema = true,
              content =
                  @Content(
                      schema =
                          @Schema(
                              example =
                                  // language=JSON
                                  """
   {
     "serviceIds": [
       "c3916e7f-7bea-4490-b9d1-0d4064adbe8b",
       "9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66"
     ],
     "parameters": {
       "sex": "male",
       "age": "33"
     }
   }
   """)))
          VisitParameters parameters,
      @QueryValue Boolean printTicket,
      @Nullable @QueryValue String segmentationRuleId,
      @Nullable @CookieValue("sid") String staffId)
      throws BusinessException, SystemException {
    Branch branch;
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("branch_not_found", eventService, HttpStatus.NOT_FOUND);
    }
    if (new HashSet<>(branch.getServices().values().stream().map(BranchEntity::getId).toList())
        .containsAll(parameters.getServiceIds())) {
      if (segmentationRuleId == null || segmentationRuleId.isEmpty()) {
        return visitService.createVisitFromReception(
            branchId, printerId, parameters, printTicket, staffId);
      } else {
        return visitService.createVisitFromReception(
            branchId, printerId, parameters, printTicket, segmentationRuleId, staffId);
      }
    }
    throw new BusinessException("services_not_found", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Обновляет параметры визита.
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор визита
   * @param parameterMap карта дополнительных параметров визита
   * @return визит с обновлёнными параметрами
   */
  @Put(
      uri = "/branches/{branchId}/visits/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Обновление параметров визита",
      description = "Назначает или обновляет дополнительные параметры визита",
      tags = {TAG_VISIT_PARAMETERS},
      responses = {
        @ApiResponse(responseCode = "200", description = "Параметры обновлены"),
        @ApiResponse(
            responseCode = "404",
            description = "Отделение или визит с указанным идентификатором не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit setParameterMap(
      @PathVariable String branchId,
      @PathVariable String visitId,
      @Body HashMap<String, String> parameterMap) {
    Visit visit = visitService.getVisit(branchId, visitId);
    visit.setParameterMap(parameterMap);

    branchService.updateVisit(visit, "VISIT_SET_PARAMETER_MAP", visitService);
    return visit;
  }

  /**
   * Возвращает список доступных для обслуживания услуг.
   *
   * <p>Услуга считается доступной, если в отделении присутствует сотрудник, рабочий профиль
   * которого позволяет её выполнить.
   *
   * @param branchId идентификатор отделения
   * @return список доступных услуг
   */
  @Get(uri = "/branches/{branchId}/services", produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Доступные услуги",
      description = "Возвращает список услуг, доступных для обслуживания в отделении",
      tags = {TAG_SERVICE_AVAILABILITY},
      responses = {
        @ApiResponse(responseCode = "200", description = "Список доступных услуг"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public List<Service> getAllAvailableServices(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return services.getAllAvailableServices(branchId);
  }

  /**
   * Возвращает полный перечень услуг отделения.
   *
   * @param branchId идентификатор отделения
   * @return список всех услуг
   */
  @Get(uri = "/branches/{branchId}/services/all", produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Все услуги отделения",
      description = "Возвращает полный список услуг отделения",
      tags = {TAG_SERVICE_CATALOG},
      responses = {
        @ApiResponse(responseCode = "200", description = "Список услуг отделения"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public List<Service> getAllServices(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return services.getAllServices(branchId);
  }
}
