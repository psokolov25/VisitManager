package ru.aritmos.service;

import static org.mockito.Mockito.*;

import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ru.aritmos.clients.PrinterClient;
import ru.aritmos.model.visit.Visit;

class PrinterServiceTest {

    @Test
    void printDelegatesToClient() {
        PrinterClient client = mock(PrinterClient.class);
        Visit visit = Visit.builder().build();
        when(client.print("UTF-8", true, visit)).thenReturn(Mono.just(HttpResponse.ok()));

        PrinterService service = new PrinterService();
        service.printerClient = client;

        service.print("printer", visit);

        verify(client).print("UTF-8", true, visit);
    }
}
