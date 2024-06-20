package ru.aritmos;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import ru.aritmos.model.*;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.VisitService;

import java.util.ArrayList;
import java.util.HashMap;


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

    @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

    Branch branch;

    @BeforeEach
    void CreateBranchh() {
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


@Test
void createVisit() {
    String branchId = "37493d1c-8282-4417-a729-dceac1f3e2b1";
    String serviceId = "c3916e7f-7bea-4490-b9d1-0d4064adbe8c";
    Service service;
    service = branchService.getBranch(branchId).getServices().stream().filter(f -> f.getId().equals(serviceId)).findFirst().orElse(null);
    ArrayList<String> serviceIds = new ArrayList<>();
    serviceIds.add(serviceId);
    assert service != null;

    Visit visit = visitService.createVisit(branchId, "1", serviceIds, false);
    Queue queue = branchService.getBranch(branchId).getQueues().get(service.getLinkedQueueId());
    Assertions.assertTrue(visit.getTicketId().contains(String.format("%03d", queue.getTicketCounter())));
    Assertions.assertTrue(visit.getTicketId().contains(queue.getTicketPrefix()));
    Assertions.assertEquals(visit.getTicketId(), queue.getTicketPrefix() + String.format("%03d", queue.getTicketCounter()));

}
//@Test
//    void testUpdateBranchInCache() {
//
//        String key = "f094b52f-b316-4441-a6b4-bf9902c8231d";
//        Branch branch = new Branch(key, "tst");
//        String name = branch.getName();
//
//
//        Branch result=branchService.add(key,branch);
//        log.info("Branch added {}",result);
//        Branch br2 = branchService.getBranch(key);
//        br2.setName("tst344");
//        branchService.add(key,br2);
//        String name3 = branchService.getBranch(key).getName();
//        Assertions.assertNotEquals(name3, name);
//
//
//    }
//    @Test
//    void  getNotExistBranch()
//    {
//        Exception exception = assertThrows(BusinessException.class, () -> branchService.getBranch("not exist"));
//        Assertions.assertEquals(exception.getMessage(),"Branch not found!!");
//
//    }


}
