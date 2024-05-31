package ru.aritmos.clients;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import ru.aritmos.model.Branch;

import java.util.HashMap;

@Client(value = "${micronaut.application.configurationURL}")
public interface ConfigurationClient {
    @Get(uri = "/configuration")
    HashMap<String, Branch> getConfiguration();
}
