package ru.aritmos.model.visit;

import static ru.aritmos.test.LoggingAssertions.*;

import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VisitEventInformationTest {

    @Test
    void builderShouldCreateInstanceWithAllFields() {
        ZonedDateTime now = ZonedDateTime.now();
        Map<String, String> params = Map.of("key", "value");

        VisitEventInformation info = VisitEventInformation.builder()
                .visitEvent(VisitEvent.CREATED)
                .eventDateTime(now)
                .parameters(params)
                .transactionCompletionStatus(TransactionCompletionStatus.OK)
                .build();

        assertEquals(VisitEvent.CREATED, info.getVisitEvent());
        assertEquals(now, info.getEventDateTime());
        assertEquals(params, info.getParameters());
        assertEquals(TransactionCompletionStatus.OK, info.getTransactionCompletionStatus());
    }
}
