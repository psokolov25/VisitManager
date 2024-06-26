package ru.aritmos.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ru.aritmos.model.Service;

import java.util.List;

@Singleton
public class Services {
    @Inject
    BranchService branchService;
    public List<Service> getAllAvilableServies(String branchId)  {
        return branchService.getBranch(branchId)
                .getServices()
                .values()
                .stream()
                .filter(Service::getIsAvailable).toList();
    }
}
