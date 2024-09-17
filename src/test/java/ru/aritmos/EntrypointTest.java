package ru.aritmos;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import ru.aritmos.api.EntrypointController;
import ru.aritmos.api.ManagementController;
import ru.aritmos.api.ServicePointController;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.VisitService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;


@Nested
@Slf4j
@MicronautTest
class EntrypointTest {

    @Inject
    BranchService branchService;
    @Inject
    EmbeddedApplication<?> application;
    @Inject
    VisitService visitService;
    @Inject
    EntrypointController entrypointController;
    @Inject
    ServicePointController servicePointController;
    @Inject
    ManagementController managementController;
    @Inject
    SecurityService securityService;
    final String branchId = "bc08b7d2-c731-438d-9785-eba2078b2089";
    final String serviceId = "c3916e7f-7bea-4490-b9d1-0d4064adbe8c";
    final String creditCardId = "35d73fdd-1597-4d94-a087-fd8a99c9d1ed";
    final String acceptedOutcomeID = "462bac1a-568a-4f1f-9548-1c7b61792b4b";
    final String creditCardGivenId = "8dc29622-cd87-4384-85a7-04b66b28dd0f";

    @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

    Branch branch;

    /**
     * Создание отделения для проведения юнит теста
     */
    @BeforeEach
    void CreateBranch() {
        if (!branchService.getBranches().containsKey("bc08b7d2-c731-438d-9785-eba2078b2089")) {
            branch = new Branch("bc08b7d2-c731-438d-9785-eba2078b2089", "Отделение на Тверской");
            branch.getMarks().put(
                    "04992364-9e96-4ec9-8a05-923766aa57e7",
                    Mark
                            .builder()
                            .id("04992364-9e96-4ec9-8a05-923766aa57e7")
                            .value("Клиент доволен")
                            .build()
            );
            branch.getMarks().put(
                    "d75076be-d3e0-4323-b7db-b32ea6b30817",
                    Mark
                            .builder()
                            .id("d75076be-d3e0-4323-b7db-b32ea6b30817")
                            .value("Клиент не доволен")
                            .build()
            );
            EntryPoint entryPoint = new EntryPoint();
            entryPoint.setPrinterId("2");
            entryPoint.setId("1");
            HashMap<String, EntryPoint> entryPoints = new HashMap<>();
            entryPoints.put(entryPoint.getId(), entryPoint);
            branch.setEntryPoints(entryPoints);
            Queue queueCredit = new Queue("371986f3-7872-4a4f-ab02-731f0ae4598e","Кредиты", "F");
            Service creditService = new Service("c3916e7f-7bea-4490-b9d1-0d4064adbe8c", "Кредит", 9000, queueCredit.getId());
            DeliveredService creditCard = new DeliveredService("35d73fdd-1597-4d94-a087-fd8a99c9d1ed", "Кредитная карта");
            DeliveredService insurance = new DeliveredService("daa17035-7bd7-403f-a036-6c14b81e666f", "Страховка");
            Outcome creditAccepted = new Outcome("462bac1a-568a-4f1f-9548-1c7b61792b4b", "Одобрен");
            Outcome creditCardGiven = new Outcome("8dc29622-cd87-4384-85a7-04b66b28dd0f", "Выдана");
            creditAccepted.setCode(1L);
            creditService.getPossibleOutcomes().put(creditAccepted.getId(), creditAccepted);
            branch.getPossibleDeliveredServices().put(creditCard.getId(), creditCard);
            branch.getPossibleDeliveredServices().put(insurance.getId(), insurance);
            Queue queueBigCredit = new Queue("bd4b586e-c93e-4e07-9a76-586dd84ddea5","Очень большие кредиты", "S");
            Service bigCreditService = new Service("569769e8-3bb3-4263-bd2e-42d8b3ec0bd4", "Очень большой кредит", 9000, queueBigCredit.getId());
            Queue queueC = new Queue("В кассу", "C");
            Service kassaService = new Service("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67", "Касса", 9000, queueC.getId());
            ServicePoint servicePointFC = new ServicePoint("82f01817-3376-42de-b97d-cbc84549e550","Финансовый консультант");
            HashMap<String, Service> serviceList = new HashMap<>();
            serviceList.put(kassaService.getId(), kassaService);
            serviceList.put(creditService.getId(), creditService);
            serviceList.put(bigCreditService.getId(), bigCreditService);
            creditCard.getServviceIds().add(creditService.getId());
            creditCard.getServviceIds().add(bigCreditService.getId());
            insurance.getServviceIds().add(creditService.getId());
            insurance.getServviceIds().add(bigCreditService.getId());
            creditCard.getPossibleOutcomes().put(creditCardGiven.getId(), creditCardGiven);

            branch.setServices(serviceList);
            ServicePoint servicePointFSC = new ServicePoint("be675d63-c5a1-41a9-a345-c82102ac42cc", "Старший финансовый консультант");
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
            User psokolovUser = new User("2198423c-760e-4d39-8930-12602552b1a9", "psokolov");
            psokolovUser.setBranchId(branch.getId());


            psokolovUser.setCurrentWorkProfileId(workProfileFC.getId());
            servicePointFSC.setUser(psokolovUser);
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
        }
    }

    /**
     * Проверка правильности формаирования номера талона
     */


    @Test
    void checkUpdateVisit() {
        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        Visit visit;
        visit = visitService.createVisit(branchId, "1", serviceIds, false);

        Queue queue = branchService.getBranch(branchId).getQueues().get(service.getLinkedQueueId());
        Queue queue2 = branchService.getBranch(branchId).getQueues().values().stream().filter(f -> f.getName().contains("кассу")).toList().get(0);
        visit.setQueueId(queue2.getId());
        Assertions.assertEquals(branchService.getBranch(branchId).getQueues().get(queue.getId()).getVisits().size(), 1);
        Assertions.assertEquals(branchService.getBranch(branchId).getQueues().get(queue2.getId()).getVisits().size(), 0);

        branchService.updateVisit(visit, "TESTED_VISIT_UPDATE_METHOD");
        Assertions.assertEquals(branchService.getBranch(branchId).getQueues().get(queue.getId()).getVisits().size(), 0);
        Assertions.assertEquals(branchService.getBranch(branchId).getQueues().get(queue2.getId()).getVisits().size(), 1);
    }

    @Test
    void checkConfirmVisit() throws InterruptedException {
        Service service;
        service = branchService.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        Optional<Visit> visitForConfirm = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
        if(visitForConfirm.isPresent()) {
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
    void checkConfirmVisitWithCallRule() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        visitService.createVisit(branchId, "1", serviceIds, false);
        Thread.sleep(300);
        Optional<Visit> currvisit = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
        if (currvisit.isPresent()) {
            Long servtime = currvisit.get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(300);
            visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", currvisit.get());
            Thread.sleep(300);
            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", currvisit.get());
            visitService.addMark(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "d75076be-d3e0-4323-b7db-b32ea6b30817");
            visitService.addDeliveredService(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", creditCardId);

            visitService.addOutcomeDeliveredService(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", creditCardId, creditCardGivenId);
            visitService.addOutcomeService(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", acceptedOutcomeID);
            Visit visit;
            visitService.addMark(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "04992364-9e96-4ec9-8a05-923766aa57e7");
            visit = visitService.addMark(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "d75076be-d3e0-4323-b7db-b32ea6b30817");
            Assertions.assertEquals(visit.getVisitMarks().size(), 3);
            visit = visitService.deleteMark(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "d75076be-d3e0-4323-b7db-b32ea6b30817");
            Assertions.assertEquals(visit.getVisitMarks().size(), 1);
            Thread.sleep(300);

            Visit visit2 = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");

            Assertions.assertEquals(visit2.getStatus(), VisitEvent.END.name());
        }

    }

    @Test
    void checkConfirmVisitWithCallRuleTwoServices() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);
        Service service2;
        service2 = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67")).findFirst().orElse(null);

        ArrayList<String> serviceIds = new ArrayList<>();
        assert service2 != null;
        serviceIds.add(service2.getId());
        assert service != null;
        serviceIds.add(service.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        visitService.createVisit(branchId, "1", serviceIds, false);

        Thread.sleep(1000);
        if (visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc").isPresent()) {
            Long servtime = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc").get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(800);
            visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);
            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);

            Thread.sleep(900);


            Visit visit2 = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");

            Assertions.assertEquals(visit2.getStatus(), VisitEvent.END.name());
        }

    }

    @Test
    void checkBackUserToPoolVisit() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);


        ArrayList<String> serviceIds = new ArrayList<>();
        assert service != null;
        serviceIds.add(service.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        visit=visitService.visitTransferFromQueueToUserPool(branchId,"2198423c-760e-4d39-8930-12602552b1a9",visit,false);
        // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

        Thread.sleep(1000);
        Optional<Visit> visits=visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc",visit);
        if (visits.isPresent()) {
            Long servtime = visits.get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(800);
            visit = visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);

            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);
            visit = visitService.visitPutBack(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc",150L);

            Thread.sleep(900);


            Assertions.assertEquals(0, branchService.getBranch(branchId).getServicePoints().get("be675d63-c5a1-41a9-a345-c82102ac42cc").getVisits().size());
            Assertions.assertEquals(visit.getStatus(), VisitEvent.BACK_TO_USER_POOL.getState().name());
            visits = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            if(visits.isPresent()) {
                Thread.sleep(900);

                visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visits.get());
                Thread.sleep(900);
                visit = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
                Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
            }
        }

    }
    @Test
    void checkBackServicePointPoolVisit() throws InterruptedException {

        Service service;
        service = branchService.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);


        ArrayList<String> serviceIds = new ArrayList<>();
        assert service != null;
        serviceIds.add(service.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        visit=visitService.visitTransferFromQueueToServicePointPool(branchId,"be675d63-c5a1-41a9-a345-c82102ac42cc","be675d63-c5a1-41a9-a345-c82102ac42cc",visit,false);
        // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

        Thread.sleep(1000);
        Optional<Visit> visits=visitService.visitCallForConfirm(branchId, "993211e7-31c8-4dcf-bab1-ecf7b9094d4c",visit);
        if (visits.isPresent()) {
            Long servtime = visits.get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(800);
            visit = visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);

            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);
            visit = visitService.visitPutBack(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc",150L);

            Thread.sleep(900);


            Assertions.assertEquals(1, branchService.getBranch(branchId).getServicePoints().get("be675d63-c5a1-41a9-a345-c82102ac42cc").getVisits().size());
            Assertions.assertEquals(visit.getStatus(), VisitEvent.BACK_TO_SERVICE_POINT_POOL.getState().name());
            visits = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            if(visits.isPresent()) {
                Thread.sleep(900);

                visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visits.get());
                Thread.sleep(900);
                visit = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
                Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
            }
        }

    }
    @Test
    void checkBackQueueVisit() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);


        ArrayList<String> serviceIds = new ArrayList<>();
        assert service != null;
        serviceIds.add(service.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        visit=visitService.visitTransferFromQueue(branchId,"be675d63-c5a1-41a9-a345-c82102ac42cc","bd4b586e-c93e-4e07-9a76-586dd84ddea5",visit,false);
        // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

        Thread.sleep(1000);
        Optional<Visit> visits=visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc",visit);
        if (visits.isPresent()) {
            Long servtime = visits.get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(800);
            visit = visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);

            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);
            visit = visitService.visitPutBack(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc",150L);

            Thread.sleep(900);


            Assertions.assertEquals(0, branchService.getBranch(branchId).getServicePoints().get("be675d63-c5a1-41a9-a345-c82102ac42cc").getVisits().size());
            Assertions.assertEquals(visit.getStatus(), VisitEvent.BACK_TO_QUEUE.getState().name());
            visits = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            if(visits.isPresent()) {
                Thread.sleep(900);

                visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visits.get());
                Thread.sleep(900);
                visit = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
                Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
            }
        }

    }

    @Test
    void checkBackToUserPoolVisit() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);


        ArrayList<String> serviceIds = new ArrayList<>();
        assert service != null;
        serviceIds.add(service.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

        Thread.sleep(1000);
        if (visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc").isPresent()) {
            Long servtime = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc").get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(800);
            visit = visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);

            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(600);
            String servicePointId="be675d63-c5a1-41a9-a345-c82102ac42cc";
            String userId="2198423c-760e-4d39-8930-12602552b1a9";
            visit = visitService.visitBackToUserPool(branchId, servicePointId, userId,150L);

            Thread.sleep(900);


            Assertions.assertEquals(1, branchService.getBranch(branchId).getServicePoints().get("be675d63-c5a1-41a9-a345-c82102ac42cc").getUser().getVisits().size());
            Assertions.assertEquals(visit.getStatus(), VisitEvent.BACK_TO_USER_POOL.getState().name());
            Optional<Visit> visits = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            if(visits.isPresent()) {
                Thread.sleep(900);

                visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visits.get());
                Thread.sleep(900);
                visit = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
                Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
            }
        }

    }

    @Test
    void checkTransferToPoolVisit() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);


        ArrayList<String> serviceIds = new ArrayList<>();
        assert service != null;
        serviceIds.add(service.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

        Thread.sleep(1000);


        visit = visitService.visitTransferFromQueueToServicePointPool(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "82f01817-3376-42de-b97d-cbc84549e550", visit,true);

        Thread.sleep(900);


        Assertions.assertEquals(1, branchService.getBranch(branchId).getServicePoints().get("82f01817-3376-42de-b97d-cbc84549e550").getVisits().size());
        Assertions.assertEquals(visit.getStatus(), VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getState().name());
        Optional<Visit> visits = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
        if(visits.isPresent()) {
            Thread.sleep(900);

            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visits.get());
            Thread.sleep(900);
            visit = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
            Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
        }

    }
    @Test
    void checkTransferToQueueVisit() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);


        ArrayList<String> serviceIds = new ArrayList<>();
        assert service != null;
        serviceIds.add(service.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);

        Thread.sleep(1000);


        visit = visitService.visitTransferFromQueue(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "bd4b586e-c93e-4e07-9a76-586dd84ddea5", visit,true);

        Thread.sleep(900);


        Assertions.assertEquals(1, branchService.getBranch(branchId).getQueues().get("bd4b586e-c93e-4e07-9a76-586dd84ddea5").getVisits().size());
        Assertions.assertEquals(visit.getStatus(), VisitEvent.TRANSFER_TO_QUEUE.getState().name());
        Optional<Visit> visits = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
        if(visits.isPresent()) {
            Thread.sleep(900);

            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visits.get());
            Thread.sleep(900);
            visit = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
            Assertions.assertEquals(visit.getStatus(), VisitEvent.END.name());
        }

    }
    @Test
    void checkTransferToPoolVisitWithCallRuleFromQueue() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);
        Service service2;
        service2 = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67")).findFirst().orElse(null);

        ArrayList<String> serviceIds = new ArrayList<>();
        assert service != null;
        serviceIds.add(service.getId());
        assert service2 != null;
        serviceIds.add(service2.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);
        visitService.visitTransferFromQueueToServicePointPool(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", "be675d63-c5a1-41a9-a345-c82102ac42cc", visit,true);
        Thread.sleep(1000);


        Assertions.assertEquals(1, branchService.getBranch(branchId).getServicePoints().get("be675d63-c5a1-41a9-a345-c82102ac42cc").getVisits().size());
        Assertions.assertEquals(visit.getStatus(), VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getState().name());


    }

    @Test
    void checkTransferToUserPoolVisitWithCallRuleFromQueue() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);
        Service service2;
        service2 = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67")).findFirst().orElse(null);

        ArrayList<String> serviceIds = new ArrayList<>();
        assert service != null;
        serviceIds.add(service.getId());
        assert service2 != null;
        serviceIds.add(service2.getId());


        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
        // Visit visitForTransfer= visitService.createVisit(branchId, "1", serviceIds, false);
        visit = visitService.visitTransferFromQueueToUserPool(branchId, "2198423c-760e-4d39-8930-12602552b1a9", visit,true);
        Thread.sleep(1000);


        Assertions.assertEquals(1, branchService.getBranch(branchId).getServicePoints().get("be675d63-c5a1-41a9-a345-c82102ac42cc").getUser().getVisits().size());
        Assertions.assertEquals(visit.getStatus(), VisitEvent.TRANSFER_TO_USER_POOL.getState().name());


    }

    /**
     * Проверка правильности получения доступных услуг
     */

    /**
     * Проверка завершения визита с итогом "Не пришел"
     * @throws InterruptedException исключение вызываемое прерыванием потока
     */
    @Test
    void checkNoShowVisit() throws InterruptedException {
        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        Visit visit;
        visit = visitService.createVisit(branchId, "1", serviceIds, false);
        Optional<Visit> visits=visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
        if(visits.isPresent()) {
            Long servtime =visits.get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(200);
            visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(200);
            Optional<Visit> visits2 = visitService.visitNoShow(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            if (visits2.isPresent()) {
                Visit visit2 = visits2.get();

                Assertions.assertEquals(visit2.getStatus(), VisitEvent.NO_SHOW.getState().name());
            }
        }
    }

    /**
     * Проверка корректности формирования номера талона
     */
    @Test
    void checkTicetNumberlogic() {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;

        Visit visit;
        visit = visitService.createVisit(branchId, "1", serviceIds, false);

        Queue queue = managementController.getBranch(branchId).getQueues().get(service.getLinkedQueueId());


        Assertions.assertEquals(visit.getTicket(), queue.getTicketPrefix() + String.format("%03d", queue.getTicketCounter()));


    }

    /**
     * Проверка правильности работы счетчика визитов
     */
    @Test
    void checkVisitCounter() {
        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        Integer visitsbefore = managementController.getBranch(branchId).getQueues().get(service.getLinkedQueueId()).getTicketCounter();
        visitService.createVisit(branchId, "1", serviceIds, false);
        Integer visitafter = managementController.getBranch(branchId).getQueues().get(service.getLinkedQueueId()).getTicketCounter();
        Assertions.assertEquals(1, visitafter - visitsbefore);
    }

    /**
     * Проверка наличия созданного визита в очереди
     */
    @Test
    void checkVisitInQueue() {
        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        String visitId = visitService.createVisit(branchId, "1", serviceIds, false).getId();
        Queue queue = managementController.getBranch(branchId).getQueues().get(service.getLinkedQueueId());
        Assertions.assertTrue(queue.getVisits().stream().map(Visit::getId).toList().contains(visitId));


    }

    @AfterEach
    void deleteBranch() {

        branchService.delete(branchId);
    }

    /**
     * Проверка сохранения изменения состояния отделения в кэше редис
     */
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

    /**
     * Проверка правильности отработки ошибки вызова не существующего отделения
     */
    @Test
    void getNotExistBranch() {
        Exception exception = assertThrows(BusinessException.class, () -> branchService.getBranch("not exist"));
        Assertions.assertEquals(exception.getMessage(), "Branch not found!!");

    }


}
