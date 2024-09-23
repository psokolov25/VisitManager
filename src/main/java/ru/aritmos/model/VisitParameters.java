package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

@Data
@Introspected
@Serdeable
public class VisitParameters {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ArrayList<String> serviceIds=new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    HashMap<String,Object> parameters=new HashMap<>();
}
