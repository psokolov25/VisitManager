package ru.aritmos;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.aritmos.service.BranchService;


@Slf4j
@MicronautTest
class EntrypointTest {

    @Inject
    BranchService branchService;
    @Inject
    EmbeddedApplication<?> application;

   @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
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
