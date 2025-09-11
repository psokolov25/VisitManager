package ru.aritmos.config;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import ru.aritmos.keycloack.service.KeyCloackClient;

/**
 * Стаб-клиент Keycloak для окружения local-no-docker.
 * Отключает реальные сетевые вызовы, возвращая предсказуемые данные.
 */
@Singleton
@Requires(env = "local-no-docker")
@Replaces(KeyCloackClient.class)
public class LocalNoDockerKeycloakStub extends KeyCloackClient {

  @Override
  public Optional<UserRepresentation> getUserInfo(String userName) {
    UserRepresentation u = new UserRepresentation();
    u.setId(UUID.randomUUID().toString());
    u.setUsername(userName);
    u.setFirstName("Local");
    u.setLastName("User");
    u.setEmail(userName + "@local");
    return Optional.of(u);
  }

  @Override
  public List<GroupRepresentation> getAllBranchesOfUser(String username) {
    return new ArrayList<>();
  }

  @Override
  public Boolean isUserModuleTypeByUserName(String userName, String type) {
    return true; // в dev-профиле считаем пользователя администратором
  }

  @Override
  public String getBranchPathByBranchPrefix(String regionName, String prefix) {
    // Возвращаем фиктивный путь без обращений к Keycloak
    return "/" + regionName + "/" + prefix;
  }
}

