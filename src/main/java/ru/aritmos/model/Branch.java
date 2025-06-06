package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.*;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.VisitService;
import ru.aritmos.service.rules.Rule;

/** Отделение */
@Data
@EqualsAndHashCode(callSuper = false)
@Serdeable
@Introspected
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class Branch extends BranchEntity {
  /** Адрес отделения */
  String address = "";

  /** Описание отделения */
  String description = "";

  /** Префикс отделения */
  String prefix = "";

  /** Путь к отделению */
  @JsonInclude(JsonInclude.Include.ALWAYS)
  String path = "";

  /** Правила вызова */
  List<Rule> callRules = new ArrayList<>();

  /** Перечень настроек отделения */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  HashMap<String, Object> parameterMap = new HashMap<>();

  /** Точки входа */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, EntryPoint> entryPoints = new HashMap<>();

  /** Приемная */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  Reception reception = new Reception();

  /** Очереди */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, Queue> queues = new HashMap<>();

  /** Услуги */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, Service> services = new HashMap<>();

  /** Рабочие профили */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, WorkProfile> workProfiles = new HashMap<>();

  /** Точки обслуживания */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, ServicePoint> servicePoints = new HashMap<>();

  /** Возможные оказанные услуги */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, DeliveredService> possibleDeliveredServices = new HashMap<>();

  /** Возможные заметки визита */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, Mark> marks = new HashMap<>();

  /** Перечень сотрудников работающих и работавших в отделении */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, User> users = new HashMap<>();

  /**
   * Правила сегментации, где ключом является набор параметров визита, а значением идентификатор
   * соответствующей очереди
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  HashMap<String, SegmentationRuleData> segmentationRules = new HashMap<>();

  /** Перечень запрограммированных скриптом Groovy правил сегментации */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  HashMap<String, GroovyScript> customSegmentationRules = new HashMap<>();

  /** Перечень запрограммированных скриптом Groovy правил вызова */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  HashMap<String, GroovyScript> customCallRules = new HashMap<>();

  /** Группы услуг */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  HashMap<String, ServiceGroup> serviceGroups = new HashMap<>();

  /** Список * */
  HashMap<String, String> breakReasons = new HashMap<>();

  public Branch(String key, String name) {
    super(key, name);
  }

  public Integer incrementTicketCounter(Queue queue) {
    if (this.getQueues().containsKey(queue.getId())) {
      this.queues
          .get(queue.getId())
          .setTicketCounter(++this.queues.get(queue.getId()).ticketCounter);
      return this.queues.get(queue.getId()).getTicketCounter();
    }
    return -1;
  }

  /**
   * Получение перечня всех визитов отделение, с ключом - идентификатором визита
   *
   * @return перечень визитов с ключом - идентификатором визита
   */
  public HashMap<String, Visit> getAllVisits() {
    HashMap<String, Visit> visits = new HashMap<>();
    this.getServicePoints()
        .forEach(
            (k, v) -> {
              if (v.getUser() != null) {
                v.getUser().getVisits().forEach(visit -> visits.put(visit.getId(), visit));
              }
              if (v.getVisit() != null) {
                visits.put(v.getVisit().getId(), v.getVisit());
              }
              if (v.getVisits() != null) {
                v.getVisits().forEach(visit -> visits.put(visit.getId(), visit));
              }
            });
    this.getQueues()
        .forEach(
            (k, v) -> {
              if (v.getVisits() != null) {
                v.getVisits().forEach(visit -> visits.put(visit.getId(), visit));
              }
            });
    return visits;
  }

  /**
   * Получение перечня всех визитов отделение, с ключом - идентификатором визита
   *
   * @return перечень визитов с ключом - идентификатором визита
   */
  public List<Visit> getAllVisitsList() {
    List<Visit> visits = new ArrayList<>();
    this.getServicePoints()
        .forEach(
            (k, v) -> {
              if (v.getUser() != null) {
                visits.addAll(v.getUser().getVisits());
              }
              if (v.getVisit() != null) {
                visits.add(v.getVisit());
              }
              if (v.getVisits() != null) {
                visits.addAll(v.getVisits());
              }
            });
    this.getQueues()
        .forEach(
            (k, v) -> {
              if (v.getVisits() != null) {
                visits.addAll(v.getVisits());
              }
            });
    return visits;
  }

  /**
   * Получение перечня всех визитов отделение, с ключом - идентификатором визита с фильтрацией по
   * статусам визита
   *
   * @param statuses список статусов
   * @return перечень визитов с ключом - идентификатором визита
   */
  public HashMap<String, Visit> getVisitsByStatus(List<String> statuses) {
    HashMap<String, Visit> visits = new HashMap<>();
    this.getAllVisits().values().stream()
        .filter(visit -> statuses.contains(visit.getStatus()))
        .forEach(visit -> visits.put(visit.getId(), visit));
    return visits;
  }

  /**
   * Открытие точки обслуживания
   *
   * @param user сотрудник, открывший точку обслуживания
   * @param eventService служба рассылки событий
   */
  public void openServicePoint(User user, EventService eventService) {
    getUsers().put(user.getName(), user);
    if (user.servicePointId != null) {
      if (this.getServicePoints().containsKey(user.servicePointId)) {
        ServicePoint servicePoint = this.getServicePoints().get(user.getServicePointId());
        if (servicePoint.getUser() == null
            || servicePoint.getUser().getName().equals(user.getName())) {
          servicePoint.setUser(user);
          this.getUsers().put(user.getName(), user);
          eventService.send(
              "*",
              false,
              Event.builder()
                  .eventDate(ZonedDateTime.now())
                  .eventType("SERVICE_POINT_OPENED")
                  .params(new HashMap<>())
                  .body(servicePoint)
                  .build());
          eventService.send(
              "stat",
              false,
              Event.builder()
                  .eventDate(ZonedDateTime.now())
                  .eventType("SERVICE_POINT_OPENED")
                  .params(new HashMap<>())
                  .body(servicePoint)
                  .build());
          eventService.send(
              "frontend",
              false,
              Event.builder()
                  .eventDate(ZonedDateTime.now())
                  .eventType("SERVICE_POINT_OPENED")
                  .params(new HashMap<>())
                  .body(servicePoint)
                  .build());

        } else {
          throw new BusinessException(
              String.format(
                  "%s уже вошел в точку обслуживания %s ",
                  servicePoint.getUser().getName(), user.servicePointId),
              String.format(
                  "In servicePoint %s already %s logged in ",
                  user.servicePointId, servicePoint.getUser().getName()),
              eventService,
              HttpStatus.CONFLICT);
        }
      } else {
        throw new BusinessException(
            String.format("ServicePoint %s not found in %s", user.servicePointId, this.getName()),
            eventService,
            HttpStatus.CONFLICT);
      }
    }
  }

  /**
   * Закрытие точки обслуживания
   *
   * @param servicePointId идентификатор точки обслуживания
   * @param eventService служба рассылки событий
   * @param withLogout флаг закрытия сессии сотрудника
   */
  public void closeServicePoint(
      String servicePointId,
      EventService eventService,
      VisitService visitService,
      Boolean withLogout,
      Boolean isBreak,
      String breakReason) {

    if (this.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint =
          visitService.getServicePointHashMap(this.getId()).get(servicePointId);
      if (servicePoint.getUser() != null) {

        User user = servicePoint.getUser();
        eventService.send(
            "*",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("SERVICE_POINT_CLOSING")
                .params(new HashMap<>())
                .body(servicePoint)
                .build());
        eventService.send(
            "frontend",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("SERVICE_POINT_CLOSING")
                .params(new HashMap<>())
                .body(servicePoint)
                .build());

        eventService.send(
            "stat",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("SERVICE_POINT_CLOSING")
                .params(new HashMap<>())
                .body(servicePoint)
                .build());
        if (isBreak) {
          user.setLastBreakStartTime(ZonedDateTime.now());
          user.setLastBreakEndTime(null);
          user.setLastServicePointId(servicePointId);
          user.setLastBranchId(branchId);
          user.setLastBreakReason(breakReason);

          eventService.send(
              "*",
              false,
              Event.builder()
                  .eventDate(ZonedDateTime.now())
                  .eventType("STAFF_START_BREAK")
                  .params(new HashMap<>())
                  .body(user)
                  .build());
          eventService.send(
              "frontend",
              false,
              Event.builder()
                  .eventDate(ZonedDateTime.now())
                  .eventType("STAFF_START_BREAK")
                  .params(new HashMap<>())
                  .body(user)
                  .build());

          eventService.send(
              "stat",
              false,
              Event.builder()
                  .eventDate(ZonedDateTime.now())
                  .eventType("STAFF_START_BREAK")
                  .params(new HashMap<>())
                  .body(user)
                  .build());
        }
        getUsers().put(user.getName(), user);

        if (withLogout) {
          visitService.keyCloackClient.userLogout(servicePoint.getUser().getName());
        }
        servicePoint.setUser(null);
        eventService.send(
            "*",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("SERVICE_POINT_CLOSED")
                .params(new HashMap<>())
                .body(servicePoint)
                .build());
        eventService.send(
            "frontend",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("SERVICE_POINT_CLOSED")
                .params(new HashMap<>())
                .body(servicePoint)
                .build());
        eventService.send(
            "stat",
            false,
            Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("SERVICE_POINT_CLOSED")
                .params(new HashMap<>())
                .body(servicePoint)
                .build());

      } else {
        throw new BusinessException(
            String.format("ServicePoint %s already closed!", servicePointId),
            eventService,
            HttpStatus.CONFLICT);
      }

    } else {
      throw new BusinessException(
          String.format("ServicePoint %s not found in %s", servicePointId, this.getName()),
          eventService,
          HttpStatus.NOT_FOUND);
    }
    ServicePoint servicePoint =
        visitService.getServicePointHashMap(this.getId()).get(servicePointId);
    if (servicePoint.getVisit() != null) {
      visitService.visitEnd(this.getId(), servicePointId);
    }
  }

  public void updateVisit(
      Visit visit, EventService eventService, String action, VisitService visitService) {

    this.servicePoints.forEach(
        (key, value) -> {
          value.setVisit(null);
          value.getVisits().removeIf(f -> f.getId().equals(visit.getId()));
          if (value.getUser() != null) {
            value.getUser().visits.removeIf(r -> r.getId().equals(visit.getId()));
          }
          if (value.getId().equals(visit.getServicePointId())) {
            if (value.getVisit() == null || value.getVisit().getId().equals(visit.getId())) {
              value.setVisit(visit);
            } else {
              throw new BusinessException(
                  String.format(
                      "In ServicePoint %s already exists visit %s",
                      value.getId(), value.getVisit().getId()),
                  eventService,
                  HttpStatus.CONFLICT);
            }
          }
          if (value.getId().equals(visit.getPoolServicePointId())) {

            assert value.getUser() != null;
            value.getUser().getVisits().add(visit);
          }
          if (value.getUser() != null && value.getUser().getId().equals(visit.getPoolUserId())) {

            value.getUser().getVisits().add(visit);
          }
        });
    this.queues.forEach(
        (key, value) -> {
          value.getVisits().removeIf(f -> f.getId().equals(visit.getId()));
          if (value.getId().equals(visit.getQueueId())) {
            value.getVisits().add(visit);
          }
        });
    visitService.getBranchService().add(this.getId(), this);
    eventService.send(
        "*",
        false,
        Event.builder()
            .eventDate(ZonedDateTime.now())
            .eventType("VISIT_" + action)
            .params(new HashMap<>())
            .body(visit.toBuilder().build())
            .build());
    eventService.send(
        "stat",
        false,
        Event.builder()
            .eventDate(ZonedDateTime.now())
            .eventType("VISIT_" + action)
            .params(new HashMap<>())
            .body(visit.toBuilder().build())
            .build());
  }

  public void updateVisit(
      Visit visit,
      EventService eventService,
      VisitEvent visitEvent,
      VisitService visitService,
      Boolean isToStart) {
    if (isToStart) {
      updateVisit(visit, eventService, visitEvent, visitService, 0);
    } else {
      updateVisit(visit, eventService, visitEvent, visitService, -1);
    }
  }

  public void updateVisit(
      Visit visit,
      EventService eventService,
      VisitEvent visitEvent,
      VisitService visitService,
      Integer index) {
    visitService.addEvent(visit, visitEvent, eventService);
    visit.setStatus(visitEvent.getState().name());

    for (Map.Entry<String, Queue> entry : this.queues.entrySet()) {

      Queue v = entry.getValue();
      v.getVisits().removeIf(f -> f.getId().equals(visit.getId()));
      if (v.getId().equals(visit.getQueueId())) {

        try {
          if (!index.equals(-1)) {
            v.getVisits().add(index, visit);
          } else {
            v.getVisits().add(visit);
          }
        } catch (IndexOutOfBoundsException e) {
          throw new BusinessException(
              String.format(
                  "Visit position %s out of range of list range %s!", index, v.getVisits().size()),
              eventService,
              HttpStatus.CONFLICT);
        }
      }
      entry.setValue(v);
    }

    for (Map.Entry<String, ServicePoint> entry : this.getServicePoints().entrySet()) {

      ServicePoint value = entry.getValue();
      value.setVisit(null);
      value.getVisits().removeIf(f -> f.getId().equals(visit.getId()));
      if (value.getUser() != null) {
        value.getUser().visits.removeIf(r -> r.getId().equals(visit.getId()));
      }
      if (value.getId().equals(visit.getServicePointId())) {
        if (value.getVisit() == null) {
          value.setVisit(visit);
        } else {
          throw new BusinessException(
              String.format(
                  "In ServicePoint %s already exists visit %s",
                  value.getId(), value.getVisit().getId()),
              eventService,
              HttpStatus.CONFLICT);
        }
      }

      if (value.getId().equals(visit.getPoolServicePointId())) {
        try {
          value.getVisits().removeIf(f -> f.getId().equals(visit.getId()));
          if (!index.equals(-1)) {
            value.getVisits().add(index, visit);
          } else {
            value.getVisits().add(visit);
          }
        } catch (IndexOutOfBoundsException e) {
          throw new BusinessException(
              String.format(
                  "Visit position %s out of range of list range %s!",
                  index, value.getVisits().size()),
              eventService,
              HttpStatus.CONFLICT);
        }
      }

      if (value.getUser() != null) {
        value.getUser().getVisits().removeIf(f -> f.getId().equals(visit.getId()));
        getUsers().put(value.getUser().getName(), value.getUser());
        if (value.getUser().getId().equals(visit.getPoolUserId())) {

          try {
            if (!index.equals(-1)) {
              value.getUser().getVisits().add(index, visit);
            } else {
              value.getUser().getVisits().add(visit);
            }
          } catch (IndexOutOfBoundsException e) {
            throw new BusinessException(
                String.format(
                    "Visit position %s out of range of list range %s!",
                    index, value.getUser().getVisits().size()),
                eventService,
                HttpStatus.CONFLICT);
          }
        }
      }
      entry.setValue(value);
    }
    eventService.send(
        "*",
        false,
        Event.builder()
            .eventDate(ZonedDateTime.now())
            .eventType("VISIT_" + visitEvent.name())
            .params(new HashMap<>())
            .body(visit.toBuilder().build())
            .build());
    if (!VisitEvent.isIgnoredInStat(visitEvent)) {
      eventService.send(
          "stat",
          false,
          Event.builder()
              .eventDate(ZonedDateTime.now())
              .eventType("VISIT_" + visitEvent.name())
              .params(new HashMap<>())
              .body(visit.toBuilder().build())
              .build());
    }
    if (VisitEvent.isFrontEndEvent(visitEvent)) {
      eventService.send(
          "frontend",
          false,
          Event.builder()
              .eventDate(ZonedDateTime.now())
              .eventType("VISIT_" + visitEvent.name())
              .params(new HashMap<>())
              .body(visit.toBuilder().build())
              .build());
    }
    visitService.getBranchService().add(this.getId(), this);
  }

  public void updateVisit(
      Visit visit, EventService eventService, VisitEvent visitEvent, VisitService visitService) {
    updateVisit(visit, eventService, visitEvent, visitService, true);
  }

  public void addUpdateService(
      HashMap<String, Service> serviceHashMap,
      EventService eventService,
      Boolean checkVisits,
      VisitService visitService) {
    serviceHashMap.forEach(
        (k, v) -> {
          if (this.getServices().containsKey(k)) {
            if (checkVisits) {
              this.getAllVisits()
                  .forEach(
                      (k2, v2) -> {
                        if (v2.getCurrentService() != null
                            && v2.getCurrentService().getId().equals(k)) {
                          v2.setCurrentService(v.clone());
                        }

                        List<Service> unservedServices =
                            v2.getUnservedServices().stream()
                                .map(m -> m.getId().equals(k) ? v.clone() : m)
                                .toList();
                        v2.setUnservedServices(unservedServices);

                        List<Service> servedServices =
                            v2.getServedServices().stream()
                                .map(m -> m.getId().equals(k) ? v.clone() : m)
                                .toList();
                        v2.setServedServices(servedServices);

                        this.updateVisit(v2, eventService, "UPDATE_SERVICE", visitService);
                      });
            } else {
              this.getAllVisits()
                  .forEach(
                      (k2, v2) -> {
                        if (v2.getServedServices().stream().anyMatch(am -> am.getId().equals(k))
                            || v2.getUnservedServices().stream()
                                .anyMatch(am -> am.getId().equals(k))
                            || v2.getCurrentService().getId().equals(k)) {
                          throw new BusinessException(
                              "Updated service " + k + " is in use now!",
                              eventService,
                              HttpStatus.CONFLICT);
                        }
                      });
            }
            eventService.sendChangedEvent(
                "config",
                false,
                this.getServices().get(k),
                v.clone(),
                new HashMap<>(),
                "Update service");
          } else {
            eventService.sendChangedEvent(
                "config", false, null, v.clone(), new HashMap<>(), "Add service");
          }
          this.getServices().put(k, v);
        });
  }

  public void deleteServices(
      List<String> serviceIds,
      EventService eventService,
      Boolean checkVisits,
      VisitService visitService) {
    serviceIds.forEach(
        id -> {
          if (this.getServices().containsKey(id)) {
            if (checkVisits) {

              this.getAllVisits()
                  .forEach(
                      (k2, v2) -> {
                        if (v2.getCurrentService() != null
                            && v2.getCurrentService().getId().equals(id)) {
                          v2.setCurrentService(null);
                          if (!v2.getUnservedServices().isEmpty()) {
                            v2.setCurrentService(v2.getUnservedServices().get(0).clone());
                            v2.getUnservedServices().remove(0);
                          } else {
                            v2.setQueueId(null);
                            v2.setServicePointId(null);
                          }
                        }

                        List<Service> unservedServices =
                            v2.getUnservedServices().stream()
                                .filter(f -> !f.getId().equals(id))
                                .toList();
                        v2.setUnservedServices(unservedServices);

                        List<Service> servedServices =
                            v2.getServedServices().stream()
                                .filter(f -> !f.getId().equals(id))
                                .toList();
                        v2.setServedServices(servedServices);

                        this.updateVisit(v2, eventService, "SERVICE_DELETED", visitService);
                      });
            } else {
              this.getAllVisits()
                  .forEach(
                      (k2, v2) -> {
                        if (v2.getServedServices().stream().anyMatch(am -> am.getId().equals(id))
                            || v2.getUnservedServices().stream()
                                .anyMatch(am -> am.getId().equals(id))
                            || v2.getCurrentService().getId().equals(id)) {
                          throw new BusinessException(
                              "Delete service " + id + " is in use now!",
                              eventService,
                              HttpStatus.CONFLICT);
                        }
                      });
            }
          }

          eventService.sendChangedEvent(
              "config", false, null, this.getServices().get(id), new HashMap<>(), "Delete service");
          this.getServices().remove(id);
        });
  }

  public void adUpdateServiceGroups(
      HashMap<String, ServiceGroup> serviceGroupHashMap, EventService eventService) {

    serviceGroupHashMap.forEach(
        (key, value) ->
            value.serviceIds.forEach(
                serviceId -> {
                  if (!this.getServices().containsKey(serviceId)) {
                    throw new BusinessException(
                        "Service " + serviceId + " not found!", eventService, HttpStatus.NOT_FOUND);
                  } else {
                    this.getServices().get(serviceId).setServiceGroupId(key);
                  }
                }));
    this.setServiceGroups(serviceGroupHashMap);
    eventService.sendChangedEvent(
        "config",
        false,
        null,
        serviceGroupHashMap,
        new HashMap<>(),
        "Add or update service groups");
  }

  public void addUpdateServicePoint(
      HashMap<String, ServicePoint> servicePointHashMap,
      Boolean restoreVisit,
      Boolean restoreUser,
      EventService eventService) {
    servicePointHashMap.forEach(
        (k, v) -> {
          if (this.getServicePoints().containsKey(k)
              && restoreVisit
              && this.getServicePoints().get(k).getVisit() != null) {

            v.setVisit(this.getServicePoints().get(k).getVisit());
          }

          if (this.getServicePoints().containsKey(k)
              && restoreUser
              && this.getServicePoints().get(k).getUser() != null) {
            v.setUser(this.getServicePoints().get(k).getUser());
          }
          if (this.getServicePoints().containsKey(k)) {
            eventService.sendChangedEvent(
                "config",
                false,
                this.getServicePoints().get(k),
                v,
                new HashMap<>(),
                "Update service point");
          } else {
            eventService.sendChangedEvent(
                "config", false, null, v, new HashMap<>(), "Add service point");
          }
          this.getServicePoints().put(k, v);
        });
  }

  public void deleteServicePoints(List<String> servicePointIds, EventService eventService) {
    servicePointIds.forEach(
        f -> {
          if (this.getServicePoints().containsKey(f)) {
            eventService.sendChangedEvent(
                "config",
                false,
                null,
                this.getServicePoints().get(f),
                new HashMap<>(),
                "Delete service point");
            this.getServicePoints().remove(f);
          }
        });
  }

  public void addUpdateQueues(
      HashMap<String, Queue> queueHashMap, Boolean restoreVisits, EventService eventService) {
    queueHashMap.forEach(
        (k, v) -> {
          if (this.getQueues().containsKey(k)
              && restoreVisits
              && !this.getQueues().get(k).getVisits().isEmpty()) {

            v.getVisits().addAll(this.getQueues().get(k).getVisits());
            v.setTicketCounter(this.getQueues().get(k).getTicketCounter());
          }
          if (this.getServicePoints().containsKey(k)) {
            eventService.sendChangedEvent(
                "config", false, this.getQueues().get(k), v, new HashMap<>(), "Update queue");
          } else {
            eventService.sendChangedEvent("config", false, null, v, new HashMap<>(), "Add queue");
          }
          if (this.getServicePoints().containsKey(k)) {
            eventService.sendChangedEvent(
                "config",
                false,
                this.getServicePoints().get(k),
                v,
                new HashMap<>(),
                "Update service");
          } else {
            eventService.sendChangedEvent("config", false, null, v, new HashMap<>(), "Add service");
          }
          this.getQueues().put(k, v);
        });
  }

  public void deleteQueues(List<String> queueIds, EventService eventService) {

    queueIds.forEach(
        f -> {
          eventService.sendChangedEvent(
              "config", false, null, this.getQueues().get(f), new HashMap<>(), "Delete queue");
          this.getQueues().remove(f);
        });
  }

  public void adUpdateSegmentRules(
      HashMap<String, SegmentationRuleData> segmentationRuleDataHashMap,
      EventService eventService) {

    segmentationRuleDataHashMap.forEach(
        (key, value) -> {
          if (!this.getServiceGroups().containsKey(value.serviceGroupId)) {
            throw new BusinessException(
                "Service group " + value.serviceGroupId + " not found!",
                eventService,
                HttpStatus.NOT_FOUND);
          }
        });
    this.setSegmentationRules(segmentationRuleDataHashMap);
    eventService.sendChangedEvent(
        "config",
        false,
        null,
        segmentationRuleDataHashMap,
        new HashMap<>(),
        "Add or update service groups");
  }
}
