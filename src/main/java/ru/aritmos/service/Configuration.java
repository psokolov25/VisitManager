package ru.aritmos.service;

import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.*;

@Slf4j
@Context
public class Configuration {
  @Inject BranchService branchService;
  @Inject KeyCloackClient keyCloackClient;
  @Inject VisitService visitService;
  @Inject EventService eventService;

  public HashMap<String, Branch> createBranchConfiguration(Map<String, Branch> branchHashMap) {
    Event eventPublicStart =
        Event.builder()
            .eventType("PUBLIC_STARTED")
            .eventDate(ZonedDateTime.now())
            .body(branchHashMap)
            .build();
    eventService.send("stat", false, eventPublicStart);
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    //
    //    branchService
    //        .getBranches()
    //        .forEach(
    //            (key, value) -> {
    //              if (!branchHashMap.containsKey(key)) {
    //                Branch body = branchHashMap.get(key);
    //                branchService.delete(key, visitService);
    //                Event eventDeleted =
    //                    Event.builder()
    //                        .eventType("PUBLIC_BRANCH_DELETED")
    //                        .eventDate(ZonedDateTime.now())
    //                        .body(body)
    //                        .params(Map.of("branchId", key))
    //                        .build();
    //                eventService.send("stat", false, eventDeleted);
    //              }
    //            });
    branchHashMap.forEach(
        (key, value) -> {
          if (branchService.branchExists(key)) {
            branchService.delete(key, visitService);
          }
          branchService.add(key, value);
          Event eventDeleted =
              Event.builder()
                  .eventType("BRANCH_PUBLIC_COMPLETE")
                  .eventDate(ZonedDateTime.now())
                  .body(value)
                  .params(Map.of("branchId", key))
                  .build();
          eventService.send("stat", false, eventDeleted);
          try {
            Thread.sleep(20);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
    Event eventPublicFinished =
        Event.builder()
            .eventType("PUBLIC_COMPLETE")
            .eventDate(ZonedDateTime.now())
            .body(branchHashMap)
            .build();
    eventService.send("stat", false, eventPublicFinished);
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return branchService.getDetailedBranches();
  }

  public Map<String, Branch> createDemoBranch() {
    Map<String, Branch> branches = new HashMap<>();
    Branch branch = new Branch("37493d1c-8282-4417-a729-dceac1f3e2b4", "Клиника на Тверской");
    branch.setAddress("Москва, ул. Тверская 13");
    branch.setDescription("Главное отделение");
    branch.setPrefix("TVR");
    branch.setPath(keyCloackClient.getBranchPathByBranchPrefix("REGION", branch.getPrefix()));
    branch
        .getMarks()
        .put(
            "04992364-9e96-4ec9-8a05-923766aa57e7",
            Mark.builder()
                .id("04992364-9e96-4ec9-8a05-923766aa57e7")
                .value("Клиент доволен")
                .build());
    branch
        .getMarks()
        .put(
            "d75076be-d3e0-4323-b7db-b32ea6b30817",
            Mark.builder()
                .id("d75076be-d3e0-4323-b7db-b32ea6b30817")
                .value("Клиент не доволен")
                .build());
    branch.getParameterMap().put("autoCallMode", false);
    branch.getBreakReasons().put("5454e87a-9ace-46fd-be30-b00f7bb88688", "Обучение");
    branch.getBreakReasons().put("75caee17-d8f3-49b1-b298-96bbb6ba19f3", "Обед");
    EntryPoint entryPoint = new EntryPoint();
    entryPoint.setPrinter(
        Entity.builder().id("eb7ea46d-c995-4ca0-ba92-c92151473214").name("Intro18").build());
    entryPoint.setId("2");
    HashMap<String, EntryPoint> entryPoints = new HashMap<>();
    entryPoints.put(entryPoint.getId(), entryPoint);
    branch.setEntryPoints(entryPoints);
    Queue queueCredit = new Queue("55da9b66-c928-4d47-9811-dbbab20d3780", "Хирург", "F", 9000);
    queueCredit.setWaitingSL(200);
    Service creditService =
        new Service("c3916e7f-7bea-4490-b9d1-0d4064adbe8b", "Хирург", 9000, queueCredit.getId());

    Outcome creditAccepted =
        new Outcome("462bac1a-568a-4f1f-9548-1c7b61792b4b", "Запись на повторный приём");
    creditAccepted.setCode(1L);
    creditService.getPossibleOutcomes().put(creditAccepted.getId(), creditAccepted);
    DeliveredService creditCard =
        new DeliveredService("35d73fdd-1597-4d94-a087-fd8a99c9d1ed", "Консультация");
    creditCard.getServiceIds().add(creditService.getId());
    Outcome creditCardGiven = new Outcome("8dc29622-cd87-4384-85a7-04b66b28dd0f", "Получена");
    creditCard.getPossibleOutcomes().put(creditCardGiven.getId(), creditCardGiven);
    branch.getPossibleDeliveredServices().put(creditCard.getId(), creditCard);

    DeliveredService insurance =
        new DeliveredService("daa17035-7bd7-403f-a036-6c14b81e666f", "Подготовка к операции");
    insurance.getServiceIds().add(creditService.getId());
    branch.getPossibleDeliveredServices().put(insurance.getId(), insurance);

    Queue queueBigCredit =
        new Queue("c211ae6b-de7b-4350-8a4c-cff7ff98104e", "Офтальмолог", "S", 9000);
    Service bigCreditService =
        new Service(
            "569769e8-3bb3-4263-bd2e-42d8b3ec0bd2", "Офтальмолог", 9000, queueBigCredit.getId());
    Service longCreditService =
        new Service(
            "856e8e77-aa8e-4feb-b947-566f6164e46f", "Травматолог", 9000, queueCredit.getId());
    Queue queueC = new Queue("8eee7e6e-345a-4f9b-9743-ff30a4322ef5", "В кассу", "C", 9000);
    Service kassaService =
        new Service("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66", "Касса", 9000, queueC.getId());
    ServicePoint servicePointFC =
        new ServicePoint("a66ff6f4-4f4a-4009-8602-0dc278024cf2", "Каб. 121");

    HashMap<String, Service> serviceList = new HashMap<>();
    serviceList.put(kassaService.getId(), kassaService.clone());
    serviceList.put(creditService.getId(), creditService.clone());
    serviceList.put(bigCreditService.getId(), bigCreditService.clone());
    serviceList.put(longCreditService.getId(), longCreditService.clone());
    branch.getServices().putAll(serviceList);

    ServicePoint servicePointFSC =
        new ServicePoint("099c43c1-40b5-4b80-928a-1d4b363152a8", "Каб. 101");

    ServicePoint servicePointBFSC =
        new ServicePoint("090bd53d-96ba-466b-9845-d64e81894964", "Каб. 114");
    ServicePoint servicePointBBFSC =
        new ServicePoint("f9e60eaf-b4af-4bf8-8d64-e70d2e949829", "Каб. 120");

    ServicePoint servicePointC =
        new ServicePoint("043536cc-62bb-43df-bdc6-d0b9df9ff961", "Каб. 102 Касса");

    WorkProfile workProfileC = new WorkProfile("Кассир");
    workProfileC.getQueueIds().add(queueC.getId());
    WorkProfile workProfileFC = new WorkProfile("d5a84e60-e605-4527-b065-f4bd7a385790", "Хирург");
    workProfileFC.getQueueIds().add(queueCredit.getId());
    WorkProfile workProfileFSC =
        new WorkProfile("76e4d31e-1787-476a-9668-9ff5c50c6855", "Офтальмолог");
    workProfileFSC.getQueueIds().add(queueBigCredit.getId());
    workProfileFSC.getQueueIds().add(queueCredit.getId());


    HashMap<String, ServicePoint> servicePointMap = new HashMap<>();
    servicePointC.setIsConfirmRequired(false);
    servicePointFSC.setIsConfirmRequired(false);
    servicePointFC.setIsConfirmRequired(false);
    servicePointBFSC.setIsConfirmRequired(true);
    servicePointBBFSC.setIsConfirmRequired(true);
    servicePointMap.put(servicePointFC.getId(), servicePointFC);
    servicePointMap.put(servicePointFSC.getId(), servicePointFSC);
    servicePointMap.put(servicePointBFSC.getId(), servicePointBFSC);
    servicePointMap.put(servicePointBBFSC.getId(), servicePointBBFSC);
    servicePointMap.put(servicePointC.getId(), servicePointC);
    HashMap<String, Queue> queueMap = new HashMap<>();
    queueMap.put(queueCredit.getId(), queueCredit);
    queueMap.put(queueBigCredit.getId(), queueBigCredit);
    queueMap.put(queueC.getId(), queueC);

    creditService.getPossibleOutcomes().put(creditAccepted.getId(), creditAccepted);

    creditCard.getPossibleOutcomes().put(creditCardGiven.getId(), creditCardGiven);
    branch.setQueues(queueMap);

    branch.setServicePoints(servicePointMap);
    branch.getWorkProfiles().put(workProfileC.getId(), workProfileC);
    branch.getWorkProfiles().put(workProfileFC.getId(), workProfileFC);
    branch.getWorkProfiles().put(workProfileFSC.getId(), workProfileFSC);
    branch
        .getReception()
        .setPrinters(
            List.of(
                Entity.builder().id("eb7ea46d-c995-4ca0-ba92-c92151473614").name("Intro17").build(),
                Entity.builder()
                    .id("eb7ea46d-c995-4ca0-ba92-c92151473612")
                    .name("Intro8")
                    .build()));



    Branch branch2 = new Branch("e73601bd-2fbb-4303-9a58-16cbc4ad6ad3", " Клиника на Ямской");
    branch2.setPrefix("YMS");
    branchService.add(branch2.getId(), branch2);
    Branch branch3 = new Branch("e64078c1-e95a-40fd-b8b1-6bc3c8912abb", " Клиника на Арбате");
    branch3.setPrefix("ARB");
    Branch branch4 = new Branch("a06fabe5-0f69-41f8-b7d0-21da39cdaace", " Клиника на Центральной");
    branch4.setPrefix("CNT");
    Branch branch5 = new Branch("15c9d0f3-384d-4a22-a5f2-84ceac1fa094", " Клиника на Урицкого");
    branch4.setPrefix("URC");
    Branch branch6 =
        new Branch("38f46d6b-3d37-40b9-8840-b57beac0ec1e", " Клиника доктора Дмитрия Юненко");
    branch6.setPrefix("DIM");
    branches.put(branch.getId(), branch);
    branches.put(branch3.getId(), branch3);
    branches.put(branch4.getId(), branch4);
    branches.put(branch5.getId(), branch5);
    branches.put(branch6.getId(), branch6);
    return branches;
  }
}
