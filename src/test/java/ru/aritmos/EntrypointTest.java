package ru.aritmos;

import static org.junit.jupiter.api.Assertions.assertThrows;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.security.utils.SecurityService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import ru.aritmos.api.EntrypointController;
import ru.aritmos.api.ManagementController;
import ru.aritmos.api.ServicePointController;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.*;
import ru.aritmos.model.Queue;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.VisitService;

@Nested
@Slf4j
@MicronautTest
class EntrypointTest {

  final String branchId = "bc08b7d2-c731-438d-9785-eba2078b2089";
  final String serviceId = "c3916e7f-7bea-4490-b9d1-0d4064adbe8c";
  final String creditCardId = "35d73fdd-1597-4d94-a087-fd8a99c9d1ed";
  final String acceptedOutcomeID = "462bac1a-568a-4f1f-9548-1c7b61792b4b";
  final String creditCardGivenId = "8dc29622-cd87-4384-85a7-04b66b28dd0f";
  final String servicePointFcId = "82f01817-3376-42de-b97d-cbc84549e550";
  User psokolovUser;
  @Inject BranchService branchService;
  @Inject EmbeddedApplication<?> application;
  @Inject VisitService visitService;
  @Inject EntrypointController entrypointController;
  @Inject ServicePointController servicePointController;
  @Inject ManagementController managementController;
  @Inject SecurityService securityService;
  @Inject EventService eventService;
  Branch branch;
  @Inject
  private KeyCloackClient keyCloackClient;

  @Test
  void testItWorks() {
    Assertions.assertTrue(application.isRunning());
  }

  /** Создание отделения для проведения юнит теста */
  @BeforeEach
  void CreateBranch() {

    if (!branchService.getBranches().containsKey("bc08b7d2-c731-438d-9785-eba2078b2089")) {
      branch = new Branch("bc08b7d2-c731-438d-9785-eba2078b2089", "Отделение на Тверской");
      branch.setPrefix("TVR");
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
      EntryPoint entryPoint = new EntryPoint();
      entryPoint.setPrinter(
          Entity.builder().id("f1ee6a43-6584-4f16-b010-d773849f1f04").name("Intro 19").build());
      entryPoint.setId("1");
      HashMap<String, EntryPoint> entryPoints = new HashMap<>();
      entryPoints.put(entryPoint.getId(), entryPoint);
      branch.setEntryPoints(entryPoints);
      Queue queueCredit = new Queue("371986f3-7872-4a4f-ab02-731f0ae4598e", "Кредиты", "F", 9000);
      Service creditService =
          new Service("c3916e7f-7bea-4490-b9d1-0d4064adbe8c", "Кредит", 9000, queueCredit.getId());
      DeliveredService creditCard =
          new DeliveredService("35d73fdd-1597-4d94-a087-fd8a99c9d1ed", "Кредитная карта");
      DeliveredService insurance =
          new DeliveredService("daa17035-7bd7-403f-a036-6c14b81e666f", "Страховка");
      Outcome creditAccepted = new Outcome("462bac1a-568a-4f1f-9548-1c7b61792b4b", "Одобрен");
      Outcome creditCardGiven = new Outcome("8dc29622-cd87-4384-85a7-04b66b28dd0f", "Выдана");
      creditAccepted.setCode(1L);
      creditService.getPossibleOutcomes().put(creditAccepted.getId(), creditAccepted);
      branch.getPossibleDeliveredServices().put(creditCard.getId(), creditCard);
      branch.getPossibleDeliveredServices().put(insurance.getId(), insurance);
      Queue queueBigCredit =
          new Queue("bd4b586e-c93e-4e07-9a76-586dd84ddea5", "Очень большие кредиты", "S", 9000);
      Service bigCreditService =
          new Service(
              "569769e8-3bb3-4263-bd2e-42d8b3ec0bd4",
              "Очень большой кредит",
              9000,
              queueBigCredit.getId());
      Queue queueC = new Queue("В кассу", "C", 9000);
      Service kassaService =
          new Service("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67", "Касса", 9000, queueC.getId());
      ServicePoint servicePointFC = new ServicePoint(servicePointFcId, "Финансовый консультант");
      HashMap<String, Service> serviceList = new HashMap<>();
      serviceList.put(kassaService.getId(), kassaService);
      serviceList.put(creditService.getId(), creditService);
      serviceList.put(bigCreditService.getId(), bigCreditService);
      creditCard.getServiceIds().add(creditService.getId());
      creditCard.getServiceIds().add(bigCreditService.getId());
      insurance.getServiceIds().add(creditService.getId());
      insurance.getServiceIds().add(bigCreditService.getId());

      branch.setServices(serviceList);
      ServiceGroup creditServiceGroup =
          new ServiceGroup(
              "c004322d-f426-4de5-b2ff-b904480efb8b",
              "Кредиты",
              new ArrayList<String>() {
                {
                  add(creditService.getId());
                  add(bigCreditService.getId());
                }
              },
              branchId);
      branch.adUpdateServiceGroups(
          new HashMap<String, ServiceGroup>() {
            {
              put(creditServiceGroup.getId(), creditServiceGroup);
            }
          },
          eventService);
      creditCard.getPossibleOutcomes().put(creditCardGiven.getId(), creditCardGiven);
      HashMap<String, SegmentationRuleData> rules =
          new HashMap<String, SegmentationRuleData>() {
            {
              put(
                  "c7bad215-a6c1-45f1-ab59-44dac8f4c9d6",
                  SegmentationRuleData.builder()
                      .id("c7bad215-a6c1-45f1-ab59-44dac8f4c9d6")
                      .serviceGroupId(creditService.getServiceGroupId())
                      .queueId(queueCredit.getId())
                      .visitProperty(
                          new HashMap<>() {
                            {
                              put("sex", "male");
                              put("age", "18-32");
                            }
                          })
                      .build());
              put(
                  "44ab710f-4f0c-46bf-ab1a-9599d9df52b2",
                  SegmentationRuleData.builder()
                      .id("44ab710f-4f0c-46bf-ab1a-9599d9df52b2")
                      .serviceGroupId(bigCreditService.getServiceGroupId())
                      .queueId(queueBigCredit.getId())
                      .visitProperty(
                          new HashMap<>() {
                            {
                              put("sex", "male");
                              put("age", "33-55");
                            }
                          })
                      .build());
            }
          };
      branch.setSegmentationRules(rules);
      ServicePoint servicePointFSC =
          new ServicePoint(
              "be675d63-c5a1-41a9-a345-c82102ac42cc", "Старший финансовый консультант");
      ServicePoint servicePointC = new ServicePoint("Касса");
      HashMap<String, ServicePoint> servicePointMap = new HashMap<>();
      servicePointMap.put(servicePointFC.getId(), servicePointFC);
      servicePointMap.put(servicePointFSC.getId(), servicePointFSC);
      servicePointMap.put(servicePointC.getId(), servicePointC);
      WorkProfile workProfileC = new WorkProfile("Кассир");
      workProfileC.getQueueIds().add(queueC.getId());
      WorkProfile workProfileFC = new WorkProfile("Финансовый консультант");
      workProfileFC.getQueueIds().add(queueCredit.getId());
      WorkProfile workProfileFSC = new WorkProfile("Старший финансовый консультант");
      workProfileFSC.getQueueIds().add(queueBigCredit.getId());
      workProfileFSC.getQueueIds().add(queueCredit.getId());
      HashMap<String, Queue> queueMap = new HashMap<>();
      queueMap.put(queueCredit.getId(), queueCredit);
      queueMap.put(queueBigCredit.getId(), queueBigCredit);
      queueMap.put(queueC.getId(), queueC);

      branch.setQueues(queueMap);
      branch.setServicePoints(servicePointMap);

      branch.getWorkProfiles().put(workProfileC.getId(), workProfileC);
      branch.getWorkProfiles().put(workProfileFC.getId(), workProfileFC);
      branch.getWorkProfiles().put(workProfileFSC.getId(), workProfileFSC);
      branchService.add(branch.getId(), branch);
      this.psokolovUser =
          branchService.openServicePoint(
              branchId, "psokolov", servicePointFC.getId(), workProfileFSC.getId(), visitService);
      branch = branchService.getBranch(branchId);
      log.info(branchService.getBranches().toString());
    }
  }
  /** Проверка правильности формаирования номера талона */
  @Test
  void checkUpdateVisit() throws SystemException {
    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals(serviceId))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;
    Visit visit;
    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    visit = visitService.createVisit(branchId, "1", visitParameters, false);

    Queue queue = branchService.getBranch(branchId).getQueues().get(service.getLinkedQueueId());
    Queue queue2 =
        branchService.getBranch(branchId).getQueues().values().stream()
            .filter(f -> f.getName().contains("кассу"))
            .toList()
            .get(0);
    visit.setQueueId(queue2.getId());
    Assertions.assertEquals(
        branchService.getBranch(branchId).getQueues().get(queue.getId()).getVisits().size(), 1);
    Assertions.assertEquals(
        branchService.getBranch(branchId).getQueues().get(queue2.getId()).getVisits().size(), 0);

    branchService.updateVisit(visit, "TESTED_VISIT_UPDATE_METHOD", visitService);
    Assertions.assertEquals(
        branchService.getBranch(branchId).getQueues().get(queue.getId()).getVisits().size(), 0);
    Assertions.assertEquals(
        branchService.getBranch(branchId).getQueues().get(queue2.getId()).getVisits().size(), 1);
  }

  @Test
  void checkConfirmVisit() throws InterruptedException, SystemException {
    Service service;
    service =
        branchService.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals(serviceId))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;
    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    Optional<Visit> visitForConfirm =
        visitService.visitCallForConfirmWithMaxWaitingTime(
            branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
    if (visitForConfirm.isPresent()) {
      Long servtime = visitForConfirm.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(200);
      visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
      Thread.sleep(200);
      visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
      Thread.sleep(200);
      Visit visit2;
      visit2 = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
      Assertions.assertEquals(visit2.getStatus(), VisitEvent.END.name());
    }
  }

  @Test
  void checkConfirmVisitWithCallRule() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;
    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    visitService.createVisit(branchId, "1", visitParameters, false);
    Thread.sleep(300);
    Optional<Visit> currvisit =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId);
    if (currvisit.isPresent()) {
      Long servtime = currvisit.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(300);
      visitService.visitReCallForConfirm(
          branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", currvisit.get());
      Thread.sleep(300);
      visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", currvisit.get());
      visitService.addMark(
          branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "d75076be-d3e0-4323-b7db-b32ea6b30817");
      visitService.addDeliveredService(
          branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", creditCardId);

      visitService.addOutcomeOfDeliveredService(
          branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", creditCardId, creditCardGivenId);
      visitService.addOutcomeService(
          branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", acceptedOutcomeID);
      Visit visit;
      visitService.addMark(
          branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "04992364-9e96-4ec9-8a05-923766aa57e7");
      visit =
          visitService.addMark(
              branchId,
              "be675d63-c5a1-41a9-a345-c82102ac42cc",
              "d75076be-d3e0-4323-b7db-b32ea6b30817");
      Assertions.assertEquals(visit.getVisitMarks().size(), 3);
      visit =
          visitService.deleteMark(
              branchId,
              "be675d63-c5a1-41a9-a345-c82102ac42cc",
              "d75076be-d3e0-4323-b7db-b32ea6b30817");
      Assertions.assertEquals(visit.getVisitMarks().size(), 1);
      Thread.sleep(300);

      Visit visit2 = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");

      Assertions.assertEquals(visit2.getStatus(), VisitEvent.END.name());
    }
  }

  @Test
  void checkConfirmVisitWithCallRuleTwoServices() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);
    Service service2;
    service2 =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();

    serviceIds.add(service.getId());

    serviceIds.add(service2.getId());

    VisitParameters visitParameters = new VisitParameters();
    visitParameters.getParameters().put("sex", "male");
    visitParameters.getParameters().put("age", "33-55");
    visitParameters.setServiceIds(serviceIds);
    Visit visit = entrypointController.createVisit(branchId, "1", visitParameters, false, null);
    // Visit visit=visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(1000);
    Optional<Visit> visitOptional =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId);
    if (visitOptional.isPresent()) {
      Long servtime = visitOptional.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(800);
      visit =
          visitService.visitReCallForConfirm(
              branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visitOptional.get());
      Thread.sleep(600);
      visit = visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);

      Thread.sleep(900);

      Visit visit2 = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
      Thread.sleep(900);
      Assertions.assertEquals(visit2.getStatus(), VisitEvent.PLACED_IN_QUEUE.getState().name());
    }
  }

  @Test
  void checkConfirmVisitWithCallRuleMaxLifeTimeTwoServices()
      throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);
    Service service2;
    service2 =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service2 != null;
    serviceIds.add(service.getId());
    assert service != null;
    serviceIds.add(service2.getId());

    VisitParameters visitParameters = new VisitParameters();
    visitParameters.getParameters().put("sex", "male");
    visitParameters.getParameters().put("age", "33-55");
    visitParameters.setServiceIds(serviceIds);
    Visit visit = entrypointController.createVisit(branchId, "1", visitParameters, false, null);
    // Visit visit=visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(1000);
    Optional<Visit> visitOptional =
        visitService.visitCallForConfirmWithMaxLifeTime(branchId, servicePointFcId);
    if (visitOptional.isPresent()) {
      Long servtime = visitOptional.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(800);
      visit =
          visitService.visitReCallForConfirm(
              branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visitOptional.get());
      Thread.sleep(600);
      visit = visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);

      Thread.sleep(900);

      Visit visit2 = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");

      Assertions.assertEquals(visit2.getStatus(), VisitEvent.BACK_TO_QUEUE.getState().name());
    }
  }

  @Test
  void checkBackCalledUserToPoolVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    //    visit =
    //        visitService.visitTransferFromQueueToUserPool(branchId, psokolovUser.getId(), visit,
    // false);
    //    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);
    //
    //    Thread.sleep(1000);
    Optional<Visit> visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(
            branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
    //    if (visits.isPresent()) {
    //      Long servtime = visits.get().getServingTime();
    //      Assertions.assertEquals(servtime, 0);
    //      Thread.sleep(800);
    //      visit =
    //          visitService.visitReCallForConfirm(
    //              branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
    //      Thread.sleep(600);
    //
    //      visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
    //      Thread.sleep(600);
    //      visit = visitService.visitPutBack(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc",
    // 150L);
    //
    //      Thread.sleep(900);
    //
    //      Assertions.assertEquals(
    //          0,
    //          branchService
    //              .getBranch(branchId)
    //              .getServicePoints()
    //              .get("be675d63-c5a1-41a9-a345-c82102ac42cc")
    //              .getVisits()
    //              .size());
    //      Assertions.assertEquals(visit.getStatus(),
    // VisitEvent.BACK_TO_USER_POOL.getState().name());
    //      visits =
    //          visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc",
    // visit);
    Visit visittoBack = visitService.backCalledVisit(branchId, visits.get().getId(), 60L);
    visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(
            branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
    visittoBack = visitService.backCalledVisit(branchId, visits.get().getId(), 60L);
    Assertions.assertEquals(visittoBack.getStatus(), VisitEvent.BACK_TO_QUEUE.getState().name());
    //   }
  }

  // @Test
  void checkBackUserToPoolVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    visit =
        visitService.visitTransferFromQueueToUserPool(branchId, psokolovUser.getId(), visit, false,0L);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(1000);
    Optional<Visit> visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(
            branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
    if (visits.isPresent()) {
      Long servtime = visits.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(800);
      visit =
          visitService.visitReCallForConfirm(
              branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
      Thread.sleep(600);

      visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
      Thread.sleep(600);
      visit = visitService.visitPutBack(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", 150L);

      Thread.sleep(900);

      Assertions.assertEquals(
          0,
          branchService
              .getBranch(branchId)
              .getServicePoints()
              .get("be675d63-c5a1-41a9-a345-c82102ac42cc")
              .getVisits()
              .size());
      Assertions.assertEquals(visit.getStatus(), VisitEvent.BACK_TO_USER_POOL.getState().name());
      visits =
          visitService.visitCallForConfirmWithMaxWaitingTime(
              branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
      if (visits.isPresent()) {
        Thread.sleep(900);

        visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visits.get());
        Thread.sleep(900);
        visit = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
        Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
      }
    }
  }

  @Test
  void checkPostPoneVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(1000);
    Optional<Visit> visitOptional =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId);
    if (visitOptional.isPresent()) {
      Long servtime = visitOptional.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(800);
      visit = visitService.visitReCallForConfirm(branchId, servicePointFcId, visitOptional.get());
      Thread.sleep(600);

      visitService.visitConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(600);
      String servicePointId = servicePointFcId;
      String userId = psokolovUser.getId();
      visit = visitService.visitPostPone(branchId, servicePointId);

      Thread.sleep(900);

      Assertions.assertEquals(
          1,
          branchService
              .getBranch(branchId)
              .getServicePoints()
              .get(servicePointId)
              .getUser()
              .getVisits()
              .size());
      Assertions.assertEquals(visit.getStatus(), VisitEvent.BACK_TO_USER_POOL.getState().name());
      Optional<Visit> visits =
          visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
      if (visits.isPresent()) {
        Thread.sleep(900);

        visitService.visitConfirm(branchId, servicePointFcId, visits.get());
        Thread.sleep(900);
        visit = visitService.visitEnd(branchId, servicePointFcId);
        Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
      }
    }
  }

  @Test
  void checkBackServicePointPoolVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        branchService.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    visit =
        visitService.visitTransferFromQueueToServicePointPool(
            branchId, servicePointFcId, servicePointFcId, visit, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(1000);
    Optional<Visit> visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(
            branchId, "993211e7-31c8-4dcf-bab1-ecf7b9094d4c", visit);
    if (visits.isPresent()) {
      Long servtime = visits.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(800);
      visit = visitService.visitReCallForConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(600);

      visitService.visitConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(600);
      visit = visitService.visitPutBack(branchId, servicePointFcId, 150L);

      Thread.sleep(900);

      Assertions.assertEquals(
          1,
          branchService
              .getBranch(branchId)
              .getServicePoints()
              .get(servicePointFcId)
              .getVisits()
              .size());
      Assertions.assertEquals(
          visit.getStatus(), VisitEvent.BACK_TO_SERVICE_POINT_POOL.getState().name());
      visits =
          visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
      if (visits.isPresent()) {
        Thread.sleep(900);

        visitService.visitConfirm(branchId, servicePointFcId, visits.get());
        Thread.sleep(900);
        visit = visitService.visitEnd(branchId, servicePointFcId);
        Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
      }
    }
  }

  @Test
  void checkBackQueueVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    visit =
        visitService.visitTransfer(
            branchId, servicePointFcId, "bd4b586e-c93e-4e07-9a76-586dd84ddea5", visit, true);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(1000);
    Optional<Visit> visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
    if (visits.isPresent()) {
      Long servtime = visits.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(800);
      visit = visitService.visitReCallForConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(600);

      visitService.visitConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(600);
      visit = visitService.visitPutBack(branchId, servicePointFcId, 5L);

      // Thread.sleep(1500L);

      Assertions.assertEquals(
          0,
          branchService
              .getBranch(branchId)
              .getServicePoints()
              .get(servicePointFcId)
              .getVisits()
              .size());
      Assertions.assertEquals(visit.getStatus(), VisitEvent.BACK_TO_QUEUE.getState().name());
      visits =
          visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
      if (visits.isPresent()) {
        Thread.sleep(900);

        visitService.visitConfirm(branchId, servicePointFcId, visits.get());
        Thread.sleep(900);
        visit = visitService.visitEnd(branchId, servicePointFcId);
        Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
      }
    }
  }

  @Test
  void checkBackToUserPoolVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(1000);
    Optional<Visit> visitOptional =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId);
    if (visitOptional.isPresent()) {
      Long servtime = visitOptional.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(800);
      visit = visitService.visitReCallForConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(600);

      visitService.visitConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(600);
      String servicePointId = servicePointFcId;
      String userId = psokolovUser.getId();
      visit = visitService.visitBackToUserPool(branchId, servicePointId, userId, 150L);

      Thread.sleep(900);

      Assertions.assertEquals(
          1,
          branchService
              .getBranch(branchId)
              .getServicePoints()
              .get(servicePointFcId)
              .getUser()
              .getVisits()
              .size());
      Assertions.assertEquals(visit.getStatus(), VisitEvent.BACK_TO_USER_POOL.getState().name());
      Optional<Visit> visits =
          visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
      if (visits.isPresent()) {
        Thread.sleep(900);

        visitService.visitConfirm(branchId, servicePointFcId, visits.get());
        Thread.sleep(900);
        visit = visitService.visitEnd(branchId, servicePointFcId);
        Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
      }
    }
  }

  @Test
  void checkTransferToServicePoolVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(1000);

    visit =
        visitService.visitTransferFromQueueToServicePointPool(
            branchId, servicePointFcId, servicePointFcId, visit, true);

    Thread.sleep(900);

    Assertions.assertEquals(
        1,
        branchService
            .getBranch(branchId)
            .getServicePoints()
            .get(servicePointFcId)
            .getVisits()
            .size());
    Assertions.assertEquals(
        visit.getStatus(), VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getState().name());
    Optional<Visit> visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
    if (visits.isPresent()) {
      Thread.sleep(900);

      visitService.visitConfirm(branchId, servicePointFcId, visits.get());
      Thread.sleep(900);
      visit = visitService.visitEnd(branchId, servicePointFcId);
      Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
    }
  }

  @Test
  void checkTransferToUserPoolVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);
    Queue queue = managementController.getBranch(branchId).getQueues().get(visit.getQueueId());
    Thread.sleep(1000);

    visit =
        visitService.visitTransferFromQueueToUserPool(branchId, psokolovUser.getId(), visit, true,0L);

    String visitId = visit.getId();
    Assertions.assertEquals(
        queue.getVisits().stream().filter(f -> f.getId().equals(visitId)).count(), 0);
    Assertions.assertEquals(
        1,
        branchService
            .getBranch(branchId)
            .getServicePoints()
            .get(servicePointFcId)
            .getUser()
            .getVisits()
            .size());
    Assertions.assertEquals(visit.getStatus(), VisitEvent.TRANSFER_TO_USER_POOL.getState().name());
    visit =
        visitService.visitTransferFromQueueToServicePointPool(
            branchId, servicePointFcId, servicePointFcId, visit, true);
    Assertions.assertEquals(
        managementController.getBranch(branchId).getAllVisitsList().stream()
            .filter(f -> f.getId().equals(visitId))
            .count(),
        1);

    // if(queue.getVisits().stream().filter(f->f.getId().equals(visit.getId())).count()>0)
    Thread.sleep(900);

    Optional<Visit> visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
    if (visits.isPresent()) {
      Thread.sleep(900);

      visitService.visitConfirm(branchId, servicePointFcId, visits.get());
      Thread.sleep(900);
      visit =
          visitService.visitTransferFromQueueToUserPool(
              branchId, psokolovUser.getId(), visit, true,0L);
      visit =
          visitService.visitTransferFromQueueToServicePointPool(
              branchId, servicePointFcId, servicePointFcId, visit, true);
      visits =
          visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
      if (visits.isPresent()) {

        visitService.visitConfirm(branchId, servicePointFcId, visits.get());
        Thread.sleep(900);
        Assertions.assertEquals(
            managementController.getBranch(branchId).getAllVisitsList().stream()
                .filter(f -> f.getId().equals(visitId))
                .count(),
            1);
        visit = visitService.visitEnd(branchId, servicePointFcId);
        Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
        Assertions.assertEquals(
            managementController.getBranch(branchId).getAllVisitsList().stream()
                .filter(f -> f.getId().equals(visitId))
                .count(),
            0);
      }
    }
  }

  @Test
  void checkTransferToQueueVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    VisitParameters visitParameters2 =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    Visit visit2 = visitService.createVisit(branchId, "1", visitParameters2, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    visit =
        visitService.visitTransfer(
            branchId, servicePointFcId, "bd4b586e-c93e-4e07-9a76-586dd84ddea5", visit, true);
    Thread.sleep(10000);
    visit2 =
        visitService.visitTransfer(
            branchId, servicePointFcId, "bd4b586e-c93e-4e07-9a76-586dd84ddea5", visit2, true);

    Thread.sleep(900);

    Assertions.assertEquals(
        2,
        branchService
            .getBranch(branchId)
            .getQueues()
            .get("bd4b586e-c93e-4e07-9a76-586dd84ddea5")
            .getVisits()
            .size());
    Assertions.assertEquals(visit.getStatus(), VisitEvent.TRANSFER_TO_QUEUE.getState().name());
    Optional<Visit> visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
    if (visits.isPresent()) {
      Thread.sleep(900);

      visitService.visitConfirm(branchId, servicePointFcId, visits.get());
      Thread.sleep(900);
      visit = visitService.visitEnd(branchId, servicePointFcId);
      Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
    }
  }

  @Test
  void checkTransferToQueueVisitWithoutConfirm() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    VisitParameters visitParameters2 =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    Visit visit2 = visitService.createVisit(branchId, "1", visitParameters2, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    visit =
        visitService.visitTransfer(
            branchId, servicePointFcId, "bd4b586e-c93e-4e07-9a76-586dd84ddea5", visit, true);
    Thread.sleep(1000);
    Assertions.assertTrue(visit.getWaitingTime() >= 0);

    visit2 =
        visitService.visitTransfer(
            branchId, servicePointFcId, "bd4b586e-c93e-4e07-9a76-586dd84ddea5", visit2, true);
    Thread.sleep(1000);
    Assertions.assertTrue(visit2.getWaitingTime() >= 0);

    Thread.sleep(900);

    Assertions.assertEquals(
        2,
        branchService
            .getBranch(branchId)
            .getQueues()
            .get("bd4b586e-c93e-4e07-9a76-586dd84ddea5")
            .getVisits()
            .size());
    Assertions.assertEquals(visit.getStatus(), VisitEvent.TRANSFER_TO_QUEUE.getState().name());
    Optional<Visit> visits =
        visitService.visitCallWithMaximalWaitingTime(
            branchId, servicePointFcId, List.of("bd4b586e-c93e-4e07-9a76-586dd84ddea5"));
    if (visits.isPresent()) {
      Thread.sleep(900);

      Thread.sleep(900);
      visit = visitService.visitEnd(branchId, servicePointFcId);
      Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
    }
    Optional<Visit> visits2 =
        visitService.visitCallWithMaximalWaitingTime(
            branchId, servicePointFcId, List.of("bd4b586e-c93e-4e07-9a76-586dd84ddea5"));
    if (visits2.isPresent()) {
      Thread.sleep(900);

      Thread.sleep(900);
      Visit visit3 =
          visitService.visitTransfer(
              branchId,
              servicePointFcId,
              "bd4b586e-c93e-4e07-9a76-586dd84ddea5",
              visits2.get(),
              true);
      Assertions.assertTrue(visit2.getWaitingTime() >= 0);
    }
  }

  @Test
  void checkTransferToPoolVisitWithCallRuleFromQueue()
      throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);
    Service service2;
    service2 =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());
    assert service2 != null;
    serviceIds.add(service2.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);
    visitService.visitTransferFromQueueToServicePointPool(
        branchId, servicePointFcId, servicePointFcId, visit, true);
    Thread.sleep(1000);

    Assertions.assertEquals(
        1,
        branchService
            .getBranch(branchId)
            .getServicePoints()
            .get(servicePointFcId)
            .getVisits()
            .size());
    Assertions.assertEquals(
        visit.getStatus(), VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getState().name());
  }

  @Test
  void checkTransferToUserPoolVisitWithCallRuleFromQueue()
      throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);
    Service service2;
    service2 =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());
    assert service2 != null;
    serviceIds.add(service2.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVisit(branchId, "1", visitParameters, false);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);
    visit =
        visitService.visitTransferFromQueueToUserPool(branchId, psokolovUser.getId(), visit, true,0L);
    Thread.sleep(1000);

    Assertions.assertEquals(
        1,
        branchService
            .getBranch(branchId)
            .getServicePoints()
            .get(servicePointFcId)
            .getUser()
            .getVisits()
            .size());
    Assertions.assertEquals(visit.getStatus(), VisitEvent.TRANSFER_TO_USER_POOL.getState().name());
  }

  @Test
  void checkcreateVirtualVisit() throws InterruptedException, SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c"))
            .findFirst()
            .orElse(null);
    Service service2;
    service2 =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67"))
            .findFirst()
            .orElse(null);

    ArrayList<String> serviceIds = new ArrayList<>();
    assert service != null;
    serviceIds.add(service.getId());

    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    Visit visit = visitService.createVirtualVisit(branchId, servicePointFcId, visitParameters);
    // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

    Thread.sleep(3000);
    visit = visitService.visitEnd(branchId, servicePointFcId);

    Assertions.assertEquals(visit.getStatus(), VisitEvent.END.getState().name());
  }

  /**
   * Проверка завершения визита с итогом "Не пришел"
   *
   * @throws InterruptedException исключение вызываемое прерыванием потока
   */
  @Test
  void checkNoShowVisit() throws InterruptedException, SystemException {
    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals(serviceId))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;
    Visit visit;
    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    visit = visitService.createVisit(branchId, "1", visitParameters, false);
    Optional<Visit> visits =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
    if (visits.isPresent()) {
      Long servtime = visits.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(200);
      visit = visitService.visitReCallForConfirm(branchId, servicePointFcId, visits.get());
      Thread.sleep(200);
      Optional<Visit> visits2 = visitService.visitNoShow(branchId, servicePointFcId, visit);
      if (visits2.isPresent()) {
        Visit visit2 = visits2.get();

        Assertions.assertEquals(visit2.getStatus(), VisitEvent.NO_SHOW.getState().name());
      }
    }
  }

  @Test
  void checkNoShowVisitWithoutConfirm() throws InterruptedException, SystemException {
    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals(serviceId))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;
    Visit visit;
    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    visit = visitService.createVisit(branchId, "1", visitParameters, false);
    Optional<Visit> visits =
        visitService.visitCallWithMaximalWaitingTime(branchId, servicePointFcId);
    if (visits.isPresent()) {
      Long servtime = visits.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(200);
      visit = visitService.visitReCallForConfirm(branchId, servicePointFcId, visits.get());
      Thread.sleep(200);
      Optional<Visit> visits2 = visitService.visitNoShow(branchId, servicePointFcId, visit);
      if (visits2.isPresent()) {
        Visit visit2 = visits2.get();

        Assertions.assertEquals(visit2.getStatus(), VisitEvent.NO_SHOW.getState().name());
      }
    }
  }


  @Test
  void checkConfirmVisitWithRuleInterrupted() throws InterruptedException, SystemException {
    Service service;
    service =
        branchService.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals(serviceId))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;

    Visit visit =
        entrypointController.createVisit(
            branchId,
            "1",
            VisitParameters.builder()
                .serviceIds(serviceIds)
                .parameters(
                    new HashMap<>() {
                      {
                        put("sex", "male");
                        put("age", "33-55");
                        put("level", "vip");
                      }
                    })
                .build(),
            false,
            null);

    Assertions.assertEquals(visit.getQueueId(), "bd4b586e-c93e-4e07-9a76-586dd84ddea5");
    Optional<Visit> visitForConfirm =
        visitService.visitCallForConfirmWithMaxWaitingTime(branchId, servicePointFcId, visit);
    if (visitForConfirm.isPresent()) {
      Long servtime = visitForConfirm.get().getServingTime();
      Assertions.assertEquals(servtime, 0);
      Thread.sleep(200);
      visitService.visitReCallForConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(200);
      visitService.visitConfirm(branchId, servicePointFcId, visit);
      Thread.sleep(200);
      Visit visit2;
    }
  }

  /** Проверка корректности формирования номера талона */
  @Test
  void checkTicetNumberlogic() throws SystemException {

    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals(serviceId))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;

    Visit visit;
    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    visit = visitService.createVisit(branchId, "1", visitParameters, false);

    Queue queue =
        managementController.getBranch(branchId).getQueues().get(service.getLinkedQueueId());

    Assertions.assertEquals(
        visit.getTicket(),
        queue.getTicketPrefix() + String.format("%03d", queue.getTicketCounter()));
  }
 // @Test
  public void getSessions()
  {
    keyCloackClient.getUserBySid("0426430a-972a-4c12-baa5-ed0cb91a1f2d");
  }
  /** Проверка правильности работы счетчика визитов */
  @Test
  void checkVisitCounter() throws SystemException {
    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals(serviceId))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;
    Integer visitsbefore =
        managementController
            .getBranch(branchId)
            .getQueues()
            .get(service.getLinkedQueueId())
            .getTicketCounter();
    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();
    visitService.createVisit(branchId, "1", visitParameters, false);
    Integer visitafter =
        managementController
            .getBranch(branchId)
            .getQueues()
            .get(service.getLinkedQueueId())
            .getTicketCounter();
    Assertions.assertEquals(1, visitafter - visitsbefore);
  }

  /** Проверка наличия созданного визита в очереди */
  @Test
  void checkVisitInQueue() throws SystemException {
    Service service;
    service =
        managementController.getBranch(branchId).getServices().values().stream()
            .filter(f -> f.getId().equals(serviceId))
            .findFirst()
            .orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;
    VisitParameters visitParameters =
        VisitParameters.builder().serviceIds(serviceIds).parameters(new HashMap<>()).build();

    String visitId = visitService.createVisit(branchId, "1", visitParameters, false).getId();
    Queue queue =
        managementController.getBranch(branchId).getQueues().get(service.getLinkedQueueId());
    Assertions.assertTrue(queue.getVisits().stream().map(Visit::getId).toList().contains(visitId));
  }

  @AfterEach
  void deleteBranch() {
    branchService.closeServicePoint(branchId, servicePointFcId, visitService, true, false, "");
    branchService.delete(branchId);
  }

  /** Проверка сохранения изменения состояния отделения в кэше редис */
  @Test()
  void testUpdateBranchInCache() {

    Branch branch = branchService.getBranch(branchId);
    String name = branch.getName();

    Branch br2 = branchService.getBranch(branchId);
    br2.setName("Отделение на Ямской");
    branchService.add(branchId, br2);
    String name2 = branchService.getBranch(branchId).getName();
    Assertions.assertNotEquals(name2, name);
  }

  /** Проверка правильности отработки ошибки вызова не существующего отделения */
  @Test
  void getNotExistBranch() {
    Exception exception =
        assertThrows(HttpStatusException.class, () -> branchService.getBranch("not exist"));
    Assertions.assertEquals(exception.getMessage(), "Branch not found!!");
  }

  @Test
  public void testGroovy() throws IOException, URISyntaxException, InterruptedException {

    ClassLoader classLoader = getClass().getClassLoader();

    // Объект привязки скрипта со средой выполнения (для передачи значений переменных, свойств и т д
    // )
    // и для получения значений переменных и свойств после выполнения скрипта
    Binding binding = new Binding();
    // Объект выполнения скрипта Groovy
    GroovyShell shell = new GroovyShell(binding);
    // Текст скрипта на groovy, подгружается из файла (возможно так же и другое хранение скрипта)
    String scriptCode =
        Files.readString(
            Paths.get(Objects.requireNonNull(classLoader.getResource("test.groovy")).toURI()));
    // Обработка скрипта перед выполнением
    Script script = shell.parse(scriptCode);
    // Передача двух тестовых визитов
    binding.setVariable(
        "visits",
        new ArrayList<Visit>() {
          {
            add(
                Visit.builder()
                    .id("test1")
                    .createDateTime(ZonedDateTime.now())
                    .queueId("123")
                    .build());
            Thread.sleep(0);
            add(
                Visit.builder()
                    .id("test2")
                    .createDateTime(ZonedDateTime.now())
                    .servicePointId("123")
                    .build());
            Thread.sleep(10);
            add(
                Visit.builder()
                    .id("test2")
                    .createDateTime(ZonedDateTime.now())
                    .poolUserId("20")
                    .build());
          }
        });
    // Передача дополнительных параметров, в данном случае идентификаторы очереди, юзерпула и пула
    // точки обслуживания
    // из которых нужно извлекать визиты
    binding.setVariable(
        "params",
        new HashMap<String, Object>() {
          {
            put("queueId", "123");
            put("userPoolId", "123");
            put("servicePointId", "123");
          }
        });
    // Запуск выполнения скрипта
    script.run();
    // Получение значений всех переменных скрипта
    var result = binding.getVariables();
    // Получение оптимального визита из двух, согласно алгоритму описанному в groovya crhbgnt
    Optional<Visit> optimalVisit = (Optional<Visit>) result.get("result");

    Assertions.assertTrue(optimalVisit.isPresent());
    Assertions.assertEquals(optimalVisit.get().getQueueId(), "123");
    log.info("Оптимальный визит:{}!", optimalVisit);
  }
}
