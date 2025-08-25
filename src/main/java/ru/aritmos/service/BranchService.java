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

  HashMap<String, Branch> branches = new HashMap<>();
  @Inject EventService eventService;
  @Inject KeyCloackClient keyCloackClient;

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

  public void updateVisit(Visit visit, String action, VisitService visitService) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, action, visitService);
  }

  public void updateVisit(Visit visit, VisitEvent visitEvent, VisitService visitService) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService);
  }

  public void updateVisit(
      Visit visit, VisitEvent visitEvent, VisitService visitService, Boolean isToStart) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService, isToStart);
  }

  public void updateVisit(
      Visit visit, VisitEvent visitEvent, VisitService visitService, Integer index) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService, index);
  }

  public User openServicePoint(
      String branchId,
      String userName,
      String servicePointId,
      String workProfileId,
      VisitService visitService)
      throws IOException {
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

  public HashMap<String, User> getUsers(String branchId) {
    Branch branch = this.getBranch(branchId);

    return branch.getUsers();
  }

  public Integer incrementTicketCounter(String branchId, Queue queue) {

    Branch branch = this.getBranch(branchId);
    Integer result = branch.incrementTicketCounter(queue);
    this.add(branch.getId(), branch);
    return result;
  }

  public void addUpdateService(
      String branchId,
      HashMap<String, Service> serviceHashMap,
      Boolean checkVisits,
      VisitService visitService) {
    Branch branch = this.getBranch(branchId);
    branch.addUpdateService(serviceHashMap, eventService, checkVisits, visitService);
    this.add(branch.getId(), branch);
  }

  public void deleteServices(
      String branchId, List<String> serviceIds, Boolean checkVisits, VisitService visitService) {
    Branch branch = this.getBranch(branchId);
    branch.deleteServices(serviceIds, eventService, checkVisits, visitService);
  }

  public void addUpdateServicePoint(
      String branchId,
      HashMap<String, ServicePoint> servicePointHashMap,
      Boolean restoreVisit,
      Boolean restoreUser) {
    Branch branch = this.getBranch(branchId);
    branch.addUpdateServicePoint(servicePointHashMap, restoreVisit, restoreUser, eventService);
    this.add(branch.getId(), branch);
  }

  public void addUpdateServiceGroups(
      String branchId, HashMap<String, ServiceGroup> serviceGroupsHashMap) {
    Branch branch = this.getBranch(branchId);
    branch.adUpdateServiceGroups(serviceGroupsHashMap, eventService);
    this.add(branch.getId(), branch);
  }

  public void deleteServicePoints(String branchId, List<String> servicePointIds) {
    Branch branch = this.getBranch(branchId);
    branch.deleteServicePoints(servicePointIds, eventService);
    this.add(branch.getId(), branch);
  }

  public void addUpdateQueues(
      String branchId, HashMap<String, Queue> queueHashMap, Boolean restoreVisits) {
    Branch branch = this.getBranch(branchId);
    branch.addUpdateQueues(queueHashMap, restoreVisits, eventService);
    this.add(branch.getId(), branch);
  }

  public void deleteQueues(String branchId, List<String> queueIds) {
    Branch branch = this.getBranch(branchId);
    branch.deleteQueues(queueIds, eventService);
    this.add(branch.getId(), branch);
  }

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

  public List<DeliveredService> getDeliveredServicesByBranchId(String branchId) {
    Branch branch = this.getBranch(branchId);
    return branch.getPossibleDeliveredServices().values().stream().toList();
  }
}
