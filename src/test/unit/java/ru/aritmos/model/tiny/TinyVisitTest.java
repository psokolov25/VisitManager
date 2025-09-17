package ru.aritmos.model.tiny;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class TinyVisitTest {

    @Test
    void calculatesWaitingTimeFromTransferDate() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime transfer = now.minusSeconds(5);
        TinyVisit visit = TinyVisit.builder()
                .createDate(now.minusSeconds(10))
                .transferDate(transfer)
                .build();

        long actual = visit.getWaitingTime();
        long expected = ChronoUnit.SECONDS.between(transfer, ZonedDateTime.now());
        assertTrue(Math.abs(actual - expected) <= 1, "waiting time should match");
    }

    @Test
    void calculatesTotalWaitingTimeFromCreateDate() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime create = now.minusSeconds(20);
        TinyVisit visit = TinyVisit.builder()
                .createDate(create)
                .build();

        long actual = visit.getTotalWaitingTime();
        long expected = ChronoUnit.SECONDS.between(create, ZonedDateTime.now());
        assertTrue(Math.abs(actual - expected) <= 1, "total waiting time should match");
    }
}

