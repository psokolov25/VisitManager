package ru.aritmos.service;

import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Branch;

import java.util.HashMap;

@Slf4j
@Singleton
@Named("Branch_cache")
@CacheConfig("branches")
public class BranchService {
    @Getter
    HashMap<String, Branch> branches = new HashMap<>();
    @Inject
    EventService eventService;
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


    @CachePut(parameters = {"key"})
    public Branch add(String key, Branch branch) {

        Branch oldBranch;


        if (this.branches.containsKey(key)) {
            oldBranch = this.branches.get(key);
            eventService.sendChangedEvent("*", true, oldBranch, branch, new HashMap<>(), "CHANGED");

        } else {
            eventService.sendChangedEvent("*", true, null, branch, new HashMap<>(), "CREATED");
        }
        branches.put(key, branch);
        log.info("Putting branchInfo {}", branch);
        return branch;
    }

    @CacheInvalidate(parameters = {"key"})

    public void delete(String key) {
        Branch oldBranch;
        if (this.branches.containsKey(key)) {
            oldBranch = this.branches.get(key);
            eventService.sendChangedEvent("*", true, oldBranch, null, new HashMap<>(), "DELETED");

        } else {
            throw new BusinessException("Branch not found!!", eventService);
        }
        log.info("Deleting branchInfo {}", key);
        branches.remove(key);
    }


}
