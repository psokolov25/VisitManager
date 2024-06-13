package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode
@Serdeable
@Builder
@Introspected
public class Visit {
    String id;
    String ticketId;
    String branchId;
    Date createDate;
    Date updateDate;
    Integer version;
    ServicePoint servicePoint;
    List<Service> unservedServices;
    List<Service> servedServices;
    Integer waitingTime;
    Service currentService;
    HashMap<String,Object> parameterMap;
    Boolean printTicket;
    EntryPoint entryPoint;
    Queue queue;


}
