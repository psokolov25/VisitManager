package ru.aritmos.service;

import io.micronaut.http.HttpStatus;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.DelayedEvents;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.*;
import ru.aritmos.model.Queue;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.model.visit.VisitEventInformation;
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.service.rules.SegmentationRule;

@Slf4j
@Singleton
@SuppressWarnings("unused")
public class VisitService {
  @Inject public KeyCloackClient keyCloackClient;
  @Getter @Inject BranchService branchService;
  @Inject EventService eventService;
  @Inject DelayedEvents delayedEvents;
  @Inject PrinterService printerService;
  CallRule waitingTimeCallRule;
  CallRule lifeTimeCallRule;
  @Inject SegmentationRule segmentationRule;

  @Inject
  public void setWaitingTimeCallRule(@Named("MaxWaitingTimeCallRule") CallRule callRule) {
    this.waitingTimeCallRule = callRule;
  }

  @Inject
  public void setLifeTimeCallRule(@Named("MaxLifeTimeCallRule") CallRule callRule) {
    this.lifeTimeCallRule = callRule;
  }

  /**
   * Возвращает визит по его отделению и идентификатору
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор визита
   * @return визит
   */
  public Visit getVisit(String branchId, String visitId) {
    if (getAllVisits(branchId).containsKey(visitId)) {
      return getAllVisits(branchId).get(visitId);
    }
    throw new BusinessException(
        String.format("Visit %s not found!", visitId), eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Возвращает доступныее сервис поинты отделения
   *
   * @param branchId идентификатор отделения
   * @return перечень сервис поинтом, с ключом - идентификатором сервис поинта
   */
  public @NotNull HashMap<String, ServicePoint> getStringServicePointHashMap(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);
    HashMap<String, ServicePoint> freeServicePoints = new HashMap<>();
    currentBranch.getServicePoints().entrySet().stream()
        .filter(f -> f.getValue().getUser() == null)
        .forEach(fe -> freeServicePoints.put(fe.getKey(), fe.getValue()));
    return freeServicePoints;
  }

  /**
   * Возвращает сервис поинты отделения
   *
   * @param branchId идентификатор отделения
   * @return перечень сервис поинтом, с ключом - идентификатором сервис поинта
   */
  public @NotNull HashMap<String, ServicePoint> getServicePointHashMap(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);
    return currentBranch.getServicePoints().entrySet().stream()
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, HashMap::new));
  }

  /**
   * Возвращает рабочие профили
   *
   * @param branchId идентификатор отделения
   * @return перечень рабочих профилей
   */
  public @NotNull List<TinyClass> getWorkProfiles(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);

    return currentBranch.getWorkProfiles().values().stream()
        .map(m -> new TinyClass(m.getId(), m.getName()))
        .toList();
  }

  /**
   * Возвращает рабочие профили
   *
   * @param branchId идентификатор отделения
   * @return перечень рабочих профилей
   */
  public @NotNull List<User> getUsers(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);

    return currentBranch.getUsers().values().stream().toList();
  }

  /**
   * Возвращает визиты содержащиеся в очереди визиты сортируются по времени ожидания, от большего к
   * мееньшкму
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @return список визитов
   */
  @ExecuteOn(TaskExecutors.IO)
  public List<Visit> getVisits(String branchId, String queueId) {
    Branch currentBranch = branchService.getBranch(branchId);
    Queue queue;
    if (currentBranch.getQueues().containsKey(queueId)) {
      queue = currentBranch.getQueues().get(queueId);
    } else {
      throw new BusinessException(
          "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    List<Visit> visits;
    visits =
        queue.getVisits().stream()
            .filter(
                f ->
                    f.getReturnTimeDelay() == null || f.getReturnTimeDelay() < f.getReturningTime())
            .toList();
    return visits.stream()
        .sorted((f1, f2) -> Long.compare(f2.getWaitingTime(), f1.getWaitingTime()))
        .toList();
  }

  /**
   * Получение списка визитов в указанной очереди указанного отделения с ограничением выдачи
   * элементов Максимальное количество визитов указывается в параметре limit, если количество
   * визитов меньше - выводятся все визиты визиты сортируются по времени ожидания, от большего к
   * мееньшкму
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @param limit максимальное количество визитов
   * @return список визитов
   */
  public List<Visit> getVisits(String branchId, String queueId, Long limit) {
    return getVisits(branchId, queueId).stream().limit(limit).toList();
  }

  /**
   * Создание визита
   *
   * @param branchId идентификатор отделения
   * @param entryPointId идентификатор энтри поинта
   * @param visitParameters передаваемые список услуг и дополнительные параметры визита
   * @param printTicket флаг печати талона
   * @return созданный визит
   */
  public Visit createVisit(
      String branchId, String entryPointId, VisitParameters visitParameters, Boolean printTicket)
      throws SystemException {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServices().keySet().stream()
        .anyMatch(visitParameters.getServiceIds()::contains)) {
      ArrayList<Service> services = new ArrayList<>();
      visitParameters
          .getServiceIds()
          .forEach(f -> services.add(currentBranch.getServices().get(f).clone()));

      return visitAutoCall(
          createVisit2(
              branchId, entryPointId, services, visitParameters.getParameters(), printTicket));

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Создание визита с указанием правила сегментации
   *
   * @param branchId идентификатор отделения
   * @param entryPointId идентификатор энтри поинта
   * @param visitParameters передаваемые список услуг и дополнительные параметры визита
   * @param printTicket флаг печати талона
   * @param segmentationRuleId идентификатор правила вызова
   * @return созданный визит
   */
  public Visit createVisit(
      String branchId,
      String entryPointId,
      VisitParameters visitParameters,
      Boolean printTicket,
      String segmentationRuleId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServices().keySet().stream()
        .anyMatch(visitParameters.getServiceIds()::contains)) {
      ArrayList<Service> services = new ArrayList<>();
      visitParameters
          .getServiceIds()
          .forEach(f -> services.add(currentBranch.getServices().get(f).clone()));

      return visitAutoCall(
          createVisit2(
              branchId,
              entryPointId,
              services,
              visitParameters.getParameters(),
              printTicket,
              segmentationRuleId));

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Создание визита из приемной
   *
   * @param branchId идентификатор отделения
   * @param printerId идентификатор энтри поинта
   * @param visitParameters передаваемые список услуг и дополнительные параметры визита
   * @param printTicket флаг печати талона
   * @return созданный визит
   */
  public Visit createVisitFromReception(
      String branchId, String printerId, VisitParameters visitParameters, Boolean printTicket)
      throws SystemException {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServices().keySet().stream()
        .anyMatch(visitParameters.getServiceIds()::contains)) {
      ArrayList<Service> services = new ArrayList<>();
      visitParameters
          .getServiceIds()
          .forEach(f -> services.add(currentBranch.getServices().get(f).clone()));

      return visitAutoCall(
          createVisit2FromReception(
              branchId, printerId, services, visitParameters.getParameters(), printTicket));

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Создание визита из приемной
   *
   * @param branchId идентификатор отделения
   * @param printerId идентификатор энтри поинта
   * @param visitParameters передаваемые список услуг и дополнительные параметры визита
   * @param printTicket флаг печати талона
   * @return созданный визит
   */
  public Visit createVisitFromReception(
      String branchId,
      String printerId,
      VisitParameters visitParameters,
      Boolean printTicket,
      String segmentationRuleId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServices().keySet().stream()
        .anyMatch(visitParameters.getServiceIds()::contains)) {
      ArrayList<Service> services = new ArrayList<>();
      visitParameters
          .getServiceIds()
          .forEach(f -> services.add(currentBranch.getServices().get(f).clone()));

      return visitAutoCall(
          createVisit2FromReception(
              branchId,
              printerId,
              services,
              visitParameters.getParameters(),
              printTicket,
              segmentationRuleId));

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  public Visit createVirtualVisit(
      String branchId, String servicePointId, VisitParameters visitParameters)
      throws SystemException {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServices().keySet().stream()
        .anyMatch(visitParameters.getServiceIds()::contains)) {
      ArrayList<Service> services = new ArrayList<>();
      visitParameters
          .getServiceIds()
          .forEach(f -> services.add(currentBranch.getServices().get(f)));

      return createVirtualVisit2(
          branchId, servicePointId, services, visitParameters.getParameters());

    } else {
      throw new BusinessException("Services not found!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление события в визит
   *
   * @param visit визит
   * @param event событие
   * @param eventService служба отправки события визита на шину данных
   */
  public void addEvent(Visit visit, VisitEvent event, EventService eventService) {
    if (visit.getVisitEvents().isEmpty()) {
      if (!event.equals(VisitEvent.CREATED))
        throw new BusinessException("wasn't early created", eventService, HttpStatus.CONFLICT);
      else {
        visit
            .getVisitEventInformationList()
            .add(
                VisitEventInformation.builder()
                    .visitEvent(event)
                    .parameters(new HashMap<>(event.getParameters()))
                    .eventDateTime(event.dateTime != null ? event.dateTime : ZonedDateTime.now())
                    .build());
        visit.getVisitEvents().add(event);
      }

    } else {
      VisitEvent prevEvent = visit.getVisitEvents().get(visit.getVisitEvents().size() - 1);
      if (prevEvent.canBeNext(event)) {
        visit
            .getVisitEventInformationList()
            .add(
                VisitEventInformation.builder()
                    .visitEvent(event)
                    .parameters(new HashMap<>(event.getParameters()))
                    .eventDateTime(event.dateTime != null ? event.dateTime : ZonedDateTime.now())
                    .build());
        visit.getVisitEvents().add(event);

      } else
        throw new BusinessException(
            String.format(
                "%s can't be next status %s",
                event.name(), visit.getVisitEvents().get(visit.getVisitEvents().size() - 1).name()),
            eventService,
            HttpStatus.CONFLICT);
    }
    event.getState();
  }

  /**
   * Получение списка очередей
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return опциональный список очередей
   */
  public Optional<List<Queue>> getQueues(String branchId, String servicePointId) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getUser() != null) {
        String workprofileId = servicePoint.getUser().getCurrentWorkProfileId();
        List<String> queueIds = currentBranch.getWorkProfiles().get(workprofileId).getQueueIds();
        List<Queue> avaibleQueues =
            currentBranch.getQueues().entrySet().stream()
                .filter(f -> queueIds.contains(f.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        return Optional.of(avaibleQueues);

      } else {
        throw new BusinessException(
            "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
      }

    } else {
      throw new BusinessException(
          "ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * @param branchId идентификатор отделения
   * @return список визитов
   */
  public HashMap<String, Visit> getAllVisits(String branchId) {
    return branchService.getBranch(branchId).getAllVisits();
  }

  /**
   * Получение списка визитов с фильтрацией по их статусам. Выводятся визиты, чей статус входит в
   * передаваемым в теле запроса списком статусов.
   *
   * @param branchId идентификатор отделения
   * @param statuses список статусов, по котором должны быть отфильтрованы визиты
   * @return список визитов
   */
  public HashMap<String, Visit> getVisitsByStatuses(String branchId, List<String> statuses) {
    return branchService.getBranch(branchId).getVisitsByStatus(statuses);
  }

  /**
   * Создание визита
   *
   * @param branchId идентификатор отделения
   * @param entryPointId идентификатор энтри поинта
   * @param services список услуг
   * @param printTicket флаг печати талона
   * @return визит
   */
  public Visit createVisit2(
      String branchId,
      String entryPointId,
      ArrayList<Service> services,
      HashMap<String, String> parametersMap,
      Boolean printTicket)
      throws SystemException {
    Branch currentBranch = branchService.getBranch(branchId);

    if (!services.isEmpty()) {
      if (currentBranch.getServices().containsKey(services.get(0).getId())) {
        Service currentService = currentBranch.getServices().get(services.get(0).getId()).clone();
        List<Service> unServedServices = new ArrayList<>();
        services.stream()
            .skip(1)
            .forEach(f -> unServedServices.add(currentBranch.getServices().get(f.getId()).clone()));

        EntryPoint entryPoint;

        if (!currentBranch.getEntryPoints().containsKey(entryPointId)) {
          throw new BusinessException(
              "EntryPoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        } else {
          entryPoint = currentBranch.getEntryPoints().get(entryPointId);
        }

        Visit visit =
            Visit.builder()
                .id(UUID.randomUUID().toString())
                .status("WAITING")
                .entryPoint(entryPoint)
                .printTicket(printTicket)
                .branchId(branchId)
                .branchName(currentBranch.getName())
                .branchPrefix(currentBranch.getPrefix())
                .branchPath(currentBranch.getPath())
                .currentService(currentService)
                .unservedServices(unServedServices)
                .createDateTime(ZonedDateTime.now())
                .visitMarks(new ArrayList<>())
                .visitNotes(new ArrayList<>())
                .visitEvents(new ArrayList<>())
                .visitEventInformationList(new ArrayList<>())
                .returnTimeDelay(0L)
                // .updateDateTime(ZonedDateTime.now())
                // .transferDateTime(ZonedDateTime.now())
                // .endDateTime(ZonedDateTime.now())
                .servicePointId(null)
                .servedServices(new ArrayList<>())
                .parameterMap(parametersMap)
                .build();
        Queue serviceQueue;
        if (segmentationRule.getQueue(visit, currentBranch).isPresent()) {
          serviceQueue = segmentationRule.getQueue(visit, currentBranch).get();

          serviceQueue.setTicketCounter(
              branchService.incrementTicketCounter(branchId, serviceQueue));
          visit.setQueueId(serviceQueue.getId());
          visit.setTicket(
              (serviceQueue.getTicketPrefix()
                  + String.format("%03d", serviceQueue.getTicketCounter())));
          VisitEvent event = VisitEvent.CREATED;
          event
              .getParameters()
              .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
          event.dateTime = ZonedDateTime.now();

          branchService.updateVisit(visit, event, this);
          if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
            VisitEvent queueEvent = VisitEvent.PLACED_IN_QUEUE;
            queueEvent.dateTime = ZonedDateTime.now();
            queueEvent
                .getParameters()
                .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
            queueEvent.getParameters().put("queueId", serviceQueue.getId());
            visit.setQueueId(serviceQueue.getId());

            if (printTicket
                && entryPoint.getPrinter() != null
                && entryPoint.getPrinter().getId() != null) {
              printerService.print(entryPoint.getPrinter().getId(), visit);
            }

            // changedVisitEventSend("CREATED", null, visit, new HashMap<>());
            branchService.updateVisit(visit, queueEvent, this);
            log.info("Visit {} created!", visit);

            return visit;
          } else {
            throw new BusinessException(
                "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
          }

        } else {
          throw new BusinessException(
              "Services can not be empty!", eventService, HttpStatus.BAD_REQUEST);
        }
      } else {
        throw new BusinessException(
            "Service  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
      }
    }

    throw new BusinessException(
        "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Создание визита
   *
   * @param branchId идентификатор отделения
   * @param entryPointId идентификатор энтри поинта
   * @param services список услуг
   * @param printTicket флаг печати талона
   * @return визит
   */
  public Visit createVisit2(
      String branchId,
      String entryPointId,
      ArrayList<Service> services,
      HashMap<String, String> parametersMap,
      Boolean printTicket,
      String segmentationRuleId) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (!services.isEmpty()) {
      if (currentBranch.getServices().containsKey(services.get(0).getId())) {
        Service currentService = currentBranch.getServices().get(services.get(0).getId()).clone();
        List<Service> unServedServices = new ArrayList<>();
        services.stream()
            .skip(1)
            .forEach(f -> unServedServices.add(currentBranch.getServices().get(f.getId()).clone()));

        EntryPoint entryPoint;

        if (!currentBranch.getEntryPoints().containsKey(entryPointId)) {
          throw new BusinessException(
              "EntryPoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        } else {
          entryPoint = currentBranch.getEntryPoints().get(entryPointId);
        }

        Visit visit =
            Visit.builder()
                .id(UUID.randomUUID().toString())
                .status("WAITING")
                .entryPoint(entryPoint)
                .printTicket(printTicket)
                .branchId(branchId)
                .branchName(currentBranch.getName())
                .branchPrefix(currentBranch.getPrefix())
                .branchPath(currentBranch.getPath())
                .currentService(currentService)
                .unservedServices(unServedServices)
                .createDateTime(ZonedDateTime.now())
                .visitMarks(new ArrayList<>())
                .visitNotes(new ArrayList<>())
                .visitEvents(new ArrayList<>())
                .visitEventInformationList(new ArrayList<>())
                .returnTimeDelay(0L)
                // .updateDateTime(ZonedDateTime.now())
                // .transferDateTime(ZonedDateTime.now())
                // .endDateTime(ZonedDateTime.now())
                .servicePointId(null)
                .servedServices(new ArrayList<>())
                .parameterMap(parametersMap)
                .build();
        Queue serviceQueue;
        Optional<Queue> queue = segmentationRule.getQueue(visit, currentBranch, segmentationRuleId);
        if (queue.isPresent()) {
          serviceQueue = queue.get();

          serviceQueue.setTicketCounter(
              branchService.incrementTicketCounter(branchId, serviceQueue));
          visit.setQueueId(serviceQueue.getId());
          visit.setTicket(
              (serviceQueue.getTicketPrefix()
                  + String.format("%03d", serviceQueue.getTicketCounter())));
          VisitEvent event = VisitEvent.CREATED;
          event
              .getParameters()
              .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
          event.dateTime = ZonedDateTime.now();

          branchService.updateVisit(visit, event, this);
          if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
            VisitEvent queueEvent = VisitEvent.PLACED_IN_QUEUE;
            queueEvent.dateTime = ZonedDateTime.now();
            queueEvent
                .getParameters()
                .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
            queueEvent.getParameters().put("queueId", serviceQueue.getId());
            visit.setQueueId(serviceQueue.getId());

            if (printTicket
                && entryPoint.getPrinter() != null
                && entryPoint.getPrinter().getId() != null) {
              printerService.print(entryPoint.getPrinter().getId(), visit);
            }

            // changedVisitEventSend("CREATED", null, visit, new HashMap<>());
            branchService.updateVisit(visit, queueEvent, this);
            log.info("Visit {} created!", visit);

            return visit;
          } else {
            throw new BusinessException(
                "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
          }

        } else {
          throw new BusinessException(
              "Services can not be empty!", eventService, HttpStatus.BAD_REQUEST);
        }
      } else {
        throw new BusinessException(
            "Service  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
      }
    }

    throw new BusinessException(
        "Queue  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Создание визита из приемной
   *
   * @param branchId идентификатор отделения
   * @param printerId идентификатор принтера
   * @param services список услуг
   * @param printTicket флаг печати талона
   * @return визит
   */
  public Visit createVisit2FromReception(
      String branchId,
      String printerId,
      ArrayList<Service> services,
      HashMap<String, String> parametersMap,
      Boolean printTicket,
      String segmentationRuleId) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (!services.isEmpty()) {
      if (currentBranch.getServices().containsKey(services.get(0).getId())) {
        Service currentService = currentBranch.getServices().get(services.get(0).getId()).clone();
        List<Service> unServedServices = new ArrayList<>();
        services.stream()
            .skip(1)
            .forEach(f -> unServedServices.add(currentBranch.getServices().get(f.getId()).clone()));

        Visit visit =
            Visit.builder()
                .id(UUID.randomUUID().toString())
                .status("WAITING")
                .printTicket(printTicket)
                .branchId(branchId)
                .branchName(currentBranch.getName())
                .branchPrefix(currentBranch.getPrefix())
                .branchPath(currentBranch.getPath())
                .currentService(currentService)
                .unservedServices(unServedServices)
                .createDateTime(ZonedDateTime.now())
                .visitMarks(new ArrayList<>())
                .visitNotes(new ArrayList<>())
                .visitEvents(new ArrayList<>())
                .visitEventInformationList(new ArrayList<>())
                .returnTimeDelay(0L)
                // .updateDateTime(ZonedDateTime.now())
                // .transferDateTime(ZonedDateTime.now())
                // .endDateTime(ZonedDateTime.now())
                .servicePointId(null)
                .servedServices(new ArrayList<>())
                .parameterMap(parametersMap)
                .build();
        Queue serviceQueue;

        Optional<Queue> queue = segmentationRule.getQueue(visit, currentBranch, segmentationRuleId);
        if (queue.isPresent()) {
          serviceQueue = queue.get();

          serviceQueue.setTicketCounter(
              branchService.incrementTicketCounter(branchId, serviceQueue));
          visit.setQueueId(serviceQueue.getId());
          visit.setTicket(
              (serviceQueue.getTicketPrefix()
                  + String.format("%03d", serviceQueue.getTicketCounter())));
          VisitEvent event = VisitEvent.CREATED;
          event
              .getParameters()
              .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
          event.dateTime = ZonedDateTime.now();

          branchService.updateVisit(visit, event, this);
          if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
            VisitEvent queueEvent = VisitEvent.PLACED_IN_QUEUE;
            queueEvent.dateTime = ZonedDateTime.now();
            queueEvent
                .getParameters()
                .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
            queueEvent.getParameters().put("queueId", serviceQueue.getId());
            visit.setQueueId(serviceQueue.getId());

            if (printTicket
                && currentBranch.getReception().getPrinters().stream()
                    .anyMatch(f -> f.getId().equals(printerId))) {
              printerService.print(printerId, visit);
            }

            // changedVisitEventSend("CREATED", null, visit, new HashMap<>());
            branchService.updateVisit(visit, queueEvent, this);
            log.info("Visit {} created!", visit);

            return visit;
          } else {
            throw new BusinessException(
                "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
          }

        } else {
          throw new BusinessException(
              "Services can not be empty!", eventService, HttpStatus.BAD_REQUEST);
        }
      } else {
        throw new BusinessException(
            "Service  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
      }
    }

    throw new BusinessException(
        "Queue  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Создание визита из приемной
   *
   * @param branchId идентификатор отделения
   * @param printerId идентификатор принтера
   * @param services список услуг
   * @param printTicket флаг печати талона
   * @return визит
   */
  public Visit createVisit2FromReception(
      String branchId,
      String printerId,
      ArrayList<Service> services,
      HashMap<String, String> parametersMap,
      Boolean printTicket)
      throws SystemException {
    Branch currentBranch = branchService.getBranch(branchId);

    if (!services.isEmpty()) {
      if (currentBranch.getServices().containsKey(services.get(0).getId())) {
        Service currentService = currentBranch.getServices().get(services.get(0).getId()).clone();
        List<Service> unServedServices = new ArrayList<>();
        services.stream()
            .skip(1)
            .forEach(f -> unServedServices.add(currentBranch.getServices().get(f.getId()).clone()));

        Visit visit =
            Visit.builder()
                .id(UUID.randomUUID().toString())
                .status("WAITING")
                .printTicket(printTicket)
                .branchId(branchId)
                .branchName(currentBranch.getName())
                .branchPrefix(currentBranch.getPrefix())
                .branchPath(currentBranch.getPath())
                .currentService(currentService)
                .unservedServices(unServedServices)
                .createDateTime(ZonedDateTime.now())
                .visitMarks(new ArrayList<>())
                .visitNotes(new ArrayList<>())
                .visitEvents(new ArrayList<>())
                .visitEventInformationList(new ArrayList<>())
                .returnTimeDelay(0L)
                // .updateDateTime(ZonedDateTime.now())
                // .transferDateTime(ZonedDateTime.now())
                // .endDateTime(ZonedDateTime.now())
                .servicePointId(null)
                .servedServices(new ArrayList<>())
                .parameterMap(parametersMap)
                .build();
        Queue serviceQueue;
        if (segmentationRule.getQueue(visit, currentBranch).isPresent()) {
          serviceQueue = segmentationRule.getQueue(visit, currentBranch).get();

          serviceQueue.setTicketCounter(
              branchService.incrementTicketCounter(branchId, serviceQueue));
          visit.setQueueId(serviceQueue.getId());
          visit.setTicket(
              (serviceQueue.getTicketPrefix()
                  + String.format("%03d", serviceQueue.getTicketCounter())));
          VisitEvent event = VisitEvent.CREATED;
          event
              .getParameters()
              .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
          event.dateTime = ZonedDateTime.now();

          branchService.updateVisit(visit, event, this);
          if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
            VisitEvent queueEvent = VisitEvent.PLACED_IN_QUEUE;
            queueEvent.dateTime = ZonedDateTime.now();
            queueEvent
                .getParameters()
                .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
            queueEvent.getParameters().put("queueId", serviceQueue.getId());
            visit.setQueueId(serviceQueue.getId());

            if (printTicket
                && this.getPrinters(branchId).stream().anyMatch(f -> f.getId().equals(printerId))) {
              printerService.print(printerId, visit);
            }

            // changedVisitEventSend("CREATED", null, visit, new HashMap<>());
            branchService.updateVisit(visit, queueEvent, this);
            log.info("Visit {} created!", visit);

            return visit;
          } else {
            throw new BusinessException(
                "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
          }

        } else {
          throw new BusinessException(
              "Services can not be empty!", eventService, HttpStatus.BAD_REQUEST);
        }
      } else {
        throw new BusinessException(
            "Service  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
      }
    }

    throw new BusinessException(
        "Queue  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Создание визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param services список услуг
   * @param parametersMap параметры визита
   * @return визит
   */
  public Visit createVirtualVisit2(
      String branchId,
      String servicePointId,
      ArrayList<Service> services,
      HashMap<String, String> parametersMap)
      throws SystemException {
    Branch currentBranch = branchService.getBranch(branchId);

    if (!services.isEmpty()) {
      if (currentBranch.getServices().containsKey(services.get(0).getId())) {
        Service currentService = currentBranch.getServices().get(services.get(0).getId()).clone();
        List<Service> unServedServices = new ArrayList<>();
        services.stream()
            .skip(1)
            .forEach(f -> unServedServices.add(currentBranch.getServices().get(f.getId())));

        Visit visit =
            Visit.builder()
                .id(UUID.randomUUID().toString())
                .status(VisitEvent.CREATED.getState().name())
                .servicePointId(servicePointId)
                .printTicket(false)
                .branchId(branchId)
                .branchName(currentBranch.getName())
                .branchPrefix(currentBranch.getPrefix())
                .branchPath(currentBranch.getPath())
                .currentService(currentService)
                .unservedServices(unServedServices)
                .createDateTime(ZonedDateTime.now())
                .visitMarks(new ArrayList<>())
                .visitNotes(new ArrayList<>())
                .visitEvents(new ArrayList<>())
                .visitEventInformationList(new ArrayList<>())
                .returnTimeDelay(0L)
                // .updateDateTime(ZonedDateTime.now())
                // .transferDateTime(ZonedDateTime.now())
                // .endDateTime(ZonedDateTime.now())
                .returnTimeDelay(0L)
                .servicePointId(null)
                .servedServices(new ArrayList<>())
                .parameterMap(parametersMap)
                .build();
        Queue serviceQueue;
        if (segmentationRule.getQueue(visit, currentBranch).isPresent()) {
          serviceQueue = segmentationRule.getQueue(visit, currentBranch).get();

          serviceQueue.setTicketCounter(
              branchService.incrementTicketCounter(branchId, serviceQueue));
          visit.setServicePointId(servicePointId);
          visit.setTicket(
              (serviceQueue.getTicketPrefix()
                  + String.format("%03d", serviceQueue.getTicketCounter())));
          VisitEvent event = VisitEvent.CREATED;
          event
              .getParameters()
              .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
          event.dateTime = ZonedDateTime.now();

          branchService.updateVisit(visit, event, this);

          event = VisitEvent.CALLED;
          event
              .getParameters()
              .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
          event.dateTime = ZonedDateTime.now();
          visit.setCallDateTime(ZonedDateTime.now());
          branchService.updateVisit(visit, event, this);
          if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
            VisitEvent queueEvent = VisitEvent.START_SERVING;
            queueEvent.dateTime = ZonedDateTime.now();
            queueEvent
                .getParameters()
                .put("serviceId", !services.isEmpty() ? services.get(0).getId() : null);
            queueEvent.getParameters().put("queueId", serviceQueue.getId());
            // visit.setQueueId(serviceQueue.getId());
            visit.setStartServingDateTime(ZonedDateTime.now());

            // changedVisitEventSend("CREATED", null, visit, new HashMap<>());
            branchService.updateVisit(visit, queueEvent, this);
            log.info("Visit {} created and started serving!", visit);

            return visit;
          } else {
            throw new BusinessException(
                "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
          }

        } else {
          throw new BusinessException(
              "Services can not be empty!", eventService, HttpStatus.BAD_REQUEST);
        }
      } else {
        throw new BusinessException(
            "Service  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
      }
    }

    throw new BusinessException(
        "Queue  not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
  }

  /**
   * Добавление фактической услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор фактической услуги
   * @return визит
   */
  public Visit addDeliveredService(
      String branchId, String servicePointId, String deliveredServiceId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getCurrentService() == null) {
          throw new BusinessException(
              "Current service is null!", eventService, HttpStatus.NOT_FOUND);
        }
        if (!currentBranch.getPossibleDeliveredServices().containsKey(deliveredServiceId)) {
          throw new BusinessException(
              String.format("Delivered service with id %s not found!", deliveredServiceId),
              eventService,
              HttpStatus.NOT_FOUND);
        }
        if (currentBranch.getPossibleDeliveredServices().values().stream()
            .noneMatch(f -> f.getServiceIds().contains(visit.getCurrentService().getId()))) {
          throw new BusinessException(
              String.format(
                  "Current service cant add delivered service with id %s", deliveredServiceId),
              eventService,
              HttpStatus.CONFLICT);
        }
        DeliveredService deliveredService =
            currentBranch.getPossibleDeliveredServices().get(deliveredServiceId).clone();
        visit
            .getCurrentService()
            .getDeliveredServices()
            .put(
                !visit
                        .getCurrentService()
                        .getDeliveredServices()
                        .containsKey(deliveredService.getId())
                    ? deliveredService.getId()
                    : deliveredService.getId()
                        + "_"
                        + (visit.getCurrentService().getDeliveredServices().keySet().stream()
                                .filter(f -> f.contains(deliveredService.getId()))
                                .count()
                            + 1),
                deliveredService);

        VisitEvent visitEvent = VisitEvent.ADDED_DELIVERED_SERVICE;
        visitEvent.getParameters().put("servicePointId", servicePoint.getId());
        visitEvent.getParameters().put("deliveredServiceId", deliveredServiceId);
        visitEvent.getParameters().put("deliveredServiceName", deliveredService.getName());
        visitEvent.getParameters().put("serviceId", visit.getCurrentService().getId());
        visitEvent.getParameters().put("serviceName", visit.getCurrentService().getName());
        visitEvent.getParameters().put("branchId", branchId);
        visitEvent.getParameters().put("staffId", visit.getUserId());
        visitEvent.getParameters().put("staffName", visit.getUserName());
        branchService.updateVisit(visit, visitEvent, this);
        return visit;

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Удаление фактической услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор фактической услуги
   * @return визит
   */
  public Visit deleteDeliveredService(
      String branchId, String servicePointId, String deliveredServiceId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getCurrentService() == null) {
          throw new BusinessException(
              "Current service is null!", eventService, HttpStatus.NOT_FOUND);
        }
        if (!currentBranch.getPossibleDeliveredServices().containsKey(deliveredServiceId)) {
          throw new BusinessException(
              String.format("Delivered service with id %s not found!", deliveredServiceId),
              eventService,
              HttpStatus.NOT_FOUND);
        }
        if (currentBranch.getPossibleDeliveredServices().values().stream()
            .noneMatch(f -> f.getServiceIds().contains(visit.getCurrentService().getId()))) {
          throw new BusinessException(
              String.format(
                  "Current service cant delete delivered service with id %s", deliveredServiceId),
              eventService,
              HttpStatus.CONFLICT);
        }
        DeliveredService deliveredService =
            currentBranch.getPossibleDeliveredServices().get(deliveredServiceId);
        visit.getCurrentService().getDeliveredServices().remove(deliveredService.getId());

        VisitEvent visitEvent = VisitEvent.DELETED_DELIVERED_SERVICE;
        visitEvent.getParameters().put("servicePointId", servicePoint.getId());
        visitEvent.getParameters().put("deliveredServiceId", deliveredServiceId);
        visitEvent.getParameters().put("deliveredServiceName", deliveredService.getName());
        visitEvent.getParameters().put("serviceId", visit.getCurrentService().getId());
        visitEvent.getParameters().put("serviceName", visit.getCurrentService().getName());
        visitEvent.getParameters().put("branchId", branchId);
        visitEvent.getParameters().put("staffId", visit.getUserId());
        visitEvent.getParameters().put("staffName", visit.getUserName());
        branchService.updateVisit(visit, visitEvent, this);
        return visit;

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param serviceId идентификатор услуги
   * @return визит
   */
  public Visit addService(String branchId, String servicePointId, String serviceId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();

        if (currentBranch.getServices().keySet().stream().noneMatch(f -> f.contains(serviceId))) {
          throw new BusinessException(
              String.format("Current visit cant add service with id %s", serviceId),
              eventService,
              HttpStatus.CONFLICT);
        }
        Service service = currentBranch.getServices().get(serviceId);
        visit.getUnservedServices().add(service);

        VisitEvent visitEvent = VisitEvent.ADD_SERVICE;
        visitEvent.getParameters().put("servicePointId", servicePoint.getId());

        visitEvent.getParameters().put("serviceId", service.getId());
        visitEvent.getParameters().put("serviceName", service.getName());
        visitEvent.getParameters().put("branchId", branchId);
        visitEvent.getParameters().put("staffId", visit.getUserId());
        visitEvent.getParameters().put("staffName", visit.getUserName());
        branchService.updateVisit(visit, visitEvent, this);
        return visit;

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление текстовой пометки в визит
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param mark пометка
   * @return визит
   */
  public Visit addMark(String branchId, String servicePointId, Mark mark) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getCurrentService() == null)
          throw new BusinessException(
              "Current service is null!", eventService, HttpStatus.NOT_FOUND);
        mark.setMarkDate(ZonedDateTime.now());
        if (servicePoint.getUser() != null) {
          User user = servicePoint.getUser().toBuilder().build();
          user.setName(servicePoint.getUser().getName());
          user.setId(servicePoint.getUser().getId());
          mark.setAuthor(user);
        }
        visit.getVisitMarks().add(mark);
        VisitEvent visitEvent = VisitEvent.ADDED_MARK;
        visitEvent.getParameters().put("servicePointId", servicePoint.getId());
        visitEvent.getParameters().put("mark", mark.getValue());
        visitEvent.getParameters().put("branchId", branchId);
        visitEvent.getParameters().put("staffId", visit.getUserId());
        visitEvent.getParameters().put("staffName", visit.getUserName());
        branchService.updateVisit(visit, visitEvent, this);
        return visit;

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Удаление текстовой пометки в визите
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param mark пометка
   * @return визит
   */
  public Visit deleteMark(String branchId, String servicePointId, Mark mark) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getCurrentService() == null)
          throw new BusinessException(
              "Current service is null!", eventService, HttpStatus.NOT_FOUND);
        visit.getVisitMarks().removeIf(f -> f.getId().equals(mark.getId()));
        VisitEvent visitEvent = VisitEvent.DELETED_MARK;
        visitEvent.getParameters().put("servicePointId", servicePoint.getId());
        visitEvent.getParameters().put("mark", mark.getValue());
        visitEvent.getParameters().put("branchId", branchId);
        visitEvent.getParameters().put("staffId", visit.getUserId());
        visitEvent.getParameters().put("staffName", visit.getUserName());
        branchService.updateVisit(visit, visitEvent, this);
        return visit;

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Удаление заметки в визите
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param markId идентификатор заметки
   * @return визит
   */
  public Visit deleteMark(String branchId, String servicePointId, String markId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getMarks().containsKey(markId)) {
      return deleteMark(branchId, servicePointId, currentBranch.getMarks().get(markId));
    } else {
      throw new BusinessException(
          String.format("Mark %s not found!", markId), eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Просмотр марков в визите
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор точки обслуживания *
   * @return визит
   */
  public List<Mark> getMarks(String branchId, String visitId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getAllVisits().containsKey(visitId)) {
      Visit visit = currentBranch.getAllVisits().get(visitId);
      return visit.getVisitMarks();
    } else {
      throw new BusinessException(
          String.format("Visit %s not found!", visitId), eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление марков в визите
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param markId идентификатор заметки
   * @return визит
   */
  public Visit addMark(String branchId, String servicePointId, String markId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getMarks().containsKey(markId)) {
      return addMark(branchId, servicePointId, currentBranch.getMarks().get(markId));
    } else {
      throw new BusinessException(
          String.format("Mark %s not found!", markId), eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление итога услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param outcomeId идентификатор итога услуги
   * @return визит
   */
  public Visit addOutcomeService(String branchId, String servicePointId, String outcomeId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getCurrentService() == null)
          throw new BusinessException(
              "Current service is null!", eventService, HttpStatus.NOT_FOUND);
        if (visit.getCurrentService().getPossibleOutcomes().keySet().stream()
            .noneMatch(f -> f.equals(outcomeId)))
          throw new BusinessException(
              String.format("Current service cant add outcome with id %s", outcomeId),
              eventService,
              HttpStatus.CONFLICT);
        else {
          Outcome outcome = visit.getCurrentService().getPossibleOutcomes().get(outcomeId).clone();
          visit.getCurrentService().setOutcome(outcome);

          VisitEvent visitEvent = VisitEvent.ADDED_SERVICE_RESULT;
          visitEvent.getParameters().put("servicePointId", servicePoint.getId());
          visitEvent.getParameters().put("outcomeId", outcomeId);
          visitEvent.getParameters().put("outcomeName", outcome.getName());
          visitEvent.getParameters().put("branchId", branchId);
          visitEvent.getParameters().put("staffId", visit.getUserId());
          visitEvent.getParameters().put("staffName", visit.getUserName());
          branchService.updateVisit(visit, visitEvent, this);
          return visit;
        }

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Добавление итога фактической услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор фактической услуги
   * @param outcomeId идентификатор итога услуги
   * @return визит
   */
  public Visit addOutcomeOfDeliveredService(
      String branchId, String servicePointId, String deliveredServiceId, String outcomeId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getCurrentService() == null) {
          throw new BusinessException(
              "Current service is null!", eventService, HttpStatus.NOT_FOUND);
        }
        if (!visit.getCurrentService().getDeliveredServices().containsKey(deliveredServiceId)) {
          throw new BusinessException(
              String.format(
                  "Delivered service %s of current service ID is not %s",
                  visit.getCurrentService().getId(), deliveredServiceId),
              eventService,
              HttpStatus.NOT_FOUND);
        }
        if (visit
            .getCurrentService()
            .getDeliveredServices()
            .get(deliveredServiceId)
            .getPossibleOutcomes()
            .keySet()
            .stream()
            .noneMatch(f -> f.equals(outcomeId))) {
          throw new BusinessException(
              String.format(
                  "Current service with delivered service %s cant add outcome with id %s",
                  deliveredServiceId, outcomeId),
              eventService,
              HttpStatus.NOT_FOUND);
        } else {
          Outcome outcome =
              visit
                  .getCurrentService()
                  .getDeliveredServices()
                  .get(deliveredServiceId)
                  .getPossibleOutcomes()
                  .get(outcomeId)
                  .clone();
          visit
              .getCurrentService()
              .getDeliveredServices()
              .get(deliveredServiceId)
              .setOutcome(outcome.clone());

          VisitEvent visitEvent = VisitEvent.ADDED_DELIVERED_SERVICE_RESULT;
          visitEvent.getParameters().put("servicePointId", servicePoint.getId());
          visitEvent.getParameters().put("deliveredServiceId", deliveredServiceId);
          visitEvent.getParameters().put("outcomeId", outcomeId);
          visitEvent.getParameters().put("branchId", branchId);
          visitEvent.getParameters().put("staffId", visit.getUserId());
          visitEvent.getParameters().put("staffName", visit.getUserName());
          branchService.updateVisit(visit, visitEvent, this);
          return visit;
        }

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Удаление итога фактической услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param deliveredServiceId идентификатор фактической услуги
   * @return визит
   */
  public Visit deleteOutcomeDeliveredService(
      String branchId, String servicePointId, String deliveredServiceId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getCurrentService() == null) {
          throw new BusinessException(
              "Current service is null!", eventService, HttpStatus.NOT_FOUND);
        }
        if (!visit.getCurrentService().getDeliveredServices().containsKey(deliveredServiceId)) {
          throw new BusinessException(
              String.format(
                  "Delivered service %s of current service ID is not %s",
                  visit.getCurrentService().getId(), deliveredServiceId),
              eventService,
              HttpStatus.NOT_FOUND);
        }

        visit.getCurrentService().getDeliveredServices().get(deliveredServiceId).setOutcome(null);

        VisitEvent visitEvent = VisitEvent.DELETED_DELIVERED_SERVICE_RESULT;
        visitEvent.getParameters().put("servicePointId", servicePoint.getId());
        visitEvent.getParameters().put("deliveredServiceId", deliveredServiceId);
        visitEvent.getParameters().put("outcomeId", "");
        visitEvent.getParameters().put("branchId", branchId);
        visitEvent.getParameters().put("staffId", visit.getUserId());
        visitEvent.getParameters().put("staffName", visit.getUserName());
        branchService.updateVisit(visit, visitEvent, this);
        return visit;

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Удаление итога услуги
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param serviceId идентификатор услуги
   * @return Услуга
   */
  public Visit deleteOutcomeService(String branchId, String servicePointId, String serviceId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (!visit.getCurrentService().getId().equals(serviceId)) {
          throw new BusinessException(
              String.format("Current service ID is not %s@", serviceId),
              eventService,
              HttpStatus.BAD_REQUEST);
        }

        visit.getCurrentService().setOutcome(null);

        VisitEvent visitEvent = VisitEvent.DELETED_SERVICE_RESULT;
        visitEvent.getParameters().put("servicePointId", servicePoint.getId());

        visitEvent.getParameters().put("branchId", branchId);
        visitEvent.getParameters().put("staffId", visit.getUserId());
        visitEvent.getParameters().put("staffName", visit.getUserName());
        branchService.updateVisit(visit, visitEvent, this);
        return visit;

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /** Возвращение вызванного визита в очередь */
  public Visit backCalledVisit(String branchId, String visitId, Long returnTimeDelay) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getAllVisits().containsKey(visitId)) {
      Visit visit = currentBranch.getAllVisits().get(visitId);

      visit.setReturnDateTime(ZonedDateTime.now());
      visit.setReturnTimeDelay(returnTimeDelay);
      visit.setStartServingDateTime(null);
      Optional<VisitEventInformation> event =
          visit.getVisitEventInformationList().stream()
              .max(Comparator.comparing(VisitEventInformation::getEventDateTime));

      if (event.isPresent() && event.get().getParameters().containsKey("servicePointId")) {
        VisitEvent visitEvent =
            visit.getQueueId() != null
                ? VisitEvent.BACK_TO_QUEUE
                : visit.getPoolUserId() != null
                    ? VisitEvent.BACK_TO_USER_POOL
                    : VisitEvent.BACK_TO_SERVICE_POINT_POOL;

        visitEvent
            .getParameters()
            .put("servicePointId", event.get().getParameters().get("servicePointId"));

        visitEvent.getParameters().put("branchId", branchId);
        branchService.updateVisit(visit, visitEvent, this);
      }
      visit = branchService.getBranch(branchId).getAllVisits().get(visit.getId());
      return visit;
    } else {
      throw new BusinessException("Visit not fount!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Возвращение визита в очередь с задержкой
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param returnTimeDelay задержка возвращения в секундах
   * @return визит
   */
  @ExecuteOn(TaskExecutors.SCHEDULED)
  public Visit visitBackToQueue(String branchId, String servicePointId, Long returnTimeDelay) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        visit.setReturnDateTime(ZonedDateTime.now());
        visit.setReturnTimeDelay(returnTimeDelay);
        visit.setStartServingDateTime(null);
        VisitEvent visitEvent = VisitEvent.STOP_SERVING;
        visitEvent.getParameters().put("servicePointId", servicePoint.getId());
        visitEvent.getParameters().put("branchId", branchId);

        branchService.updateVisit(visit, visitEvent, this);
        visit = branchService.getBranch(branchId).getAllVisits().get(visit.getId());
        if (visit.getParameterMap().containsKey("LastQueueId")) {
          Event delayedEvent =
              Event.builder()
                  .eventType("QUEUE_REFRESHED")
                  .body(
                      TinyClass.builder()
                          .id(visit.getParameterMap().get("LastQueueId"))
                          .name(
                              currentBranch
                                  .getQueues()
                                  .get(visit.getParameterMap().get("LastQueueId"))
                                  .getName())
                          .build())
                  .params(
                      Map.of(
                          "queueId",
                          visit.getParameterMap().get("LastQueueId"),
                          "branchId",
                          branchId))
                  .build();
          delayedEvents.delayedEventService(
              "frontend", false, delayedEvent, returnTimeDelay, eventService);
          return visitTransfer(
              branchId, servicePointId, visit.getParameterMap().get("LastQueueId"));
        } else {
          throw new BusinessException("Visit cant be transfer!", eventService, HttpStatus.CONFLICT);
        }
      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Перевод визита в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueId идентификатор очереди
   * @return визит
   */
  public Visit visitTransfer(String branchId, String servicePointId, String queueId) {

    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();

        Queue queue;
        if (currentBranch.getQueues().containsKey(queueId)) {
          queue = currentBranch.getQueues().get(queueId);
        } else {

          throw new BusinessException(
              "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        }

        assert queue != null;
        visit.setQueueId(queue.getId());
        visit.setPoolServicePointId(null);
        visit.setPoolUserId(null);
        visit.setServicePointId(null);
        visit.setStartServingDateTime(null);
        visit.setTransferDateTime(ZonedDateTime.now());
        queue.getVisits().add(visit);
        currentBranch.getQueues().put(queue.getId(), queue);
        VisitEvent event = VisitEvent.BACK_TO_QUEUE;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("queueId", queueId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);

        branchService.updateVisit(visit, event, this);
        // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        log.info("Visit {} transfered!", visit);
        return visit;
      } else {
        throw new BusinessException(
            String.format("Visit in ServicePoint %s! not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Возвращение визита в пул точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор точки обслуживания в чей пул осуществляется возвращение
   * @param returnTimeDelay задержка возвращения в секундах
   * @return визит
   */
  public Visit visitBackToServicePointPool(
      String branchId, String servicePointId, String poolServicePointId, Long returnTimeDelay) {

    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();

        ServicePoint poolServicePoint;
        if (currentBranch.getServicePoints().containsKey(poolServicePointId)) {
          poolServicePoint = currentBranch.getServicePoints().get(poolServicePointId);
        } else {

          throw new BusinessException(
              "Service point not found in branch configuration!",
              eventService,
              HttpStatus.NOT_FOUND);
        }
        VisitEvent event = VisitEvent.STOP_SERVING;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("poolServicePointId", poolServicePointId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);
        branchService.updateVisit(visit, event, this);

        visit.getParameterMap().remove("LastPoolServicePointId");
        assert poolServicePoint != null;

        visit.setServicePointId(null);
        visit.setPoolUserId(null);
        visit.setQueueId(null);
        visit.setPoolServicePointId(poolServicePointId);
        visit.setTransferDateTime(ZonedDateTime.now());
        visit.setReturnDateTime(ZonedDateTime.now());
        visit.setReturnTimeDelay(returnTimeDelay);
        visit.setStartServingDateTime(null);
        event = VisitEvent.BACK_TO_SERVICE_POINT_POOL;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("poolServicePointId", poolServicePointId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);
        branchService.updateVisit(visit, event, this);
        // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        log.info("Visit {} transfered!", visit);
        return visit;
      } else {
        throw new BusinessException(
            String.format("Visit in ServicePoint %s! not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  public Visit visitPutBack(String branchId, String servicePointId, Long returnTimeDelay) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getParameterMap().containsKey("LastPoolServicePointId")) {
          String poolId = visit.getParameterMap().get("LastPoolServicePointId");
          // visit.getParameterMap().remove("LastPoolServicePointId");
          return visitBackToServicePointPool(branchId, servicePointId, poolId, returnTimeDelay);

        } else if (visit.getParameterMap().containsKey("LastPoolUserId")) {
          String ppolId = visit.getParameterMap().get("LastPoolUserId");
          // visit.getParameterMap().remove("LastPoolUserId");
          return visitBackToUserPool(branchId, servicePointId, ppolId, returnTimeDelay);
        } else if (visit.getParameterMap().containsKey("LastQueueId")) {
          // visit.getParameterMap().remove("LastQueueId");
          return visitBackToQueue(branchId, servicePointId, returnTimeDelay);
        } else {
          throw new BusinessException(
              String.format("Visit in ServicePoint %s! cant be put back!", servicePointId),
              eventService,
              HttpStatus.NOT_FOUND);
        }
      } else {
        throw new BusinessException(
            String.format("Visit in ServicePoint %s! not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    }
    throw new BusinessException(
        String.format("Visit in ServicePoint %s! cant be put back!", servicePointId),
        eventService,
        HttpStatus.NOT_FOUND);
  }

  /**
   * Отложить визит в указанной точке обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return отложенный визит
   */
  public Visit visitPostPone(String branchId, String servicePointId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null && servicePoint.getUser() != null) {
        return visitBackToUserPool(branchId, servicePointId, servicePoint.getUser().getId(), 0L);
      } else {
        if (servicePoint.getVisit() == null) {
          throw new BusinessException(
              "Visit in ServicePoint " + servicePointId + " not exist!",
              eventService,
              HttpStatus.NOT_FOUND);
        } else {
          throw new BusinessException("User not exist!", eventService, HttpStatus.NOT_FOUND);
        }
      }

    } else {
      throw new BusinessException(
          "Service point " + servicePointId + " not exist!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Возвращение визита в пул сотрудника
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param userId идентификатор cотрудника
   * @param returnTimeDelay задержка возвращения в секундах
   * @return визит
   */
  public Visit visitBackToUserPool(
      String branchId, String servicePointId, String userId, Long returnTimeDelay) {

    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();

        User user;
        if (this.getAllWorkingUsers(branchId).containsKey(userId)) {
          user = this.getAllWorkingUsers(branchId).get(userId);
        } else {

          throw new BusinessException(
              "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        }

        User staff;
        if (currentBranch.getServicePoints().containsKey(servicePointId)
            && currentBranch.getServicePoints().get(servicePointId).getUser() != null) {
          staff = currentBranch.getServicePoints().get(servicePointId).getUser();
        } else {
          throw new BusinessException(
              "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        }
        VisitEvent event = VisitEvent.STOP_SERVING;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("staffId", staff.getId());
        event.getParameters().put("staffName", staff.getName());
        event.getParameters().put("servicePointId", servicePointId);
        branchService.updateVisit(visit, event, this);
        visit.setServicePointId(null);
        visit.setQueueId(null);
        assert user != null;
        visit.setServicePointId(null);
        visit.setPoolServicePointId(null);
        visit.setPoolUserId(user.getId());

        visit.setStartServingDateTime(null);
        visit.setTransferDateTime(ZonedDateTime.now());
        visit.setReturnDateTime(ZonedDateTime.now());
        visit.setReturnTimeDelay(returnTimeDelay);
        visit.getParameterMap().remove("LastPoolUserId");
        event = VisitEvent.BACK_TO_USER_POOL;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("userId", userId);
        event.getParameters().put("staffId", staff.getId());
        event.getParameters().put("staffName", staff.getName());

        event.getParameters().put("poolUserId", user.getId());
        event.getParameters().put("poolUserName", user.getName());
        event.getParameters().put("servicePointId", servicePointId);
        branchService.updateVisit(visit, event, this);
        // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        log.info("Visit {} transfered!", visit);
        return visit;
      } else {
        throw new BusinessException(
            String.format("Visit in ServicePoint %s! not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Перевод визита из очереди в очередь в определенную позицию
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueId идентификатор очереди
   * @param visit визит
   * @param index позиция визита в списке
   * @return визит
   */
  public Visit visitTransfer(
      String branchId, String servicePointId, String queueId, Visit visit, Integer index) {
    Branch currentBranch = branchService.getBranch(branchId);
    String oldQueueID = visit.getQueueId();

    Queue queue;
    if (currentBranch.getQueues().containsKey(queueId)) {
      queue = currentBranch.getQueues().get(queueId);
    } else {

      throw new BusinessException(
          "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    User user;
    if (currentBranch.getServicePoints().containsKey(servicePointId)
        && currentBranch.getServicePoints().get(servicePointId).getUser() != null) {
      user = currentBranch.getServicePoints().get(servicePointId).getUser();
    } else {
      throw new BusinessException(
          "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    visit.setServicePointId(null);
    visit.setPoolUserId(null);
    visit.setPoolServicePointId(null);
    visit.setStartServingDateTime(null);
    assert queue != null;
    visit.setQueueId(queue.getId());

    VisitEvent event = VisitEvent.TRANSFER_TO_QUEUE;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);
    event.getParameters().put("newQueueID", queueId);
    event.getParameters().put("servicePointId", servicePointId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", user.getId());
    event.getParameters().put("staffName", user.getName());
    branchService.updateVisit(visit, event, this, index);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transfered!", visit);
    return visit;
  }

  /**
   * Перевод визита в очередь
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueId идентификатор очереди
   * @param visit визит
   * @param isToStart флаг вставки визита в начало или в конец (по умолчанию в начало)
   * @return визит
   */
  public Visit visitTransfer(
      String branchId, String servicePointId, String queueId, Visit visit, Boolean isToStart) {
    Branch currentBranch = branchService.getBranch(branchId);
    String oldQueueID = visit.getQueueId();
    User user;
    if (currentBranch.getServicePoints().containsKey(servicePointId)
        && currentBranch.getServicePoints().get(servicePointId).getUser() != null) {
      user = currentBranch.getServicePoints().get(servicePointId).getUser();
    } else {
      throw new BusinessException(
          "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    Queue queue;
    if (currentBranch.getQueues().containsKey(queueId)) {
      queue = currentBranch.getQueues().get(queueId);
    } else {

      throw new BusinessException(
          "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    visit.setTransferDateTime(ZonedDateTime.now());
    visit.setServicePointId(null);
    visit.setPoolUserId(null);
    visit.setPoolServicePointId(null);
    visit.setStartServingDateTime(null);
    if (visit.getServicePointId() != null) {
      visit.setReturnDateTime(ZonedDateTime.now());
    }

    if (isToStart) {
      DateTimeFormatter format =
          DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
      visit.getParameterMap().put("isTransferredToStart", ZonedDateTime.now().format(format));
    }

    assert queue != null;
    visit.setQueueId(queue.getId());

    VisitEvent event = VisitEvent.TRANSFER_TO_QUEUE;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);
    event.getParameters().put("newQueueID", queueId);
    event.getParameters().put("servicePointId", servicePointId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", user.getId());
    event.getParameters().put("staffName", user.getName());
    branchService.updateVisit(visit, event, this, isToStart);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transfered!", visit);
    return visit;
  }

  /**
   * Перевод визита из очереди в очередь внешней службой
   *
   * @param branchId идентификатор отделения
   * @param queueId идентификатор очереди
   * @param visit визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return визит
   */
  public Visit visitTransfer(
      String branchId,
      String queueId,
      Visit visit,
      Boolean isAppend,
      HashMap<String, String> serviceInfo) {
    Branch currentBranch = branchService.getBranch(branchId);
    String oldQueueID = visit.getQueueId();

    Queue queue;
    if (currentBranch.getQueues().containsKey(queueId)) {
      queue = currentBranch.getQueues().get(queueId);
    } else {

      throw new BusinessException(
          "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }

    if (visit.getServicePointId() != null) {
      visit.setReturnDateTime(ZonedDateTime.now());
    }

    if (!isAppend) {
      visit.getParameterMap().put("isTransferredToStart", "true");
    }

    visit.setPoolServicePointId(null);
    visit.setServicePointId(null);
    visit.setPoolUserId(null);
    visit.setStartServingDateTime(null);
    assert queue != null;
    visit.setQueueId(queue.getId());
    VisitEvent event = VisitEvent.TRANSFER_TO_QUEUE;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);
    event.getParameters().put("newQueueID", queueId);

    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", visit.getUserId());
    event.getParameters().put("staffName", visit.getUserName());
    event.getParameters().putAll(serviceInfo);
    branchService.updateVisit(visit, event, this, !isAppend);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transferred!", visit);
    return visit;
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания в определенную позицию в пуле
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор очереди
   * @param visit визит
   * @param index позиция визита в списке
   * @return визит
   */
  public Visit visitTransferFromQueueToServicePointPool(
      String branchId,
      String servicePointId,
      String poolServicePointId,
      Visit visit,
      Integer index) {
    Branch currentBranch = branchService.getBranch(branchId);
    String oldQueueID = visit.getQueueId();
    User user;
    if (currentBranch.getServicePoints().containsKey(servicePointId)
        && currentBranch.getServicePoints().get(servicePointId).getUser() != null) {
      user = currentBranch.getServicePoints().get(servicePointId).getUser();
    } else {
      throw new BusinessException(
          "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    ServicePoint poolServicePoint;
    if (currentBranch.getServicePoints().containsKey(poolServicePointId)) {
      poolServicePoint = currentBranch.getServicePoints().get(poolServicePointId);
    } else {

      throw new BusinessException(
          "Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }

    visit.setQueueId(null);
    assert poolServicePoint != null;
    visit.setServicePointId(null);
    visit.setPoolUserId(null);
    visit.setStartServingDateTime(null);
    visit.setPoolServicePointId(poolServicePoint.getId());
    VisitEvent event = VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);
    event.getParameters().put("poolServicePointId", poolServicePointId);
    event.getParameters().put("servicePointId", servicePointId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", user.getId());
    event.getParameters().put("staffName", user.getName());
    branchService.updateVisit(visit, event, this, index);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transfered!", visit);
    return visit;
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param poolServicePointId идентификатор очереди
   * @param visit визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return визит
   */
  public Visit visitTransferFromQueueToServicePointPool(
      String branchId,
      String servicePointId,
      String poolServicePointId,
      Visit visit,
      Boolean isAppend) {
    Branch currentBranch = branchService.getBranch(branchId);
    String oldQueueID = visit.getQueueId();
    //    if (visit.getQueueId().isBlank()) {
    //      throw new BusinessException("Visit not in a queue!", eventService);
    //    }

    ServicePoint poolServicePoint;
    if (currentBranch.getServicePoints().containsKey(poolServicePointId)) {
      poolServicePoint = currentBranch.getServicePoints().get(poolServicePointId);
    } else {

      throw new BusinessException(
          "Service Point not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }

    visit.setServicePointId(null);
    visit.setPoolUserId(null);
    visit.setQueueId(null);
    visit.setStartServingDateTime(null);
    assert poolServicePoint != null;

    visit.setPoolServicePointId(poolServicePoint.getId());
    VisitEvent event = VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);
    event.getParameters().put("poolServicePointId", poolServicePointId);
    event.getParameters().put("servicePointId", servicePointId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", visit.getUserId());
    event.getParameters().put("staffName", visit.getUserName());
    branchService.updateVisit(visit, event, this, !isAppend);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transfered!", visit);
    return visit;
  }

  /**
   * Перевод визита из очереди в пул точки обслуживания внешней службой (MI, Ресепшен и т д)
   *
   * @param branchId идентификатор отделения
   * @param poolServicePointId идентификатор очереди
   * @param visit визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param serviceInfo данные о внешней службе
   * @return визит
   */
  public Visit visitTransferFromQueueToServicePointPool(
      String branchId,
      String poolServicePointId,
      Visit visit,
      Boolean isAppend,
      HashMap<String, String> serviceInfo) {
    Branch currentBranch = branchService.getBranch(branchId);
    String oldQueueID = visit.getQueueId();

    ServicePoint poolServicePoint;
    if (currentBranch.getServicePoints().containsKey(poolServicePointId)) {
      poolServicePoint = currentBranch.getServicePoints().get(poolServicePointId);
    } else {

      throw new BusinessException(
          "Service Point not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }

    visit.setQueueId(null);
    visit.setPoolUserId(null);
    visit.setServicePointId(null);
    assert poolServicePoint != null;
    visit.setStartServingDateTime(null);
    visit.setPoolServicePointId(poolServicePoint.getId());

    VisitEvent event = VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);
    event.getParameters().put("poolServicePointId", poolServicePointId);
    event.getParameters().putAll(serviceInfo);
    event.getParameters().put("branchID", branchId);
    branchService.updateVisit(visit, event, this, !isAppend);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transfered!", visit);
    return visit;
  }

  /**
   * Получение списка всех сотрудников работающих в отделении
   *
   * @param branchId идентификатор отделения
   */
  public HashMap<String, User> getAllWorkingUsers(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);
    HashMap<String, User> result = new HashMap<>();
    currentBranch
        .getServicePoints()
        .forEach(
            (k, v) -> {
              if (v.getUser() != null) {
                result.put(v.getUser().getId(), v.getUser());
              }
            });
    return result;
  }

  /**
   * Перевод визита из очереди в пул сотрудника
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор cотрудника
   * @param visit визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @return визит
   */
  public Visit visitTransferFromQueueToUserPool(
      String branchId, String userId, Visit visit, Boolean isAppend) {

    String oldQueueID = visit.getQueueId();

    Branch currentBranch = branchService.getBranch(branchId);
    if (!currentBranch.getUsers().values().stream()
        .map(BranchEntity::getId)
        .toList()
        .contains(userId)) {

      throw new BusinessException(
          "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }

    visit.setQueueId(null);
    visit.setServicePointId(null);
    visit.setPoolServicePointId(null);
    visit.setPoolUserId(userId);
    VisitEvent event = VisitEvent.TRANSFER_TO_USER_POOL;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);

    event.getParameters().put("userId", userId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", visit.getUserId());
    event.getParameters().put("staffName", visit.getUserName());
    branchService.updateVisit(visit, event, this, !isAppend);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transfered!", visit);
    return visit;
  }

  /**
   * Перевод визита из очереди в пул сотрудника из внешней службы (MI, Ресепшен)
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор cотрудника
   * @param visit визит
   * @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец)
   * @param serviceInfo данные о внешней службе
   * @return визит
   */
  public Visit visitTransferFromQueueToUserPool(
      String branchId,
      String userId,
      Visit visit,
      Boolean isAppend,
      HashMap<String, String> serviceInfo) {

    String oldQueueID = visit.getQueueId();

    Branch currentBranch = branchService.getBranch(branchId);
    if (!currentBranch.getUsers().values().stream()
        .map(BranchEntity::getId)
        .toList()
        .contains(userId)) {

      throw new BusinessException(
          "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }

    visit.setQueueId(null);
    visit.setServicePointId(null);
    visit.setPoolServicePointId(null);
    visit.setPoolUserId(userId);

    VisitEvent event = VisitEvent.TRANSFER_TO_USER_POOL;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);

    event.getParameters().put("userId", userId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().putAll(serviceInfo);

    branchService.updateVisit(visit, event, this, !isAppend);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transfered!", visit);
    return visit;
  }

  /**
   * Перевод визита из очереди в пул сотрудника в определенную позицию
   *
   * @param branchId идентификатор отделения
   * @param userId идентификатор cотрудника
   * @param visit визит
   * @param index позиция визита в списке
   * @return визит
   */
  public Visit visitTransferFromQueueToUserPool(
      String branchId, String userId, Visit visit, Integer index) {

    String oldQueueID = visit.getQueueId();

    if (!this.getAllWorkingUsers(branchId).containsKey(userId)) {

      throw new BusinessException(
          "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }

    visit.setQueueId(null);
    visit.setServicePointId(null);
    visit.setPoolServicePointId(null);
    visit.setPoolUserId(userId);

    VisitEvent event = VisitEvent.TRANSFER_TO_USER_POOL;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("oldQueueID", oldQueueID);

    event.getParameters().put("userId", userId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", visit.getUserId());
    event.getParameters().put("staffName", visit.getUserName());
    branchService.updateVisit(visit, event, this, index);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    log.info("Visit {} transfered!", visit);
    return visit;
  }

  /**
   * Завершение визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return визит
   */
  public Visit visitEnd(String branchId, String servicePointId) {
    Branch currentBranch = branchService.getBranch(branchId);
    Visit visit;

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        visit = servicePoint.getVisit();

        visit.setServicePointId(servicePoint.getId());
        visit.setTransferDateTime(ZonedDateTime.now());

        VisitEvent stopServingEvent;
        VisitEvent backToQueueEvent;
        VisitEvent endEvent;

        if (visit.getUnservedServices() != null && !visit.getUnservedServices().isEmpty()) {

          visit.getServedServices().add(visit.toBuilder().build().getCurrentService());
          visit.setCurrentService(visit.toBuilder().build().getUnservedServices().get(0));
          visit.getUnservedServices().remove(0);
          String queueIdToReturn = visit.getCurrentService().getLinkedQueueId();
          visit.setQueueId(queueIdToReturn);

          visit.setServedDateTime(ZonedDateTime.now());
          stopServingEvent = VisitEvent.STOP_SERVING;
          stopServingEvent.dateTime = ZonedDateTime.now();
          stopServingEvent.getParameters().put("servicePointId", servicePointId);
          stopServingEvent.getParameters().put("branchID", branchId);
          stopServingEvent.getParameters().put("staffId", visit.getUserId());
          stopServingEvent.getParameters().put("staffName", visit.getUserName());
          branchService.updateVisit(visit, stopServingEvent, this,true);

          visit.setReturnDateTime(ZonedDateTime.now());
          visit.setCallDateTime(null);

          visit.setStartServingDateTime(null);

          backToQueueEvent = VisitEvent.BACK_TO_QUEUE;
          backToQueueEvent.getParameters().put("branchID", branchId);
          backToQueueEvent.getParameters().put("queueId", queueIdToReturn);
          backToQueueEvent.getParameters().put("servicePointId", servicePointId);
          backToQueueEvent.getParameters().put("staffId", visit.getUserId());
          backToQueueEvent.getParameters().put("staffName", visit.getUserName());

          visit.setServicePointId(null);
          branchService.updateVisit(visit, backToQueueEvent, this,true);

        } else {
          visit.getServedServices().add(visit.getCurrentService());
          visit.setCurrentService(null);
          visit.setServedDateTime(ZonedDateTime.now());
          visit.setQueueId(null);
          visit.setServedDateTime(ZonedDateTime.now());
          stopServingEvent = VisitEvent.STOP_SERVING;
          stopServingEvent.dateTime = ZonedDateTime.now();
          stopServingEvent.getParameters().put("branchID", branchId);
          stopServingEvent.getParameters().put("staffId", visit.getUserId());
          stopServingEvent.getParameters().put("staffName", visit.getUserName());
          stopServingEvent.getParameters().put("servicePointId", servicePointId);
          branchService.updateVisit(visit, stopServingEvent, this,true);
          endEvent = VisitEvent.END;
          endEvent.dateTime = ZonedDateTime.now();

          visit.setServicePointId(null);
          branchService.updateVisit(visit, endEvent, this,true);
        }

        log.info("Visit {} ended", visit);
        return visit;

      } else {
        throw new BusinessException(
            "Visit not found in ServicePoint ", eventService, HttpStatus.NOT_FOUND);
      }

    } else {

      throw new BusinessException(
          "ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Вызов визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return визит
   */
  public Optional<Visit> visitCall(String branchId, String servicePointId, Visit visit) {
    Branch currentBranch = branchService.getBranch(branchId);

    Optional<Queue> queue;

    visit.setStatus("CALLED");
    visit.setCallDateTime(ZonedDateTime.now());
    visit.getParameterMap().remove("isTransferredToStart");
    visit.setTransferDateTime(null);
    visit.setReturnDateTime(null);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        throw new BusinessException(
            "Visit alredey called in the ServicePoint! ", eventService, HttpStatus.CONFLICT);
      }
      visit.setServicePointId(servicePointId);
      visit.setUserName(servicePoint.getUser() != null ? servicePoint.getUser().getName() : null);
      visit.setUserId(servicePoint.getUser() != null ? servicePoint.getUser().getId() : null);
      servicePoint.setVisit(visit);

    } else {
      if (!servicePointId.isEmpty()
          && !currentBranch.getServicePoints().containsKey(servicePointId)) {
        throw new BusinessException(
            "ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
      }
    }
    VisitEvent event = VisitEvent.CALLED;
    if (visit.getQueueId() != null) {
      queue =
          currentBranch.getQueues().values().stream()
              .filter(f -> f.getId().equals(visit.getQueueId()))
              .findFirst();
      if ((queue.isPresent())) {
        List<Visit> visits = queue.get().getVisits();
        visits.removeIf(f -> f.getId().equals(visit.getId()));
        queue.get().setVisits(visits);
        currentBranch.getQueues().put(queue.get().getId(), queue.get());
      } else {
        throw new BusinessException(
            "Queue not found in branch configuration or not available for current workProfile!",
            eventService,
            HttpStatus.NOT_FOUND);
      }
      event.getParameters().put("queueId", queue.map(BranchEntity::getId).orElse(null));
      visit.getParameterMap().put("LastQueueId", visit.getQueueId());
      visit.setQueueId(null);
    }
    if (visit.getPoolServicePointId() != null) {
      visit.getParameterMap().put("LastPoolServicePointId", visit.getPoolServicePointId());
      visit.setPoolServicePointId(null);
    }
    if (visit.getPoolUserId() != null) {
      visit.getParameterMap().put("LastPoolUserId", visit.getPoolUserId());
      visit.setPoolUserId(null);
    }

    event.getParameters().put("servicePointId", servicePointId);

    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", visit.getUserId());
    event.getParameters().put("staffName", visit.getUserName());
    event.dateTime = ZonedDateTime.now();
    branchService.updateVisit(visit, event, this);

    VisitEvent servingEvent = VisitEvent.START_SERVING;
    servingEvent.dateTime = ZonedDateTime.now();
    visit.setStartServingDateTime(ZonedDateTime.now());

    branchService.updateVisit(visit, servingEvent, this);

    log.info("Visit {} called!", visit);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    return Optional.of(visit);
  }

  /**
   * Вызов визита по идентификатору
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visitId идентификатор визита
   * @return визит
   */
  public Optional<Visit> visitCall(String branchId, String servicePointId, String visitId) {
    if (this.getAllVisits(branchId).containsKey(visitId)) {
      Visit visit = this.getAllVisits(branchId).get(visitId);
      return this.visitCall(branchId, servicePointId, visit);
    }
    return Optional.empty();
  }

  /**
   * Вызов визита с ожиданием подтверждения прихода клиента
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return визит
   */
  public Optional<Visit> visitCallForConfirmWithMaxWaitingTime(
      String branchId, String servicePointId, Visit visit) {

    String userId = "";
    String userName = "";
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      userId =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getId()
              : "";
      userName =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getName()
              : "";
    }

    // visit.setStatus("CALLED");
    visit.setCallDateTime(ZonedDateTime.now());
    visit.getParameterMap().put("LastQueueId", visit.getQueueId());
    visit.getParameterMap().remove("isTransferredToStart");

    VisitEvent event = VisitEvent.CALLED;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("servicePointId", servicePointId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("queueId", visit.getQueueId() != null ? visit.getQueueId() : "");
    event
        .getParameters()
        .put(
            "PoolServicePointId",
            visit.getPoolServicePointId() != null ? visit.getPoolServicePointId() : "");
    event.getParameters().put("staffId", userId);
    event.getParameters().put("staffName", userName);
    branchService.updateVisit(visit, event, this);

    log.info("Visit {} called!", visit);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    return Optional.of(visit);
  }

  /**
   * Повторный вызов визита с ожиданием подтверждения прихода клиента
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return визит
   */
  public Visit visitReCallForConfirm(String branchId, String servicePointId, Visit visit) {

    String userId = "";
    String userName = "";
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      userId =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getId()
              : "";
      userName =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getName()
              : "";
    }

    visit.setCallDateTime(ZonedDateTime.now());
    visit.getParameterMap().remove("isTransferredToStart");
    VisitEvent event = VisitEvent.RECALLED;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("ServicePointId", servicePointId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("queueId", visit.getQueueId());
    event.getParameters().put("staffId", userId);
    event.getParameters().put("staffName", userName);
    branchService.updateVisit(visit, event, this);

    log.info("Visit {} called!", visit);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    return visit;
  }

  /**
   * Подтверждение прихода клиента
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return визит
   */
  public Visit visitConfirm(String branchId, String servicePointId, Visit visit) {
    Branch currentBranch = branchService.getBranch(branchId);
    String userId = "";
    String userName = "";

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      userId =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getId()
              : "";
      userName =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getName()
              : "";
    }

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        throw new BusinessException(
            "Visit alredey called in the ServicePoint! ", eventService, HttpStatus.CONFLICT);
      }
      visit.setServicePointId(servicePointId);

      visit.setUserName(servicePoint.getUser() != null ? servicePoint.getUser().getName() : "");
      visit.setUserId(servicePoint.getUser() != null ? servicePoint.getUser().getId() : "");
      visit.setTransferDateTime(null);
      visit.setReturnDateTime(null);
    } else {
      if (!servicePointId.isEmpty()
          && !currentBranch.getServicePoints().containsKey(servicePointId)) {
        throw new BusinessException(
            "ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
      }
    }

    visit.setStatus("START_SERVING");
    visit.setStartServingDateTime(ZonedDateTime.now());
    visit.getParameterMap().put("LastQueueId", visit.getQueueId());
    if (visit.getQueueId() != null) {
      visit.getParameterMap().put("LastQueueId", visit.getQueueId());
      visit.setQueueId(null);
    }
    if (visit.getPoolServicePointId() != null) {
      visit.getParameterMap().put("LastPoolServicePointId", visit.getPoolServicePointId());
      visit.setPoolServicePointId(null);
    }
    if (visit.getPoolUserId() != null) {
      visit.getParameterMap().put("LastPoolUserId", visit.getPoolUserId());
      visit.setPoolUserId(null);
    }
    if (visit.getUserId() != null) {
      visit.getParameterMap().put("LastUserId", visit.getPoolUserId());
    }
    visit.setUserId(userId);
    visit.setUserName(userName);
    VisitEvent event = VisitEvent.START_SERVING;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("ServicePointId", servicePointId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("serviceId", visit.getCurrentService().getId());
    event.getParameters().put("staffId", userId);
    event.getParameters().put("staffName", userName);
    branchService.updateVisit(visit, event, this);

    log.info("Visit {} statted serving!", visit);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    return visit;
  }

  /**
   * Завершение не пришедшего визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param visit визит
   * @return визит
   */
  public Optional<Visit> visitNoShow(String branchId, String servicePointId, Visit visit) {

    Branch currentBranch = branchService.getBranch(branchId);
    String userId = "";
    String userName = "";

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      userId =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getId()
              : "";
      userName =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getName()
              : "";
    }

    visit.setStatus("NO_SHOW");
    visit.setStartServingDateTime(null);
    visit.setQueueId(null);
    visit.setServicePointId(null);

    VisitEvent event = VisitEvent.NO_SHOW;
    event.dateTime = ZonedDateTime.now();
    event.getParameters().put("ServicePointId", servicePointId);
    event.getParameters().put("branchID", branchId);
    event.getParameters().put("staffId", userId);
    event.getParameters().put("staffName", userName);
    branchService.updateVisit(visit, event, this);

    log.info("Visit {} statted serving!", visit);
    // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
    return Optional.of(visit);
  }

  /**
   * Вызов визита с подтверждением прихода c максимальным временем ожидания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return визит
   */
  public Optional<Visit> visitCallForConfirmWithMaxWaitingTime(
      String branchId, String servicePointId) {
    Branch currentBranch = branchService.getBranch(branchId);
    String userId = "";
    String userName = "";

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      userId =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getId()
              : "";
      userName =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getName()
              : "";
    }

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);

      Optional<Visit> visit = waitingTimeCallRule.call(currentBranch, servicePoint);
      VisitEvent event = VisitEvent.CALLED;
      event.dateTime = ZonedDateTime.now();
      event.getParameters().put("ServicePointId", servicePointId);
      event.getParameters().put("branchID", branchId);
      event.getParameters().put("staffId", userId);
      event.getParameters().put("staffName", userName);
      if (visit.isPresent()) {
        branchService.updateVisit(visit.get(), event, this);
        return visit;
      }

    } else {
      throw new BusinessException(
          "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      servicePoint.setAutoCallMode(true);
      currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
      branchService.add(currentBranch.getId(), currentBranch);
      throw new BusinessException("Autocall mode enabled!", eventService, HttpStatus.valueOf(207));
    }
    return Optional.empty();
  }

  /**
   * Вызов визита с подтверждением прихода с максимальным временем ожидания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueIds идентификаторы очередей
   * @return визит
   */
  public Optional<Visit> visitCallForConfirmWithMaxWaitingTime(
      String branchId, String servicePointId, List<String> queueIds) {
    Branch currentBranch = branchService.getBranch(branchId);
    String userId = "";
    String userName = "";

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      userId =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getId()
              : "";
      userName =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getName()
              : "";
    }

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);

      Optional<Visit> visit = waitingTimeCallRule.call(currentBranch, servicePoint, queueIds);
      VisitEvent event = VisitEvent.CALLED;
      event.dateTime = ZonedDateTime.now();
      event.getParameters().put("ServicePointId", servicePointId);
      event.getParameters().put("branchID", branchId);
      event.getParameters().put("staffId", userId);
      event.getParameters().put("staffName", userName);
      if (visit.isPresent()) {
        branchService.updateVisit(visit.get(), event, this);
        return visit;
      }

    } else {
      throw new BusinessException(
          "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      servicePoint.setAutoCallMode(true);
      currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
      branchService.add(currentBranch.getId(), currentBranch);
      throw new BusinessException("Autocall mode enabled!", eventService, HttpStatus.valueOf(207));
    }
    return Optional.empty();
  }

  /**
   * Вызов визита с подтверждением прихода c максимальным временем создания визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return визит
   */
  public Optional<Visit> visitCallForConfirmWithMaxLifeTime(
      String branchId, String servicePointId) {
    Branch currentBranch = branchService.getBranch(branchId);
    String userId = "";
    String userName = "";

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      userId =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getId()
              : "";
      userName =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getName()
              : "";
    }

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);

      Optional<Visit> visit = lifeTimeCallRule.call(currentBranch, servicePoint);
      VisitEvent event = VisitEvent.CALLED;
      event.dateTime = ZonedDateTime.now();
      event.getParameters().put("ServicePointId", servicePointId);
      event.getParameters().put("branchID", branchId);
      event.getParameters().put("staffId", userId);
      event.getParameters().put("staffName", userName);
      if (visit.isPresent()) {
        branchService.updateVisit(visit.get(), event, this);
        return visit;
      }

    } else {
      throw new BusinessException(
          "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      servicePoint.setAutoCallMode(true);
      currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
      branchService.add(currentBranch.getId(), currentBranch);
      throw new BusinessException("Autocall mode enabled!", eventService, HttpStatus.valueOf(207));
    }
    return Optional.empty();
  }

  /**
   * Включение-выключение режима авовызова для отделения
   *
   * @param branchId идентификатор отделения *
   * @param isAutoCallMode режим автовызова
   * @return отделение
   */
  public Optional<Branch> setAutoCallModeOfBranch(String branchId, Boolean isAutoCallMode) {
    Branch currentBranch = branchService.getBranch(branchId);
    currentBranch.getParameterMap().put("autoCallMode", isAutoCallMode.toString());
    currentBranch
        .getServicePoints()
        .forEach(
            (key, value) -> {
              if (!isAutoCallMode) {
                value.setAutoCallMode(false);
              }
            });
    branchService.add(currentBranch.getId(), currentBranch);
    return Optional.of(currentBranch);
  }

  /**
   * Включение-выключение режима авовызова для точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param isAutoCallMode режим автовызова
   * @return точка обслуживания
   */
  public Optional<ServicePoint> setAutoCallModeOfServicePoint(
      String branchId, String servicePointId, Boolean isAutoCallMode) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (!currentBranch.getServicePoints().containsKey(servicePointId)) {
      throw new BusinessException(
          String.format("Service poiunt %s not found!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      servicePoint.setAutoCallMode(isAutoCallMode);
      currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
      branchService.add(currentBranch.getId(), currentBranch);
      HashMap<String, String> parameterMap = new HashMap<>();
      parameterMap.put("branchId", currentBranch.getId());
      parameterMap.put("servicePointId", servicePoint.getId());
      Event autocallEvent =
          Event.builder()
              .eventType("SERVUCEPOINT_AUTOCALL_MODE_TURN_ON")
              .eventDate(ZonedDateTime.now())
              .params(parameterMap)
              .body(servicePoint)
              .build();
      eventService.send("frontend", false, autocallEvent);
      return Optional.of(servicePoint);
    } else if (isAutoCallMode) {
      throw new BusinessException(
          String.format(
              "Service point %s cannot be turn on because auto call mode turned off in current branch!",
              servicePointId),
          eventService,
          HttpStatus.CONFLICT);
    } else {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      return Optional.of(servicePoint);
    }
  }

  /**
   * Включение-выключение режима необходимости подтверждения прихода для точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param isConfirmRequiredMode режим необходимости подтверждения
   * @return точка обслуживания
   */
  public Optional<ServicePoint> setConfirmRequiredModeOfServicePoint(
      String branchId, String servicePointId, Boolean isConfirmRequiredMode) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (!currentBranch.getServicePoints().containsKey(servicePointId)) {
      throw new BusinessException(
          String.format("Service poiunt %s not found!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }

    ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
    servicePoint.setIsConfirmRequired(isConfirmRequiredMode);
    currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
    branchService.add(currentBranch.getId(), currentBranch);
    HashMap<String, String> parameterMap = new HashMap<>();
    parameterMap.put("branchId", currentBranch.getId());
    parameterMap.put("servicePointId", servicePoint.getId());
    Event autocallEvent =
        Event.builder()
            .eventType(
                "SERVUCEPOINT_CONFIRM_REQUIRED_MODE_TURN_" + (isConfirmRequiredMode ? "ON" : "OFF"))
            .eventDate(ZonedDateTime.now())
            .params(parameterMap)
            .body(servicePoint)
            .build();
    eventService.send("frontend", false, autocallEvent);
    return Optional.of(servicePoint);
  }

  /**
   * Отмена режима автовызова для точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return точка обслуживания
   */
  public Optional<ServicePoint> cancelAutoCallModeOfServicePoint(
      String branchId, String servicePointId) {
    return setAutoCallModeOfServicePoint(branchId, servicePointId, false);
  }

  /**
   * Включение режима автовызова для точки обслуживания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return точка обслуживания
   */
  public Optional<ServicePoint> startAutoCallModeOfServicePoint(
      String branchId, String servicePointId) {

    return setAutoCallModeOfServicePoint(branchId, servicePointId, true);
  }

  /**
   * Вызов визита с подтверждением прихода c максимальным временем создания визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueIds идентификаторы очередей
   * @return визит
   */
  public Optional<Visit> visitCallForConfirmWithMaxLifeTime(
      String branchId, String servicePointId, List<String> queueIds) {
    Branch currentBranch = branchService.getBranch(branchId);
    String userId = "";
    String userName = "";

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      userId =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getId()
              : "";
      userName =
          currentBranch.getServicePoints().get(servicePointId).getUser() != null
              ? currentBranch.getServicePoints().get(servicePointId).getUser().getName()
              : "";
    }

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);

      Optional<Visit> visit = lifeTimeCallRule.call(currentBranch, servicePoint, queueIds);
      VisitEvent event = VisitEvent.CALLED;
      event.dateTime = ZonedDateTime.now();
      event.getParameters().put("ServicePointId", servicePointId);
      event.getParameters().put("branchID", branchId);
      event.getParameters().put("staffId", userId);
      event.getParameters().put("staffName", userName);
      if (visit.isPresent()) {
        branchService.updateVisit(visit.get(), event, this);
        return visit;
      }

    } else {
      throw new BusinessException(
          "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      setAutoCallMode(servicePoint, currentBranch);
    }
    return Optional.empty();
  }

  private void setAutoCallMode(ServicePoint servicePoint, Branch currentBranch) {
    if (servicePoint.getVisit() == null) {
      servicePoint.setAutoCallMode(true);
      currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
      branchService.add(currentBranch.getId(), currentBranch);
      HashMap<String, String> parameterMap = new HashMap<>();
      parameterMap.put("branchId", currentBranch.getId());
      parameterMap.put("servicePointId", servicePoint.getId());
      Event autocallEvent =
          Event.builder()
              .eventType("SERVUCEPOINT_AUTOCALL_MODE_TURN_ON")
              .eventDate(ZonedDateTime.now())
              .params(parameterMap)
              .body(servicePoint)
              .build();
      eventService.send("frontend", false, autocallEvent);
      throw new BusinessException("Autocall mode enabled!", eventService, HttpStatus.valueOf(207));
    }
  }

  /**
   * Автовызов визита
   *
   * @param visit созданный визит
   * @return визит после аввтовызова
   */
  public Visit visitAutoCall(Visit visit) {
    Branch currentBranch = branchService.getBranch(visit.getBranchId());
    Optional<Visit> visit2;
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      Optional<ServicePoint> servicePoint =
          waitingTimeCallRule.getAvailiableServicePoints(currentBranch, visit).stream()
              .filter(f -> f.getAutoCallMode() && f.getVisit() == null)
              .findFirst();
      if (servicePoint.isPresent()) {
        if (!servicePoint.get().getIsConfirmRequired()) {
          visit2 = visitCall(visit.getBranchId(), servicePoint.get().getId(), visit);
        } else {
          visit2 =
              visitCallForConfirmWithMaxWaitingTime(
                  visit.getBranchId(), servicePoint.get().getId(), visit);
        }
        if (visit2.isPresent()) {
          servicePoint.get().setAutoCallMode(false);
          currentBranch.getServicePoints().put(servicePoint.get().getId(), servicePoint.get());
          return visit2.get();
        }
      }
    }
    return visit;
  }

  /**
   * Вызов визита с максимальным временем ожидания
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return визит
   */
  public Optional<Visit> visitCallWithMaximalWaitingTime(String branchId, String servicePointId) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getUser() != null) {

        Optional<Visit> visit = waitingTimeCallRule.call(currentBranch, servicePoint);
        if (visit.isPresent()) {

          return visitCall(branchId, servicePoint.getId(), visit.get());
        }

      } else {
        throw new BusinessException(
            "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
      }

    } else {
      throw new BusinessException(
          "ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      setAutoCallMode(servicePoint, currentBranch);
    }
    return Optional.empty();
  }

  /**
   * Вызов визита с максимальным временем ожидания из очередей, чьи идентификаторы указаны в @param
   * queueIds
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueIds идентификаторы очередей
   * @return визит
   */
  public Optional<Visit> visitCallWithMaximalWaitingTime(
      String branchId, String servicePointId, List<String> queueIds) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getUser() != null) {

        Optional<Visit> visit = waitingTimeCallRule.call(currentBranch, servicePoint, queueIds);
        if (visit.isPresent()) {

          return visitCall(branchId, servicePoint.getId(), visit.get());
        }

      } else {
        throw new BusinessException(
            "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
      }

    } else {
      throw new BusinessException(
          "ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      setAutoCallMode(servicePoint, currentBranch);
    }
    return Optional.empty();
  }

  /**
   * Вызов визита с максимальным временем жизни визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @return визит
   */
  public Optional<Visit> visitCallWithMaxLifeTime(String branchId, String servicePointId) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getUser() != null) {

        Optional<Visit> visit = lifeTimeCallRule.call(currentBranch, servicePoint);
        if (visit.isPresent()) {

          return visitCall(branchId, servicePoint.getId(), visit.get());
        }

      } else {
        throw new BusinessException(
            "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
      }

    } else {
      throw new BusinessException(
          "ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      setAutoCallMode(servicePoint, currentBranch);
    }
    return Optional.empty();
  }

  /**
   * Вызов визита с максимальным временем жизни визита
   *
   * @param branchId идентификатор отделения
   * @param servicePointId идентификатор точки обслуживания
   * @param queueIds идентификаторы очередей
   * @return визит
   */
  public Optional<Visit> visitCallWithMaxLifeTime(
      String branchId, String servicePointId, List<String> queueIds) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getUser() != null) {

        Optional<Visit> visit = lifeTimeCallRule.call(currentBranch, servicePoint, queueIds);
        if (visit.isPresent()) {

          return visitCall(branchId, servicePoint.getId(), visit.get());
        }

      } else {
        throw new BusinessException(
            "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
      }

    } else {
      throw new BusinessException(
          "ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
    }
    if (currentBranch.getParameterMap().containsKey("autoCallMode")
        && currentBranch.getParameterMap().get("autoCallMode").toString().equals("true")) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      setAutoCallMode(servicePoint, currentBranch);
    }
    return Optional.empty();
  }

  /**
   * Удаление визита
   *
   * @param visit визит
   */
  public void deleteVisit(Visit visit) {

    if (visit.getReturningTime() > 0 && visit.getReturningTime() < visit.getReturnTimeDelay()) {
      throw new BusinessException(
          "You cant delete just returned visit!", eventService, HttpStatus.CONFLICT);
    }
    visit.setServicePointId(null);
    visit.setQueueId(null);
    VisitEvent event = VisitEvent.DELETED;
    event.dateTime = ZonedDateTime.now();

    branchService.updateVisit(visit, event, this);

    log.info("Visit {} deleted!", visit);
    // changedVisitEventSend("DELETED", visit, null, new HashMap<>());
  }

  public Visit visitTransferToUserPool(String branchId, String servicePointId, String userId) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();

        User user;
        if (this.getAllWorkingUsers(branchId).containsKey(userId)) {
          user = this.getAllWorkingUsers(branchId).get(userId);
        } else {

          throw new BusinessException(
              "User not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        }
        VisitEvent event = VisitEvent.STOP_SERVING;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);
        branchService.updateVisit(visit, event, this);

        assert user != null;

        visit.setQueueId(null);
        visit.setPoolServicePointId(null);
        visit.setServicePointId(null);
        visit.setStartServingDateTime(null);
        visit.setPoolUserId(user.getId());
        visit.setTransferDateTime(ZonedDateTime.now());

        visit.getParameterMap().remove("LastPoolUserId");
        event = VisitEvent.TRANSFER_TO_USER_POOL;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("userId", userId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);
        branchService.updateVisit(visit, event, this);
        // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        log.info("Visit {} transfered!", visit);
        return visit;
      } else {
        throw new BusinessException(
            String.format("Visit in ServicePoint %s! not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  public Visit visitTransferToServicePointPool(
      String branchId, String servicePointId, String poolServicePointId) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();

        ServicePoint poolServicePoint;
        if (currentBranch.getServicePoints().containsKey(poolServicePointId)) {
          poolServicePoint = currentBranch.getServicePoints().get(poolServicePointId);
        } else {

          throw new BusinessException(
              "Service point not found in branch configuration!",
              eventService,
              HttpStatus.NOT_FOUND);
        }
        VisitEvent event = VisitEvent.STOP_SERVING;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("poolServicePointId", poolServicePointId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);
        branchService.updateVisit(visit, event, this);

        visit.getParameterMap().remove("LastPoolServicePointId");
        assert poolServicePoint != null;

        visit.setServicePointId(null);
        visit.setQueueId(null);
        visit.setPoolUserId(null);
        visit.setPoolServicePointId(poolServicePointId);
        visit.setTransferDateTime(ZonedDateTime.now());

        visit.setStartServingDateTime(null);
        event = VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("poolServicePointId", poolServicePointId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);
        branchService.updateVisit(visit, event, this);
        // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        log.info("Visit {} transfered!", visit);
        return visit;
      } else {
        throw new BusinessException(
            String.format("Visit in ServicePoint %s! not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  public Visit visitTransferToServicePointPool(
      String branchId,
      String servicePointId,
      String poolServicePointId,
      HashMap<String, String> serviceInfo) {
    Branch currentBranch = branchService.getBranch(branchId);

    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();

        ServicePoint poolServicePoint;
        if (currentBranch.getServicePoints().containsKey(poolServicePointId)) {
          poolServicePoint = currentBranch.getServicePoints().get(poolServicePointId);
        } else {

          throw new BusinessException(
              "Service point not found in branch configuration!",
              eventService,
              HttpStatus.NOT_FOUND);
        }
        VisitEvent event = VisitEvent.STOP_SERVING;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("poolServicePointId", poolServicePointId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);
        event.getParameters().putAll(serviceInfo);
        branchService.updateVisit(visit, event, this);

        visit.setServicePointId(null);
        visit.getParameterMap().remove("LastPoolServicePointId");
        visit.setQueueId(null);
        visit.setPoolUserId(null);
        assert poolServicePoint != null;
        visit.setPoolServicePointId(poolServicePointId);

        visit.setTransferDateTime(ZonedDateTime.now());

        visit.setStartServingDateTime(null);
        event = VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("branchId", branchId);
        event.getParameters().put("poolServicePointId", poolServicePointId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staffName", visit.getUserName());
        event.getParameters().put("servicePointId", servicePointId);
        event.getParameters().putAll(serviceInfo);
        branchService.updateVisit(visit, event, this);
        // changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        log.info("Visit {} transfered!", visit);
        return visit;
      } else {
        throw new BusinessException(
            String.format("Visit in ServicePoint %s! not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  public List<Entity> getQueus(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);
    return currentBranch.getQueues().values().stream()
        .map(m -> Entity.builder().id(m.getId()).name(m.getName()).build())
        .toList();
  }

  public List<Queue> getFullQueus(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);
    return currentBranch.getQueues().values().stream().toList();
  }

  public List<Entity> getPrinters(String branchId) {
    Branch currentBranch = branchService.getBranch(branchId);
    List<Entity> printers =
        new ArrayList<>(
            currentBranch.getEntryPoints().values().stream().map(EntryPoint::getPrinter).toList());
    printers.addAll(currentBranch.getReception().getPrinters());
    return printers.stream().distinct().toList();
  }

  public Visit addNote(String branchId, String servicePointId, String noteText) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getServicePoints().containsKey(servicePointId)) {
      ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
      if (servicePoint.getVisit() != null) {
        Visit visit = servicePoint.getVisit();
        if (visit.getCurrentService() == null)
          throw new BusinessException(
              "Current service is null!", eventService, HttpStatus.NOT_FOUND);

        Mark note = new Mark();
        note.setId(UUID.randomUUID().toString());
        note.setValue(noteText);
        note.setMarkDate(ZonedDateTime.now());
        if (servicePoint.getUser() != null) {

          User user = servicePoint.getUser().toBuilder().build();
          user.setName(servicePoint.getUser().getName());
          user.setId(servicePoint.getUser().getId());
          note.setAuthor(user);
        }
        visit.getVisitNotes().add(note);
        VisitEvent visitEvent = VisitEvent.ADDED_NOTE;

        visitEvent.getParameters().put("servicePointId", servicePoint.getId());
        visitEvent.getParameters().put("note", noteText);
        visitEvent.getParameters().put("branchId", branchId);
        visitEvent.getParameters().put("staffId", visit.getUserId());
        visitEvent.getParameters().put("staffName", visit.getUserName());
        branchService.updateVisit(visit, visitEvent, this);
        return visit;

      } else {
        throw new BusinessException(
            String.format("In ServicePoint %s visit not exist!", servicePointId),
            eventService,
            HttpStatus.NOT_FOUND);
      }
    } else {
      throw new BusinessException(
          String.format("ServicePoint %s! not exist!", servicePointId),
          eventService,
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Просмотр заметок в визите
   *
   * @param branchId идентификатор отделения
   * @param visitId идентификатор точки обслуживания *
   * @return визит
   */
  public List<Mark> getNotes(String branchId, String visitId) {
    Branch currentBranch = branchService.getBranch(branchId);
    if (currentBranch.getAllVisits().containsKey(visitId)) {
      Visit visit = currentBranch.getAllVisits().get(visitId);
      return visit.getVisitNotes();
    } else {
      throw new BusinessException(
          String.format("Visit %s not found!", visitId), eventService, HttpStatus.NOT_FOUND);
    }
  }
}
