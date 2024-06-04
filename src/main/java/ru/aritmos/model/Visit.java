package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
@Serdeable
@Builder
@Introspected
public class Visit {
    String id;
    String ticket;
    Date createData;
    List<Service> unservedServices;
    List<Service> servedServices;
    Integer waitingTime;
    Service currentService;
    HashMap<String,Object> parameterMap;
    Queue queue;

}
