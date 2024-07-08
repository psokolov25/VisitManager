package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.Serdeable;
import lombok.*;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.visit.Visit;

import java.util.HashMap;

/**
 * Отделение
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Serdeable
@Introspected

@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)


public class Branch extends BranchEntity {

    public Branch(String key, String name) {
        super(key, name);
    }

    /**
     * Точки входа
     */
    HashMap<String, EntryPoint> entryPoints = new HashMap<>();
    /**
     * Очереди
     */
    HashMap<String, Queue> queues = new HashMap<>();
    /**
     * Услуги
     */
    HashMap<String, Service> services = new HashMap<>();
    /**
     * Рабочие профили
     */
    HashMap<String, WorkProfile> workProfiles = new HashMap<>();
    /**
     * Точки обслуживания
     */
    HashMap<String, ServicePoint> servicePoints = new HashMap<>();

    public void updateVisit(Visit visit, EventService eventService) {
        Visit oldVisit = visit.toBuilder().build();

        this.servicePoints.forEach((key, value) -> {
            if (value.getId().equals(visit.getServicePointId())) {
                if (value.getVisit()==null || value.getVisit().getId().equals(visit.getId())) {
                    value.setVisit(visit);
                }
                else
                {
                    throw new BusinessException(String.format("In ServicePoint %s already exists visit %s",value.getId(),value.getVisit().getId()), eventService, HttpStatus.CONFLICT);
                }
            } else if (value.getVisit() != null && value.getVisit().getId().equals(visit.getId())) {
                value.setVisit(null);
            }
        });
        this.queues.forEach((key, value) -> {
            value.getVisits().removeIf(f -> f.getQueueId().equals(value.getId()));
            if (value.getId().equals(visit.getQueueId())) {
                value.getVisits().add(visit);
            }

        });
        eventService.sendChangedEvent("*",false,oldVisit,visit,new HashMap<>(),"CHANGE");
    }

}
