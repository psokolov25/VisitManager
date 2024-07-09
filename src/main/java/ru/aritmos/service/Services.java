package ru.aritmos.service;

import graphql.language.SelectionSet;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.WorkProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class Services {
    @Inject
    BranchService branchService;


    public List<Service> getAllServices(String branchId) {
        Branch currentBranch = branchService.getBranch(branchId);
        List<String> workProfilesIds =
                currentBranch
                        .getServicePoints()
                        .values().stream()
                        .filter(f->f.getUser()!=null)
                        .map(m -> m.getUser().getCurrentWorkProfileId()).toList();

        List<WorkProfile> workProfiles = currentBranch.getWorkProfiles()
                .values().stream()
                .filter(f -> workProfilesIds.contains(f.getId())).toList();

        List<String> queueIds=workProfiles.stream().flatMap(fm->fm.getQueueIds().stream()).distinct().toList();
        List<Service> services = new ArrayList<>();
                currentBranch.getServices().values().forEach(
                        f->{
                            if(queueIds.contains(f.getLinkedQueueId())){
                                f.setIsAvailable(true);
                            }
                            else
                            {
                                f.setIsAvailable(false);
                            }
                            services.add(f);
                        }

                );
                return services;
    }

    public List<Service> getAllAvilableServies(String branchId) {

        return this.getAllServices(branchId).stream().filter(Service::getIsAvailable).toList();
    }
}
