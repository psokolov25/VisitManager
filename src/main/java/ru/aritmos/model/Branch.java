package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.Serdeable;
import lombok.*;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.VisitService;

import java.time.ZonedDateTime;
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
    HashMap<String, Object> parameterMap = new HashMap<>();

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
    /**
     * Возможные оказанные услуги
     */
    HashMap<String, DeliveredService> possibleDeliveredServices = new HashMap<>();


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

    public void openServicePoint(User user, EventService eventService) {
        if (user.servicePointId != null) {
            if (this.getServicePoints().containsKey(user.servicePointId)) {
                ServicePoint servicePoint = this.getServicePoints().get(user.getServicePointId());
                if (servicePoint.getUser() == null) {
                    servicePoint.setUser(user);
                    eventService.send("*", false, Event.builder()
                            .eventDate(ZonedDateTime.now())
                            .eventType("SERVICE_POINT_OPENED")
                            .params(new HashMap<>())
                            .body(servicePoint).build());
                    eventService.send("stat", false, Event.builder()
                            .eventDate(ZonedDateTime.now())
                            .eventType("SERVICE_POINT_OPENED")
                            .params(new HashMap<>())
                            .body(servicePoint).build());
                } else {
                    throw new BusinessException(String.format("In servicePoint %s already %s logged in ", user.servicePointId, servicePoint.getUser().getName()), eventService, HttpStatus.CONFLICT);
                }
            } else {
                throw new BusinessException(String.format("ServicePoint %s not found in %s", user.servicePointId, this.getName()), eventService, HttpStatus.CONFLICT);
            }
        }

    }


    public void closeServicePoint(String servicePointId, EventService eventService) {

            if (this.getServicePoints().containsKey(servicePointId)) {
                ServicePoint servicePoint = this.getServicePoints().get(servicePointId);
                if (servicePoint.getUser() != null) {

                    servicePoint.setUser(null);
                    eventService.send("*", false, Event.builder()
                            .eventDate(ZonedDateTime.now())
                            .eventType("SERVICE_POINT_CLOSED")
                            .params(new HashMap<>())
                            .body(servicePoint).build());
                    eventService.send("stat", false, Event.builder()
                            .eventDate(ZonedDateTime.now())
                            .eventType("SERVICE_POINT_CLOSED")
                            .params(new HashMap<>())
                            .body(servicePoint).build());

                }
                else
                {
                    throw new BusinessException(String.format("ServicePoint %s already closed!", servicePointId), eventService, HttpStatus.CONFLICT);
                }

            } else {
                throw new BusinessException(String.format("ServicePoint %s not found in %s", servicePointId, this.getName()), eventService, HttpStatus.NOT_FOUND);
            }


    }


    public void updateVisit(Visit visit, EventService eventService, String action) {


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
        eventService.send("*", false, Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_" + action)
                .params(new HashMap<>())
                .body(visit.toBuilder().build()).build());
        eventService.send("stat", false, Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_" + action)
                .params(new HashMap<>())
                .body(visit.toBuilder().build()).build());
    }
    public void updateVisit(Visit visit, EventService eventService, VisitEvent visitEvent, VisitService visitService) {
        visitService.addEvent(visit,visitEvent,eventService);
        visit.setStatus(visitEvent.getState().name());
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
        eventService.send("*", false, Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_" + visitEvent.name())
                .params(new HashMap<>())
                .body(visit.toBuilder().build()).build());
        eventService.send("stat", false, Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("VISIT_" + visitEvent.name())
                .params(new HashMap<>())
                .body(visit.toBuilder().build()).build());
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


                                        this.updateVisit(v2, eventService, "UPDATE_SERVICE");
                                    }

                            );
                        } else {
                            this.getAllVisits().forEach((k2, v2) ->
                            {
                                if (v2.getServedServices().stream().anyMatch(am -> am.getId().equals(k)) ||
                                        v2.getUnservedServices().stream().anyMatch(am -> am.getId().equals(k)) ||
                                        v2.getCurrentService().getId().equals(k)) {
                                    throw new BusinessException("Updated service " + k + " is in use now!", eventService, HttpStatus.CONFLICT);
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
                                    if (!v2.getUnservedServices().isEmpty()) {
                                        v2.setCurrentService(v2.getUnservedServices().get(0));
                                        v2.getUnservedServices().remove(0);
                                    } else {
                                        v2.setQueueId(null);
                                        v2.setServicePointId(null);

                                    }
                                }

                                List<Service> unservedServices = v2.getUnservedServices().stream().filter(f -> !f.getId().equals(id)).toList();
                                v2.setUnservedServices(unservedServices);


                                List<Service> servedServices = v2.getServedServices().stream().filter(f -> !f.getId().equals(id)).toList();
                                v2.setServedServices(servedServices);


                                this.updateVisit(v2, eventService, "SERVICE_DELETED");
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
                v.setTicketCounter(this.getQueues().get(k).getTicketCounter());

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
