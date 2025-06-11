package ru.aritmos.service;

import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.cache.interceptor.ParametersKey;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.SerdeImport;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.*;
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

  @Cacheable(
      parameters = {"key"},
      value = {"branch"})
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

  @CachePut(
      parameters = {"key"},
      value = {"branch"})
  public Branch add(String key, Branch branch) {

    Branch oldBranch;

    if (this.branches.containsKey(key)) {
      oldBranch = this.branches.get(key);
      eventService.sendChangedEvent(
          "config", true, oldBranch, branch, new HashMap<>(), "BRANCH_CHANGED");
      // eventService.sendChangedEvent("*", true, oldBranch, branch, new HashMap<>(), "CHANGED");

    } else {
      eventService.sendChangedEvent(
          "config", true, null, branch, new HashMap<>(), "BRANCH_CREATED");
      // eventService.sendChangedEvent("*", true, null, branch, new HashMap<>(), "CREATED");
    }
    branch.getQueues().forEach((key1, value) -> value.setBranchId(key));
    branch.getServicePoints().forEach((key1, value) -> value.setBranchId(key));
    branch.getEntryPoints().forEach((key1, value) -> value.setBranchId(key));
    branch.getServices().forEach((key1, value) -> value.setBranchId(key));
    branch.getWorkProfiles().forEach((key1, value) -> value.setBranchId(key));
    branch.getServiceGroups().forEach((key1, value) -> value.setBranchId(key));
    branch.getReception().setBranchId(key);
    branches.put(key, branch);

    log.info("Putting branchInfo {}", branch);
    return branch;
  }

  @CacheInvalidate(parameters = {"key"})
  public void delete(String key) {
    Branch oldBranch;
    if (this.branches.containsKey(key)) {
      oldBranch = this.branches.get(key);
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
      VisitService visitService) {
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
                closeServicePoint(branchId, servicePoint.getId(), visitService, false, false, "",true));

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
      user.setServicePointId(servicePointId);
      user.setCurrentWorkProfileId(workProfileId);
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
      user.setServicePointId(servicePointId);
      user.setCurrentWorkProfileId(workProfileId);

      branch.openServicePoint(user, eventService);
      this.add(branch.getId(), branch);
      return user;
    }
  }

  public void closeServicePoint(
      String branchId,
      String servicePointId,
      VisitService visitService,
      Boolean isWithLogout,
      Boolean isBreak,
      String breakReason,
      Boolean isForced) {

    Branch branch = this.getBranch(branchId);
    branch.closeServicePoint(
        servicePointId, eventService, visitService, isWithLogout, isBreak, breakReason, isForced);
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
