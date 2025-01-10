package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.serde.annotation.Serdeable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/* Приемная */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Serdeable
@SuppressWarnings("unused")
public class Reception {
  /* Идентификатор отделения */
  String branchId;
  /* Перечень идентификаторов принтера */
  List<Entity> printers;

  /* Перечень сеансов работы сотрудника */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  List<ReceptionSession> receptionSessions = new ArrayList<>();
}
