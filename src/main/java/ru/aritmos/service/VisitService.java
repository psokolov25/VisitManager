package ru.aritmos.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Queue;
import ru.aritmos.model.*;
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.rules.SegmentationRule;

import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Singleton
public class VisitService {
    @Inject
    BranchService branchService;
    @Inject
    EventService eventService;
    @Inject
    PrinterService printerService;
    @Inject
    CallRule callRule;
    @Inject
    SegmentationRule segmentationRule;
    @Value("${micronaut.application.name}")
    String applicationName;

    /**
     * Возвращает визит по его отделению и идентификатору
     *
     * @param branchId идентификатор отделения
     * @param visitId  идентификатор визита
     * @return визит
     */
    public Visit getVisit(String branchId, String visitId) {
        if (getAllVisits(branchId).containsKey(visitId)) {
            return getAllVisits(branchId).get(visitId);
        }
        throw new BusinessException(String.format("Visit %s not found!", visitId), eventService, HttpStatus.NOT_FOUND);
    }

    /**
     * Возвращает сервис поинты отделения
     *
     * @param branchId идентификатор отделения
     * @return перечень сервис поинтом, с ключом - идентификатором сервис поинта
     */
    public @NotNull HashMap<String, ServicePoint> getStringServicePointHashMap(String branchId) {
        Branch currentBranch = branchService.getBranch(branchId);
        HashMap<String, ServicePoint> freeServicePoints = new HashMap<>();
        currentBranch.getServicePoints().entrySet().stream().filter(f -> f.getValue().getUser() == null).forEach(fe -> freeServicePoints.put(fe.getKey(), fe.getValue()));
        return freeServicePoints;
    }

    /**
     * Возвращает визиты содержащиеся в очереди
     *
     * @param branchId идентификатор отделения
     * @param queueId  идентификатор очереди
     * @return список визитов
     */
    @ExecuteOn(TaskExecutors.IO)
    public List<Visit> getVisits(String branchId, String queueId) {
        Branch currentBranch = branchService.getBranch(branchId);
        Queue queue;
        if (currentBranch.getQueues().containsKey(queueId)) {
            queue = currentBranch.getQueues().get(queueId);
        } else {
            throw new BusinessException("Queue not found in branch configuration!", eventService);
        }
        List<Visit> visits;
        visits = queue.getVisits();
        return visits.stream().sorted((f1, f2) -> Long.compare(f2.getWaitingTime(), f1.getWaitingTime())).toList();

    }

    /**
     * Получение списка визитов в указанной очереди указанного отделения с ограничением выдачи элементов
     *
     * @param branchId идентификатор отделения
     * @param queueId  идентификатор очереди
     * @param limit    максимальное количество визитов
     * @return список визитов
     */
    public List<Visit> getVisits(String branchId, String queueId, Long limit) {
        return getVisits(branchId, queueId).stream().limit(limit).toList();
    }

    /**
     * Создание визита
     *
     * @param branchId     идентификатор отделения
     * @param entryPointId идентификатор энтри поинта
     * @param servicesIds  идентификаторы услуг
     * @param printTicket  флаг печати талона
     * @return созданный визит
     */
    public Visit createVisit(String branchId, String entryPointId, ArrayList<String> servicesIds, Boolean printTicket) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServices().keySet().stream().anyMatch(servicesIds::contains)) {
            ArrayList<Service> services = new ArrayList<>();
            servicesIds.forEach(f -> services.add(currentBranch.getServices().get(f)));


            return createVisit2(branchId, entryPointId, services, printTicket);


        } else {
            throw new BusinessException("Services not found!", eventService);
        }
    }

    /**
     * Добавление события в визит
     *
     * @param visit        визит
     * @param event        событие
     * @param eventService служба отправки события визита на шину данных
     */
    public void addEvent(Visit visit, VisitEvent event, EventService eventService) {
        if (visit.getVisitEvents().isEmpty()) {
            if (!event.equals(VisitEvent.CREATED))
                throw new BusinessException("wasn't early created", eventService, HttpStatus.CONFLICT);
            else visit.getVisitEvents().add(event);

        } else {
            VisitEvent prevEvent = visit.getVisitEvents().get(visit.getVisitEvents().size() - 1);
            if (prevEvent.canBeNext(event)) {
                visit.getVisitEvents().add(event);


            } else
                throw new BusinessException(String.format("%s can't be next status %s", event.name(), visit.getVisitEvents().get(visit.getVisitEvents().size() - 1).name()), eventService, HttpStatus.CONFLICT);


        }
        event.getState();
    }

    /**
     * Получение списка очередей
     *
     * @param branchId       идентификатор отделения
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
                List<Queue> avaibleQueues = currentBranch.getQueues().entrySet().stream().filter(f -> queueIds.contains(f.getKey())).map(Map.Entry::getValue).toList();
                return Optional.of(avaibleQueues);

            } else {
                throw new BusinessException("User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);

            }


        } else {
            throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);

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
     * Получение списка визитов с фильтрацией по их статусам
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
     * @param branchId     идентификатор отделения
     * @param entryPointId идентификатор энтри поинта
     * @param services     список услуг
     * @param printTicket  флаг печати талона
     * @return визит
     */
    public Visit createVisit2(String branchId, String entryPointId, ArrayList<Service> services, Boolean printTicket) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (!services.isEmpty()) {
            if (currentBranch.getServices().containsKey(services.get(0).getId())) {
                Service currentService = currentBranch.getServices().get(services.get(0).getId());
                List<Service> unServedServices = new ArrayList<>();
                services.stream().skip(1).forEach(f -> unServedServices.add(currentBranch.getServices().get(f.getId())));

                EntryPoint entryPoint;

                if (!currentBranch.getEntryPoints().containsKey(entryPointId)) {
                    throw new BusinessException("EntryPoint not found in branch configuration!", eventService);
                } else {
                    entryPoint = currentBranch.getEntryPoints().get(entryPointId);
                }


                Visit visit = Visit.builder()
                        .id(UUID.randomUUID().toString())
                        .status("WAITING")
                        .entryPoint(entryPoint)
                        .printTicket(printTicket)
                        .branchId(branchId)
                        .branchName(currentBranch.getName())
                        .currentService(currentService)
                        .unservedServices(unServedServices)
                        .createDateTime(ZonedDateTime.now())
                        .visitMarks(new ArrayList<>())
                        .visitEvents(new ArrayList<>())
                        //.updateDateTime(ZonedDateTime.now())
                        //.transferDateTime(ZonedDateTime.now())
                        // .endDateTime(ZonedDateTime.now())
                        .servicePointId(null)

                        .servedServices(new ArrayList<>())

                        .parameterMap(new HashMap<>()).build();
                Queue serviceQueue;
                if (segmentationRule.getQueue(visit, currentBranch).isPresent()) {
                    serviceQueue = segmentationRule.getQueue(visit, currentBranch).get();

                    serviceQueue.setTicketCounter(branchService.incrementTicetCounter(branchId, serviceQueue));
                    visit.setQueueId(serviceQueue.getId());
                    visit.setTicket((serviceQueue.getTicketPrefix() + String.format("%03d", serviceQueue.getTicketCounter())));
                    VisitEvent event = VisitEvent.CREATED;
                    event.dateTime = ZonedDateTime.now();

                    branchService.updateVisit(visit, event, this);
                    if (currentBranch.getQueues().containsKey(serviceQueue.getId())) {
                        VisitEvent queueEvent = VisitEvent.PLACED_IN_QUEUE;
                        queueEvent.dateTime = ZonedDateTime.now();
                        queueEvent.getParameters().put("queueId", serviceQueue.getId());
                        visit.setQueueId(serviceQueue.getId());


                        if (printTicket && entryPoint.getPrinterId() != null) {
                            printerService.print(entryPoint.getPrinterId(), visit);
                        }


                        //changedVisitEventSend("CREATED", null, visit, new HashMap<>());
                        branchService.updateVisit(visit, queueEvent, this);
                        log.info("Visit {} created!", visit);

                        return visit;
                    } else {
                        throw new BusinessException("Queue not found in branch configuration!", eventService);
                    }

                } else {
                    throw new BusinessException("Services can not be empty!", eventService);
                }
            } else {
                throw new BusinessException("Service  not found in branch configuration!", eventService);
            }

        }

        throw new BusinessException("Queue  not found in branch configuration!", eventService);
    }


    /**
     * Добавление оказанной услуги
     *
     * @param branchId           идентификатор отделения
     * @param servicePointId     идентификатор точки обслуживания
     * @param deliveredServiceId идентификатор оказанной услуги
     * @return визит
     */
    public Visit addDeliveredService(String branchId, String servicePointId, String deliveredServiceId) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                if (visit.getCurrentService() == null) {
                    throw new BusinessException("Current service is null!", eventService, HttpStatus.NOT_FOUND);
                }
                if(!currentBranch.getPossibleDeliveredServices().containsKey(deliveredServiceId))
                {
                    throw new BusinessException(String.format("Delivered service with id %s not found!", deliveredServiceId), eventService,HttpStatus.NOT_FOUND);
                }
                if (currentBranch.getPossibleDeliveredServices().values().stream().noneMatch(f ->f.getServviceIds().contains(visit.getCurrentService().getId()))) {
                    throw new BusinessException(String.format("Current service cant add delivered service with id %s", deliveredServiceId), eventService,HttpStatus.CONFLICT);
                }
                DeliveredService deliveredService = currentBranch.getPossibleDeliveredServices().get(deliveredServiceId);
                visit.getCurrentService().getDeliveredServices().put(deliveredService.getId(), deliveredService);

                VisitEvent visitEvent = VisitEvent.ADDED_DELIVERED_SERVICE;
                visitEvent.getParameters().put("servicePointId", servicePoint.getId());
                visitEvent.getParameters().put("deliveredServiceId", deliveredServiceId);
                visitEvent.getParameters().put("deliveredServiceName", deliveredService.getName());
                visitEvent.getParameters().put("serviceId", visit.getCurrentService().getId());
                visitEvent.getParameters().put("serviceName", visit.getCurrentService().getName());
                visitEvent.getParameters().put("branchId", branchId);
                visitEvent.getParameters().put("staffId", visit.getUserId());
                visitEvent.getParameters().put("staff?Name", visit.getUserName());
                branchService.updateVisit(visit, visitEvent, this);
                return visit;

            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Добавление услуги
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param serviceId      идентификатор услуги
     * @return визит
     */
    public Visit addService(String branchId, String servicePointId, String serviceId) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();

                if (visit.getCurrentService().getId().equals(serviceId) || visit.getUnservedServices().stream().anyMatch(f -> f.getId().equals(serviceId)) || visit.getServedServices().stream().anyMatch(f -> f.getId().equals(serviceId))) {
                    throw new BusinessException("Service already added!", eventService, HttpStatus.NOT_FOUND);
                }
                if (currentBranch.getServices().keySet().stream().noneMatch(f -> f.contains(serviceId))) {
                    throw new BusinessException(String.format("Current visit cant add  service with id %s", serviceId), eventService);
                }
                Service service = currentBranch.getServices().get(serviceId);
                visit.getUnservedServices().add(service);

                VisitEvent visitEvent = VisitEvent.ADD_SERVICE;
                visitEvent.getParameters().put("servicePointId", servicePoint.getId());

                visitEvent.getParameters().put("serviceId", service.getId());
                visitEvent.getParameters().put("serviceName", service.getName());
                visitEvent.getParameters().put("branchId", branchId);
                visitEvent.getParameters().put("staffId", visit.getUserId());
                visitEvent.getParameters().put("staff?Name", visit.getUserName());
                branchService.updateVisit(visit, visitEvent, this);
                return visit;

            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Добавление текстовой пометки в визит
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param mark  пометка
     * @return визит
     */
    public Visit addMark(String branchId, String servicePointId, Mark mark) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                if (visit.getCurrentService() == null)
                    throw new BusinessException("Current service is null!", eventService, HttpStatus.NOT_FOUND);
                mark.setMarkDate(ZonedDateTime.now());
                visit.getVisitMarks().add(mark);
                VisitEvent visitEvent = VisitEvent.ADDED_MARK;
                visitEvent.getParameters().put("servicePointId", servicePoint.getId());
                visitEvent.getParameters().put("mark", mark.getValue());
                visitEvent.getParameters().put("branchId", branchId);
                visitEvent.getParameters().put("staffId", visit.getUserId());
                visitEvent.getParameters().put("staff?Name", visit.getUserName());
                branchService.updateVisit(visit, visitEvent, this);
                return visit;


            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }
    /**
     * Удаление текстовой пометки в визите
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param mark  пометка
     * @return визит
     */
    public Visit deleteMark(String branchId, String servicePointId, Mark mark) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                if (visit.getCurrentService() == null)
                    throw new BusinessException("Current service is null!", eventService, HttpStatus.NOT_FOUND);
                visit.getVisitMarks().removeIf(f->f.getId().equals(mark.getId()));
                VisitEvent visitEvent = VisitEvent.DELETED_MARK;
                visitEvent.getParameters().put("servicePointId", servicePoint.getId());
                visitEvent.getParameters().put("mark", mark.getValue());
                visitEvent.getParameters().put("branchId", branchId);
                visitEvent.getParameters().put("staffId", visit.getUserId());
                visitEvent.getParameters().put("staff?Name", visit.getUserName());
                branchService.updateVisit(visit, visitEvent, this);
                return visit;


            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }
    /**
     * Удаление пометки в визите
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param markId  идентификатор пометки
     * @return визит
     */
    public Visit deleteMark(String branchId, String servicePointId, String markId) {
        Branch currentBranch = branchService.getBranch(branchId);
       if(currentBranch.getMarks().containsKey(markId))
       {
           return deleteMark(branchId,servicePointId,currentBranch.getMarks().get(markId));
       }
       else
       {
           throw new BusinessException(String.format("Mark %s not found!",markId), eventService, HttpStatus.NOT_FOUND);
       }
    }
    /**
     * Добавление пометки в визите
     * @param branchId идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param markId  идентификатор пометки
     * @return визит
     */
    public Visit addMark(String branchId, String servicePointId, String markId) {
        Branch currentBranch = branchService.getBranch(branchId);
        if(currentBranch.getMarks().containsKey(markId))
        {
            return addMark(branchId,servicePointId,currentBranch.getMarks().get(markId));
        }
        else
        {
            throw new BusinessException(String.format("Mark %s not found!",markId), eventService, HttpStatus.NOT_FOUND);
        }
    }



    /**
     * Добавление итога услуги
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param outcomeId      идентификатор итога услуги
     * @return визит
     */
    public Visit addOutcomeService(String branchId, String servicePointId, String outcomeId) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                if (visit.getCurrentService() == null)
                    throw new BusinessException("Current service is null!", eventService, HttpStatus.NOT_FOUND);
                if (visit.getCurrentService().getPossibleOutcomes().keySet().stream().noneMatch(f -> f.equals(outcomeId)))
                    throw new BusinessException(String.format("Current service cant add outcome with id %s", outcomeId), eventService, HttpStatus.CONFLICT);
                else {
                    Outcome outcome = visit.getCurrentService().getPossibleOutcomes().get(outcomeId);
                    visit.getCurrentService().setOutcome(outcome);

                    VisitEvent visitEvent = VisitEvent.ADDED_SERVICE_RESULT;
                    visitEvent.getParameters().put("servicePointId", servicePoint.getId());
                    visitEvent.getParameters().put("outcomeId", outcomeId);
                    visitEvent.getParameters().put("outcomeName", outcome.getName());
                    visitEvent.getParameters().put("branchId", branchId);
                    visitEvent.getParameters().put("staffId", visit.getUserId());
                    visitEvent.getParameters().put("staff?Name", visit.getUserName());
                    branchService.updateVisit(visit, visitEvent, this);
                    return visit;
                }


            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Добавление итога оказанной услуги
     *
     * @param branchId           идентификатор отделения
     * @param servicePointId     идентификатор точки обслуживания
     * @param deliveredServiceId идентификатор оказанной услуги
     * @param outcomeId          идентификатор итога услуги
     * @return визит
     */
    public Visit addOutcomeDeliveredService(String branchId, String servicePointId, String deliveredServiceId, String outcomeId) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                if (visit.getCurrentService() == null) {
                    throw new BusinessException("Current service is null!", eventService, HttpStatus.NOT_FOUND);
                }
                if (!visit.getCurrentService().getDeliveredServices().containsKey(deliveredServiceId)) {
                    throw new BusinessException(String.format("Delivered service %s of current service ID is not %s", visit.getCurrentService().getId(), deliveredServiceId), eventService, HttpStatus.NOT_FOUND);
                }
                if (visit.getCurrentService().getDeliveredServices().get(deliveredServiceId).getPossibleOutcomes().keySet().stream().noneMatch(f -> f.equals(outcomeId))) {
                    throw new BusinessException(String.format("Current service with delivered service %s cant add outcome with id %s", deliveredServiceId, outcomeId), eventService, HttpStatus.NOT_FOUND);
                } else {
                    Outcome outcome = visit.getCurrentService().getPossibleOutcomes().get(outcomeId);
                    visit.getCurrentService().getDeliveredServices().get(deliveredServiceId).setOutcome(outcome);

                    VisitEvent visitEvent = VisitEvent.ADDED_DELIVERED_SERVICE_RESULT;
                    visitEvent.getParameters().put("servicePointId", servicePoint.getId());
                    visitEvent.getParameters().put("deliveredServiceId", deliveredServiceId);
                    visitEvent.getParameters().put("outcomeId", outcomeId);
                    visitEvent.getParameters().put("branchId", branchId);
                    visitEvent.getParameters().put("staffId", visit.getUserId());
                    visitEvent.getParameters().put("staff?Name", visit.getUserName());
                    branchService.updateVisit(visit, visitEvent, this);
                    return visit;
                }


            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Удаление итога оказанной услуги
     *
     * @param branchId           идентификатор отделения
     * @param servicePointId     идентификатор точки обслуживания
     * @param deliveredServiceId идентификатор оказанной услуги
     * @return визит
     */
    public Visit deleteOutcomeDeliveredService(String branchId, String servicePointId, String deliveredServiceId) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                if (visit.getCurrentService() == null) {
                    throw new BusinessException("Current service is null!", eventService, HttpStatus.NOT_FOUND);
                }
                if (!visit.getCurrentService().getDeliveredServices().containsKey(deliveredServiceId)) {
                    throw new BusinessException(String.format("Delivered service %s of current service ID is not %s", visit.getCurrentService().getId(), deliveredServiceId), eventService, HttpStatus.NOT_FOUND);
                }


                visit.getCurrentService().getDeliveredServices().get(deliveredServiceId).setOutcome(null);

                VisitEvent visitEvent = VisitEvent.ADDED_DELIVERED_SERVICE_RESULT;
                visitEvent.getParameters().put("servicePointId", servicePoint.getId());
                visitEvent.getParameters().put("deliveredServiceId", deliveredServiceId);
                visitEvent.getParameters().put("outcomeId", "");
                visitEvent.getParameters().put("branchId", branchId);
                visitEvent.getParameters().put("staffId", visit.getUserId());
                visitEvent.getParameters().put("staff?Name", visit.getUserName());
                branchService.updateVisit(visit, visitEvent, this);
                return visit;


            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }


    /**
     * Удаление итога услуги
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param serviceId      идентификатор услуги
     * @return Услуга
     */
    public Visit deleteOutcomeService(String branchId, String servicePointId, String serviceId) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                if (!visit.getCurrentService().getId().equals(serviceId)) {
                    throw new BusinessException(String.format("Current service ID is not %s@", serviceId), eventService);
                }


                visit.getCurrentService().setOutcome(null);

                VisitEvent visitEvent = VisitEvent.DELETED_SERVICE_RESULT;
                visitEvent.getParameters().put("servicePointId", servicePoint.getId());

                visitEvent.getParameters().put("branchId", branchId);
                visitEvent.getParameters().put("staffId", visit.getUserId());
                visitEvent.getParameters().put("staff?Name", visit.getUserName());
                branchService.updateVisit(visit, visitEvent, this);
                return visit;

            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Возвращение визита в очередь с задержкой
     *
     * @param branchId        идентификатор отделения
     * @param servicePointId  идентификатор точки обслуживания
     * @param returnTimeDelay задержка возвращения в секундах
     * @return визит
     */
    public Visit returnVisit(String branchId, String servicePointId, Long returnTimeDelay) {
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                Visit visit = servicePoint.getVisit();
                visit.setReturnDateTime(ZonedDateTime.now());
                visit.setReturnTimeDelay(returnTimeDelay);
                VisitEvent visitEvent = VisitEvent.TRANSFER_TO_QUEUE;
                visitEvent.getParameters().put("servicePointId", servicePoint.getId());
                visitEvent.getParameters().put("branchId", branchId);
                branchService.updateVisit(visit, visitEvent, this);
                if (visit.getParameterMap().containsKey("LastQueueId")) {

                    return visitTransfer(branchId, servicePointId, visit.getParameterMap().get("LastQueueId").toString());
                } else {
                    throw new BusinessException("Visit cant be transfer!", eventService);
                }
            } else {
                throw new BusinessException(String.format("In ServicePoint %s visit not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Перевод визита  в очередь
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param queueId        идентификатор очереди
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

                    throw new BusinessException("Queue not found in branch configuration!", eventService);
                }
                visit.setServicePointId(null);
                currentBranch.getServicePoints().get(servicePointId);
                visit.setServicePointId(null);

                assert queue != null;
                visit.setQueueId(queue.getId());
                visit.setServicePointId(null);

                visit.setTransferDateTime(ZonedDateTime.now());
                queue.getVisits().add(visit);
                currentBranch.getQueues().put(queue.getId(), queue);
                VisitEvent event = VisitEvent.BACK_TO_QUEUE;
                event.dateTime = ZonedDateTime.now();
                event.getParameters().put("branchId", queueId);
                event.getParameters().put("queueId", queueId);
                event.getParameters().put("staffId", visit.getUserId());
                event.getParameters().put("staff?Name", visit.getUserName());
                branchService.updateVisit(visit, event, this);
                //changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
                log.info("Visit {} transfered!", visit);
                return visit;
            } else {
                throw new BusinessException(String.format("Visit in ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new BusinessException(String.format("ServicePoint %s! not exist!", servicePointId), eventService, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Перевод визита из очереди в очередь
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param queueId        идентификатор очереди
     * @param visit          визит
     * @return визит
     */
    public Visit visitTransferFromQueue(String branchId, String servicePointId, String queueId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        String oldQueueID = visit.getQueueId();
        if (visit.getQueueId().isBlank()) {
            throw new BusinessException("Visit not in a queue!", eventService);
        }

        Queue queue;
        if (currentBranch.getQueues().containsKey(queueId)) {
            queue = currentBranch.getQueues().get(queueId);
        } else {

            throw new BusinessException("Queue not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
        }

        currentBranch.getServicePoints().get(servicePointId);
        visit.setServicePointId(null);

        assert queue != null;
        visit.setQueueId(queue.getId());


        queue.getVisits().add(visit);
        currentBranch.getQueues().put(queue.getId(), queue);
        VisitEvent event = VisitEvent.BACK_TO_QUEUE;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("oldQueueID", oldQueueID);
        event.getParameters().put("newQueueID", queueId);
        event.getParameters().put("servicePointId", servicePointId);
        event.getParameters().put("branchID", branchId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staff?Name", visit.getUserName());
        branchService.updateVisit(visit, event, this);
        //changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        log.info("Visit {} transfered!", visit);
        return visit;
    }

    /**
     * Завершение визита
     *
     * @param branchId       идентификатор отделения
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
                servicePoint.setVisit(null);
                //currentBranch.getServicePoints().put(servicePoint.getId(), servicePoint);
                //currentBranch.getServicePoints().get(servicePointId);
                visit.setServicePointId(null);

                VisitEvent event;

                if (visit.getUnservedServices() != null && !visit.getUnservedServices().isEmpty()) {

                    visit.getServedServices().add(visit.toBuilder().build().getCurrentService());
                    visit.setCurrentService(visit.toBuilder().build().getUnservedServices().get(0));
                    visit.getUnservedServices().remove(0);
                    String queueIdToReturn = visit.getCurrentService().getLinkedQueueId();
                    visit.setQueueId(queueIdToReturn);


                    visit.setServedDateTime(ZonedDateTime.now());
                    event = VisitEvent.STOP_SERVING;
                    event.dateTime = ZonedDateTime.now();
                    event.getParameters().put("servicePointId", servicePointId);
                    event.getParameters().put("branchID", branchId);
                    event.getParameters().put("staffId", visit.getUserId());
                    event.getParameters().put("staff?Name", visit.getUserName());
                    branchService.updateVisit(visit, event, this);
                    //event = VisitEvent.VISIT_END_TRANSACTION;
                    //event.dateTime = ZonedDateTime.now();
                    visit.setReturnDateTime(ZonedDateTime.now());
                    visit.setCallDateTime(null);
                    //branchService.updateVisit(visit, event,this);
                    visit.setStartServingDateTime(null);
                    //visit.updateTransaction(event, eventService,branchService);
                    event = VisitEvent.BACK_TO_QUEUE;
                    event.getParameters().put("branchID", branchId);
                    event.getParameters().put("queueId", queueIdToReturn);
                    event.getParameters().put("servicePointId", servicePointId);
                    event.getParameters().put("staffId", visit.getUserId());
                    event.getParameters().put("staff?Name", visit.getUserName());

                    visit.setServicePointId(null);
                    branchService.updateVisit(visit, event, this);
                    //Queue queue = currentBranch.getQueues().get(queueIdToReturn);
                    //queue.getVisits().add(visit);
                    //currentBranch.getQueues().put(queue.getId(), queue);

                } else {
                    visit.getServedServices().add(visit.getCurrentService());
                    visit.setCurrentService(null);
                    visit.setServedDateTime(ZonedDateTime.now());
                    visit.setQueueId(null);
                    visit.setServedDateTime(ZonedDateTime.now());
                    event = VisitEvent.STOP_SERVING;
                    event.dateTime = ZonedDateTime.now();
                    event.getParameters().put("branchID", branchId);
                    event.getParameters().put("staffId", visit.getUserId());
                    event.getParameters().put("staff?Name", visit.getUserName());
                    event.getParameters().put("servicePointId", servicePointId);
                    branchService.updateVisit(visit, event, this);
                    event = VisitEvent.END;
                    event.dateTime = ZonedDateTime.now();

                    visit.setServicePointId(null);
                    branchService.updateVisit(visit, event, this);
                }


                //changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
                log.info("Visit {} ended", visit);
                return visit;

            } else {
                throw new BusinessException("Visit not found in ServicePoint ", eventService, HttpStatus.NOT_FOUND);
            }

        } else {

            throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);

        }

    }

    /**
     * Вызов визита
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return визит
     */
    public Visit visitCall(String branchId, String servicePointId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);

        Optional<Queue> queue;

        visit.setStatus("CALLED");
        visit.setCallDateTime(ZonedDateTime.now());

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                throw new BusinessException("Visit alredey called in the ServicePoint! ", eventService, HttpStatus.CONFLICT);
            }
            visit.setServicePointId(servicePointId);
            visit.setUserName(servicePoint.getUser() != null ? servicePoint.getUser().getName() : null);
            visit.setUserId(servicePoint.getUser() != null ? servicePoint.getUser().getId() : null);
            servicePoint.setVisit(visit);

        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
            }
        }

        queue = currentBranch.getQueues().values().stream().filter(f -> f.getId().equals(visit.getQueueId())).findFirst();
        if ((queue.isPresent())) {
            List<Visit> visits = queue.get().getVisits();
            visits.removeIf(f -> f.getId().equals(visit.getId()));
            queue.get().setVisits(visits);
            currentBranch.getQueues().put(queue.get().getId(), queue.get());
        } else {
            throw new BusinessException("Queue not found in branch configuration or not available for current workprofile!", eventService, HttpStatus.NOT_FOUND);
        }
        visit.getParameterMap().put("LastQueueId", visit.getQueueId());
        visit.setQueueId(null);
        VisitEvent event = VisitEvent.CALLED;
        event.getParameters().put("servicePointId", servicePointId);
        event.getParameters().put("queueId", queue.map(BranchEntity::getId).orElse(null));
        event.getParameters().put("branchID", branchId);
        event.getParameters().put("staffId", visit.getUserId());
        event.getParameters().put("staff?Name", visit.getUserName());
        event.dateTime = ZonedDateTime.now();
        branchService.updateVisit(visit, event, this);

        VisitEvent servingEvent = VisitEvent.START_SERVING;
        servingEvent.dateTime = ZonedDateTime.now();
        visit.setStartServingDateTime(ZonedDateTime.now());

        branchService.updateVisit(visit, servingEvent, this);

        log.info("Visit {} called!", visit);
        //changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    /**
     * Вызов визита по идентификатору
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visitId        идентификатор визита
     * @return визит
     */
    public Visit visitCall(String branchId, String servicePointId, String visitId) {
        if (this.getAllVisits(branchId).containsKey(visitId)) {
            Visit visit = this.getAllVisits(branchId).get(visitId);
            return this.visitCall(branchId, servicePointId, visit);
        }
        throw new BusinessException(String.format("Visit %s not found!", visitId), eventService, HttpStatus.NOT_FOUND);


    }

    /**
     * Вызов визита с ожиданием подтверждения прихода клиента
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return визит
     */
    public Visit visitCallForConfirm(String branchId, String servicePointId, Visit visit) {

        String userId = "";
        String userName = "";
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            userId = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getId() : "";
            userName = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getName() : "";
        }

        //visit.setStatus("CALLED");
        visit.setCallDateTime(ZonedDateTime.now());


        VisitEvent event = VisitEvent.CALLED;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("servicePointId", servicePointId);
        event.getParameters().put("branchID", branchId);
        event.getParameters().put("queueId", visit.getQueueId());
        event.getParameters().put("staffId", userId);
        event.getParameters().put("staff?Name", userName);
        branchService.updateVisit(visit, event, this);


        log.info("Visit {} called!", visit);
        //changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    /**
     * Повторный вызов визита с ожиданием подтверждения прихода клиента
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return визит
     */
    public Visit visitReCallForConfirm(String branchId, String servicePointId, Visit visit) {


        String userId = "";
        String userName = "";
        Branch currentBranch = branchService.getBranch(branchId);
        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            userId = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getId() : "";
            userName = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getName() : "";
        }

        visit.setCallDateTime(ZonedDateTime.now());


        VisitEvent event = VisitEvent.RECALLED;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("ServicePointId", servicePointId);
        event.getParameters().put("branchID", branchId);
        event.getParameters().put("queueId", visit.getQueueId());
        event.getParameters().put("staffId", userId);
        event.getParameters().put("staff?Name", userName);
        branchService.updateVisit(visit, event, this);


        log.info("Visit {} called!", visit);
        //changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    /**
     * Подтверждение прихода клиента
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return визит
     */
    public Visit visitConfirm(String branchId, String servicePointId, Visit visit) {
        Branch currentBranch = branchService.getBranch(branchId);
        String userId = "";
        String userName = "";

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            userId = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getId() : "";
            userName = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getName() : "";
        }

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getVisit() != null) {
                throw new BusinessException("Visit alredey called in the ServicePoint! ", eventService, HttpStatus.CONFLICT);
            }
            visit.setServicePointId(servicePointId);
            visit.setUserName(servicePoint.getUser() != null ? servicePoint.getUser().getName() : "");
            visit.setUserId(servicePoint.getUser() != null ? servicePoint.getUser().getId() : "");


        } else {
            if (!servicePointId.isEmpty() && !currentBranch.getServicePoints().containsKey(servicePointId)) {
                throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);
            }
        }

        visit.setStatus("START_SERVING");
        visit.setStartServingDateTime(ZonedDateTime.now());
        visit.getParameterMap().put("LastQueueId", visit.getQueueId());
        visit.setQueueId(null);


        VisitEvent event = VisitEvent.START_SERVING;
        event.dateTime = ZonedDateTime.now();
        event.getParameters().put("ServicePointId", servicePointId);
        event.getParameters().put("branchID", branchId);
        event.getParameters().put("serviceId", visit.getCurrentService().getId());
        event.getParameters().put("staffId", userId);
        event.getParameters().put("staff?Name", userName);
        branchService.updateVisit(visit, event, this);


        log.info("Visit {} statted serving!", visit);
        //changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    /**
     * Завершение не пришедшего визита
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @param visit          визит
     * @return визит
     */
    public Visit visitNoShow(String branchId, String servicePointId, Visit visit) {

        Branch currentBranch = branchService.getBranch(branchId);
        String userId = "";
        String userName = "";

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            userId = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getId() : "";
            userName = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getName() : "";
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
        event.getParameters().put("staff?Name", userName);
        branchService.updateVisit(visit, event, this);


        log.info("Visit {} statted serving!", visit);
        //changedVisitEventSend("CHANGED", oldVisit, visit, new HashMap<>());
        return visit;

    }

    /**
     * Вызов визита с подтверждением прихода
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @return визит
     */
    public Optional<Visit> visitCallForConfirm(String branchId, String servicePointId) {
        Branch currentBranch = branchService.getBranch(branchId);
        String userId = "";
        String userName = "";

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            userId = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getId() : "";
            userName = currentBranch.getServicePoints().get(servicePointId).getUser() != null ? currentBranch.getServicePoints().get(servicePointId).getUser().getName() : "";
        }

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);

            Optional<Visit> visit = callRule.call(currentBranch, servicePoint);
            VisitEvent event = VisitEvent.CALLED;
            event.dateTime = ZonedDateTime.now();
            event.getParameters().put("ServicePointId", servicePointId);
            event.getParameters().put("branchID", branchId);
            event.getParameters().put("staffId", userId);
            event.getParameters().put("staff?Name", userName);
            if (visit.isPresent()) {
                branchService.updateVisit(visit.get(), event, this);
                return visit;
            }

        } else {
            throw new BusinessException("User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);

        }


        return Optional.empty();

    }

    /**
     * Вызов визита
     *
     * @param branchId       идентификатор отделения
     * @param servicePointId идентификатор точки обслуживания
     * @return визит
     */

    public Optional<Visit> visitCall(String branchId, String servicePointId) {
        Branch currentBranch = branchService.getBranch(branchId);

        if (currentBranch.getServicePoints().containsKey(servicePointId)) {
            ServicePoint servicePoint = currentBranch.getServicePoints().get(servicePointId);
            if (servicePoint.getUser() != null) {

                Optional<Visit> visit = callRule.call(currentBranch, servicePoint);
                if (visit.isPresent()) {

                    return visit.map(value -> this.visitCall(branchId, servicePointId, value));
                }

            } else {
                throw new BusinessException("User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);

            }


        } else {
            throw new BusinessException("ServicePoint not found in branch configuration!", eventService, HttpStatus.NOT_FOUND);

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
            throw new BusinessException("You cant delete just returned visit!", eventService, HttpStatus.NOT_FOUND);
        }
        visit.setServicePointId(null);
        visit.setQueueId(null);
        VisitEvent event = VisitEvent.VISIT_DELETED;
        event.dateTime = ZonedDateTime.now();

        branchService.updateVisit(visit, event, this);


        log.info("Visit {} deleted!", visit);
        //changedVisitEventSend("DELETED", visit, null, new HashMap<>());
    }

}
