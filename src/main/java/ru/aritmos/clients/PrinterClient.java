package ru.aritmos.clients;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;
import ru.aritmos.model.Visit;

@Client(value = "${micronaut.application.printerServiceURL}")
public interface PrinterClient {
    @Retryable(delay = "${micronaut.application.printerRetryPeriod:30s}",maxDelay = "${micronaut.application.printerRetryMaxPeriod:45m}",attempts = "${micronaut.application.printerUrlRetryRepeat:30}")
    @Post(uri = "/printer/visit", produces = "application/json", consumes = "application/json")
    @ExecuteOn(TaskExecutors.IO)
    Publisher<HttpResponse<?>> print(@QueryValue(defaultValue = "UTF-8") String charset, @QueryValue Boolean responseRequired, @Body Visit visit);
}
