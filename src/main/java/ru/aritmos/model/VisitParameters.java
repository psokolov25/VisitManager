package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

@Data
public class VisitParameters {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ArrayList<String> serviceIds=new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    HashMap<String,Object> parameters=new HashMap<>();
}
