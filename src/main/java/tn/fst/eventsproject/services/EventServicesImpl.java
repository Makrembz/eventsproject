package tn.fst.eventsproject.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.fst.eventsproject.entities.Event;
import tn.fst.eventsproject.entities.Logistics;
import tn.fst.eventsproject.entities.Participant;
import tn.fst.eventsproject.entities.Tache;
import tn.fst.eventsproject.repositories.EventRepository;
import tn.fst.eventsproject.repositories.LogisticsRepository;
import tn.fst.eventsproject.repositories.ParticipantRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventServicesImpl implements IEventServices {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final LogisticsRepository logisticsRepository;

    @Override
    public Participant addParticipant(Participant participant) {
        return participantRepository.save(participant);
    }

    @Override
    public Event addAffectEvenParticipant(Event event, int idParticipant) {
        Participant participant = participantRepository.findById(idParticipant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Participant not found: %d", idParticipant)));

        if (participant.getEvents() == null) {
            Set<Event> events = new HashSet<>();
            events.add(event);
            participant.setEvents(events);
        } else {
            participant.getEvents().add(event);
        }

        // Ensure bidirectional relation if needed
        if (event.getParticipants() == null) {
            Set<Participant> parts = new HashSet<>();
            parts.add(participant);
            event.setParticipants(parts);
        } else {
            event.getParticipants().add(participant);
        }

        return eventRepository.save(event);
    }

    @Override
    public Event addAffectEvenParticipant(Event event) {
        Set<Participant> participants = event.getParticipants();
        if (participants == null || participants.isEmpty()) {
            // nothing to link, just save event
            return eventRepository.save(event);
        }

        for (Participant aParticipant : participants) {
            Participant participant = participantRepository.findById(aParticipant.getIdPart())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            String.format("Participant not found: %d", aParticipant.getIdPart())));

            if (participant.getEvents() == null) {
                Set<Event> events = new HashSet<>();
                events.add(event);
                participant.setEvents(events);
            } else {
                participant.getEvents().add(event);
            }
        }
        return eventRepository.save(event);
    }

    @Override
    public Logistics addAffectLog(Logistics logistics, String descriptionEvent) {
        Event event = eventRepository.findFirstByDescription(descriptionEvent);
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Event not found with description: %s", descriptionEvent));
        }

        // Save logistics first to avoid TransientObjectException, then associate to
        // event
        Logistics saved = logisticsRepository.save(logistics);

        if (event.getLogistics() == null) {
            Set<Logistics> logisticsSet = new HashSet<>();
            logisticsSet.add(saved);
            event.setLogistics(logisticsSet);
        } else {
            event.getLogistics().add(saved);
        }

        eventRepository.save(event);
        return saved;
    }

    @Override
    public List<Logistics> getLogisticsDates(LocalDate date_debut, LocalDate date_fin) {
        List<Event> events = eventRepository.findByDateDebutBetween(date_debut, date_fin);

        List<Logistics> logisticsList = new ArrayList<>();
        for (Event event : events) {
            if (event.getLogistics() == null || event.getLogistics().isEmpty()) {
                continue;
            }
            Set<Logistics> logisticsSet = event.getLogistics();
            for (Logistics logistics : logisticsSet) {
                if (logistics.isReserve()) {
                    logisticsList.add(logistics);
                }
            }
        }
        return logisticsList;
    }

    @Scheduled(cron = "*/60 * * * * *")
    @Override
    public void calculCout() {
        List<Event> events = eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache("Tounsi",
                "Ahmed", Tache.ORGANISATEUR);
        // eventRepository.findAll();
        float somme = 0f;
        for (Event event : events) {
            log.info(event.getDescription());
            Set<Logistics> logisticsSet = event.getLogistics();
            if (logisticsSet != null) {
                for (Logistics logistics : logisticsSet) {
                    if (logistics.isReserve()) {
                        somme += logistics.getPrixUnit() * logistics.getQuantite();
                    }
                }
            }
            event.setCout(somme);
            eventRepository.save(event);
            log.info("Cout de l'Event {} est {}", event.getDescription(), somme);

        }
    }

}
