package ru.aritmos.clients;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import java.util.HashMap;
import ru.aritmos.model.Branch;

@Client(value = "${micronaut.application.configurationURL}")
public interface ConfigurationClient {
  @Get(uri = "/configuration")
  HashMap<String, Branch> getConfiguration();
}
