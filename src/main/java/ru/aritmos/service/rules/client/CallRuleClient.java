package ru.aritmos.service.rules.client;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import java.util.Optional;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

@Client(value = "${micronaut.application.rules.callRuleApiUrl}")
public interface CallRuleClient {
  @Post()
  Optional<Visit> callRule(@Body Branch branch, @Body ServicePoint servicePoint);
}
