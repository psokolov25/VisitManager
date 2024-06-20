package ru.aritmos;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.*;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.VisitService;

import java.util.ArrayList;
import java.util.HashMap;

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
    String branchId = "37493d1c-8282-4417-a729-dceac1f3e2b1";
    String serviceId = "c3916e7f-7bea-4490-b9d1-0d4064adbe8c";

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
        if (!branchService.getBranches().containsKey("37493d1c-8282-4417-a729-dceac1f3e2b1")) {
            branch = new Branch("37493d1c-8282-4417-a729-dceac1f3e2b1", "Отделение на Тверской");

            Queue queueCredit = new Queue("Кредиты", "F");
            Service creditService = new Service("c3916e7f-7bea-4490-b9d1-0d4064adbe8c", "Кредит", 9000, queueCredit.getId());
            Queue queueeBigCredit = new Queue("Очень большие кредиты", "S");
            Service bigCreditService = new Service("569769e8-3bb3-4263-bd2e-42d8b3ec0bd4", "Очень большой кредит", 9000, queueeBigCredit.getId());
            Queue queueC = new Queue("В кассу", "C");
            Service kassaService = new Service("9a6cc8cf-c7c4-4cfd-90fc-d5d525a92a67", "Касса", 9000, queueC.getId());
            ServicePoint servicePointFC = new ServicePoint("Финансовый консультант");
            ArrayList<Service> serviceList = new ArrayList<>();
            serviceList.add(kassaService);
            serviceList.add(creditService);
            serviceList.add(bigCreditService);
            branch.setServices(serviceList);
            ServicePoint servicePointFSC = new ServicePoint("Старший финансовый консультант");
            ServicePoint servicePointC = new ServicePoint("Касса");
            HashMap<String, ServicePoint> servicePointMap = new HashMap<>();
            servicePointMap.put(servicePointFC.getId(), servicePointFC);
            servicePointMap.put(servicePointFSC.getId(), servicePointFSC);
            servicePointMap.put(servicePointC.getId(), servicePointC);
            HashMap<String, Queue> queueMap = new HashMap<>();
            queueMap.put(queueCredit.getId(), queueCredit);
            queueMap.put(queueeBigCredit.getId(), queueeBigCredit);
            queueMap.put(queueC.getId(), queueC);

            branch.setQueues(queueMap);
            branch.setServicePoints(servicePointMap);
            branchService.add(branch.getId(), branch);
        }
    }

    /**
     * Проверка правильности формаирования номера талона
     */


    @Test
    void checkTicetNumberlogic() {

        Service service;
        service = branchService.getBranch(branchId).getServices().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;

        Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);

        Queue queue = branchService.getBranch(branchId).getQueues().get(service.getLinkedQueueId());

        Assertions.assertEquals(visit.getTicketId(), queue.getTicketPrefix() + String.format("%03d", queue.getTicketCounter()));

    }
    /**     *
     * Проверка правильности работы счетчика визитов
     */
    @Test
    void checkVisitcounter() {
        Service service;
        service = branchService.getBranch(branchId).getServices().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        Integer visitsbefore = branchService.getBranch(branchId).getQueues().get(service.getLinkedQueueId()).getTicketCounter();
        visitService.createVisit(branchId, "1", serviceIds, false);
        Integer visitafter = branchService.getBranch(branchId).getQueues().get(service.getLinkedQueueId()).getTicketCounter();
        Assertions.assertEquals(1, visitafter - visitsbefore);
    }
    /**
     *  Проверка наличия созданного визита в очереди
     */
    @Test
    void checkVisitInQueue() {
        Service service;
        service = branchService.getBranch(branchId).getServices().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);
        assert service != null;
        String visitId=visitService.createVisit(branchId, "1", serviceIds, false).getId();
        Queue queue = branchService.getBranch(branchId).getQueues().get(service.getLinkedQueueId());
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
