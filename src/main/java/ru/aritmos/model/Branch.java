package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = false)
@Serdeable
@Introspected

@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)


public  class Branch extends BranchEntity {

    public Branch(String key, String name) {
        super(key, name);
    }

    HashMap<String,EntryPoint> entryPoints=new HashMap<>();
    HashMap<String,Queue> queues = new HashMap<>();
    ArrayList<Service> services = new ArrayList<>();

    HashMap<String,ServicePoint> servicePoints = new HashMap<>();

}
