package ru.aritmos.api;

import groovy.util.logging.Slf4j;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.*;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Configuration;
import ru.aritmos.service.VisitService;

/**
 * @author Pavel Sokolov REST API управления конфигурацией отделений
 */
@lombok.extern.slf4j.Slf4j
@Controller("/configuration")
@Slf4j
public class ConfigurationController {
  @Inject BranchService branchService;
  @Inject Configuration configuration;
  @Inject VisitService visitService;
  @Inject EventService eventService;

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
    Event eventPublicStart =
        Event.builder()
            .eventType("PUBLIC_STARTED")
            .eventDate(ZonedDateTime.now())
            .body(branchHashMap)
            .build();
    eventService.send("stat", false, eventPublicStart);
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    //
    //    branchService
    //        .getBranches()
    //        .forEach(
    //            (key, value) -> {
    //              if (!branchHashMap.containsKey(key)) {
    //                Branch body = branchHashMap.get(key);
    //                branchService.delete(key, visitService);
    //                Event eventDeleted =
    //                    Event.builder()
    //                        .eventType("PUBLIC_BRANCH_DELETED")
    //                        .eventDate(ZonedDateTime.now())
    //                        .body(body)
    //                        .params(Map.of("branchId", key))
    //                        .build();
    //                eventService.send("stat", false, eventDeleted);
    //              }
    //            });
    branchHashMap.forEach(
        (key, value) -> {

          branchService.delete(key, visitService);
          branchService.add(key, value);
          Event eventDeleted =
              Event.builder()
                  .eventType("BRANCH_PUBLIC_COMPLETE")
                  .eventDate(ZonedDateTime.now())
                  .body(value)
                  .params(Map.of("branchId", key))
                  .build();
          eventService.send("stat", false, eventDeleted);
          try {
            Thread.sleep(20);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
    Event eventPublicFinished =
        Event.builder()
            .eventType("PUBLIC_COMPLETE")
            .eventDate(ZonedDateTime.now())
            .body(branchHashMap)
            .build();
    eventService.send("stat", false, eventPublicFinished);
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return branchService.getBranches();
  }

  /**
   * Обновление "захардкоженной" конфигурации отделений
   *
   * @return список отделений
   */
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Post(uri = "/branches/hardcode")
  public Map<String, Branch> update() {

    HashMap<String, Branch> branchHashMap = new HashMap<>();
    HashMap<String, Branch> oldBranchHashMap = new HashMap<>();
    branchService
        .getBranches()
        .forEach(
            (key, value) ->
                oldBranchHashMap.put(key, branchService.getDetailedBranches().get(key)));
    Event eventPublicStart =
        Event.builder()
            .eventType("PUBLIC_STARTED")
            .eventDate(ZonedDateTime.now())
            .body(oldBranchHashMap)
            .build();
    eventService.send(List.of("branchconfigurer", "frontend", "stat"), false, eventPublicStart);
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    //
    //    branchService
    //        .getBranches()
    //        .forEach((key, value) -> oldBranchHashMap.put(key,
    // branchService.getDetailedBranches().get(key)));
    //    branchService
    //        .getBranches()
    //        .forEach(
    //            (key, value) -> {
    //              try {
    //                branchService.delete(key, visitService);
    //                Event eventDeleted =
    //                        Event.builder()
    //                                .eventType("PUBLIC_BRANCH_DELETED")
    //                                .eventDate(ZonedDateTime.now())
    //                                .body(value)
    //                                .params(Map.of("branchId", key))
    //                                .build();
    //                eventService.send(List.of("branchconfigurer", "frontend","stat"), false,
    // eventDeleted);
    //              } catch (Exception e) {
    //                log.warn("Branch {} not found", key);
    //              }
    //            });
    configuration.getCleanedConfiguration();

    branchService
        .getBranches()
        .forEach(
            (key, value) -> {
              branchHashMap.put(key, branchService.getDetailedBranches().get(key));
              Event eventDeleted =
                  Event.builder()
                      .eventType("BRANCH_PUBLIC_COMPLETE")
                      .eventDate(ZonedDateTime.now())
                      .body(branchService.getDetailedBranches().get(key))
                      .params(Map.of("branchId", key))
                      .build();
              eventService.send(
                  List.of("branchconfigurer", "frontend", "stat"), false, eventDeleted);
              try {
                Thread.sleep(20);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    Event eventPublicFinished =
        Event.builder()
            .eventType("PUBLIC_COMPLETE")
            .eventDate(ZonedDateTime.now())
            .body(branchHashMap)
            .build();
    eventService.send(List.of("branchconfigurer", "frontend", "stat"), false, eventPublicFinished);
    return branchHashMap;
  }

  /**
   * Добавление или обновление услуг
   *
   * @param branchId идентификатор подразделения
   * @param serviceHashMap список услуг
   * @param checkVisits флаг учета визитов при обновлении услуг (при наличии услуги в каком нибудь
   *     визите - она обновляется)
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
   * @param checkVisits флаг учета визитов при обновлении услуг (при наличии услуги в каком нибудь
   *     визите - она удаляется)
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

    branchService.addUpdateSegmentationRules(branchId, segmentationRuleDataHashMap);
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
   * Включение автоматического вызова для отделения
   *
   * @param branchId идентификатор отделения *
   * @return @return отделение
   */
  @Tag(name = "Автоматический вызов")
  @Tag(name = "Конфигурация отделений")
  @Tag(name = "Полный список")
  @Put(uri = "/branches/{branchId}/autocallModeOn")
  public Optional<Branch> setAutoCallModeOfBranchOn(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String branchId) {
    return visitService.setAutoCallModeOfBranch(branchId, true);
  }

  /**
   * Выключение автоматического вызова для отделения
   *
   * @param branchId идентификатор отделения *
   * @return @return отделение
   */
  @Tag(name = "Автоматический вызов")
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
