package ru.aritmos;

import io.micronaut.runtime.EmbeddedApplication;
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
            EntryPoint entryPoint = new EntryPoint();
            entryPoint.setPrinterId("2");
            entryPoint.setId("1");
            HashMap<String, EntryPoint> entryPoints = new HashMap<>();
            entryPoints.put(entryPoint.getId(), entryPoint);
            branch.setEntryPoints(entryPoints);
            Queue queueCredit = new Queue("Кредиты", "F");
            Service creditService = new Service("c3916e7f-7bea-4490-b9d1-0d4064adbe8c", "Кредит", 9000, queueCredit.getId());
            DeliveredService creditCard = new DeliveredService("35d73fdd-1597-4d94-a087-fd8a99c9d1ed", "Кредитная карта");
            DeliveredService insurance = new DeliveredService("daa17035-7bd7-403f-a036-6c14b81e666f", "Страховка");
            Outcome creditAccepted = new Outcome("462bac1a-568a-4f1f-9548-1c7b61792b4b", "Одобрен");
            Outcome creditCardGiven = new Outcome("8dc29622-cd87-4384-85a7-04b66b28dd0f", "Выдана");
            creditAccepted.setCode(1L);
            creditService.getPossibleOutcomes().put(creditAccepted.getId(), creditAccepted);
            branch.getPossibleDeliveredServices().put(creditCard.getId(), creditCard);
            branch.getPossibleDeliveredServices().put(insurance.getId(), insurance);
            Queue queueBigCredit = new Queue("Очень большие кредиты", "S");
            Service bigCreditService = new Service("569769e8-3bb3-4263-bd2e-42d8b3ec0bd4", "Очень большой кредит", 9000, queueBigCredit.getId());
            Queue queueC = new Queue("В кассу", "C");
            Service kassaService = new Service("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67", "Касса", 9000, queueC.getId());
            ServicePoint servicePointFC = new ServicePoint("Финансовый консультант");
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
            User psokolovUser = new User("psokolov");
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
        Visit visitForConfirm = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
        Long servtime = visitForConfirm.getServingTime();
        Assertions.assertEquals(servtime, 0);
        Thread.sleep(2000);
        visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
        Thread.sleep(2000);
        visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
        Thread.sleep(2000);
        Visit visit2;
        visit2 = visitService.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
        Assertions.assertEquals(visit2.getStatus(), VisitEvent.END.name());


    }


    @Test
    void checkConfirmVisitWithCallRule() throws InterruptedException {

        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals("c3916e7f-7bea-4490-b9d1-0d4064adbe8c")).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        visitService.createVisit(branchId, "1", serviceIds, false);
        Thread.sleep(3000);
        Optional<Visit> currvisit = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");
        if (currvisit.isPresent()) {
            Long servtime = currvisit.get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(3000);
            visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", currvisit.get());
            Thread.sleep(3000);
            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", currvisit.get());
            visitService.addMark(branchId,"be675d63-c5a1-41a9-a345-c82102ac42cc","Клиент был не в настроении");
            visitService.addDeliveredService(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", creditCardId);

            visitService.addOutcomeDeliveredService(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", creditCardId, creditCardGivenId);
            visitService.addOutcomeService(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", acceptedOutcomeID);
            Visit visit;
            visitService.addMark(branchId,"be675d63-c5a1-41a9-a345-c82102ac42cc","Клиент ушел довольный");
            visit=visitService.addMark(branchId,"be675d63-c5a1-41a9-a345-c82102ac42cc","Клиент ушел не довольный");
            Assertions.assertEquals(visit.getCurrentService().getMarks().size(),3);
            visit=visitService.deleteMark(branchId,"be675d63-c5a1-41a9-a345-c82102ac42cc","Клиент ушел не довольный");
            Assertions.assertEquals(visit.getCurrentService().getMarks().size(),2);
            Thread.sleep(3000);

            Visit visit2 = servicePointController.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");

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
        Thread.sleep(10000);
        if (visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc").isPresent()) {
            Long servtime = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc").get().getServingTime();
            Assertions.assertEquals(servtime, 0);
            Thread.sleep(8000);
            visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
            Thread.sleep(6000);
            visitService.visitConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);

            Thread.sleep(9000);


            Visit visit2 = servicePointController.visitEnd(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc");

            Assertions.assertEquals(visit2.getStatus(), VisitEvent.END.name());
        }

    }

    @Test
    void checkNoShowVisit() throws InterruptedException {
        Service service;
        service = managementController.getBranch(branchId).getServices().values().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        Visit visit;
        visit = visitService.createVisit(branchId, "1", serviceIds, false);
        Long servtime = visitService.visitCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit).getServingTime();
        Assertions.assertEquals(servtime, 0);
        Thread.sleep(2000);
        visitService.visitReCallForConfirm(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);
        Thread.sleep(2000);


        Visit visit2 = visitService.visitNoShow(branchId, "be675d63-c5a1-41a9-a345-c82102ac42cc", visit);

        Assertions.assertEquals(visit2.getStatus(), VisitEvent.NO_SHOW.getState().name());


    }

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
