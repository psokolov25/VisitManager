package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode
@Serdeable
@Builder(toBuilder = true)
@Introspected
@ToString
public class Visit {
    String id;
    String status;
    String ticketId;
    String branchId;
    ZonedDateTime createDate;
    ZonedDateTime updateDate;
    ZonedDateTime transferDate;
    Integer version;
    String servicePointId;
    String userName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Service> unservedServices;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Service> servedServices;


    @JsonGetter
    public Long getWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        waitingTime = unit.between(transferDate, ZonedDateTime.now());
        return waitingTime;
    }

    Long waitingTime;

    @JsonGetter
    public Long getTotalWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        waitingTime = unit.between(createDate, ZonedDateTime.now());
        return waitingTime;
    }

    Long totalWaitingTime;

    Service currentService;
    HashMap<String, Object> parameterMap;
    Boolean printTicket;
    EntryPoint entryPoint;
    String queueId;


}
