package ru.aritmos.model.visit;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class VisitTest {

    @DisplayName("Расчёт времени ожидания вычисляет разницу во времени")
    @Test
    void getWaitingTimeCalculatesDifference() {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusSeconds(10);
        Visit visit = Visit.builder()
                .createDateTime(start)
                .startServingDateTime(end)
                .build();

        assertEquals(10L, visit.getWaitingTime());
    }

    @DisplayName("Расчёт времени возврата использует сохранённую дату")
    @Test
    void getReturningTimeUsesReturnDateTime() {
        ZonedDateTime returnTime = ZonedDateTime.now().minusSeconds(5);
        Visit visit = Visit.builder().returnDateTime(returnTime).build();
        long expected = ChronoUnit.SECONDS.between(returnTime, ZonedDateTime.now());
        long actual = visit.getReturningTime();
        assertTrue(Math.abs(actual - expected) <= 1);
    }

    @DisplayName("Расчёт времени возврата возвращает ноль при отсутствии данных")
    @Test
    void getReturningTimeReturnsZeroWhenNull() {
        Visit visit = Visit.builder().build();
        assertEquals(0L, visit.getReturningTime());
    }

    @DisplayName("Расчёт времени переноса опирается на дату переноса")
    @Test
    void getTransferingTimeUsesTransferDateTime() {
        ZonedDateTime transferTime = ZonedDateTime.now().minusSeconds(7);
        Visit visit = Visit.builder().transferDateTime(transferTime).build();
        long expected = ChronoUnit.SECONDS.between(transferTime, ZonedDateTime.now());
        long actual = visit.getTransferingTime();
        assertTrue(Math.abs(actual - expected) <= 1);
    }

    @DisplayName("Расчёт длительности визита определяет разницу между началом и окончанием")
    @Test
    void getVisitLifeTimeCalculatesDifference() {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusSeconds(20);
        Visit visit = Visit.builder()
                .createDateTime(start)
                .endDateTime(end)
                .build();
        assertEquals(20L, visit.getVisitLifeTime());
    }

    @DisplayName("Расчёт времени обслуживания определяет длительность обслуживания")
    @Test
    void getServingTimeCalculatesDifference() {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusSeconds(15);
        Visit visit = Visit.builder()
                .startServingDateTime(start)
                .servedDateTime(end)
                .build();
        assertEquals(15L, visit.getServingTime());
    }
}
