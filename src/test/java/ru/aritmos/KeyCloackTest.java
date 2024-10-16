package ru.aritmos;

import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import java.io.InputStream;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;

@Slf4j
public class KeyCloackTest {
  DefaultClassPathResourceLoader resourceLoader =
      new DefaultClassPathResourceLoader(this.getClass().getClassLoader());

  @Test
  public void test() {

    Optional<InputStream> res = resourceLoader.getResourceAsStream("keycloak.json");
    if (res.isPresent()) {
      AuthzClient authzClient = AuthzClient.create(res.get());

      AuthorizationResponse t = authzClient.authorization("psokolov2", "bd290776").authorize();

      // AuthorizationResponse t2 = authzClient.authorization("admin", "admin").authorize();
      // AuthorizationResponse t3 = authzClient.authorization("admin", "admin").authorize();
      Keycloak keycloak =
          Keycloak.getInstance("http://192.168.8.45:9090", "Aritmos", "myclient", t.getToken());

      ClientRepresentation t4 =
          keycloak.realm("Aritmos").clients().findByClientId("myclient").get(0);
      keycloak
          .realm("Aritmos")
          .users()
          .get(keycloak.realm("Aritmos").users().search("psokolov").get(0).getId())
          .getUserSessions()
          .forEach(f -> keycloak.realm("Aritmos").deleteSession(f.getId(), false));
      keycloak
          .realm("Aritmos")
          .users()
          .get(keycloak.realm("Aritmos").users().search("admin").get(0).getId())
          .getUserSessions()
          .forEach(f -> keycloak.realm("Aritmos").deleteSession(f.getId(), false));
      log.info("{}", keycloak.serverInfo().getInfo());
      log.info(t4.toString());
    }
  }
}
