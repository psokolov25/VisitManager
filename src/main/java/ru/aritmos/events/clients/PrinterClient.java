package ru.aritmos.events.clients;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import ru.aritmos.model.Visit;

@Client(value = "${micronaut.application.printerServiceURL}")
public interface PrinterClient {
    @Retryable(delay = "${micronaut.application.dataBusUrlRetryPeriod:30s}",maxDelay = "${micronaut.application.dataBusUrlRetryMaxPeriod:45m}",attempts = "${micronaut.application.dataBusUrlRetryRepeat:30}")
    @Post(uri = "/printer/{id}", produces = "application/json", consumes = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    public void print(@PathVariable String id,@Body Visit visit);
}
