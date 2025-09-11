package ru.aritmos.clients;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import java.util.HashMap;
import ru.aritmos.model.Branch;

/**
 * HTTP‑клиент получения конфигурации отделений.
 */
@Client(value = "${micronaut.application.configurationURL}")
@SuppressWarnings("unused")
public interface ConfigurationClient {
  /**
   * Получить конфигурацию отделений.
   *
   * @return карта отделений (id -> отделение)
   */
  @Get(uri = "/configuration")
  HashMap<String, Branch> getConfiguration();
}
