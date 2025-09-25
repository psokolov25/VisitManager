package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.VisitService;

/**
 * Модульные проверки для {@link Branch}.
 */
class BranchTest {

    @DisplayName("проверяется сценарий «increment ticket counter returns new value»")
    @Test
    void incrementTicketCounterReturnsNewValue() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        branch.getQueues().put("q1", queue);

        int result = branch.incrementTicketCounter(queue);

        assertEquals(1, result);
        assertEquals(1, queue.getTicketCounter());
    }

    @DisplayName("проверяется сценарий «increment ticket counter returns minus one for foreign queue»")
    @Test
    void incrementTicketCounterReturnsMinusOneForForeignQueue() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "Q", 1);

        int result = branch.incrementTicketCounter(queue);

        assertEquals(-1, result);
    }

    @DisplayName("проверяется сценарий «get all visits collects from users service points and queues»")
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

    @DisplayName("проверяется сценарий «get all visits list collects from users service points and queues»")
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

    @DisplayName("проверяется сценарий «get visits by status filters visits»")
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

    @DisplayName("проверяется сценарий «get visits by status returns empty for missing statuses»")
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

    @DisplayName("проверяется сценарий «get visits by status collects from users service points and queues»")
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

    @DisplayName("проверяется сценарий «последовательное увеличение счётчика талонов при инкременте»")
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

    @DisplayName("проверяется сценарий «список всех визитов содержит дубликаты для общего экземпляра»")
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

    @DisplayName("проверяется сценарий «get all visits returns empty when no entities»")
    @Test
    void getAllVisitsReturnsEmptyWhenNoEntities() {
        Branch branch = new Branch("b1", "Branch");

        Map<String, Visit> visits = branch.getAllVisits();

        assertTrue(visits.isEmpty());
    }

    @DisplayName("проверяется сценарий «get all visits list returns empty when no entities»")
    @Test
    void getAllVisitsListReturnsEmptyWhenNoEntities() {
        Branch branch = new Branch("b1", "Branch");

        List<Visit> visits = branch.getAllVisitsList();

        assertTrue(visits.isEmpty());
    }


    @DisplayName("проверяется сценарий «open service point assigns user and publishes events»")
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

    @DisplayName("проверяется сценарий «open service point throws if busy»")
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


    @DisplayName("проверяется сценарий «open service point adds user without service point»")
    @Test
    void openServicePointAddsUserWithoutServicePoint() throws IOException {
        // Пользователь не указал точку обслуживания
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "СП1");
        branch.getServicePoints().put("sp1", sp);

        User user = new User("u1", null); // servicePointId остаётся null
        EventService eventService = mock(EventService.class);

        branch.openServicePoint(user, eventService);

        // Пользователь добавлен в отделение, но точка обслуживания не занята
        assertSame(user, branch.getUsers().get("u1"));
        assertNull(sp.getUser());

        // События не публиковались
        verifyNoInteractions(eventService);
    }

    @DisplayName("проверяется сценарий «open service point throws if service point not found»")
    @Test
    void openServicePointThrowsIfServicePointNotFound() {
        // Пользователь пытается открыть отсутствующую точку
        Branch branch = new Branch("b1", "Branch");
        User user = new User("u1", null);
        user.setServicePointId("sp1");
        EventService eventService = mock(EventService.class);

        HttpStatusException thrown =
                assertThrows(HttpStatusException.class, () -> branch.openServicePoint(user, eventService));
        assertEquals(HttpStatus.CONFLICT, thrown.getStatus());

        // Отправлено событие бизнес-ошибки
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("BUSINESS_ERROR", captor.getValue().getEventType());

        // Пользователь всё равно сохранён в списке отделения
        assertSame(user, branch.getUsers().get("u1"));
    }

    @DisplayName("проверяется сценарий «close service point sends events and ends visit»")
    @Test
    void closeServicePointSendsEventsAndEndsVisit() {
        Branch branch = spy(new Branch("b1", "B1"));
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setBranchId("b1");
        User user = new User("u1", null);
        sp.setUser(user);

        Visit visit = Visit.builder().id("v1").build();
        visit.setUnservedServices(new ArrayList<>(List.of(new Service("s1", "S1", 0, null))));
        sp.setVisit(visit);
        branch.getServicePoints().put("sp1", sp);

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        visitService.keyCloackClient = keyCloackClient;
        when(visitService.getServicePointHashMap("b1"))
                .thenReturn(new HashMap<>(Map.of("sp1", sp)));
        when(visitService.visitEnd(anyString(), anyString(), anyBoolean(), anyString())).thenReturn(null);
        doNothing().when(branch)
                .updateVisit(any(Visit.class), eq(eventService), anyString(), eq(visitService));

        branch.closeServicePoint("sp1", eventService, visitService, true, true, "coffee", true, "force");

        assertNull(sp.getUser());
        assertTrue(branch.getUsers().containsKey("u1"));
        assertTrue(visit.getUnservedServices().isEmpty());

        verify(keyCloackClient).userLogout("u1", true, "force");
        verify(visitService).visitEnd("b1", "sp1", true, "force");
        verify(branch)
                .updateVisit(eq(visit), eq(eventService), eq("UNSERVED_SERVICES_CANCELED"), eq(visitService));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService, atLeast(3)).send(anyString(), eq(false), captor.capture());
        List<String> types = captor.getAllValues().stream().map(Event::getEventType).toList();
        assertTrue(types.contains("SERVICE_POINT_CLOSING"));
        assertTrue(types.contains("STAFF_START_BREAK"));
        assertTrue(types.contains("SERVICE_POINT_CLOSED"));
    }

    @DisplayName("проверяется сценарий «close service point throws when point missing»")
    @Test
    void closeServicePointThrowsWhenPointMissing() {
        // В отделении нет точки обслуживания с указанным идентификатором
        Branch branch = new Branch("b1", "B1");
        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);

        HttpStatusException thrown =
                assertThrows(
                        HttpStatusException.class,
                        () ->
                                branch.closeServicePoint(
                                        "sp1",
                                        eventService,
                                        visitService,
                                        false,
                                        false,
                                        null,
                                        false,
                                        null));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        verify(eventService)
                .send(eq("*"), eq(false), argThat(event -> "BUSINESS_ERROR".equals(event.getEventType())));
        verifyNoInteractions(visitService);
    }

    @DisplayName("проверяется сценарий «close service point throws when already closed»")
    @Test
    void closeServicePointThrowsWhenAlreadyClosed() {
        // Точка обслуживания существует, но на ней уже нет сотрудника
        Branch branch = new Branch("b1", "B1");
        ServicePoint servicePoint = new ServicePoint("sp1", "СП1");
        branch.getServicePoints().put("sp1", servicePoint);

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        when(visitService.getServicePointHashMap("b1"))
                .thenReturn(new HashMap<>(Map.of("sp1", servicePoint)));

        HttpStatusException thrown =
                assertThrows(
                        HttpStatusException.class,
                        () ->
                                branch.closeServicePoint(
                                        "sp1",
                                        eventService,
                                        visitService,
                                        false,
                                        false,
                                        null,
                                        false,
                                        null));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatus());
        verify(eventService)
                .send(eq("*"), eq(false), argThat(event -> "BUSINESS_ERROR".equals(event.getEventType())));
        verify(visitService, times(1)).getServicePointHashMap("b1");
    }

    @DisplayName("проверяется сценарий «add update service refreshes visits when check enabled»")
    @Test
    void addUpdateServiceRefreshesVisitsWhenCheckEnabled() {
        // Обновляем услугу, которая используется в активном визите
        Branch branch = spy(new Branch("b1", "B1"));
        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);

        Service existing = new Service("s1", "Старая услуга", 10, "q1");
        branch.getServices().put("s1", existing);

        Service updated = new Service("s1", "Новая услуга", 20, "q2");

        Visit visit =
                Visit.builder()
                        .id("v1")
                        .currentService(existing.clone())
                        .unservedServices(new ArrayList<>(List.of(existing.clone())))
                        .servedServices(new ArrayList<>(List.of(existing.clone())))
                        .build();

        ServicePoint servicePoint = new ServicePoint("sp1", "СП1");
        servicePoint.setVisit(visit);
        branch.getServicePoints().put("sp1", servicePoint);

        doNothing().when(branch)
                .updateVisit(eq(visit), eq(eventService), eq("UPDATE_SERVICE"), eq(visitService));

        branch.addUpdateService(
                new HashMap<>(Map.of("s1", updated)), eventService, true, visitService);

        assertEquals("Новая услуга", branch.getServices().get("s1").getName());
        assertEquals("Новая услуга", visit.getCurrentService().getName());
        assertEquals("Новая услуга", visit.getUnservedServices().get(0).getName());
        assertEquals("Новая услуга", visit.getServedServices().get(0).getName());

        verify(branch)
                .updateVisit(eq(visit), eq(eventService), eq("UPDATE_SERVICE"), eq(visitService));
        verify(eventService)
                .sendChangedEvent(
                        eq("config"),
                        eq(false),
                        same(existing),
                        argThat(service ->
                                service instanceof Service svc
                                        && svc.getId().equals("s1")
                                        && svc.getName().equals("Новая услуга")),
                        anyMap(),
                        eq("Update service"));
    }

    @DisplayName("проверяется сценарий «add update service fails when check disabled and service in use»")
    @Test
    void addUpdateServiceFailsWhenCheckDisabledAndServiceInUse() {
        // При отключённой проверке визитов обновление должно быть запрещено
        Branch branch = new Branch("b1", "B1");
        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);

        Service existing = new Service("s1", "Старая услуга", 10, "q1");
        branch.getServices().put("s1", existing);

        Visit visit =
                Visit.builder()
                        .id("v1")
                        .currentService(existing)
                        .unservedServices(new ArrayList<>(List.of(existing.clone())))
                        .servedServices(new ArrayList<>(List.of(existing.clone())))
                        .build();

        ServicePoint servicePoint = new ServicePoint("sp1", "СП1");
        servicePoint.setVisit(visit);
        branch.getServicePoints().put("sp1", servicePoint);

        Service updated = new Service("s1", "Новая услуга", 20, "q2");

        HttpStatusException thrown =
                assertThrows(
                        HttpStatusException.class,
                        () ->
                                branch.addUpdateService(
                                        new HashMap<>(Map.of("s1", updated)),
                                        eventService,
                                        false,
                                        visitService));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatus());
        verify(eventService)
                .send(eq("*"), eq(false), argThat(event -> "BUSINESS_ERROR".equals(event.getEventType())));
        assertSame(existing, branch.getServices().get("s1"));
    }

    @DisplayName("проверяется сценарий «delete services cleans visit references»")
    @Test
    void deleteServicesCleansVisitReferences() {
        // Удаляем услугу и проверяем обновление всех связей визита
        Branch branch = spy(new Branch("b1", "B1"));
        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);

        Service serviceToDelete = new Service("s1", "Удаляемая", 10, "q1");
        Service nextService = new Service("s2", "Следующая", 15, "q1");
        branch.getServices().put("s1", serviceToDelete);
        branch.getServices().put("s2", nextService);

        Visit visit =
                Visit.builder()
                        .id("v1")
                        .queueId("q1")
                        .servicePointId("sp1")
                        .currentService(serviceToDelete.clone())
                        .unservedServices(new ArrayList<>(List.of(nextService.clone())))
                        .servedServices(new ArrayList<>(List.of(serviceToDelete.clone())))
                        .build();

        ServicePoint servicePoint = new ServicePoint("sp1", "СП1");
        servicePoint.setVisit(visit);
        branch.getServicePoints().put("sp1", servicePoint);

        doNothing().when(branch)
                .updateVisit(eq(visit), eq(eventService), eq("SERVICE_DELETED"), eq(visitService));

        branch.deleteServices(List.of("s1"), eventService, true, visitService);

        assertEquals("s2", visit.getCurrentService().getId());
        assertTrue(visit.getUnservedServices().isEmpty());
        assertTrue(visit.getServedServices().stream().noneMatch(service -> service.getId().equals("s1")));
        assertFalse(branch.getServices().containsKey("s1"));

        verify(branch)
                .updateVisit(eq(visit), eq(eventService), eq("SERVICE_DELETED"), eq(visitService));
        verify(eventService)
                .sendChangedEvent(
                        eq("config"), eq(false), isNull(), same(serviceToDelete), anyMap(), eq("Delete service"));
    }


    @DisplayName("проверяется сценарий «update visit places entities and sends events»")
    @Test
    void updateVisitPlacesEntitiesAndSendsEvents() {
        // Готовим отделение с очередью и пулом визитов
        Branch branch = new Branch("b1", "Отделение");
        Queue queue = new Queue("q1", "Очередь", "Q", 1);
        Visit existingHead = Visit.builder().id("queue-1").build();
        Visit existingTail = Visit.builder().id("queue-2").build();
        queue.getVisits().addAll(List.of(existingHead, existingTail));
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint mainPoint = new ServicePoint("sp-main", "Основная точка");
        branch.getServicePoints().put(mainPoint.getId(), mainPoint);

        ServicePoint poolPoint = new ServicePoint("sp-pool", "Пул точки");
        User poolPointUser = new User("pool-point", null);
        poolPoint.setUser(poolPointUser);
        poolPoint.getVisits().add(Visit.builder().id("pool-old").build());
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        ServicePoint poolUserPoint = new ServicePoint("sp-user", "Пул сотрудника");
        User poolUser = new User("pool-user", null);
        poolUser.getVisits().add(Visit.builder().id("saved").build());
        poolUserPoint.setUser(poolUser);
        branch.getServicePoints().put(poolUserPoint.getId(), poolUserPoint);

        Visit visit =
                Visit.builder()
                        .id("visit-new")
                        .branchId("b1")
                        .queueId("q1")
                        .servicePointId("sp-main")
                        .poolServicePointId("sp-pool")
                        .poolUserId(poolUser.getId())
                        .visitEvents(new ArrayList<>())
                        .events(new ArrayList<>())
                        .servedServices(new ArrayList<>())
                        .unservedServices(new ArrayList<>())
                        .build();

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        branch.updateVisit(visit, eventService, VisitEvent.CALLED, visitService, 1);

        assertEquals("CALLED", visit.getStatus());
        assertEquals(
                List.of("queue-1", "visit-new", "queue-2"),
                queue.getVisits().stream().map(Visit::getId).toList());
        assertSame(visit, mainPoint.getVisit());
        assertTrue(poolPointUser.getVisits().isEmpty());
        assertEquals(
                List.of("pool-old", "visit-new"),
                poolPoint.getVisits().stream().map(Visit::getId).toList());
        assertEquals(2, poolUser.getVisits().size());
        assertEquals("saved", poolUser.getVisits().get(0).getId());
        assertSame(visit, poolUser.getVisits().get(1));
        assertSame(poolUser, branch.getUsers().get(poolUser.getName()));
        assertSame(poolPointUser, branch.getUsers().get(poolPointUser.getName()));

        verify(visitService).addEvent(visit, VisitEvent.CALLED, eventService);
        verify(branchService).add("b1", branch);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        assertEquals("VISIT_CALLED", captor.getValue().getEventType());
        assertEquals("visit-new", ((Visit) captor.getValue().getBody()).getId());

        verify(eventService)
                .send(eq("stat"), eq(false), argThat(event -> "VISIT_CALLED".equals(event.getEventType())));
        verify(eventService)
                .send(eq("frontend"), eq(false), argThat(event -> "VISIT_CALLED".equals(event.getEventType())));
    }

    @DisplayName("проверяется сценарий «update visit replaces existing service point visit»")
    @Test
    void updateVisitReplacesExistingServicePointVisit() {
        // На точке обслуживания уже был другой визит и пользователь
        Branch branch = new Branch("b1", "Отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "СП1");
        Visit previous = Visit.builder().id("existing").build();
        servicePoint.setVisit(previous);
        User staff = new User("staff", null);
        staff.getVisits().add(previous);
        servicePoint.setUser(staff);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getQueues().put("q1", new Queue("q1", "Очередь", "Q", 1));

        Visit visit =
                Visit.builder()
                        .id("new")
                        .branchId("b1")
                        .queueId("q1")
                        .servicePointId("sp1")
                        .visitEvents(new ArrayList<>())
                        .events(new ArrayList<>())
                        .build();

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);
        BranchService branchService = mock(BranchService.class);
        when(visitService.getBranchService()).thenReturn(branchService);

        branch.updateVisit(visit, eventService, VisitEvent.CALLED, visitService, 0);

        assertSame(visit, servicePoint.getVisit());
        assertTrue(staff.getVisits().stream().anyMatch(saved -> "existing".equals(saved.getId())));
        assertTrue(staff.getVisits().stream().noneMatch(saved -> "new".equals(saved.getId())));
        assertEquals(1, branch.getQueues().get("q1").getVisits().size());
        assertSame(visit, branch.getQueues().get("q1").getVisits().get(0));

        verify(visitService).addEvent(visit, VisitEvent.CALLED, eventService);
        verify(branchService).add("b1", branch);
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
    }

    @DisplayName("проверяется сценарий «update visit fails when queue index out of range»")
    @Test
    void updateVisitFailsWhenQueueIndexOutOfRange() {
        // Индекс вставки выходит за пределы очереди
        Branch branch = new Branch("b1", "Отделение");
        Queue queue = new Queue("q1", "Очередь", "Q", 1);
        queue.getVisits().add(Visit.builder().id("keep").build());
        branch.getQueues().put(queue.getId(), queue);

        Visit visit =
                Visit.builder()
                        .id("new")
                        .branchId("b1")
                        .queueId("q1")
                        .visitEvents(new ArrayList<>())
                        .events(new ArrayList<>())
                        .build();

        EventService eventService = mock(EventService.class);
        VisitService visitService = mock(VisitService.class);

        HttpStatusException thrown =
                assertThrows(
                        HttpStatusException.class,
                        () -> branch.updateVisit(visit, eventService, VisitEvent.CALLED, visitService, 5));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatus());
        assertEquals(1, queue.getVisits().size());
        assertEquals("keep", queue.getVisits().get(0).getId());

        verify(visitService).addEvent(visit, VisitEvent.CALLED, eventService);
        verify(eventService)
                .send(eq("*"), eq(false), argThat(event -> "BUSINESS_ERROR".equals(event.getEventType())));
        verify(eventService, never()).send(eq("stat"), anyBoolean(), any(Event.class));
        verify(visitService, never()).getBranchService();
    }

}