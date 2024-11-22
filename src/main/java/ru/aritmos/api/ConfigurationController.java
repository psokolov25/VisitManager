package ru.aritmos.api;

import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.aritmos.model.*;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.VisitService;

/**
 * @author Pavel Sokolov REST API управления конфигурацией отделений
 */
@Controller("/configuration")
public class ConfigurationController {
  @Inject BranchService branchService;
  @Inject VisitService visitService;

  /**
   * Обновление конфигурации отделений
   *
   * @param branchHashMap список отделений
   * @return список отделений
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Post(uri = "/branches")
  public Map<String, Branch> update(@Body Map<String, Branch> branchHashMap) {

    branchService
        .getBranches()
        .forEach(
            (key, value) -> {
              if (!branchHashMap.containsKey(key)) {
                branchService.delete(key);
              }
            });
    branchHashMap.forEach((key, value) -> branchService.add(key, value));
    return branchService.getBranches();
  }

  /**
   * Добавление или обновление услуг
   *
   * @param branchId идентификатор подразделения
   * @param serviceHashMap список услуг
   * @param checkVisits флаг учитывания визитов при обновлении услуг (при наличии услуги в кааком
   *     нибудь визите - она обновляется)
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/services")
  public void addUpdateService(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, Service> serviceHashMap,
      @QueryValue Boolean checkVisits) {
    branchService.addUpdateService(branchId, serviceHashMap, checkVisits, visitService);
  }

  /**
   * Список причин перерыва
   *
   * @param branchId идентификатор подразделения
   * @return список причин перерыва
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Перерывы")
  @Tag(name = "Полный список")
  @Get(uri = "/branches/{branchId}/break/reasons")
  public HashMap<String, String> getBreakReasons(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return branchService.getBranch(branchId).getBreakReasons();
  }

  /**
   * Удаление услуг
   *
   * @param branchId идентификатор подразделения
   * @param serviceIds список идентификаторов услуг
   * @param checkVisits флаг учитывания визитов при обновлении услуг (при наличии услуги в кааком
   *     нибудь визите - она удаляется)
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Delete(uri = "/branches/{branchId}/services")
  public void deleteServices(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body List<String> serviceIds,
      @QueryValue Boolean checkVisits) {
    branchService.deleteServices(branchId, serviceIds, checkVisits, visitService);
  }

  /**
   * Добавление или обновление точек обслуживания
   *
   * @param branchId идентификатор подразделения
   * @param servicePointHashMap список услуг
   * @param restoreUser флаг восстановления сотрудника после обновления
   * @param restoreVisit флаг восстановления визита после обновления
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/servicePoints")
  public void addUpdateServicePoint(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, ServicePoint> servicePointHashMap,
      @QueryValue Boolean restoreVisit,
      @QueryValue Boolean restoreUser) {
    branchService.addUpdateServicePoint(branchId, servicePointHashMap, restoreVisit, restoreUser);
  }

  /**
   * Добавление или обновление точек обслуживания
   *
   * @param branchId идентификатор подразделения
   * @param serviceGroupHashMap список групп услуг
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Tag(name = "Группы услуг")
  @Put(uri = "/branches/{branchId}/serviceGroups")
  public void addUpdateServiceGroups(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, ServiceGroup> serviceGroupHashMap) {

    branchService.addUpdateServiceGroups(branchId, serviceGroupHashMap);
  }

  /**
   * Добавление или обновление правил сегментации
   *
   * @param branchId идентификатор подразделения
   * @param segmentationRuleDataHashMap список правил сегментации
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Tag(name = "Правила сегментации")
  @Put(uri = "/branches/{branchId}/segmentationRules")
  public void addUpdateSegmentationRules(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, SegmentationRuleData> segmentationRuleDataHashMap) {

    branchService.addUpdateSegentationRules(branchId, segmentationRuleDataHashMap);
  }

  /**
   * Удаление точек обслуживания
   *
   * @param branchId идентификатор подразделения
   * @param servicePointIds список идентификаторов точек обслуживания
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Delete(uri = "/branches/{branchId}/servicePoints")
  public void deleteServicePoints(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body List<String> servicePointIds) {
    branchService.deleteServicePoints(branchId, servicePointIds);
  }

  /**
   * Включение авовызова для отделения
   *
   * @param branchId идентификатор отделения *
   * @return @return отделение
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/autocallModeOn")
  public Optional<Branch> setAutoCallModeOfBranchOn(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.setAutoCallModeOfBranch(branchId, true);
  }

  /**
   * Выключение авовызова для отделения
   *
   * @param branchId идентификатор отделения *
   * @return @return отделение
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/autocallModeOff")
  public Optional<Branch> setAutoCallModeOfBranchOff(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.setAutoCallModeOfBranch(branchId, false);
  }

  /**
   * Добавление или обновление очередей
   *
   * @param branchId идентификатор подразделения
   * @param queueHashMap список услуг *
   * @param restoreVisits флаг восстановления визита после обновления
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/queues")
  public void addUpdateQueues(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body HashMap<String, Queue> queueHashMap,
      @QueryValue Boolean restoreVisits) {
    branchService.addUpdateQueues(branchId, queueHashMap, restoreVisits);
  }

  /**
   * Удаление очередей
   *
   * @param branchId идентификатор подразделения
   * @param queueIds список идентификаторов очередей
   */
  @Tag(name = "Конфигурация отделений (в разработке!)")
  @Tag(name = "Полный список")
  @Delete(uri = "/branches/{branchId}/queues")
  public void deleteQueues(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId,
      @Body List<String> queueIds) {
    branchService.deleteQueues(branchId, queueIds);
  }
}
