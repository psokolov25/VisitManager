package ru.aritmos.api;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.aritmos.model.Branch;
import ru.aritmos.service.BranchService;

import java.util.HashMap;
/**
 * @author Pavel Sokolov
 * REST API управления конфигурацией отделений
 */

@Controller("/configuration")
public class ConfigurationController {
    BranchService branchService;
    @Tag(name = "Конфигурация отделений")
    @Post(uri = "/")
    public HashMap<String, Branch> update(@Body HashMap<String, Branch> branchHashMap) {


        branchService.getBranches().forEach((key, value) -> {
            if (!branchHashMap.containsKey(key)) {
                branchService.delete(key);
            }

        });
        branchHashMap.forEach((key, value) -> {
            branchService.add(key, value);


        });
        return branchService.getBranches();
    }
}
