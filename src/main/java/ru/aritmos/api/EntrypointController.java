package ru.aritmos.api;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * @author Pavel Sokolov REST API управления зоной ожидания
 */
@Controller("/entrypoint")
public class EntrypointController {
  /** Служба по работе с услугами */
  @Inject Services services;

  /** Служба по работе с отделениями */
  @Inject BranchService branchService;

  /** Служба для работы с визитами */
  @Inject VisitService visitService;

  /** Служба по отправке событий на шину данных */
  @Inject EventService eventService;

  /**
   * Создание виртуального визита сотрудником
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param serviceIds массив идентификаторов услуг (на пример [
   *     "c3916e7f-7bea-4490-b9d1-0d4064adbe8b","9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66" ] )
   * @param sid идентификатор сессии сотрудника (cookie sid)
   * @return созданный визит
   * @throws SystemException системная ошибка
   */
  @Tag(name = "Зона ожидания")
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/servicePoint/{servicePointId}/virtualVisit",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Создание виртуального визита",
      description = "Создает визит без печати талона",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит создан"),
        @ApiResponse(responseCode = "404", description = "Отделение или услуги не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit createVirtualVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "a66ff6f4-4f4a-4009-8602-0dc278024cf2") String servicePointId,
      @Body
          @RequestBody(
              description = "Массив идентификаторов услуг",
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
          ArrayList<String> serviceIds,
      @Nullable @CookieValue("sid") String sid)
      throws SystemException {
    Branch branch;
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("Branch not found!", eventService, HttpStatus.NOT_FOUND);
    }
    if (new HashSet<>(branch.getServices().values().stream().map(BranchEntity::getId).toList())
        .containsAll(serviceIds)) {

      VisitParameters visitParameters =
          VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
      return visitService.createVirtualVisit(branchId, servicePointId, visitParameters, sid);

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Создание визита.
   *
   * @param branchId идентификатор отделения
   * @param entryPointId идентификатор точки создания визита
   * @param serviceIds массив идентификаторов услуг (на пример [
   *     "c3916e7f-7bea-4490-b9d1-0d4064adbe8b","9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66" ] )
   * @param printTicket флаг печати талона
   * @param segmentationRuleId идентификатор правила сегментации (опционально)
   * @return созданный визит
   * @throws SystemException системная ошибка
   */
  @Tag(name = "Зона ожидания")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/entryPoints/{entryPointId}/visit",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Создание визита",
      description = "Создает визит в отделении",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит создан"),
        @ApiResponse(responseCode = "404", description = "Отделение или услуги не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit createVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "2") String entryPointId,
      @Body
          @RequestBody(
              description = "Массив идентификаторов услуг",
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
          ArrayList<String> serviceIds,
      @QueryValue(defaultValue = "false") Boolean printTicket,
      @Nullable @QueryValue String segmentationRuleId)
      throws SystemException {
    Branch branch;
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("Branch not found!", eventService, HttpStatus.NOT_FOUND);
    }
    if (new HashSet<>(branch.getServices().values().stream().map(BranchEntity::getId).toList())
        .containsAll(serviceIds)) {

      VisitParameters visitParameters =
          VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
      if (segmentationRuleId == null || segmentationRuleId.isEmpty()) {
        return visitService.createVisit(branchId, entryPointId, visitParameters, printTicket);
      } else {
        return visitService.createVisit(
            branchId, entryPointId, visitParameters, printTicket, segmentationRuleId);
      }

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Создание визита с передачей параметров визита и перечня услуг.
   *
   * @param branchId идентификатор отделения
   * @param entryPointId идентификатор точки создания визита
   * @param printTicket флаг печати талона
   * @param parameters услуги и параметры визита (пример { "serviceIds": [
   *     "c3916e7f-7bea-4490-b9d1-0d4064adbe8b", "9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66" ],
   *     "parameters": { "sex": "male", "age": "33" } }
   * @param segmentationRuleId идентификатор правила сегментации (опционально)
   * @return визит
   * @throws SystemException системная ошибка
   */
  @Tag(name = "Зона ожидания")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/entryPoints/{entryPointId}/visitWithParameters",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Создание визита с параметрами",
      description = "Создает визит с дополнительными параметрами",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит создан"),
        @ApiResponse(responseCode = "404", description = "Отделение или услуги не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit createVisit(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "2") String entryPointId,
      @Body
          @RequestBody(
              description = "Идентификаторы услуг и параметры визита",
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
      throws SystemException {
    Branch branch;
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("Branch not found!", eventService, HttpStatus.NOT_FOUND);
    }
    if (new HashSet<>(branch.getServices().values().stream().map(BranchEntity::getId).toList())
        .containsAll(parameters.getServiceIds())) {
      if (segmentationRuleId == null || segmentationRuleId.isEmpty()) {
        return visitService.createVisit(branchId, entryPointId, parameters, printTicket);
      } else {
        return visitService.createVisit(
            branchId, entryPointId, parameters, printTicket, segmentationRuleId);
      }

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Создание визита с передачей параметров визита и перечня услуг из приемной
   *
   * @param branchId идентификатор отделения
   * @param printerId идентификатор принтера
   * @param printTicket флаг печати талона
   * @param parameters услуги и параметры визита (пример { "serviceIds": [
   *     "c3916e7f-7bea-4490-b9d1-0d4064adbe8b", "9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66" ],
   *     "parameters": { "description": "Визит на получение кредита", "age": "48" } }
   * @param segmentationRuleId идентификатор правила сегментации (опционально)
   * @param staffId идентификатор сессии сотрудника (cookie sid)
   * @return визит
   * @throws SystemException системная ошибка обработки визита
   */
  @Tag(name = "Зона ожидания")
  @Tag(name = "Полный список")
  @Post(
      uri = "/branches/{branchId}/printer/{printerId}/visitWithParameters",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Создание визита из приемной",
      description = "Создает визит с параметрами из приемной",
      responses = {
        @ApiResponse(responseCode = "200", description = "Визит создан"),
        @ApiResponse(responseCode = "404", description = "Отделение или услуги не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public Visit createVisitFromReception(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @PathVariable(defaultValue = "eb7ea46d-c995-4ca0-ba92-c92151473614") String printerId,
      @Body
          @RequestBody(
              description = "Идентификаторы услуг и параметры визита",
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
      throws SystemException {
    Branch branch;
    try {
      branch = branchService.getBranch(branchId);
    } catch (Exception ex) {
      throw new BusinessException("Branch not found!", eventService, HttpStatus.NOT_FOUND);
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

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление параметров в визит
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор визита
   * @param parameterMap набор параметров
   * @return визит
   */
  @Tag(name = "Зона ожидания")
  @Tag(name = "Полный список")
  @Put(
      uri = "/branches/{branchId}/visits/{visitId}",
      consumes = "application/json",
      produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Добавление параметров визита",
      description = "Устанавливает параметры визита",
      responses = {
        @ApiResponse(responseCode = "200", description = "Параметры установлены"),
        @ApiResponse(responseCode = "404", description = "Отделение или визит не найдены"),
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
   * Получение списка доступных услуг Услуга считается доступной, если на рабочем месте присутствует
   * сотрудник чей рабочий профиль позволяет обслужить данную услугу
   *
   * @param branchId идентификатор отделения
   * @return список услуг
   */
  @Tag(name = "Зона ожидания")
  @Tag(name = "Полный список")
  @Get(uri = "/branches/{branchId}/services", produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Доступные услуги",
      description = "Возвращает список доступных услуг отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список услуг"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public List<Service> getAllAvilableServies(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    try {

      return services.getAllAvailableServices(branchId);

    } catch (BusinessException ex) {
      if (ex.getMessage().contains("not found")) {
        throw new HttpStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
      } else {
        throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
      }
    }
  }

  /**
   * Получение списка всех услуг
   *
   * @param branchId идентификатор отделения
   * @return список услуг
   */
  @Tag(name = "Зона ожидания")
  @Tag(name = "Зона обслуживания")
  @Tag(name = "Полный список")
  @Get(uri = "/branches/{branchId}/services/all", produces = "application/json")
  @ExecuteOn(TaskExecutors.IO)
  @Operation(
      summary = "Все услуги отделения",
      description = "Возвращает полный список услуг отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список услуг"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  public List<Service> getAllServices(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    try {

      return services.getAllServices(branchId);

    } catch (BusinessException ex) {
      if (ex.getMessage().contains("not found")) {
        throw new HttpStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
      } else {
        throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
      }
    }
  }
}
