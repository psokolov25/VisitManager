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
import java.util.List;
import java.util.stream.Collectors;

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

    public Integer incrementTicketCounter(Queue queue) {
        if (this.getQueues().containsKey(queue.getId())) {
            this.queues.get(queue.getId()).setTicketCounter(++this.queues.get(queue.getId()).ticketCounter);
            return this.queues.get(queue.getId()).getTicketCounter();
        }
        return -1;
    }

    public HashMap<String, Visit> getAllVisits() {
        HashMap<String, Visit> visits = new HashMap<>();
        this.getServicePoints().forEach((k, v) -> {
            if (v.getVisit() != null) {
                visits.put(v.getVisit().getId(), v.getVisit());
            }
        });
        this.getQueues().forEach((k, v) -> {
            if(v.getVisits()!=null) {
                v.getVisits().forEach(f -> {
                    visits.put(f.getId(), f);
                });
            }
        });
        return visits;
    }

    public HashMap<String, Visit> getVisitsByStatus(List<String> statuses) {
        HashMap<String, Visit> visits = new HashMap<>();
        this.getAllVisits()
                .values()
                .stream()
                .filter(f -> statuses.contains(f.getStatus()))
                .forEach(
                        f ->
                        {
                            visits.put(f.getId(), f);
                        }
                );
        return visits;
    }

    public void updateVisit(Visit visit, EventService eventService) {
        Visit oldVisit = visit.toBuilder().build();

        this.servicePoints.forEach((key, value) -> {
            if (value.getId().equals(visit.getServicePointId())) {
                if (value.getVisit() == null || value.getVisit().getId().equals(visit.getId())) {
                    value.setVisit(visit);
                } else {
                    throw new BusinessException(String.format("In ServicePoint %s already exists visit %s", value.getId(), value.getVisit().getId()), eventService, HttpStatus.CONFLICT);
                }
            } else if (value.getVisit() != null && value.getVisit().getId().equals(visit.getId())) {
                value.setVisit(null);
            }
        });
        this.queues.forEach((key, value) -> {
            value.getVisits().removeIf(f -> f.getId().equals(visit.getId()));
            if (value.getId().equals(visit.getQueueId())) {
                value.getVisits().add(visit);
            }

        });
        eventService.sendChangedEvent("*", false, oldVisit, visit, new HashMap<>(), "CHANGE");
    }

}
