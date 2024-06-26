package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

import java.util.HashMap;
/**
 * Отделение
 *
 */
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

    /**
     * Точки входа
     */
    HashMap<String,EntryPoint> entryPoints=new HashMap<>();
    /**
     * Очереди
     */
    HashMap<String,Queue> queues = new HashMap<>();
    /**
     * Услуги
     */
    HashMap<String,Service> services = new HashMap<>();
    /**
     * Рабочие профили
     */
    HashMap<String,WorkProfile> workProfiles = new HashMap<>();
    /**
     * Точки обслуживания
     */
    HashMap<String,ServicePoint> servicePoints = new HashMap<>();

}
