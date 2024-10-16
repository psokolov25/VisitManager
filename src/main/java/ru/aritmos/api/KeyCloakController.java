package ru.aritmos.api;

import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import ru.aritmos.keycloack.model.Credentials;

@Slf4j
@Controller
@SerdeImport(AuthorizationResponse.class)
public class KeyCloakController {
  DefaultClassPathResourceLoader resourceLoader =
      new DefaultClassPathResourceLoader(this.getClass().getClassLoader());

  @Tag(name = "Полный список")
  @Tag(name = "Взаимодействие с Keycloak")
  @Post(uri = "/keycloak", consumes = "application/json", produces = "application/json")
  Optional<AuthorizationResponse> Auth(@Body Credentials credentials) {
    Optional<InputStream> res = resourceLoader.getResourceAsStream("keycloak.json");
    if (res.isPresent()) {
      AuthzClient authzClient = AuthzClient.create(res.get());

      return Optional.of(
          authzClient.authorization(credentials.getLogin(), credentials.getPassword()).authorize());
    }
    return Optional.empty();
  }
}
