package ru.aritmos.api;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.*;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

/**
 * @author Pavel Sokolov REST API управления зоной ожидания
 */
@Controller("/managementinformation")
public class ManagementController {
  @Inject Services services;
  @Inject BranchService branchService;
  @Inject VisitService visitService;
  @Inject EventService eventService;

  @Value("${micronaut.application.name}")
  String applicationName;

  /**
   * Возвращает данные об отделении
   *
   * @param id идентификатор отделения
   * @return состояние отделения
   */
  @Tag(name = "Информация об отделении")
  @Tag(name = "Полный список")
  @Get(uri = "/branches/{id}")
  @ExecuteOn(TaskExecutors.IO)
  public Branch getBranch(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String id) {
    Branch branch;
    try {
      branch = branchService.getBranch(id);
    } catch (Exception ex) {
      throw new HttpStatusException(HttpStatus.NOT_FOUND, "Branch not found!");
    }
    return branch;
  }

  /**
   * Получение массива идентификаторов и объектов отделений
   *
   * @return массив идентификаторов и отделений
   */
  @Tag(name = "Информация об отделении")
  @Tag(name = "Полный список")
  @Get(uri = "/branches")
  @ExecuteOn(TaskExecutors.IO)
  public Map<String, Branch> getBranches() {
    return branchService.getBranches();
  }

  /**
   * Получение массива идентификаторов и названий отделений
   *
   * @return массив идентификаторов и названий отделений
   */
  @Tag(name = "Информация об отделении")
  @Tag(name = "Полный список")
  @Get(uri = "/branches/tiny")
  @ExecuteOn(TaskExecutors.IO)
  public List<TinyClass> getTinyBranches() {
    return branchService.getBranches().values().stream()
        .map(m -> new TinyClass(m.getId(), m.getName()))
        .toList();
  }
}
