package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

/** Юнит-тесты для метода {@link VisitService#addService(String, String, String)}. */
class VisitServiceAddServiceTest {

  @DisplayName("Добавляет услугу в список необслуженных")
  @Test
  void addsServiceToUnservedServices() {
    Branch branch = new Branch("b1", "Branch");
    ServicePoint sp = new ServicePoint("sp1", "SP1");
    Visit visit = Visit.builder().id("v1").unservedServices(new ArrayList<>()).build();
    sp.setVisit(visit);
    branch.getServicePoints().put(sp.getId(), sp);

    Service serviceEntity = new Service("s1", "Service", 10, "q1");
    branch.getServices().put(serviceEntity.getId(), serviceEntity);

    BranchService branchService = mock(BranchService.class);
    when(branchService.getBranch("b1")).thenReturn(branch);

    VisitService service = new VisitService();
    service.branchService = branchService;
    service.eventService = mock(EventService.class);

    Visit result = service.addService("b1", "sp1", "s1");
    assertEquals(1, result.getUnservedServices().size());
    assertSame(serviceEntity, result.getUnservedServices().get(0));
    verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
  }

  @DisplayName("Выбрасывает исключение, если услуга не найдена")
  @Test
  void throwsWhenServiceMissing() {
    Branch branch = new Branch("b1", "Branch");
    ServicePoint sp = new ServicePoint("sp1", "SP1");
    Visit visit = Visit.builder().id("v1").unservedServices(new ArrayList<>()).build();
    sp.setVisit(visit);
    branch.getServicePoints().put(sp.getId(), sp);

    BranchService branchService = mock(BranchService.class);
    when(branchService.getBranch("b1")).thenReturn(branch);

    EventService eventService = mock(EventService.class);

    VisitService service = new VisitService();
    service.branchService = branchService;
    service.eventService = eventService;

    assertThrows(HttpStatusException.class, () -> service.addService("b1", "sp1", "missing"));
    verify(eventService).send(eq("*"), eq(false), any());
  }
}
