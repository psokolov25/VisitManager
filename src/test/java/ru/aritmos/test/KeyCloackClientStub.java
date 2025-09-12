package ru.aritmos.test;

import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import ru.aritmos.keycloack.service.KeyCloackClient;

/**
 * Тестовый стаб клиента Keycloak: отключает внешние вызовы.
 */
@Singleton
@Replaces(KeyCloackClient.class)
public class KeyCloackClientStub extends KeyCloackClient {

  @Override
  public Optional<UserRepresentation> getUserInfo(String userName) {
    UserRepresentation u = new UserRepresentation();
    u.setId(UUID.randomUUID().toString());
    u.setUsername(userName);
    u.setFirstName("Test");
    u.setLastName("User");
    u.setEmail(userName + "@example.local");
    return Optional.of(u);
  }

  @Override
  public List<GroupRepresentation> getAllBranchesOfUser(String username) {
    return new ArrayList<>();
  }

  @Override
  public Boolean isUserModuleTypeByUserName(String userName, String type) {
    return true; // дать права администратора в тестах
  }

  @Override
  public void userLogout(String login, Boolean isForced, String reason) {
    // отключить реальные сетевые вызовы при выходе пользователя
  }

  @Override
  public Optional<UserRepresentation> getUserBySid(String sid) {
    UserRepresentation u = new UserRepresentation();
    u.setId(UUID.randomUUID().toString());
    u.setUsername("test-user");
    u.setFirstName("Test");
    u.setLastName("User");
    u.setEmail("test-user@example.local");
    return Optional.of(u);
  }
}

