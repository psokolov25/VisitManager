package ru.aritmos.service;

import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.model.*;

import java.util.HashMap;

@Slf4j
@Context
public class Configuration {
    @Inject
    BranchService branchService;


    public void getConfiguration() {


        if(branchService.getBranches().isEmpty()){
//        if (branchService.getBranches().isEmpty()) {


            Branch branch = new Branch("37493d1c-8282-4417-a729-dceac1f3e2b4", "Отделение на Тверской");
            EntryPoint entryPoint = new EntryPoint();
            entryPoint.setPrinterId("2");
            entryPoint.setId("2");
            HashMap<String, EntryPoint> entryPoints = new HashMap<>();
            entryPoints.put(entryPoint.getId(), entryPoint);
            branch.setEntryPoints(entryPoints);
            Queue queueCredit = new Queue("55da9b66-c928-4d47-9811-dbbab20d3780", "Кредиты", "F");
            Service creditService = new Service("c3916e7f-7bea-4490-b9d1-0d4064adbe8b", "Кредит", 9000, queueCredit.getId());
            Queue queueBigCredit = new Queue("c211ae6b-de7b-4350-8a4c-cff7ff98104e", "Очень большие кредиты", "S");
            Service bigCreditService = new Service("569769e8-3bb3-4263-bd2e-42d8b3ec0bd2", "Очень большой кредит", 9000, queueBigCredit.getId());
            Queue queueC = new Queue("8eee7e6e-345a-4f9b-9743-ff30a4322ef5", "В кассу", "C");
            Service kassaService = new Service("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66", "Касса", 9000, queueC.getId());
            ServicePoint servicePointFC = new ServicePoint("a66ff6f4-4f4a-4009-8602-0dc278024cf2", "Финансовый консультант");
            HashMap<String, Service> serviceList = new HashMap<>();
            serviceList.put(kassaService.getId(), kassaService);
            serviceList.put(creditService.getId(), creditService);
            serviceList.put(bigCreditService.getId(), bigCreditService);
            branch.setServices(serviceList);
            ServicePoint servicePointFSC = new ServicePoint("099c43c1-40b5-4b80-928a-1d4b363152a8", "Старший финансовый консультант");
            ServicePoint servicePointC = new ServicePoint("043536cc-62bb-43df-bdc6-d0b9df9ff961", "Касса");
            WorkProfile workProfileC = new WorkProfile("Кассир");
            workProfileC.getQueueIds().add(queueC.getId());
            WorkProfile workProfileFC = new WorkProfile("Финансовый консультант");
            workProfileFC.getQueueIds().add(queueCredit.getId());
            WorkProfile workProfileFSC = new WorkProfile("Старший финансовый консультант");
            workProfileFSC.getQueueIds().add(queueBigCredit.getId());
            workProfileFSC.getQueueIds().add(queueCredit.getId());

            User psokolovUser = new User("psokolov");
            User sidorovUser = new User("isidorov");
            psokolovUser.setBranchId(branch.getId());
            sidorovUser.setBranchId(branch.getId());

            psokolovUser.setCurrentWorkProfileId(workProfileFC.getId());

            sidorovUser.setCurrentWorkProfileId(workProfileC.getId());
            branchService.loginUser(psokolovUser,branch);
            branchService.loginUser(sidorovUser,branch);
            HashMap<String, ServicePoint> servicePointMap = new HashMap<>();
            servicePointMap.put(servicePointFC.getId(), servicePointFC);
            servicePointMap.put(servicePointFSC.getId(), servicePointFSC);
            servicePointMap.put(servicePointC.getId(), servicePointC);
            HashMap<String, Queue> queueMap = new HashMap<>();
            queueMap.put(queueCredit.getId(), queueCredit);
            queueMap.put(queueBigCredit.getId(), queueBigCredit);
            queueMap.put(queueC.getId(), queueC);

            branch.setQueues(queueMap);

            branch.setServicePoints(servicePointMap);
            branch.getServicePoints().get(servicePointFC.getId()).setUser(psokolovUser);
            branch.getWorkProfiles().put(workProfileC.getId(), workProfileC);
            branch.getWorkProfiles().put(workProfileFC.getId(), workProfileFC);
            branch.getWorkProfiles().put(workProfileFSC.getId(), workProfileFSC);
            branchService.add(branch.getId(), branch);
        }
    }
}
