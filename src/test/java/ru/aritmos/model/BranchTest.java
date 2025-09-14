package ru.aritmos.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.visit.Visit;

/**
 * Модульные проверки для {@link Branch}.
 */
class BranchTest {

    @Test
    void incrementTicketCounterReturnsNewValue() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        branch.getQueues().put("q1", queue);

        int result = branch.incrementTicketCounter(queue);

        assertEquals(1, result);
        assertEquals(1, queue.getTicketCounter());
    }

    @Test
    void incrementTicketCounterReturnsMinusOneForForeignQueue() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "Q", 1);

        int result = branch.incrementTicketCounter(queue);

        assertEquals(-1, result);
    }

    @Test
    void getAllVisitsCollectsFromUsersServicePointsAndQueues() {
        Branch branch = new Branch("b1", "Branch");

        // Визит пользователя
        Visit visit1 = Visit.builder().id("v1").status("NEW").build();
        User user = new User("u1", null);
        user.getVisits().add(visit1);
        ServicePoint sp1 = new ServicePoint("sp1", "SP1");
        sp1.setUser(user);

        // Текущий визит точки обслуживания
        Visit visit2 = Visit.builder().id("v2").status("NEW").build();
        ServicePoint sp2 = new ServicePoint("sp2", "SP2");
        sp2.setVisit(visit2);

        // Визиты точки обслуживания
        Visit visit3 = Visit.builder().id("v3").status("DONE").build();
        ServicePoint sp3 = new ServicePoint("sp3", "SP3");
        sp3.setVisits(List.of(visit3));

        // Визит очереди
        Visit visit4 = Visit.builder().id("v4").status("NEW").build();
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        queue.getVisits().add(visit4);

        branch.getServicePoints().put("sp1", sp1);
        branch.getServicePoints().put("sp2", sp2);
        branch.getServicePoints().put("sp3", sp3);
        branch.getQueues().put("q1", queue);

        Map<String, Visit> all = branch.getAllVisits();

        assertEquals(4, all.size());
        assertTrue(all.containsKey("v1"));
        assertTrue(all.containsKey("v2"));
        assertTrue(all.containsKey("v3"));
        assertTrue(all.containsKey("v4"));
    }

    @Test
    void getAllVisitsListCollectsFromUsersServicePointsAndQueues() {
        Branch branch = new Branch("b1", "Branch");

        // Визит пользователя
        Visit visit1 = Visit.builder().id("v1").status("NEW").build();
        User user = new User("u1", null);
        user.getVisits().add(visit1);
        ServicePoint sp1 = new ServicePoint("sp1", "SP1");
        sp1.setUser(user);

        // Текущий визит точки обслуживания
        Visit visit2 = Visit.builder().id("v2").status("NEW").build();
        ServicePoint sp2 = new ServicePoint("sp2", "SP2");
        sp2.setVisit(visit2);

        // Визиты точки обслуживания
        Visit visit3 = Visit.builder().id("v3").status("DONE").build();
        ServicePoint sp3 = new ServicePoint("sp3", "SP3");
        sp3.setVisits(List.of(visit3));

        // Визит очереди
        Visit visit4 = Visit.builder().id("v4").status("NEW").build();
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        queue.getVisits().add(visit4);

        branch.getServicePoints().put("sp1", sp1);
        branch.getServicePoints().put("sp2", sp2);
        branch.getServicePoints().put("sp3", sp3);
        branch.getQueues().put("q1", queue);

        List<Visit> all = branch.getAllVisitsList();

        assertEquals(4, all.size());
        assertTrue(all.stream().anyMatch(v -> v.getId().equals("v1")));
        assertTrue(all.stream().anyMatch(v -> v.getId().equals("v2")));
        assertTrue(all.stream().anyMatch(v -> v.getId().equals("v3")));
        assertTrue(all.stream().anyMatch(v -> v.getId().equals("v4")));
    }

    @Test
    void getVisitsByStatusFiltersVisits() {
        Branch branch = new Branch("b1", "Branch");
        Visit v1 = Visit.builder().id("v1").status("NEW").build();
        Visit v2 = Visit.builder().id("v2").status("DONE").build();
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setVisits(List.of(v1, v2));
        branch.getServicePoints().put("sp1", sp);

        Map<String, Visit> filtered = branch.getVisitsByStatus(List.of("NEW"));

        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("v1"));
        assertFalse(filtered.containsKey("v2"));
    }

    @Test
    void getVisitsByStatusReturnsEmptyForMissingStatuses() {
        Branch branch = new Branch("b1", "Branch");
        Visit visit = Visit.builder().id("v1").status("DONE").build();
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setVisits(List.of(visit));
        branch.getServicePoints().put("sp1", sp);

        Map<String, Visit> filtered = branch.getVisitsByStatus(List.of("NEW"));

        assertTrue(filtered.isEmpty());
    }

    @Test
    void getVisitsByStatusCollectsFromUsersServicePointsAndQueues() {
        // Формируем отделение с визитами из разных источников
        Branch branch = new Branch("b1", "Branch");

        // Визит пользователя
        Visit visit1 = Visit.builder().id("v1").status("NEW").build();
        User user = new User("u1", null);
        user.getVisits().add(visit1);
        ServicePoint sp1 = new ServicePoint("sp1", "SP1");
        sp1.setUser(user);

        // Текущий визит точки обслуживания
        Visit visit2 = Visit.builder().id("v2").status("NEW").build();
        ServicePoint sp2 = new ServicePoint("sp2", "SP2");
        sp2.setVisit(visit2);

        // История визитов точки обслуживания
        Visit visit3 = Visit.builder().id("v3").status("NEW").build();
        ServicePoint sp3 = new ServicePoint("sp3", "SP3");
        sp3.setVisits(List.of(visit3));

        // Визиты очереди: один подходящий и один лишний
        Visit visit4 = Visit.builder().id("v4").status("NEW").build();
        Visit visit5 = Visit.builder().id("v5").status("DONE").build();
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        queue.getVisits().addAll(List.of(visit4, visit5));

        branch.getServicePoints().put("sp1", sp1);
        branch.getServicePoints().put("sp2", sp2);
        branch.getServicePoints().put("sp3", sp3);
        branch.getQueues().put("q1", queue);

        // Фильтруем только по статусу NEW
        Map<String, Visit> filtered = branch.getVisitsByStatus(List.of("NEW"));

        // Ожидаем увидеть все визиты со статусом NEW из всех источников
        assertEquals(4, filtered.size());
        assertTrue(filtered.containsKey("v1"));
        assertTrue(filtered.containsKey("v2"));
        assertTrue(filtered.containsKey("v3"));
        assertTrue(filtered.containsKey("v4"));
        assertFalse(filtered.containsKey("v5"));
    }

    @Test
    void incrementTicketCounterIncrementsSequentially() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        branch.getQueues().put("q1", queue);

        int first = branch.incrementTicketCounter(queue);
        int second = branch.incrementTicketCounter(queue);

        assertEquals(1, first);
        assertEquals(2, second);
        assertEquals(2, queue.getTicketCounter());
    }

    @Test
    void getAllVisitsListContainsDuplicatesForSameVisit() {
        // Один и тот же визит встречается у пользователя и как текущий визит точки обслуживания
        Branch branch = new Branch("b1", "Branch");
        Visit visit = Visit.builder().id("v1").status("NEW").build();

        User user = new User("u1", null);
        user.getVisits().add(visit);
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setUser(user);
        sp.setVisit(visit); // тот же экземпляр

        branch.getServicePoints().put("sp1", sp);

        Map<String, Visit> visitMap = branch.getAllVisits();
        List<Visit> visitList = branch.getAllVisitsList();

        assertEquals(1, visitMap.size(), "В карте должен быть один визит по идентификатору");
        assertEquals(2, visitList.size(), "Список содержит дубликаты одного визита");
        assertSame(visitList.get(0), visitList.get(1), "Оба элемента списка указывают на один объект");
    }

    @Test
    void getAllVisitsReturnsEmptyWhenNoEntities() {
        Branch branch = new Branch("b1", "Branch");

        Map<String, Visit> visits = branch.getAllVisits();

        assertTrue(visits.isEmpty());
    }

    @Test
    void getAllVisitsListReturnsEmptyWhenNoEntities() {
        Branch branch = new Branch("b1", "Branch");

        List<Visit> visits = branch.getAllVisitsList();

        assertTrue(visits.isEmpty());
    }

    @Test
    void openServicePointAssignsUserAndPublishesEvents() throws IOException {
        // Готовим отделение с точкой обслуживания без пользователя
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "СП1");
        branch.getServicePoints().put("sp1", sp);

        // Пользователь открывает точку обслуживания
        User user = new User("u1", null);
        user.setServicePointId("sp1");
        EventService eventService = mock(EventService.class);

        branch.openServicePoint(user, eventService);

        // Пользователь назначен на точку обслуживания и сохранён в отделении
        assertSame(user, sp.getUser());
        assertSame(user, branch.getUsers().get("u1"));

        // На шину отправлены события открытия
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("SERVICE_POINT_OPENED", captor.getValue().getEventType());
        assertSame(sp, captor.getValue().getBody());

        captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("stat"), eq(false), captor.capture());
        assertEquals("SERVICE_POINT_OPENED", captor.getValue().getEventType());
        assertSame(sp, captor.getValue().getBody());

        captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("frontend"), eq(false), captor.capture());
        assertEquals("SERVICE_POINT_OPENED", captor.getValue().getEventType());
        assertSame(sp, captor.getValue().getBody());
    }

    @Test
    void openServicePointThrowsIfBusy() {
        // Точка обслуживания уже занята другим пользователем
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "СП1");
        User current = new User("u0", null);
        sp.setUser(current);
        branch.getServicePoints().put("sp1", sp);

        User other = new User("u2", null);
        other.setServicePointId("sp1");
        EventService eventService = mock(EventService.class);

        HttpStatusException thrown =
                assertThrows(HttpStatusException.class, () -> branch.openServicePoint(other, eventService));
        assertEquals(HttpStatus.CONFLICT, thrown.getStatus());

        // Отправлено событие бизнес-ошибки
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());

        // Исходный пользователь остался на точке обслуживания
        assertSame(current, sp.getUser());
        // Новый пользователь присутствует в общем списке, но не назначен
        assertTrue(branch.getUsers().containsKey("u2"));
    }
}
