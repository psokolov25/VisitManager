package ru.aritmos.model.tiny;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.aritmos.model.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Data
@EqualsAndHashCode
@Serdeable
@Builder(toBuilder = true)
@Introspected
public class Visit {
    String id;

    String ticketId;
    Service currentService;
    ZonedDateTime transferDate;
    @JsonGetter
    public Long getWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
        waitingTime = unit.between( transferDate,ZonedDateTime.now());
        return waitingTime;
    }

    Long waitingTime;


}
