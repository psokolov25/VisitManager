package ru.aritmos.service;

import static org.mockito.Mockito.*;

import io.micronaut.http.HttpResponse;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ru.aritmos.clients.PrinterClient;
import ru.aritmos.model.visit.Visit;

/** Integration tests for {@link PrinterService}. */
@MicronautTest(environments = "local-no-docker")
class PrinterServiceIT {

    @Inject
    PrinterService printerService;

    @Inject
    PrinterClient printerClient;

    @Test
    void printDelegatesToClient() {
        Visit visit = Visit.builder().build();
        when(printerClient.print("UTF-8", true, visit)).thenReturn(Mono.just(HttpResponse.ok()));

        printerService.print("p1", visit);

        verify(printerClient).print("UTF-8", true, visit);
    }

    @MockBean(PrinterClient.class)
    PrinterClient printerClient() {
        return mock(PrinterClient.class);
    }
}
