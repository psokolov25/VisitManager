package ru.aritmos.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.WorkProfile;

/** Класс отвечающий за работу с услугами */
@Singleton
public class Services {
  @Inject BranchService branchService;

  /**
   * Получение всех услуг отделения
   *
   * @param branchId идентификатор отделения
   * @return список услуг
   */
  public List<Service> getAllServices(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);
    List<String> workProfilesIds =
        currentBranch.getServicePoints().values().stream()
            .filter(f -> f.getUser() != null)
            .map(m -> m.getUser().getCurrentWorkProfileId())
            .toList();

    List<WorkProfile> workProfiles =
        currentBranch.getWorkProfiles().values().stream()
            .filter(f -> workProfilesIds.contains(f.getId()))
            .toList();

    List<String> queueIds =
        workProfiles.stream().flatMap(fm -> fm.getQueueIds().stream()).distinct().toList();
    List<Service> services = new ArrayList<>();
    currentBranch
        .getServices()
        .values()
        .forEach(
            f -> {
              f.setIsAvailable(queueIds.contains(f.getLinkedQueueId()));
              services.add(f);
            });
    return services;
  }

  /**
   * Получение всех доступных на данный момент услуг
   *
   * @param branchId идентификатор отделения
   * @return список доступных услуг
   */
  public List<Service> getAllAvilableServies(String branchId) {

    return this.getAllServices(branchId).stream().filter(Service::getIsAvailable).toList();
  }
}
