package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
@Serdeable
@Builder

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
