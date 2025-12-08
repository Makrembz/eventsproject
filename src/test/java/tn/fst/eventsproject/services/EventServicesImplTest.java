package tn.fst.eventsproject.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tn.fst.eventsproject.entities.Event;
import tn.fst.eventsproject.entities.Logistics;
import tn.fst.eventsproject.entities.Participant;
import tn.fst.eventsproject.entities.Tache;
import tn.fst.eventsproject.repositories.EventRepository;
import tn.fst.eventsproject.repositories.LogisticsRepository;
import tn.fst.eventsproject.repositories.ParticipantRepository;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link tn.fst.eventsproject.services.EventServicesImpl}.
 *
 * <p>
 * These tests use Mockito to mock the repository dependencies (EventRepository,
 * ParticipantRepository, LogisticsRepository) and verify the business logic of
 * the service layer in isolation. Each test follows a simple Given/When/Then
 * structure mentioned in the method-level comments.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class EventServicesImplTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    ParticipantRepository participantRepository;

    @Mock
    LogisticsRepository logisticsRepository;

    @InjectMocks
    EventServicesImpl eventServices;

    @BeforeEach
    void setUp() {
    }

    @Test
    /**
     * Given: a new Participant without id
     * When: addParticipant is called
     * Then: repository.save(...) should be invoked and the returned participant
     * should contain the id assigned by the repository.
     */
    void addParticipant_delegatesToRepository() {
        Participant p = new Participant();
        p.setNom("Alice");
        when(participantRepository.save(any(Participant.class))).thenAnswer(i -> {
            Participant arg = i.getArgument(0);
            arg.setIdPart(42);
            return arg;
        });

        Participant saved = eventServices.addParticipant(p);
        assertEquals(42, saved.getIdPart());
        assertEquals("Alice", saved.getNom());
        verify(participantRepository).save(any(Participant.class));
    }

    @Test
    /**
     * Given: an existing participant id and a new Event
     * When: addAffectEvenParticipant(event, id) is called
     * Then: the event is linked and saved through the repository
     * and the returned event reflects the saved values.
     */
    void addAffectEvenParticipant_withId_addsEventAndSaves() {
        Participant existing = new Participant();
        existing.setIdPart(7);
        when(participantRepository.findById(7)).thenReturn(Optional.of(existing));

        Event event = new Event();
        event.setDescription("E1");

        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        Event saved = eventServices.addAffectEvenParticipant(event, 7);

        // ensure repository save was called and returned the given event
        assertEquals("E1", saved.getDescription());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    /**
     * Given: participantRepository.findById(...) returns empty
     * When: addAffectEvenParticipant(event, id) is called with a missing id
     * Then: a ResponseStatusException is thrown.
     */
    void addAffectEvenParticipant_withId_participantNotFound_throwsException() {
        when(participantRepository.findById(anyInt())).thenReturn(Optional.empty());

        Event event = new Event();
        event.setDescription("E1");

        assertThrows(ResponseStatusException.class, () -> {
            eventServices.addAffectEvenParticipant(event, 999);
        });
    }

    @Test
    /**
     * Given: a participant that already has events
     * When: adding an event with addAffectEvenParticipant(event, id)
     * Then: the new event is added to the participant's event set and the
     * eventRepository.save(...) is invoked.
     */
    void addAffectEvenParticipant_withId_existingEvents_addsToSet() {
        Participant existing = new Participant();
        existing.setIdPart(7);
        existing.setEvents(new HashSet<>());
        when(participantRepository.findById(7)).thenReturn(Optional.of(existing));

        Event event = new Event();
        event.setDescription("E2");
        event.setParticipants(new HashSet<>());

        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        Event saved = eventServices.addAffectEvenParticipant(event, 7);

        assertEquals("E2", saved.getDescription());
        assertTrue(existing.getEvents().contains(event));
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    /**
     * Given: an Event with no participants (null)
     * When: addAffectEvenParticipant(event) is called
     * Then: the event is saved via eventRepository.
     */
    void addAffectEvenParticipant_withoutId_noParticipants_savesEvent() {
        Event event = new Event();
        event.setDescription("E3");
        event.setParticipants(null);

        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        Event saved = eventServices.addAffectEvenParticipant(event);

        assertEquals("E3", saved.getDescription());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    /**
     * Given: an Event with an empty participants set
     * When: addAffectEvenParticipant(event) is called
     * Then: the event is saved and no exceptions occur.
     */
    void addAffectEvenParticipant_withoutId_emptyParticipants_savesEvent() {
        Event event = new Event();
        event.setDescription("E4");
        event.setParticipants(new HashSet<>());

        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        Event saved = eventServices.addAffectEvenParticipant(event);

        assertEquals("E4", saved.getDescription());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    /**
     * Given: an Event containing participants (by id)
     * When: addAffectEvenParticipant(event) is called
     * Then: repository lookups link the existing participants and the
     * event is saved.
     */
    void addAffectEvenParticipant_withoutId_withParticipants_linksAndSaves() {
        Participant p1 = new Participant();
        p1.setIdPart(1);
        p1.setNom("P1");

        Participant existingP1 = new Participant();
        existingP1.setIdPart(1);
        existingP1.setNom("P1");
        existingP1.setEvents(null);

        Set<Participant> participants = new HashSet<>();
        participants.add(p1);

        Event event = new Event();
        event.setDescription("E5");
        event.setParticipants(participants);

        when(participantRepository.findById(1)).thenReturn(Optional.of(existingP1));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        Event saved = eventServices.addAffectEvenParticipant(event);

        assertEquals("E5", saved.getDescription());
        assertNotNull(existingP1.getEvents());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    /**
     * Given: a participant who already has events
     * When: addAffectEvenParticipant(event) uses that participant
     * Then: the participant's events set contains the new event.
     */
    void addAffectEvenParticipant_withoutId_participantWithExistingEvents_addsToSet() {
        Participant p1 = new Participant();
        p1.setIdPart(1);

        Participant existingP1 = new Participant();
        existingP1.setIdPart(1);
        existingP1.setEvents(new HashSet<>());

        Set<Participant> participants = new HashSet<>();
        participants.add(p1);

        Event event = new Event();
        event.setDescription("E6");
        event.setParticipants(participants);

        when(participantRepository.findById(1)).thenReturn(Optional.of(existingP1));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        Event saved = eventServices.addAffectEvenParticipant(event);

        assertEquals("E6", saved.getDescription());
        assertTrue(existingP1.getEvents().contains(event));
    }

    @Test
    /**
     * Given: event participants reference a non-existent participant id
     * When: addAffectEvenParticipant(event) is called
     * Then: a ResponseStatusException is thrown to indicate the missing
     * participant.
     */
    void addAffectEvenParticipant_withoutId_participantNotFound_throwsException() {
        Participant p1 = new Participant();
        p1.setIdPart(999);

        Set<Participant> participants = new HashSet<>();
        participants.add(p1);

        Event event = new Event();
        event.setDescription("E7");
        event.setParticipants(participants);

        when(participantRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            eventServices.addAffectEvenParticipant(event);
        });
    }

    @Test
    /**
     * Given: an Event found by description and a new Logistics
     * When: addAffectLog(logistics, description) is called
     * Then: logisticsRepository.save(...) and eventRepository.save(...) are
     * invoked and the event contains the saved logistics.
     */
    void addAffectLog_eventFound_savesAndLinksLogistics() {
        Event event = new Event();
        event.setDescription("TestEvent");
        event.setLogistics(null);

        Logistics logistics = new Logistics();
        logistics.setDescription("Projector");

        Logistics savedLogistics = new Logistics();
        savedLogistics.setIdLog(1);
        savedLogistics.setDescription("Projector");

        when(eventRepository.findFirstByDescription("TestEvent")).thenReturn(event);
        when(logisticsRepository.save(any(Logistics.class))).thenReturn(savedLogistics);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        Logistics result = eventServices.addAffectLog(logistics, "TestEvent");

        assertEquals("Projector", result.getDescription());
        assertNotNull(event.getLogistics());
        assertTrue(event.getLogistics().contains(savedLogistics));
        verify(logisticsRepository).save(logistics);
        verify(eventRepository).save(event);
    }

    @Test
    /**
     * Given: an Event that already has logistics in its set
     * When: addAffectLog(newLogistics, description) is called
     * Then: the new logistics is added to the event's set and the size
     * reflects the addition.
     */
    void addAffectLog_eventWithExistingLogistics_addsToSet() {
        Logistics existingLogistics = new Logistics();
        existingLogistics.setIdLog(1);

        Set<Logistics> logisticsSet = new HashSet<>();
        logisticsSet.add(existingLogistics);

        Event event = new Event();
        event.setDescription("TestEvent");
        event.setLogistics(logisticsSet);

        Logistics newLogistics = new Logistics();
        newLogistics.setDescription("Speaker");

        Logistics savedLogistics = new Logistics();
        savedLogistics.setIdLog(2);
        savedLogistics.setDescription("Speaker");

        when(eventRepository.findFirstByDescription("TestEvent")).thenReturn(event);
        when(logisticsRepository.save(any(Logistics.class))).thenReturn(savedLogistics);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        Logistics result = eventServices.addAffectLog(newLogistics, "TestEvent");

        assertEquals("Speaker", result.getDescription());
        assertEquals(2, event.getLogistics().size());
    }

    @Test
    /**
     * Given: eventRepository.findFirstByDescription returns null
     * When: addAffectLog(logistics, description) is called
     * Then: a ResponseStatusException is thrown.
     */
    void addAffectLog_eventNotFound_throwsException() {
        when(eventRepository.findFirstByDescription("NonExistent")).thenReturn(null);

        Logistics logistics = new Logistics();

        assertThrows(ResponseStatusException.class, () -> {
            eventServices.addAffectLog(logistics, "NonExistent");
        });
    }

    @Test
    /**
     * Given: no events in the date range
     * When: getLogisticsDates(start, end) is called
     * Then: an empty list is returned.
     */
    void getLogisticsDates_noEvents_returnsEmptyList() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);

        when(eventRepository.findByDateDebutBetween(start, end)).thenReturn(new ArrayList<>());

        List<Logistics> result = eventServices.getLogisticsDates(start, end);

        assertTrue(result.isEmpty());
    }

    @Test
    /**
     * Given: events with null logistics
     * When: getLogisticsDates(start, end) is called
     * Then: an empty list is returned.
     */
    void getLogisticsDates_eventsWithNoLogistics_returnsEmptyList() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);

        Event event = new Event();
        event.setLogistics(null);

        when(eventRepository.findByDateDebutBetween(start, end)).thenReturn(List.of(event));

        List<Logistics> result = eventServices.getLogisticsDates(start, end);

        assertTrue(result.isEmpty());
    }

    @Test
    /**
     * Given: events with empty logistics sets
     * When: getLogisticsDates(start, end) is called
     * Then: an empty list is returned.
     */
    void getLogisticsDates_eventsWithEmptyLogistics_returnsEmptyList() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);

        Event event = new Event();
        event.setLogistics(new HashSet<>());

        when(eventRepository.findByDateDebutBetween(start, end)).thenReturn(List.of(event));

        List<Logistics> result = eventServices.getLogisticsDates(start, end);

        assertTrue(result.isEmpty());
    }

    @Test
    /**
     * Given: events with both reserved and not-reserved logistics
     * When: getLogisticsDates(start, end) is called
     * Then: only reserved logistics are returned.
     */
    void getLogisticsDates_eventsWithReservedLogistics_returnsOnlyReserved() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);

        Logistics reserved = new Logistics();
        reserved.setIdLog(1);
        reserved.setReserve(true);

        Logistics notReserved = new Logistics();
        notReserved.setIdLog(2);
        notReserved.setReserve(false);

        Set<Logistics> logisticsSet = new HashSet<>();
        logisticsSet.add(reserved);
        logisticsSet.add(notReserved);

        Event event = new Event();
        event.setLogistics(logisticsSet);

        when(eventRepository.findByDateDebutBetween(start, end)).thenReturn(List.of(event));

        List<Logistics> result = eventServices.getLogisticsDates(start, end);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isReserve());
    }

    @Test
    /**
     * Given: no events matching the selector
     * When: calculCout() is called
     * Then: no save operations should be invoked on the repository.
     */
    void calculCout_noEvents_doesNothing() {
        when(eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache(
                "Tounsi", "Ahmed", Tache.ORGANISATEUR)).thenReturn(new ArrayList<>());

        eventServices.calculCout();

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    /**
     * Given: an event with null logistics
     * When: calculCout() is called
     * Then: the event's cost is set to 0 and eventRepository.save(...) is
     * called.
     */
    void calculCout_eventWithNoLogistics_setsCoutToZero() {
        Event event = new Event();
        event.setDescription("Test");
        event.setLogistics(null);

        when(eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache(
                "Tounsi", "Ahmed", Tache.ORGANISATEUR)).thenReturn(List.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        eventServices.calculCout();

        assertEquals(0f, event.getCout());
        verify(eventRepository).save(event);
    }

    @Test
    /**
     * Given: an event with reserved and non-reserved logistics
     * When: calculCout() is called
     * Then: only reserved logistics contribute to the event cost and the
     * result is saved.
     */
    void calculCout_eventWithReservedLogistics_calculatesCost() {
        Logistics reserved = new Logistics();
        reserved.setReserve(true);
        reserved.setPrixUnit(10.0f);
        reserved.setQuantite(5);

        Logistics notReserved = new Logistics();
        notReserved.setReserve(false);
        notReserved.setPrixUnit(20.0f);
        notReserved.setQuantite(3);

        Set<Logistics> logisticsSet = new HashSet<>();
        logisticsSet.add(reserved);
        logisticsSet.add(notReserved);

        Event event = new Event();
        event.setDescription("Test");
        event.setLogistics(logisticsSet);

        when(eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache(
                "Tounsi", "Ahmed", Tache.ORGANISATEUR)).thenReturn(List.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        eventServices.calculCout();

        assertEquals(50f, event.getCout()); // 10 * 5 = 50, not reserved is skipped
        verify(eventRepository).save(event);
    }

}
