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
                    throw new BusinessException(String.format("In servicePoint %s already %s logged in %s ", user.servicePoinrtId, servicePoint.getUser().getName(), servicePoint.getUser().getName()), eventService, HttpStatus.CONFLICT);
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

    public void addUpdateService(HashMap<String, Service> serviceHashMap, EventService eventService, Boolean checkVisits) {
        serviceHashMap.forEach((k, v) ->
                {

                    if (this.getServices().containsKey(k)) {
                        if (checkVisits) {
                            this.getAllVisits().forEach((k2, v2) ->
                                    {
                                        if (v2.getCurrentService() != null && v2.getCurrentService().getId().equals(k)) {
                                            v2.setCurrentService(v);
                                        }

                                        List<Service> unservedServices = v2.getUnservedServices().stream().map(m -> m.getId().equals(k) ? v : m).toList();
                                        v2.setUnservedServices(unservedServices);


                                        List<Service> servedServices = v2.getServedServices().stream().map(m -> m.getId().equals(k) ? v : m).toList();
                                        v2.setServedServices(servedServices);


                                        this.updateVisit(v2, eventService);
                                    }

                            );
                        }
                        else {
                            this.getAllVisits().forEach((k2, v2) ->
                            {
                                if (v2.getServedServices().stream().anyMatch(am -> am.getId().equals(k)) ||
                                        v2.getUnservedServices().stream().anyMatch(am -> am.getId().equals(k)) ||
                                        v2.getCurrentService().getId().equals(k)) {
                                    throw new BusinessException("Delete service " + k + " is in use now!", eventService, HttpStatus.CONFLICT);
                                }


                            });
                        }
                        eventService.sendChangedEvent("config", false, this.getServices().get(k), v, new HashMap<>(), "Update service");
                    } else {
                        eventService.sendChangedEvent("config", false, null, v, new HashMap<>(), "Add service");
                    }
                    this.getServices().put(k, v);
                }
        );
    }

    public void deleteServices(List<String> serviceIds, EventService eventService, Boolean checkVisits) {
        serviceIds.forEach(id -> {
            if (this.getServices().containsKey(id)) {
                if (checkVisits) {

                    this.getAllVisits().forEach((k2, v2) ->
                            {
                                if (v2.getCurrentService() != null && v2.getCurrentService().getId().equals(id)) {
                                    v2.setCurrentService(null);
                                }

                                List<Service> unservedServices = v2.getUnservedServices().stream().filter(f -> !f.getId().equals(id)).toList();
                                v2.setUnservedServices(unservedServices);


                                List<Service> servedServices = v2.getServedServices().stream().filter(f -> !f.getId().equals(id)).toList();
                                v2.setServedServices(servedServices);


                                this.updateVisit(v2, eventService);
                            }
                    );
                } else {
                    this.getAllVisits().forEach((k2, v2) ->
                    {
                        if (v2.getServedServices().stream().anyMatch(am -> am.getId().equals(id)) ||
                                v2.getUnservedServices().stream().anyMatch(am -> am.getId().equals(id)) ||
                                v2.getCurrentService().getId().equals(id)) {
                            throw new BusinessException("Delete service " + id + " is in use now!", eventService, HttpStatus.CONFLICT);
                        }


                    });
                }


            }
            
            eventService.sendChangedEvent("config", false, null, this.getServices().get(id), new HashMap<>(), "Delete service");
            this.getServices().remove(id);


        });
    }

    public void addUpdateServicePoint(HashMap<String, ServicePoint> servicePointHashMap, Boolean restoreVisit, Boolean restoreUser, EventService eventService) {
        servicePointHashMap.forEach((k, v) -> {

            if (this.getServicePoints().containsKey(k) && restoreVisit && this.getServicePoints().get(k).getVisit() != null) {

                v.setVisit(this.getServicePoints().get(k).getVisit());

            }

            if (this.getServicePoints().containsKey(k) && restoreUser && this.getServicePoints().get(k).getUser() != null) {
                v.setUser(this.getServicePoints().get(k).getUser());

            }
            if (this.getServicePoints().containsKey(k)) {
                eventService.sendChangedEvent("config", false, this.getServicePoints().get(k), v, new HashMap<>(), "Update service point");
            } else {
                eventService.sendChangedEvent("config", false, null, v, new HashMap<>(), "Add service point");
            }
            this.getServicePoints().put(k, v);

        });

    }

    public void deleteServicePoints(List<String> servicePointIds, EventService eventService) {
        servicePointIds.forEach(f -> {
                    if (this.getServicePoints().containsKey(f)) {
                        eventService.sendChangedEvent("config", false, null, this.getServicePoints().get(f), new HashMap<>(), "Delete service point");
                        this.getServicePoints().remove(f);
                    }
                }
        );
    }

    public void addUpdateQueues(HashMap<String, Queue> queueHashMap, Boolean restoreVisits, EventService eventService) {
        queueHashMap.forEach((k, v) -> {


            if (this.getQueues().containsKey(k) && restoreVisits && !this.getQueues().get(k).getVisits().isEmpty()) {

                v.getVisits().addAll(this.getQueues().get(k).getVisits());

            }
            if (this.getServicePoints().containsKey(k)) {
                eventService.sendChangedEvent("config", false, this.getQueues().get(k), v, new HashMap<>(), "Update queue");
            } else {
                eventService.sendChangedEvent("config", false, null, v, new HashMap<>(), "Add queue");
            }
            if (this.getServicePoints().containsKey(k)) {
                eventService.sendChangedEvent("config", false, this.getServicePoints().get(k), v, new HashMap<>(), "Update service");
            } else {
                eventService.sendChangedEvent("config", false, null, v, new HashMap<>(), "Add service");
            }
            this.getQueues().put(k, v);

        });

    }

    public void deleteQueues(List<String> sueueIds, EventService eventService) {

        sueueIds.forEach(f -> {
            eventService.sendChangedEvent("config", false, null, this.getQueues().get(f), new HashMap<>(), "Delete queue");
            this.getQueues().remove(f);
        });
    }


}
