package ru.aritmos.service;

import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.cache.interceptor.ParametersKey;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.SerdeImport;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
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

  @Value("${micronaut.application.name}")
  String applicationName;

  @Cacheable(parameters = {"key"})
  public Branch getBranch(String key) throws BusinessException {

    Branch result = branches.get(key);
    if (result == null) {
      throw new BusinessException("Branch not found!!", eventService);
    }
    log.info("Getting branchInfo {}", result);
    return result;
  }

  // @Cacheable(parameters = {"id"}, value = {"branches"})
  public HashMap<String, Branch> getBranches() {

    HashMap<String, Branch> result = new HashMap<>();
    branches
        .values()
        .forEach(
            f -> {
              Branch branch = new Branch(f.getId(), f.getName());
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
      throw new BusinessException("Branch not found!!", eventService);
    }
    log.info("Deleting branchInfo {}", key);
    branches.remove(key);
  }

  public void updateVisit(Visit visit, String action) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, action);
    this.add(branch.getId(), branch);
  }

  public void updateVisit(Visit visit, VisitEvent visitEvent, VisitService visitService) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService);
    this.add(branch.getId(), branch);
  }

  public void updateVisit(
      Visit visit, VisitEvent visitEvent, VisitService visitService, Boolean isToStart) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService, isToStart);
    this.add(branch.getId(), branch);
  }

  public void updateVisit(
      Visit visit, VisitEvent visitEvent, VisitService visitService, Integer index) {

    Branch branch = this.getBranch(visit.getBranchId());
    branch.updateVisit(visit, eventService, visitEvent, visitService, index);
    this.add(branch.getId(), branch);
  }

  public User openServicePoint(
      String branchId, String userName, String servicePointId, String workProfileId) {
    Branch branch = this.getBranch(branchId);
    if (!branch.getWorkProfiles().containsKey(workProfileId)) {
      throw new BusinessException(
          "Work profile not found!!", eventService, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (!branch.getServicePoints().containsKey(servicePointId)) {
      throw new BusinessException(
          "Service point not found!!", eventService, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (branch.getUsers().containsKey(userName)) {
      User user = branch.getUsers().get(userName);
      user.setServicePointId(servicePointId);
      user.setCurrentWorkProfileId(workProfileId);
      branch.openServicePoint(user, eventService);
      this.add(branch.getId(), branch);
      return branch.getUsers().get(userName);
    } else {
      User user = new User(userName);
      user.setBranchId(branchId);
      user.setServicePointId(servicePointId);
      user.setCurrentWorkProfileId(workProfileId);

      branch.openServicePoint(user, eventService);
      this.add(branch.getId(), branch);
      return user;
    }
  }

  public void closeServicePoint(String branchId, String servicePointId, VisitService visitService) {

    Branch branch = this.getBranch(branchId);
    branch.closeServicePoint(servicePointId, eventService, visitService);
    this.add(branch.getId(), branch);
  }

  public HashMap<String, User> getUsers(String branchId) {
    Branch branch = this.getBranch(branchId);
    return branch.getUsers();
  }

  public Integer incrementTicetCounter(String branchId, Queue queue) {

    Branch branch = this.getBranch(branchId);
    Integer result = branch.incrementTicketCounter(queue);
    this.add(branch.getId(), branch);
    return result;
  }

  public void addUpdateService(
      String branchId, HashMap<String, Service> serviceHashMap, Boolean checkVisits) {
    Branch branch = this.getBranch(branchId);
    branch.addUpdateService(serviceHashMap, eventService, checkVisits);
    this.add(branch.getId(), branch);
  }

  public void deleteServices(String branchId, List<String> serviceIds, Boolean checkVisits) {
    Branch branch = this.getBranch(branchId);
    branch.deleteServices(serviceIds, eventService, checkVisits);
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

  public void addUpdateSegentationRules(
      String branchId, HashMap<String, SegmentationRuleData> segmentationRuleDataHashMap) {
    Branch branch = this.getBranch(branchId);
    branch.adUpdateSegmentRules(segmentationRuleDataHashMap, eventService);
    this.add(branch.getId(), branch);
  }
}
