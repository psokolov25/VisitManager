package ru.aritmos.service.rules;

import io.micronaut.http.HttpStatus;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.BranchEntity;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

@Singleton
@Named("MaxWaitingTimeCallRule")
public class MaxWaitingTimeCallRule implements CallRule {
  @Inject EventService eventService;

  private Integer visitComparer(Visit visit1, Visit visit2) {
    if (visit1.getParameterMap().containsKey("isTransferredToStart")) {
      if (visit2.getParameterMap().containsKey("isTransferredToStart")) {
        return getDateNyString(visit2.getParameterMap().get("isTransferredToStart"))
            .compareTo(getDateNyString(visit1.getParameterMap().get("isTransferredToStart")));

      } else {
        return 1;
      }
    } else {
      if (visit2.getParameterMap().containsKey("isTransferredToStart")) {
        return -1;
      }
    }

    return Long.compare(visit1.getWaitingTime(), visit2.getWaitingTime());
  }

  /**
   * Конвертация даты типа {@link ZonedDateTime} в строку формата EEE, dd MMM yyyy HH:mm:ss zzz
   *
   * @param date дата типа {@link ZonedDateTime}
   * @return строка даты формата EEE, dd MMM yyyy HH:mm:ss zzz
   */
  ZonedDateTime getDateNyString(String date) {

    DateTimeFormatter format =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    return ZonedDateTime.parse(date, format);
  }

  @Override
  public Optional<Visit> call(Branch branch, ServicePoint servicePoint) {

    if (servicePoint.getUser() != null) {

      String workprofileId = servicePoint.getUser().getCurrentWorkProfileId();
      if (branch.getWorkProfiles().containsKey(workprofileId)) {
        List<String> queueIds = branch.getWorkProfiles().get(workprofileId).getQueueIds();
        List<Queue> availableQueues =
            branch.getQueues().entrySet().stream()
                .filter(f -> queueIds.contains(f.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        Optional<Visit> result =
            availableQueues.stream()
                .map(Queue::getVisits)
                .flatMap(List::stream)
                .filter(
                    f ->
                        ((f.getReturnDateTime() == null
                                    || f.getReturningTime() > f.getReturnTimeDelay())
                                && (f.getTransferDateTime() == null
                                    || f.getTransferingTime() > f.getTransferTimeDelay()))
                            && f.getStatus().contains("WAITING"))
                .max(this::visitComparer);
        result.ifPresent(visit -> visit.getParameterMap().remove("isTransferredToStart"));
        result.ifPresent(visit -> visit.setReturnDateTime(null));
        result.ifPresent(visit -> visit.setTransferDateTime(null));
        return result;
      } else {
        throw new BusinessException(
            "User with not correct workprofile in in service point!",
            eventService,
            HttpStatus.FORBIDDEN);
      }
    }
    throw new BusinessException(
        "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
  }

  @Override
  public Optional<Visit> call(Branch branch, ServicePoint servicePoint, List<String> queueIds) {

    if (servicePoint.getUser() != null) {

      String workprofileId = servicePoint.getUser().getCurrentWorkProfileId();
      if (branch.getWorkProfiles().containsKey(workprofileId)) {
        for (String f : queueIds) {
          Queue availableQueue = branch.getQueues().get(f);
          if (!availableQueue.getVisits().isEmpty()) {
            Optional<Visit> result =
                availableQueue.getVisits().stream()
                    .filter(
                        f2 ->
                            ((f2.getReturnDateTime() == null
                                        || f2.getReturningTime() > f2.getReturnTimeDelay())
                                    && (f2.getTransferDateTime() == null
                                        || f2.getTransferingTime() > f2.getTransferTimeDelay()))
                                && f2.getStatus().contains("WAITING"))
                    .max(this::visitComparer);
            result.ifPresent(visit -> visit.getParameterMap().remove("isTransferredToStart"));
            result.ifPresent(visit -> visit.setReturnDateTime(null));
            result.ifPresent(visit -> visit.setTransferDateTime(null));

            return result;
          }
        }
      }

    } else {
      throw new BusinessException(
          "User not logged in in service point!", eventService, HttpStatus.FORBIDDEN);
    }
    return Optional.empty();
  }

  /**
   * Возвращает список точек обслуживания, которые могут вызвать данный визит
   *
   * @param currentBranch текущее отделение
   * @param visit визит
   * @return список точек обслуживания
   */
  @Override
  public List<ServicePoint> getAvailiableServicePoints(Branch currentBranch, Visit visit) {

    List<String> workProfileIds =
        currentBranch.getWorkProfiles().values().stream()
            .filter(f -> f.getQueueIds().contains(visit.getCurrentService().getLinkedQueueId()))
            .map(BranchEntity::getId)
            .toList();
    return currentBranch.getServicePoints().values().stream()
        .filter(f -> f.getUser() != null)
        .filter(f2 -> workProfileIds.contains(f2.getUser().getCurrentWorkProfileId()))
        .toList();
  }
}
