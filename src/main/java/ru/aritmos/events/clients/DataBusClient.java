package ru.aritmos.events.clients;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

import io.swagger.v3.oas.annotations.Parameter;
import org.reactivestreams.Publisher;
import io.micronaut.retry.annotation.Retryable;

import java.util.HashMap;
import java.util.Map;

@Client("${micronaut.application.dataBusUrl}")
public interface DataBusClient {
    @Retryable(delay = "30s")
    @Post(uri = "/events/senders/{senderService}/send/types/{type}")
    Publisher<String> send(
                           @Header("Service-Destination") String destinationServices,
                           @Header("Send-To-OtherBus") Boolean sendToOtherBus,
                           @Header("Send-Date") @Parameter(example = "Wed, 09 Apr 2008 23:55:38 GMT") String sendDate,
                           @PathVariable String senderService,
                           @PathVariable String type,
                           @Body Object body);

}