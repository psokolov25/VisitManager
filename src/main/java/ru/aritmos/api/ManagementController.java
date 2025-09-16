package ru.aritmos.api;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import org.keycloak.representations.idm.UserRepresentation;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.User;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.service.BranchService;

/**
 * @author Pavel Sokolov REST API управления зоной ожидания
 */
@Controller("/managementinformation")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
    @ApiResponse(responseCode = "401", description = "Не авторизован"),
    @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
    @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
    @ApiResponse(responseCode = "405", description = "Метод не поддерживается"),
    @ApiResponse(responseCode = "409", description = "Конфликт состояния"),
    @ApiResponse(responseCode = "413", description = "Превышен размер запроса"),
    @ApiResponse(responseCode = "415", description = "Неподдерживаемый тип данных"),
    @ApiResponse(responseCode = "429", description = "Превышено количество запросов"),
    @ApiResponse(responseCode = "500", description = "Ошибка сервера"),
    @ApiResponse(responseCode = "503", description = "Сервис недоступен")

})
public class ManagementController {

  /** Сервис управления отделениями. */
  @Inject BranchService branchService;

  /** Клиент Keycloak для получения данных о пользователях. */
  @Inject KeyCloackClient keyCloakClient;

  /**
   * Возвращает данные об отделении
   *
   * @param id идентификатор отделения
   * @return состояние отделения
   */
  @Operation(
      summary = "Получение состояния отделения",
      description = "Возвращает информацию об отделении по его идентификатору",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Данные отделения",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Branch.class))),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Tag(name = "Информация об отделении")
  @Tag(name = "Полный список")
  @Get(uri = "/branches/{id}")
  @ExecuteOn(TaskExecutors.IO)
  public Branch getBranch(
      @PathVariable(defaultValue = "37493d1c-8282-4417-a729-dceac1f3e2b4") String id) {
    return branchService.getBranch(id);
  }

  /**
   * Получение массива идентификаторов и объектов отделений
   *
   * @param userName (опционально) имя пользователя для фильтрации доступных отделений
   * @return массив идентификаторов и отделений
   */
  @Tag(name = "Информация об отделении")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Получение списка отделений",
      description = "Возвращает карту доступных отделений",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список отделений"),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(uri = "/branches")
  @ExecuteOn(TaskExecutors.IO)
  public Map<String, Branch> getBranches(@Nullable String userName) {
    if (userName != null) {
      Optional<UserRepresentation> userInfo = keyCloakClient.getUserInfo(userName);

      if (userInfo.isPresent()) {
        User user = new User(userInfo.get().getUsername(), keyCloakClient);

        user.setAllBranches(keyCloakClient.getAllBranchesOfUser(userName));
        try {
          user.setIsAdmin(keyCloakClient.isUserModuleTypeByUserName(userName, "admin"));
        } catch (Exception e) {
          user.setIsAdmin(false);
        }
        return branchService.getBranches().entrySet().stream()
            .filter(
                f ->
                    user.getIsAdmin()
                        || user.getAllBranches().stream()
                            .anyMatch(
                                f2 ->
                                    f2.getAttributes().containsKey("branchPrefix")
                                        && f2.getAttributes()
                                            .get("branchPrefix")
                                            .contains(f.getValue().getPrefix())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      }
    }
    return branchService.getBranches();
  }

  /**
   * Получение массива идентификаторов и названий отделений
   *
   * @return массив идентификаторов и названий отделений
   */
  @Tag(name = "Информация об отделении")
  @Tag(name = "Полный список")
  @Operation(
      summary = "Минимальная информация об отделениях",
      description = "Возвращает идентификаторы и названия отделений",
      responses = {
        @ApiResponse(responseCode = "200", description = "Список отделений"),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера")
      })
  @Get(uri = "/branches/tiny")
  @ExecuteOn(TaskExecutors.IO)
  public List<TinyClass> getTinyBranches() {
    return branchService.getBranches().values().stream()
        .map(m -> new TinyClass(m.getId(), m.getName()))
        .toList();
  }
}
