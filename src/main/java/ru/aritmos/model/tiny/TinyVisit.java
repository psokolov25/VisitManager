package ru.aritmos.model.tiny;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.aritmos.model.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Сокращенный визит
 */
@Data
@EqualsAndHashCode
@Serdeable
@Builder(toBuilder = true)
@Introspected
@ToString
public class TinyVisit {
    /**
     * Идентификатор визита
     */
    String id;
    /**
     * Талон
     */
    String ticketId;
    /**
     * Текущая услуга
     */
    Service currentService;
    /**
     * Дата перевода
     */
    ZonedDateTime transferDate;
    @JsonGetter
    public Long getWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
        waitingTime = unit.between( transferDate,ZonedDateTime.now());
        return waitingTime;
    }
    /**
     * Время ожидания в последней очереди в секундах
     */
    Long waitingTime;


}
