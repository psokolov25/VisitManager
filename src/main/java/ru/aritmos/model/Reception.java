package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Serdeable
@SuppressWarnings("unused")
public class Reception {
  String branchId;
  List<String> printerIds;
  List<ReceptionSession> receptionSessions = new ArrayList<>();
}
