package ru.aritmos.service;

import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;

import java.util.ArrayList;
import java.util.HashMap;

@Context
public class Configuration {
    @Inject
    BranchService branchService;

    public void getConfiguration() {
        if( !branchService.getBranches().containsKey("37493d1c-8282-4417-a729-dceac1f3e2b4")){
        Branch branch = new Branch("37493d1c-8282-4417-a729-dceac1f3e2b4","Отделение на Тверской");

        Queue queueCredit =new Queue("Кредиты","F");
        Service creditService = new Service("Кредит",9000, queueCredit.getId());
        Queue queueeBigCredit =new Queue("Очень большие кредиты","S");
        Service bigCreditService = new Service("Очень большой кредит",9000, queueeBigCredit.getId());
        Queue queueC=new Queue("В кассу","C");
        Service kassaService = new Service("Касса",9000, queueC.getId());
        ServicePoint servicePointFC=new ServicePoint("Финансовый консультант");
        ArrayList<Service> serviceList=new ArrayList<>();
        serviceList.add(kassaService);
        serviceList.add(creditService);
        serviceList.add(bigCreditService);
        branch.setServices(serviceList);
        ServicePoint servicePointFSC=new ServicePoint("Старший финансовый консультант");
        ServicePoint servicePointC=new ServicePoint("Касса");
        HashMap<String,ServicePoint> servicePointMap=new HashMap<>();
        servicePointMap.put(servicePointFC.getId(),servicePointFC);
        servicePointMap.put(servicePointFSC.getId(),servicePointFSC);
        servicePointMap.put(servicePointC.getId(),servicePointC);
        HashMap<String, Queue> queueMap=new HashMap<>();
        queueMap.put(queueCredit.getId(), queueCredit);
        queueMap.put(queueeBigCredit.getId(), queueeBigCredit);
        queueMap.put(queueC.getId(), queueC);

        branch.setQueues(queueMap);
        branch.setServicePoints(servicePointMap);
        branchService.add(branch.getId(),branch);
    }}
}
