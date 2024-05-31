package ru.aritmos.service;

import jakarta.inject.Inject;
import ru.aritmos.model.Service;

import java.util.List;

public class Services {
    @Inject
    BranchService branchService;
    public List<Service> getAllAvilableServies(String branchId)
    {
        return branchService.getBranch(branchId)
                .getServices()
                .stream()
                .filter(Service::getIsAvailable).toList();
    }
}
