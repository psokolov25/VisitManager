package ru.aritmos.service;

import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.interceptor.ParametersKey;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.SerdeImport;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.*;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

/** Служба отвечающая за работу с отделениями */
@Slf4j
@Singleton
@Named("Branch_cache")
@CacheConfig("branches")
@SerdeImport(ParametersKey.class)
public class BranchService {

  /** Текущее состояние отделений (id -> отделение). */
  HashMap<String, Branch> branches = new HashMap<>();
  /** Сервис отправки событий. */
  @Inject EventService eventService;
  /** Клиент Keycloak для работы с пользователями. */
  @Inject KeyCloackClient keyCloackClient;

  /**
   * Получение детальной информации об отделении по идентификатору.
   *
   * @param key идентификатор отделения
   * @return отделение
   * @throws BusinessException если отделение не найдено
   */
  @CachePut(parameters = {"key"})
  public Branch getBranch(String key) throws BusinessException {

    Branch branch = branches.get(key);
    if (branch == null) {
      throw new BusinessException("Branch not found!!", eventService, HttpStatus.NOT_FOUND);
    }
    log.info("Getting branchInfo {}", branch);
    return branch;
  }

  // @Cacheable(parameters = {"id"}, value = {"branches"})
  /**
   * Получение списка отделений (без детальной информации).
   *
   * @return карта отделений (id -> отделение)
   */
  public HashMap<String, Branch> getBranches() {

    HashMap<String, Branch> result = new HashMap<>();
    branches
        .values()
        .forEach(
            f -> {
              Branch branch = new Branch(f.getId(), f.getName());
              branch.setPrefix(f.getPrefix());
              result.put(branch.getId(), branch);
            });
    return result;
  }

  /**
   * Получение списка отделений с детальной информацией.
   *
   * @return карта отделений (id -> отделение)
   */
  public HashMap<String, Branch> getDetailedBranches() {

    HashMap<String, Branch> result = new HashMap<>();
    branches
        .values()
        .forEach(
            f -> {
              Branch branch = getBranch(f.getId());
              result.put(branch.getId(), branch);
            });
    return result;
  }

  /**
   * Создание или обновление отделения.
   *
   * Отправляет событие об изменении сущности.
   *
   * @param key идентификатор отделения
   * @param value модель отделения
   * @return сохранённое отделение
   */
  @CachePut(parameters = {"key"})
  public Branch add(String key, Branch value) {

    Branch oldBranch;

    if (this.branches.containsKey(key)) {
      oldBranch = this.branches.get(key);
      eventService.sendChangedEvent(
          "config", true, oldBranch, value, new HashMap<>(), "BRANCH_CHANGED");
      // eventService.sendChangedEvent("*", true, oldBranch, value, new HashMap<>(), "CHANGED");

    } else {
      eventService.sendChangedEvent("config", true, null, value, new HashMap<>(), "BRANCH_CREATED");
      // eventService.sendChangedEvent("*", true, null, value, new HashMap<>(), "CREATED");
    }
    value.getQueues().forEach((key1, value2) -> value2.setBranchId(key));
    value.getServicePoints().forEach((key1, value2) -> value2.setBranchId(key));
    value.getEntryPoints().forEach((key1, value2) -> value2.setBranchId(key));
    value.getServices().forEach((key1, value2) -> value2.setBranchId(key));
    value.getWorkProfiles().forEach((key1, value2) -> value2.setBranchId(key));
    value.getServiceGroups().forEach((key1, value2) -> value2.setBranchId(key));
    value.getReception().setBranchId(key);
    branches.put(key, value);

    log.info("Putting branchInfo {}", value);
    return value;
  }

  /**
   * Проверка на наличие отделения в списке отделений по ключу
   *
   * @param key ключ отделения
   * @return флаг существования отделения
   */
  public Boolean branchExists(String key) {
    try {
      getBranch(key);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Удаление отделения.
   *
   * @param key идентификатор отделения
   * @param visitService сервис визитов (для корректного закрытия точек)
   * @throws BusinessException если отделение не найдено
   */
  @CacheInvalidate
  public void delete(String key, VisitService visitService) {
    Branch oldBranch;
    if (this.branches.containsKey(key)) {
      oldBranch = this.branches.get(key);
      oldBranch
          .getServicePoints()
          .forEach(
              (key1, value) -> {
                if (oldBranch.getServicePoints().get(key1).getUser() != null) {
                  closeServicePoint(
                      oldBranch.getId(),
                      key1,
                      visitService,
                      true,
                      false,
                      "",
                      true,
                      "BRANCH_DELETED");
                }
              });
      eventService.sendChangedEvent(
          "config", true, oldBranch, null, new HashMap<>(), "BRANCH_DELETED");
      // eventService.sendChangedEvent("*", true, oldBranch, null, new HashMap<>(), "DELETED");

    } else {
      throw new BusinessException("Branch not found!!", eventService, HttpStatus.NOT_FOUND);
    }
    log.info("Deleting branchInfo {}", key);
    branches.remove(key);
  }

  /**
   * Обновление визита и рассылка соответствующего события.
   *
   * @param visit визит
   * @param action действие для имени события
   * @param visitService сервис визитов
   */
  public void updateVisit(Visit visit, String action, VisitService visitService) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, action, visitService);
  }

  /**
   * Обновление визита по событию.
   *
   * @param visit визит
   * @param visitEvent событие визита
   * @param visitService сервис визитов
   */
  public void updateVisit(Visit visit, VisitEvent visitEvent, VisitService visitService) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService);
  }

  /**
   * Обновление визита по событию с возможностью задать начало списка.
   *
   * @param visit визит
   * @param visitEvent событие визита
   * @param visitService сервис визитов
   * @param isToStart поместить визит в начало списка очереди/пула
   */
  public void updateVisit(
      Visit visit, VisitEvent visitEvent, VisitService visitService, Boolean isToStart) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService, isToStart);
  }

  /**
   * Обновление визита по событию с указанием позиции.
   *
   * @param visit визит
   * @param visitEvent событие визита
   * @param visitService сервис визитов
   * @param index позиция вставки (или -1 для добавления в конец)
   */
  public void updateVisit(
      Visit visit, VisitEvent visitEvent, VisitService visitService, Integer index) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService, index);
  }

  /**
   * Смена рабочего профиля пользователя на точке обслуживания.
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param workProfileId идентификатор рабочего профиля
   * @return пользователь с обновлённым рабочим профилем
   * @throws BusinessException если профиль, точка или пользователь не найдены
   */
  public User changeUserWorkProfileInServicePoint(
      String branchId, String servicePointId, String workProfileId) throws BusinessException {
    Branch branch = this.getBranch(branchId);
    if (!branch.getWorkProfiles().containsKey(workProfileId)) {
      throw new BusinessException("Work profile not found!", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getServicePoints().containsKey(servicePointId)) {
      throw new BusinessException("Service point not found!", eventService, HttpStatus.NOT_FOUND);
    }
    User user = branch.getServicePoints().get(servicePointId).getUser();
    if (user == null) {
      throw new BusinessException(
          "User not found in Service point!", eventService, HttpStatus.NOT_FOUND);
    }
    String oldWorkProfileId = user.getCurrentWorkProfileId();
    branch.getServicePoints().get(servicePointId).getUser().setCurrentWorkProfileId(workProfileId);

    this.add(branch.getId(), branch);
    checkWorkProfileChange(workProfileId, user, oldWorkProfileId);
    return branch.getServicePoints().get(servicePointId).getUser();
  }

  /**
   * Открытие точки обслуживания пользователем.
   *
   * @param branchId идентификатор отделения
   * @param userName логин пользователя
   * @param servicePointId идентификатор точки обслуживания
   * @param workProfileId идентификатор рабочего профиля
   * @param visitService сервис визитов
   * @return пользователь, открывший точку
   * @throws IOException при ошибке интеграции
   * @throws BusinessException при нарушении бизнес‑правил
   */
  public User openServicePoint(
      String branchId,
      String userName,
      String servicePointId,
      String workProfileId,
      VisitService visitService)
      throws BusinessException, IOException {
    Branch branch = this.getBranch(branchId);
    if (!branch.getWorkProfiles().containsKey(workProfileId)) {
      throw new BusinessException("Work profile not found!!", eventService, HttpStatus.NOT_FOUND);
    }
    if (!branch.getServicePoints().containsKey(servicePointId)) {
      throw new BusinessException("Service point not found!!", eventService, HttpStatus.NOT_FOUND);
    }
    Optional<UserRepresentation> userInfo = Optional.empty();

    try {
      userInfo = keyCloackClient.getUserInfo(userName);

    } catch (Exception ex) {
      log.warn("User not found!!", ex);
    }
    branch.getServicePoints().values().stream()
        .filter(
            f ->
                f.getUser() != null
                    && f.getUser().getName().equals(userName)
                    && !f.getId().equals(servicePointId))
        .forEach(
            servicePoint ->
                closeServicePoint(
                    branchId, servicePoint.getId(), visitService, false, false, "", false, ""));

    if (branch.getUsers().containsKey(userName)) {
      User user = branch.getUsers().get(userName);
      if (userInfo.isPresent()) {
        user.setId(userInfo.get().getId());
        user.setEmail(userInfo.get().getEmail());
        user.setFirstName(userInfo.get().getFirstName());
        user.setLastName(userInfo.get().getLastName());
        user.setAllBranches(keyCloackClient.getAllBranchesOfUser(userName));
        user.setIsAdmin(keyCloackClient.isUserModuleTypeByUserName(userName, "admin"));
      }
      if (user.getLastBreakStartTime() != null && user.getLastBreakEndTime() == null) {

        user.setLastBreakEndTime(ZonedDateTime.now());
        eventService.send(
            "*",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("STAFF_END_BREAK")
                .params(new HashMap<>())
                .body(user)
                .build());
        eventService.send(
            "frontend",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("STAFF_END_BREAK")
                .params(new HashMap<>())
                .body(user)
                .build());

        eventService.send(
            "stat",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("STAFF_END_BREAK")
                .params(new HashMap<>())
                .body(user)
                .build());
      }
      String oldServicePointId = user.getServicePointId() != null ? user.getServicePointId() : "";
      user.setServicePointId(servicePointId);
      checkServicePointChange(servicePointId, user, oldServicePointId);
      String oldWorkProfileId =
          user.getCurrentWorkProfileId() != null ? user.getCurrentWorkProfileId() : "";
      checkWorkProfileChange(workProfileId, user, oldWorkProfileId);
      if (!user.getIsAdmin()
          && user.getAllBranches().stream()
              .noneMatch(
                  f ->
                      f.getAttributes().containsKey("branchPrefix")
                          && f.getAttributes().get("branchPrefix").contains(branch.getPrefix()))) {

        throw new BusinessException(
            String.format(
                "User %s dont have permissions to access in branch '%s'!",
                userName, branch.getName()),
            eventService,
            HttpStatus.valueOf(403));
      }
      branch.openServicePoint(user, eventService);
      this.add(branch.getId(), branch);
      return branch.getUsers().get(userName);
    } else {

      User user = new User(userName, keyCloackClient);
      if (userInfo.isPresent()) {
        user.setId(userInfo.get().getId());
        user.setEmail(userInfo.get().getEmail());
        user.setFirstName(userInfo.get().getFirstName());
        user.setLastName(userInfo.get().getLastName());
      } else {
        user.setFirstName("Отсутствует");
        user.setLastName("Отсутствует");
      }
      user.setBranchId(branchId);
      String oldServicePointId = user.getServicePointId() != null ? user.getServicePointId() : "";
      user.setServicePointId(servicePointId);

      String oldWorkProfileId =
          user.getCurrentWorkProfileId() != null ? user.getCurrentWorkProfileId() : "";
      user.setServicePointId(servicePointId);
      checkServicePointChange(servicePointId, user, oldServicePointId);
      checkWorkProfileChange(workProfileId, user, oldWorkProfileId);
      branch.openServicePoint(user, eventService);
      this.add(branch.getId(), branch);
      return user;
    }
  }

  /**
   * Отправка события о смене точки обслуживания пользователем.
   *
   * @param servicePointId новая точка обслуживания
   * @param user пользователь
   * @param oldServicePointId предыдущая точка обслуживания
   */
  private void checkServicePointChange(String servicePointId, User user, String oldServicePointId) {
    if (!oldServicePointId.equals(servicePointId)) {
      eventService.send(
          "stat",
          false,
          Event.builder()
              .eventDate(ZonedDateTime.now())
              .eventType("USER_SERVICE_POINT_CHANGED")
              .params(new HashMap<>())
              .body(
                  new HashMap<>(
                      Map.ofEntries(
                          Map.entry("userName", user.getName()),
                          Map.entry("userId", user.getId()),
                          Map.entry("oldServicePointId", oldServicePointId),
                          Map.entry("newServicePointId", servicePointId))))
              .build());
    }
  }

  /**
   * Отправка события о смене рабочего профиля пользователем.
   *
   * @param workProfileId новый рабочий профиль
   * @param user пользователь
   * @param oldWorkProfileId предыдущий рабочий профиль
   */
  private void checkWorkProfileChange(String workProfileId, User user, String oldWorkProfileId) {
    user.setCurrentWorkProfileId(workProfileId);
    if (!oldWorkProfileId.equals(workProfileId)) {
      eventService.send(
          "stat",
          false,
          Event.builder()
              .eventDate(ZonedDateTime.now())
              .eventType("USER_WORK_PROFILE_CHANGED")
              .params(new HashMap<>())
              .body(
                  new HashMap<>(
                      Map.ofEntries(
                          Map.entry("userName", user.getName()),
                          Map.entry("userId", user.getId()),
                          Map.entry("oldWorkProfileId", oldWorkProfileId),
                          Map.entry("newWorkProfileId", workProfileId))))
              .build());
    }
  }

  /**
   * Закрытие точки обслуживания.
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visitService сервис визитов
   * @param isWithLogout флаг завершения сессии пользователя
   * @param isBreak флаг начала перерыва
   * @param breakReason причина перерыва
   * @param isForced принудительное закрытие визита
   * @param reason причина принудительного закрытия
   */
  public void closeServicePoint(
      String branchId,
      String servicePointId,
      VisitService visitService,
      Boolean isWithLogout,
      Boolean isBreak,
      String breakReason,
      Boolean isForced,
      String reason) {

    Branch branch = this.getBranch(branchId);
    branch.closeServicePoint(
        servicePointId,
        eventService,
        visitService,
        isWithLogout,
        isBreak,
        breakReason,
        isForced,
        reason);
    this.add(branch.getId(), branch);
  }

  /**
   * Получение пользователей отделения.
   *
   * @param branchId идентификатор отделения
   * @return карта пользователей (логин -> пользователь)
   */
  public HashMap<String, User> getUsers(String branchId) {
    Branch branch = this.getBranch(branchId);

    return branch.getUsers();
  }

  /**
   * Инкремент счётчика талонов очереди.
   *
   * @param branchId идентификатор отделения
   * @param queue очередь
   * @return новое значение счётчика либо -1, если очередь не найдена
   */
  public Integer incrementTicketCounter(String branchId, Queue queue) {

    Branch branch = this.getBranch(branchId);
    Integer result = branch.incrementTicketCounter(queue);
    this.add(branch.getId(), branch);
    return result;
  }

  /**
   * Добавление/обновление услуг отделения.
   *
   * @param branchId идентификатор отделения
   * @param serviceHashMap карта услуг (id -> услуга)
   * @param checkVisits учитывать активные визиты при изменении
   * @param visitService сервис визитов
   */
  public void addUpdateService(
      String branchId,
      HashMap<String, Service> serviceHashMap,
      Boolean checkVisits,
      VisitService visitService) {
    Branch branch = this.getBranch(branchId);
    branch.addUpdateService(serviceHashMap, eventService, checkVisits, visitService);
    this.add(branch.getId(), branch);
  }

  /**
   * Удаление услуг отделения.
   *
   * @param branchId идентификатор отделения
   * @param serviceIds список идентификаторов услуг
   * @param checkVisits учитывать активные визиты при удалении
   * @param visitService сервис визитов
   */
  public void deleteServices(
      String branchId, List<String> serviceIds, Boolean checkVisits, VisitService visitService) {
    Branch branch = this.getBranch(branchId);
    branch.deleteServices(serviceIds, eventService, checkVisits, visitService);
  }

  /**
   * Добавление/обновление точек обслуживания.
   *
   * @param branchId идентификатор отделения
   * @param servicePointHashMap карта точек обслуживания
   * @param restoreVisit восстановить визит на точке
   * @param restoreUser восстановить пользователя на точке
   */
  public void addUpdateServicePoint(
      String branchId,
      HashMap<String, ServicePoint> servicePointHashMap,
      Boolean restoreVisit,
      Boolean restoreUser) {
    Branch branch = this.getBranch(branchId);
    branch.addUpdateServicePoint(servicePointHashMap, restoreVisit, restoreUser, eventService);
    this.add(branch.getId(), branch);
  }

  /**
   * Добавление/обновление групп услуг.
   *
   * @param branchId идентификатор отделения
   * @param serviceGroupsHashMap карта групп услуг
   */
  public void addUpdateServiceGroups(
      String branchId, HashMap<String, ServiceGroup> serviceGroupsHashMap) {
    Branch branch = this.getBranch(branchId);
    branch.adUpdateServiceGroups(serviceGroupsHashMap, eventService);
    this.add(branch.getId(), branch);
  }

  /**
   * Удаление точек обслуживания.
   *
   * @param branchId идентификатор отделения
   * @param servicePointIds список идентификаторов точек обслуживания
   */
  public void deleteServicePoints(String branchId, List<String> servicePointIds) {
    Branch branch = this.getBranch(branchId);
    branch.deleteServicePoints(servicePointIds, eventService);
    this.add(branch.getId(), branch);
  }

  /**
   * Добавление/обновление очередей.
   *
   * @param branchId идентификатор отделения
   * @param queueHashMap карта очередей
   * @param restoreVisits восстановить визиты очередей
   */
  public void addUpdateQueues(
      String branchId, HashMap<String, Queue> queueHashMap, Boolean restoreVisits) {
    Branch branch = this.getBranch(branchId);
    branch.addUpdateQueues(queueHashMap, restoreVisits, eventService);
    this.add(branch.getId(), branch);
  }

  /**
   * Удаление очередей.
   *
   * @param branchId идентификатор отделения
   * @param queueIds список идентификаторов очередей
   */
  public void deleteQueues(String branchId, List<String> queueIds) {
    Branch branch = this.getBranch(branchId);
    branch.deleteQueues(queueIds, eventService);
    this.add(branch.getId(), branch);
  }

  /**
   * Добавление/обновление правил сегментации.
   *
   * @param branchId идентификатор отделения
   * @param segmentationRuleDataHashMap карта правил сегментации
   */
  public void addUpdateSegmentationRules(
      String branchId, HashMap<String, SegmentationRuleData> segmentationRuleDataHashMap) {
    Branch branch = this.getBranch(branchId);
    branch.adUpdateSegmentRules(segmentationRuleDataHashMap, eventService);
    this.add(branch.getId(), branch);
  }

  /**
   * Получение услуг соответствующего рабочего профиля
   *
   * @param branchId идентификатор отделения
   * @param workProfileId идентификатор рабочего профиля
   * @return список услуг
   */
  public List<Service> getServicesByWorkProfileId(String branchId, String workProfileId) {
    Branch branch = this.getBranch(branchId);
    if (!branch.getWorkProfiles().containsKey(workProfileId)) {
      throw new BusinessException("Work profile not found!!", eventService, HttpStatus.NOT_FOUND);
    }
    List<Service> services = new ArrayList<>();
    branch
        .getWorkProfiles()
        .get(workProfileId)
        .getQueueIds()
        .forEach(
            q ->
                services.addAll(
                    branch.getServices().values().stream()
                        .filter(f -> f.getLinkedQueueId().equals(q))
                        .toList()));
    return services;
  }

  /**
   * Получение услуг соответствующей очереди
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @return список услуг
   */
  public List<Service> getServicesByQueueId(String branchId, String queueId) {
    Branch branch = this.getBranch(branchId);
    if (!branch.getQueues().containsKey(queueId)) {
      throw new BusinessException("Queue not found!!", eventService, HttpStatus.NOT_FOUND);
    }
    return new ArrayList<>(
        branch.getServices().values().stream()
            .filter(f -> f.getLinkedQueueId().equals(queueId))
            .toList());
  }

  /**
   * Получение перечня возможных оказанных услуг отделения.
   *
   * @param branchId идентификатор отделения
   * @return список возможных оказанных услуг
   */
  public List<DeliveredService> getDeliveredServicesByBranchId(String branchId) {
    Branch branch = this.getBranch(branchId);
    return branch.getPossibleDeliveredServices().values().stream().toList();
  }
}
