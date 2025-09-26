package ru.aritmos.api;

import groovy.util.logging.Slf4j;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.aritmos.model.*;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Configuration;
import ru.aritmos.service.VisitService;

/**
 * REST API управления конфигурацией отделений.
 *
 * <p>Контроллер предоставляет эндпоинты для публикации конфигураций, обновления справочников и
 * синхронизации услуг с локальной базой визитов. Используется как в ежедневной эксплуатации, так и
 * при подготовке демонстрационных данных.
 */
@lombok.extern.slf4j.Slf4j
@Controller("/configuration")
@Slf4j
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
  @ApiResponse(responseCode = "401", description = "Не авторизован"),
  @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
  @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
  @ApiResponse(responseCode = "405", description = "Метод не поддерживается"),
  @ApiResponse(responseCode = "415", description = "Неподдерживаемый тип данных"),
  @ApiResponse(responseCode = "500", description = "Ошибка сервера")
})
public class ConfigurationController {
  /** Сервис управления отделениями. */
  @Inject BranchService branchService;

  /** Сервис визитов. */
  @Inject VisitService visitService;

  /** Сервис формирования конфигурации. */
  @Inject Configuration configuration;

  /**
   * Создаёт или обновляет конфигурацию отделений.
   *
   * @param branchHashMap набор отделений, индексированных по идентификатору
   * @return актуализированная конфигурация отделений
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Operation(
      operationId = "updateBranchConfiguration",
      summary = "Обновление конфигурации отделений",
      description = "Создает или обновляет конфигурацию отделений",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Конфигурация обновлена",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            type = "object",
                            additionalProperties =
                                @Schema(implementation = Branch.class)))),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post(uri = "/branches")
  public Map<String, Branch> update(@Body Map<String, Branch> branchHashMap) {
    return configuration.createBranchConfiguration(branchHashMap);
  }

  /**
   * Публикует демонстрационную конфигурацию отделений.
   *
   * @return конфигурация отделений из демо-данных
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Operation(
      operationId = "publishDemoConfiguration",
      summary = "Обновление демо-конфигурации отделений",
      description = "Создает конфигурацию отделений на основе демо-данных",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Конфигурация обновлена",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            type = "object",
                            additionalProperties =
                                @Schema(implementation = Branch.class)))),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Post(uri = "/branches/hardcode")
  public Map<String, Branch> update() {

    return configuration.createBranchConfiguration(configuration.createDemoBranch());
  }

  /**
   * Добавляет новые услуги отделения или обновляет существующие.
   *
   * @param branchId идентификатор отделения
   * @param serviceHashMap карта услуг по идентификатору
   * @param checkVisits учитывать ли существующие визиты при обновлении услуг
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Operation(
      operationId = "addOrUpdateServices",
      summary = "Добавление или обновление услуг",
      description = "Создает или изменяет услуги отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Услуги обновлены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(uri = "/branches/{branchId}/services")
  public void addUpdateService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, Service> serviceHashMap,
      @QueryValue Boolean checkVisits) {
    branchService.addUpdateService(branchId, serviceHashMap, checkVisits, visitService);
  }

  /**
   * Возвращает список причин перерыва для отделения.
   *
   * @param branchId идентификатор отделения
   * @return карта причин перерыва по идентификатору
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Перерывы")
  @Tag(name = "Полный список")
  @Operation(
      operationId = "getBreakReasons",
      summary = "Получение списка причин перерыва",
      description = "Возвращает перечень причин перерыва для отделения",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Список причин перерыва",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            type = "object",
                            additionalProperties = @Schema(implementation = String.class)))),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(uri = "/branches/{branchId}/break/reasons")
  public HashMap<String, String> getBreakReasons(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return branchService.getBranch(branchId).getBreakReasons();
  }

  /**
   * Удаляет услуги отделения и при необходимости учитывает активные визиты.
   *
   * @param branchId идентификатор отделения
   * @param serviceIds список идентификаторов услуг
   * @param checkVisits учитывать ли существующие визиты при удалении услуг
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Operation(
      operationId = "deleteServices",
      summary = "Удаление услуг",
      description = "Удаляет услуги отделения",
      responses = {
        @ApiResponse(responseCode = "204", description = "Услуги удалены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Услуги не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Delete(uri = "/branches/{branchId}/services")
  @Status(HttpStatus.NO_CONTENT)
  public void deleteServices(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body List<String> serviceIds,
      @QueryValue Boolean checkVisits) {
    branchService.deleteServices(branchId, serviceIds, checkVisits, visitService);
  }

  /**
   * Добавляет точки обслуживания или обновляет их параметры.
   *
   * @param branchId идентификатор отделения
   * @param servicePointHashMap карта точек обслуживания по идентификатору
   * @param restoreUser восстановить ли назначенных сотрудников после обновления
   * @param restoreVisit восстановить ли активные визиты после обновления
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление или обновление точек обслуживания",
      description = "Создает или изменяет точки обслуживания отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Точки обслуживания обновлены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(uri = "/branches/{branchId}/servicePoints")
  public void addUpdateServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, ServicePoint> servicePointHashMap,
      @QueryValue Boolean restoreVisit,
      @QueryValue Boolean restoreUser) {
    branchService.addUpdateServicePoint(branchId, servicePointHashMap, restoreVisit, restoreUser);
  }

  /**
   * Добавляет или обновляет группы услуг отделения.
   *
   * @param branchId идентификатор отделения
   * @param serviceGroupHashMap карта групп услуг
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Tag(name = "Группы услуг")
  @Operation(
      summary = "Добавление или обновление групп услуг",
      description = "Создает или изменяет группы услуг отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Группы услуг обновлены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Указанная услуга не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(uri = "/branches/{branchId}/serviceGroups")
  public void addUpdateServiceGroups(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, ServiceGroup> serviceGroupHashMap) {

    branchService.addUpdateServiceGroups(branchId, serviceGroupHashMap);
  }

  /**
   * Добавляет или обновляет правила сегментации отделения.
   *
   * @param branchId идентификатор отделения
   * @param segmentationRuleDataHashMap карта правил сегментации
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Tag(name = "Правила сегментации")
  @Operation(
      summary = "Добавление или обновление правил сегментации",
      description = "Создает или изменяет правила сегментации отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Правила сегментации обновлены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Группа услуг для правила не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(uri = "/branches/{branchId}/segmentationRules")
  public void addUpdateSegmentationRules(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, SegmentationRuleData> segmentationRuleDataHashMap) {

    branchService.addUpdateSegmentationRules(branchId, segmentationRuleDataHashMap);
  }

  /**
   * Удаляет точки обслуживания отделения.
   *
   * @param branchId идентификатор отделения
   * @param servicePointIds список идентификаторов точек обслуживания
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Удаление точек обслуживания",
      description = "Удаляет точки обслуживания отделения",
      responses = {
        @ApiResponse(responseCode = "204", description = "Точки обслуживания удалены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Точки обслуживания не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Delete(uri = "/branches/{branchId}/servicePoints")
  @Status(HttpStatus.NO_CONTENT)
  public void deleteServicePoints(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body List<String> servicePointIds) {
    branchService.deleteServicePoints(branchId, servicePointIds);
  }

  /**
   * Включает автоматический вызов визитов для отделения.
   *
   * @param branchId идентификатор отделения
   * @return обновлённое состояние отделения
   */
  @Tag(name = "Автоматический вызов")
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Включение автоматического вызова",
      description = "Активирует режим авто вызова для отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Режим включен"),
        @ApiResponse(responseCode = "207", description = "Режим уже включен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(uri = "/branches/{branchId}/autocallModeOn")
  public Optional<Branch> setAutoCallModeOfBranchOn(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.setAutoCallModeOfBranch(branchId, true);
  }

  /**
   * Выключает автоматический вызов визитов для отделения.
   *
   * @param branchId идентификатор отделения
   * @return обновлённое состояние отделения
   */
  @Tag(name = "Автоматический вызов")
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Выключение автоматического вызова",
      description = "Деактивирует режим авто вызова для отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Режим выключен"),
        @ApiResponse(responseCode = "207", description = "Режим уже выключен"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(uri = "/branches/{branchId}/autocallModeOff")
  public Optional<Branch> setAutoCallModeOfBranchOff(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.setAutoCallModeOfBranch(branchId, false);
  }

  /**
   * Добавляет или обновляет очереди отделения.
   *
   * @param branchId идентификатор отделения
   * @param queueHashMap карта очередей по идентификатору
   * @param restoreVisits восстановить ли активные визиты после обновления
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Добавление или обновление очередей",
      description = "Создает или изменяет очереди отделения",
      responses = {
        @ApiResponse(responseCode = "200", description = "Очереди обновлены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Put(uri = "/branches/{branchId}/queues")
  public void addUpdateQueues(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, Queue> queueHashMap,
      @QueryValue Boolean restoreVisits) {
    branchService.addUpdateQueues(branchId, queueHashMap, restoreVisits);
  }

  /**
   * Удаляет очереди отделения.
   *
   * @param branchId идентификатор отделения
   * @param queueIds список идентификаторов очередей
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Удаление очередей",
      description = "Удаляет очереди отделения",
      responses = {
        @ApiResponse(responseCode = "204", description = "Очереди удалены"),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "404", description = "Очереди не найдены"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Delete(uri = "/branches/{branchId}/queues")
  @Status(HttpStatus.NO_CONTENT)
  public void deleteQueues(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body List<String> queueIds) {
    branchService.deleteQueues(branchId, queueIds);
  }
}
