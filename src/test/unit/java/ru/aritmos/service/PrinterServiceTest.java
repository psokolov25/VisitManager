package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
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

    /**
     * Не выбрасывает исключение при ошибке клиента печати.
     */
    @Test
    void printHandlesClientError() {
        PrinterClient client = mock(PrinterClient.class);
        Visit visit = Visit.builder().build();
        when(client.print("UTF-8", true, visit)).thenReturn(Mono.error(new RuntimeException("fail")));

        PrinterService service = new PrinterService();
        service.printerClient = client;

        assertDoesNotThrow(() -> service.print("printer", visit));
        verify(client).print("UTF-8", true, visit);
    }
}
