package ru.aritmos.service;

import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.*;

@Slf4j
@Context
public class Configuration {
  @Inject BranchService branchService;
  @Inject KeyCloackClient keyCloackClient;

  public void getConfiguration() {

    if (branchService.getBranches().isEmpty()) {
      //        if (branchService.getBranches().isEmpty()) {

      Branch branch = new Branch("37493d1c-8282-4417-a729-dceac1f3e2b4", "Отделение на Тверской");
      branch.setAddress("Москва, ул. Тверская 13");
      branch.setDescription("Главное отделение");
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
      branch.getBreakReasons().put("5454e87a-9ace-46fd-be30-b00f7bb88688", "Вышел покурить");
      branch.getBreakReasons().put("75caee17-d8f3-49b1-b298-96bbb6ba19f3", "Вышел на обед");
      EntryPoint entryPoint = new EntryPoint();
      entryPoint.setPrinter(
          Entity.builder().id("eb7ea46d-c995-4ca0-ba92-c92151473214").name("Intro18").build());
      entryPoint.setId("2");
      HashMap<String, EntryPoint> entryPoints = new HashMap<>();
      entryPoints.put(entryPoint.getId(), entryPoint);
      branch.setEntryPoints(entryPoints);
      Queue queueCredit = new Queue("55da9b66-c928-4d47-9811-dbbab20d3780", "Кредиты", "F");
      queueCredit.setWaitingSL(200);
      Service creditService =
          new Service("c3916e7f-7bea-4490-b9d1-0d4064adbe8b", "Кредит", 9000, queueCredit.getId());
      Outcome creditAccepted = new Outcome("462bac1a-568a-4f1f-9548-1c7b61792b4b", "Одобрен");
      creditAccepted.setCode(1L);
      creditService.getPossibleOutcomes().put(creditAccepted.getId(), creditAccepted);
      DeliveredService creditCard =
          new DeliveredService("35d73fdd-1597-4d94-a087-fd8a99c9d1ed", "Кредитная карта");
      creditCard.getServiceIds().add(creditService.getId());
      Outcome creditCardGiven = new Outcome("8dc29622-cd87-4384-85a7-04b66b28dd0f", "Выдана");
      creditCard.getPossibleOutcomes().put(creditCardGiven.getId(), creditCardGiven);
      branch.getPossibleDeliveredServices().put(creditCard.getId(), creditCard);

      DeliveredService insurance =
          new DeliveredService("daa17035-7bd7-403f-a036-6c14b81e666f", "Страховка");
      insurance.getServiceIds().add(creditService.getId());
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

      HashMap<String, Service> serviceList = new HashMap<>();
      serviceList.put(kassaService.getId(), kassaService);
      serviceList.put(creditService.getId(), creditService);
      serviceList.put(bigCreditService.getId(), bigCreditService);
      branch.setServices(serviceList);

      ServicePoint servicePointFSC =
          new ServicePoint(
              "099c43c1-40b5-4b80-928a-1d4b363152a8", "Старший финансовый консультант");

      ServicePoint servicePointBFSC =
          new ServicePoint(
              "090bd53d-96ba-466b-9845-d64e81894964", "Самый старший финансовый консультант");
      ServicePoint servicePointBBFSC =
          new ServicePoint(
              "f9e60eaf-b4af-4bf8-8d64-e70d2e949829", "Самый самый старший финансовый консультант");

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
      try {
        Optional<UserRepresentation> userInfo = keyCloackClient.getUserInfo(psokolovUser.getName());

        if (userInfo.isPresent()) {
          psokolovUser.setFirstName(userInfo.get().getFirstName());
          psokolovUser.setLastName(userInfo.get().getLastName());
        }
      } catch (Exception ex) {
        log.warn("Error {}", ex.getLocalizedMessage());
        psokolovUser.setFirstName("Pavel");
        psokolovUser.setLastName("Sokolov");
      }
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
      branch.getServicePoints().get(servicePointFC.getId()).setUser(psokolovUser);
      branch.getWorkProfiles().put(workProfileC.getId(), workProfileC);
      branch.getWorkProfiles().put(workProfileFC.getId(), workProfileFC);
      branch.getWorkProfiles().put(workProfileFSC.getId(), workProfileFSC);
      branch
          .getReception()
          .setPrinters(
              List.of(
                  Entity.builder()
                      .id("eb7ea46d-c995-4ca0-ba92-c92151473614")
                      .name("Intro17")
                      .build(),
                  Entity.builder()
                      .id("eb7ea46d-c995-4ca0-ba92-c92151473612")
                      .name("Intro8")
                      .build()));
      branchService.add(branch.getId(), branch);
      branchService.openServicePoint(
          branch.getId(), psokolovUser.getName(), servicePointFC.getId(), workProfileFC.getId());

      Branch branch2 = new Branch("e73601bd-2fbb-4303-9a58-16cbc4ad6ad3", "Отделение на Ямской");

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
