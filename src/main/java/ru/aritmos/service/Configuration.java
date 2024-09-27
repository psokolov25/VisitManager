package ru.aritmos.service;

import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.model.*;

@Slf4j
@Context
public class Configuration {
  @Inject BranchService branchService;

  public void getConfiguration() {

    if (branchService.getBranches().isEmpty()) {
      //        if (branchService.getBranches().isEmpty()) {

      Branch branch = new Branch("37493d1c-8282-4417-a729-dceac1f3e2b4", "Отделение на Тверской");
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
      EntryPoint entryPoint = new EntryPoint();
      entryPoint.setPrinterId("2");
      entryPoint.setId("2");
      HashMap<String, EntryPoint> entryPoints = new HashMap<>();
      entryPoints.put(entryPoint.getId(), entryPoint);
      branch.setEntryPoints(entryPoints);
      Queue queueCredit = new Queue("55da9b66-c928-4d47-9811-dbbab20d3780", "Кредиты", "F");
      Service creditService =
          new Service("c3916e7f-7bea-4490-b9d1-0d4064adbe8b", "Кредит", 9000, queueCredit.getId());
      Outcome creditAccepted = new Outcome("462bac1a-568a-4f1f-9548-1c7b61792b4b", "Одобрен");
      creditAccepted.setCode(1L);
      creditService.getPossibleOutcomes().put(creditAccepted.getId(), creditAccepted);
      DeliveredService creditCard =
          new DeliveredService("35d73fdd-1597-4d94-a087-fd8a99c9d1ed", "Кредитная карта");
      creditCard.getServviceIds().add(creditService.getId());
      Outcome creditCardGiven = new Outcome("8dc29622-cd87-4384-85a7-04b66b28dd0f", "Выдана");
      creditCard.getPossibleOutcomes().put(creditCardGiven.getId(), creditCardGiven);
      branch.getPossibleDeliveredServices().put(creditCard.getId(), creditCard);

      DeliveredService insurance =
          new DeliveredService("daa17035-7bd7-403f-a036-6c14b81e666f", "Страховка");
      insurance.getServviceIds().add(creditService.getId());
      branch.getPossibleDeliveredServices().put(insurance.getId(), insurance);

      Queue queueBigCredit =
          new Queue("c211ae6b-de7b-4350-8a4c-cff7ff98104e", "Очень большие кредиты", "S");
      Service bigCreditService =
          new Service(
              "569769e8-3bb3-4263-bd2e-42d8b3ec0bd2",
              "Очень большой кредит",
              9000,
              queueBigCredit.getId());

      Queue queueC = new Queue("8eee7e6e-345a-4f9b-9743-ff30a4322ef5", "В кассу", "C");
      Service kassaService =
          new Service("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a66", "Касса", 9000, queueC.getId());
      ServicePoint servicePointFC =
          new ServicePoint("a66ff6f4-4f4a-4009-8602-0dc278024cf2", "Финансовый консультант");
      servicePointFC.setIsConfirmRequired(true);
      HashMap<String, Service> serviceList = new HashMap<>();
      serviceList.put(kassaService.getId(), kassaService);
      serviceList.put(creditService.getId(), creditService);
      serviceList.put(bigCreditService.getId(), bigCreditService);
      branch.setServices(serviceList);

      ServicePoint servicePointFSC =
          new ServicePoint(
              "099c43c1-40b5-4b80-928a-1d4b363152a8", "Старший финансовый консультант");

      ServicePoint servicePointC =
          new ServicePoint("043536cc-62bb-43df-bdc6-d0b9df9ff961", "Касса");

      WorkProfile workProfileC = new WorkProfile("Кассир");
      workProfileC.getQueueIds().add(queueC.getId());
      WorkProfile workProfileFC =
          new WorkProfile("d5a84e60-e605-4527-b065-f4bd7a385790", "Финансовый консультант");
      workProfileFC.getQueueIds().add(queueCredit.getId());
      WorkProfile workProfileFSC =
          new WorkProfile("76e4d31e-1787-476a-9668-9ff5c50c6855", "Старший финансовый консультант");
      workProfileFSC.getQueueIds().add(queueBigCredit.getId());
      workProfileFSC.getQueueIds().add(queueCredit.getId());

      User psokolovUser = new User("f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89", "psokolov");
      User sidorovUser = new User("isidorov");
      psokolovUser.setBranchId(branch.getId());
      sidorovUser.setBranchId(branch.getId());

      psokolovUser.setCurrentWorkProfileId(workProfileFC.getId());

      sidorovUser.setCurrentWorkProfileId(workProfileC.getId());

      // branchService.openServicePoint(psokolovUser, branch);
      // branchService.openServicePoint(sidorovUser, branch);
      HashMap<String, ServicePoint> servicePointMap = new HashMap<>();
      servicePointMap.put(servicePointFC.getId(), servicePointFC);
      servicePointMap.put(servicePointFSC.getId(), servicePointFSC);
      servicePointMap.put(servicePointC.getId(), servicePointC);
      HashMap<String, Queue> queueMap = new HashMap<>();
      queueMap.put(queueCredit.getId(), queueCredit);
      queueMap.put(queueBigCredit.getId(), queueBigCredit);
      queueMap.put(queueC.getId(), queueC);

      creditService.getPossibleOutcomes().put(creditAccepted.getId(), creditAccepted);

      creditCard.getPossibleOutcomes().put(creditCardGiven.getId(), creditCardGiven);
      branch.setQueues(queueMap);

      branch.setServicePoints(servicePointMap);
      branch.getServicePoints().get(servicePointFC.getId()).setUser(psokolovUser);
      branch.getWorkProfiles().put(workProfileC.getId(), workProfileC);
      branch.getWorkProfiles().put(workProfileFC.getId(), workProfileFC);
      branch.getWorkProfiles().put(workProfileFSC.getId(), workProfileFSC);
      branchService.openServicePoint(
          branch.getId(), psokolovUser.getName(), servicePointFC.getId(), workProfileFC.getId());
      branchService.add(branch.getId(), branch);

      //
      Branch branch2 = new Branch("e73601bd-2fbb-4303-9a58-16cbc4ad6ad3", "Отделение на Ямской");
      //            branch2.getMarks().put(
      //                    "86daccb9-9767-4b37-b603-e6956803933c",
      //                    Mark
      //                            .builder()
      //                            .id("86daccb9-9767-4b37-b603-e6956803933c")
      //                            .value("Клиент доволен")
      //                            .build()
      //            );
      //            branch2.getMarks().put(
      //                    "d75076be-d3e0-4323-b7db-b32ea6b30827",
      //                    Mark
      //                            .builder()
      //                            .id("d75076be-d3e0-4323-b7db-b32ea6b30827")
      //                            .value("Клиент не доволен")
      //                            .build()
      //            );
      //            EntryPoint entryPoint2 = new EntryPoint();
      //            entryPoint.setPrinterId("2");
      //            entryPoint.setId("2");
      //            AtomicReference<HashMap<String, EntryPoint>> entryPoints2 = new
      // AtomicReference<>(new HashMap<>());
      //            entryPoints2.get().put(entryPoint2.getId(), entryPoint);
      //            branch.setEntryPoints(entryPoints);
      //            Queue queueCredit2 = new Queue("9828de92-17c0-4616-bc5b-3971a778fe2a",
      // "Кредиты", "F");
      //            Service creditService2 = new Service("1ebff05b-5aa4-462d-9e9c-fb889852535c",
      // "Кредит", 9000, queueCredit.getId());
      //            Outcome creditAccepted2 = new Outcome("dccf0b9e-cb77-4af2-a50e-56189f6e3cc8",
      // "Одобрен");
      //            creditAccepted2.setCode(1L);
      //            creditService2.getPossibleOutcomes().put(creditAccepted.getId(),creditAccepted);
      //            DeliveredService creditCard2=new
      // DeliveredService("d71d3564-05b6-4f42-b17d-c2ea8aaa4c8c","Кредитная карта");
      //            creditCard2.getServviceIds().add(creditService2.getId());
      //            Outcome creditCardGiven2 = new Outcome("8dc29622-cd87-4384-85a7-04b66b28dd0f",
      // "Выдана");
      //
      // creditCard2.getPossibleOutcomes().put(creditCardGiven2.getId(),creditCardGiven2);
      //            branch2.getPossibleDeliveredServices().put(creditCard2.getId(),creditCard2);
      //
      //
      //            DeliveredService insurance2=new
      // DeliveredService("414e779a-b7c2-49a1-9917-951f3cb6cd26","Страховка");
      //            insurance2.getServviceIds().add(creditService2.getId());
      //            branch2.getPossibleDeliveredServices().put(insurance.getId(),insurance);
      //
      //            Queue queueBigCredit2 = new Queue("325fc045-a69e-4154-bfe7-204747823583", "Очень
      // большие кредиты", "S");
      //            Service bigCreditService2 = new Service("64e4d564-817c-4629-bc02-2df72affa38a",
      // "Очень большой кредит", 9000, queueBigCredit2.getId());
      //
      //            Queue queueC2 = new Queue("c0c52932-87bd-4388-8620-97e08568b851", "В кассу",
      // "C");
      //            Service kassaService2 = new Service("9cacbc14-9735-4e73-b882-dfac2bb7d7c9",
      // "Касса", 9000, queueC2.getId());
      //            ServicePoint servicePointFC2 = new
      // ServicePoint("993211e7-31c8-4dcf-bab1-ecf7b9094d4c", "Финансовый консультант");
      //            HashMap<String, Service> serviceList2 = new HashMap<>();
      //            serviceList2.put(kassaService2.getId(), kassaService2);
      //            serviceList2.put(creditService2.getId(), creditService2);
      //            serviceList2.put(bigCreditService2.getId(), bigCreditService2);
      //            branch2.setServices(serviceList2);
      //
      //            ServicePoint servicePointFSC2 = new
      // ServicePoint("eabd0664-f3bf-4492-b8f0-be2b80e3d09d", "Старший финансовый консультант");
      //            ServicePoint servicePointC2 = new
      // ServicePoint("403e1987-3c3a-4bb9-be5e-fcf951f86023", "Касса");
      //            WorkProfile workProfileC2 = new WorkProfile("Кассир");
      //            workProfileC2.getQueueIds().add(queueC.getId());
      //            WorkProfile workProfileFC2 = new
      // WorkProfile("cf1ebcd9-b757-4a48-810e-5c2be413c993","Финансовый консультант");
      //            workProfileFC2.getQueueIds().add(queueCredit2.getId());
      //            WorkProfile workProfileFSC2 = new
      // WorkProfile("1df69e27-7bdf-4084-88a4-52344a2428fc","Старший финансовый консультант");
      //            workProfileFSC2.getQueueIds().add(queueBigCredit2.getId());
      //            workProfileFSC2.getQueueIds().add(queueCredit2.getId());
      //
      //            User psokolovUser2 = new
      // User("1351b389-c444-493f-b6de-786eae18d756","psokolov");
      //            User sidorovUser2 = new User("isidorov");
      //            psokolovUser2.setBranchId(branch2.getId());
      //            sidorovUser2.setBranchId(branch2.getId());
      //
      //            psokolovUser.setCurrentWorkProfileId(workProfileFC.getId());
      //
      //            sidorovUser2.setCurrentWorkProfileId(workProfileC2.getId());
      //            branchService.openServicePoint(psokolovUser2,branch2);
      //            branchService.openServicePoint(sidorovUser2,branch2);
      //            HashMap<String, ServicePoint> servicePointMap2 = new HashMap<>();
      //            servicePointMap2.put(servicePointFC2.getId(), servicePointFC2);
      //            servicePointMap2.put(servicePointFSC2.getId(), servicePointFSC2);
      //            servicePointMap2.put(servicePointC2.getId(), servicePointC2);
      //            HashMap<String, Queue> queueMap2 = new HashMap<>();
      //            queueMap2.put(queueCredit2.getId(), queueCredit2);
      //            queueMap2.put(queueBigCredit2.getId(), queueBigCredit2);
      //            queueMap2.put(queueC2.getId(), queueC2);
      //
      //
      //
      //
      //            creditService2.getPossibleOutcomes().put(creditAccepted2.getId(),
      // creditAccepted2);
      //
      //
      //
      // creditCard2.getPossibleOutcomes().put(creditCardGiven2.getId(),creditCardGiven2);
      //            branch2.setQueues(queueMap2);
      //
      //            branch2.setServicePoints(servicePointMap2);
      //            branch2.getServicePoints().get(servicePointFC2.getId()).setUser(psokolovUser2);
      //            branch2.getWorkProfiles().put(workProfileC2.getId(), workProfileC2);
      //            branch2.getWorkProfiles().put(workProfileFC2.getId(), workProfileFC2);
      //            branch2.getWorkProfiles().put(workProfileFSC2.getId(), workProfileFSC2);
      branchService.add(branch2.getId(), branch2);
      Branch branch3 = new Branch("e64078c1-e95a-40fd-b8b1-6bc3c8912abb", "Отделение на Арбате");
      Branch branch4 =
          new Branch("a06fabe5-0f69-41f8-b7d0-21da39cdaace", "Отделение на Центральной");
      Branch branch5 = new Branch("15c9d0f3-384d-4a22-a5f2-84ceac1fa094", "Отделение на Урицкого");
      branchService.add(branch3.getId(), branch3);
      branchService.add(branch4.getId(), branch4);
      branchService.add(branch5.getId(), branch5);
    }
  }
}
