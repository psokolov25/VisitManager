package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = false)
@Serdeable
@Introspected

@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)


public  class Branch extends BranchEntity {

    public Branch(String key, String name) {
        super(key, name);
    }
    ArrayList<Service> services = new ArrayList<>();
    HashMap<String,Queue> queues = new HashMap<>();

}
