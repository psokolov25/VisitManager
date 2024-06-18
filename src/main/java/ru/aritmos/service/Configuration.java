package ru.aritmos.service;

import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;

import java.util.HashMap;

@Context
public class Configuration {
    @Inject
    BranchService branchService;

    public void getConfiguration() {
        Branch branch = new Branch("Отделение на Тверской");
        Queue queueFC=new Queue("Очередь финконсультанта","F");
        Queue queueFSC=new Queue("Очередь старшего финконсультанта","S");
        Queue queueC=new Queue("Очередь кассы","C");
        ServicePoint servicePointFC=new ServicePoint("Финансовый консультант");
        ServicePoint servicePointFSC=new ServicePoint("Старший финансовый консультант");
        ServicePoint servicePointC=new ServicePoint("Касса");
        HashMap<String,ServicePoint> servicePointMap=new HashMap<>();
        servicePointMap.put(servicePointFC.getId(),servicePointFC);
        servicePointMap.put(servicePointFSC.getId(),servicePointFSC);
        servicePointMap.put(servicePointC.getId(),servicePointC);
        HashMap<String, Queue> queueMap=new HashMap<>();
        queueMap.put(queueFC.getId(),queueFC);
        queueMap.put(queueFSC.getId(),queueFSC);
        queueMap.put(queueC.getId(),queueFC);

        branch.setQueues(queueMap);
        branch.setServicePoints(servicePointMap);
        branchService.add(branch.getId(),branch);
    }
}
