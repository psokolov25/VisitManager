package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Mark;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

class VisitServiceNoteTest {

    @DisplayName("Добавление заметки создаёт запись и инициирует обновление визита")
    @Test
    void addNoteAppendsNoteAndCallsUpdate() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        User user = new User("u1", "User", null);
        sp.setUser(user);
        Service service = new Service();
        service.setId("s1");
        Visit visit = Visit.builder()
                .id("v1")
                .currentService(service)
                .visitNotes(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService serviceBean = new VisitService();
        serviceBean.branchService = branchService;
        serviceBean.eventService = mock(EventService.class);

        Visit result = serviceBean.addNote("b1", "sp1", "text");
        assertEquals(1, result.getVisitNotes().size());
        Mark note = result.getVisitNotes().get(0);
        assertEquals("text", note.getValue());
        assertEquals("u1", note.getAuthor().getId());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(serviceBean));
    }

    @DisplayName("Добавление заметки выбрасывает исключение при отсутствии визита")
    @Test
    void addNoteThrowsWhenNoVisit() {
        Branch branch = new Branch("b1", "Branch");
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP1"));

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        EventService eventService = mock(EventService.class);

        VisitService serviceBean = new VisitService();
        serviceBean.branchService = branchService;
        serviceBean.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> serviceBean.addNote("b1", "sp1", "text"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @DisplayName("Чтение заметок возвращает сохранённый список визита")
    @Test
    void getNotesReturnsNotes() {
        Branch branch = new Branch("b1", "Branch");
        Mark note = new Mark();
        note.setId("m1");
        note.setValue("note");
        note.setMarkDate(ZonedDateTime.now());
        Visit visit = Visit.builder()
                .id("v1")
                .visitNotes(new ArrayList<>(List.of(note)))
                .build();
        User user = new User("u1", "User", null);
        user.setVisits(new ArrayList<>(List.of(visit)));
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setUser(user);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService serviceBean = new VisitService();
        serviceBean.branchService = branchService;
        serviceBean.eventService = mock(EventService.class);

        List<Mark> notes = serviceBean.getNotes("b1", "v1");
        assertEquals(1, notes.size());
        assertEquals("note", notes.get(0).getValue());
    }

    @DisplayName("Чтение заметок выбрасывает исключение при отсутствии визита")
    @Test
    void getNotesThrowsWhenVisitMissing() {
        Branch branch = new Branch("b1", "Branch");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        EventService eventService = mock(EventService.class);

        VisitService serviceBean = new VisitService();
        serviceBean.branchService = branchService;
        serviceBean.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> serviceBean.getNotes("b1", "v1"));
        verify(eventService).send(eq("*"), eq(false), any());
    }
}

