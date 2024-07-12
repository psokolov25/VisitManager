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
            if (v.getVisits() != null) {
                v.getVisits().forEach(f -> visits.put(f.getId(), f));
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
                                visits.put(f.getId(), f)
                );
        return visits;
    }

    public void userLogin(User user, EventService eventService) {
        if (user.servicePoinrtId != null) {
            if (this.getServicePoints().containsKey(user.servicePoinrtId)) {
                ServicePoint servicePoint = this.getServicePoints().get(user.getServicePoinrtId());
                if (servicePoint.getUser() == null) {
                    servicePoint.setUser(user);
                } else {
                    throw new BusinessException(String.format("In servicePoint %s already %s logged in %s ", user.servicePoinrtId, servicePoint.getUser().getName(),servicePoint.getUser().getName()),eventService, HttpStatus.CONFLICT);
                }
            } else {
                throw new BusinessException(String.format("ServicePoint %s not found in %s", user.servicePoinrtId, this.getName()), eventService, HttpStatus.CONFLICT);
            }
        }

    }

    public void userLogout(User user, EventService eventService) {
        if (user.servicePoinrtId != null) {
            if (this.getServicePoints().containsKey(user.servicePoinrtId)) {
                ServicePoint servicePoint = this.getServicePoints().get(user.getServicePoinrtId());
                if (servicePoint.getUser() != null && servicePoint.getUser().getId().equals(user.getId())) {
                    servicePoint.setUser(null);
                    user.setServicePoinrtId(null);
                    user.setCurrentWorkProfileId(null);
                }

            } else {
                throw new BusinessException(String.format("ServicePoint %s not found in %s", user.servicePoinrtId, this.getName()), eventService, HttpStatus.CONFLICT);
            }
        }

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
    public void addUpdateService(HashMap<String,Service> serviceHashMap)
    {
        serviceHashMap.forEach((k, v) -> this.getServices().put(k,v));
    }
    public void deleteServices(List<String> serviceIds)
    {
        serviceIds.forEach(f-> this.getServices().remove(f));
    }
    public void addUpdateServicePoint(HashMap<String,ServicePoint> servicePointHashMap,Boolean restoreVisit,Boolean restoreUser)
    {
        servicePointHashMap.forEach((k, v) -> {

                if (this.getServicePoints().containsKey(k) && restoreVisit && this.getServicePoints().get(k).getVisit() != null) {

                    v.setVisit(this.getServicePoints().get(k).getVisit());

                }

                if (this.getServicePoints().containsKey(k) && restoreUser && this.getServicePoints().get(k).getUser() != null) {
                    v.setUser(this.getServicePoints().get(k).getUser());

                }
                this.getServicePoints().put(k, v);

        });

    }
    public void deleteServicePoints(List<String> servicePointIds)
    {
        servicePointIds.forEach(f-> this.getServicePoints().remove(f));
    }
    public void addUpdateQueues(HashMap<String, Queue> queueHashMap, Boolean restoreVisits)
    {
        queueHashMap.forEach((k, v) -> {

            if (this.getQueues().containsKey(k) && restoreVisits && !this.getQueues().get(k).getVisits().isEmpty()) {

                v.getVisits().addAll(this.getQueues().get(k).getVisits());

            }


        });

    }
    public void deleteQueues(List<String> sueueIds)
    {
        sueueIds.forEach(f-> this.getQueues().remove(f));
    }



}
