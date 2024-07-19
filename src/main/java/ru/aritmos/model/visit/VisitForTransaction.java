package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.aritmos.model.Branch;
import ru.aritmos.service.BranchService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


@Data
@Serdeable
@AllArgsConstructor
@Builder

public final class VisitForTransaction {

    VisitForTransaction.Transaction mapForVisitTransaction(Visit visit, ru.aritmos.model.visit.Transaction f, BranchService branchService) {
        Branch currentBranch = branchService.getBranch(visit.getBranchId());
        List<Transaction.VisitEvent> events = new ArrayList<>();
        f.getVisitEvents().forEach(fe ->
                events.add(
                        Transaction.VisitEvent.builder().type(fe.name())
                                .eventTime(fe.dateTime)
                                .parameters(new HashMap<>())
                                .build()));

        return VisitForTransaction.Transaction.builder()
                .id(f.getId())

                .servingSL(visit.currentService != null && visit.currentService.getServingSL() != null ? visit.currentService.getServingSL() : 0)
                .serviceId(visit.getCurrentService() != null ? visit.getCurrentService().getId() : null)
                .waitingSL((currentBranch.getQueues().containsKey(visit.getQueueId()) && (currentBranch.getQueues().get(visit.getQueueId()).getWaitingSL() != null)) ? currentBranch.getQueues().get(visit.getQueueId()).getWaitingSL() : 0)
                .callingTime(f.getCallDateTime())
                .endTime(f.getEndDateTime())
                .startTime(f.getStartDateTime())
                .startServingTime(f.getStartServingDateTime())
                .servicePointId(f.getServicePointId())
                .queueId(f.getQueueId())
                .staffId(f.getEmployeeId())
                .visitEvents(events)
                .build();


    }

    public VisitForTransaction(Visit visit, BranchService branchService) {

        this.id = visit.getId();

        this.state = visit.getStatus();
        this.ticket = visit.getTicket();
        this.branchId = visit.getBranchId();
        if (visit.getCurrentTransaction() != null) {
            this.getTransactions().add(mapForVisitTransaction(visit, visit.getCurrentTransaction(), branchService));
        }
        if (visit.getTransactions() != null) {
            visit.getTransactions().forEach(f2 ->
                    this.getTransactions().add(mapForVisitTransaction(visit, f2, branchService))

            );
        }

    }

    /**
     * Идентификатор визита
     */
    String id;
    /**
     * Статус визита
     */
    String state;
    /**
     * Талон
     */
    String ticket;
    /**
     * Идентификатор отделения
     */
    String branchId;
    /**
     * Массив транзакций
     */

    List<VisitForTransaction.Transaction> transactions = new ArrayList<>();

    @Builder
    @Data
    @Serdeable
    @JsonInclude()
    private static class Transaction {
        String id;


        String serviceId;
        String queueId;
        ZonedDateTime startTime;
        ZonedDateTime callingTime;
        ZonedDateTime startServingTime;
        ZonedDateTime endTime;
        String servicePointId;

        int waitingSL;
        int servingSL;
        TransactionCompletionStatus completionStatus;
        String staffId;

        List<VisitEvent> visitEvents;

        @Data
        @Builder
        @Serdeable
        public static class VisitEvent {
            String type;
            ZonedDateTime eventTime;
            HashMap<String, Object> parameters;
        }
    }
}
