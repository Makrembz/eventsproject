package tn.fst.eventsproject.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import tn.fst.eventsproject.entities.Event;
import tn.fst.eventsproject.entities.Logistics;
import tn.fst.eventsproject.entities.Participant;
import tn.fst.eventsproject.services.IEventServices;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("event")
@RestController
public class EventRestController {
    private final IEventServices eventServices;

    @PostMapping("/addPart")
    @Operation(summary = "Add a participant", description = "Create a new participant. Example payload shown in the request body.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Participant.class), examples = @ExampleObject(value = "{\"nom\":\"Doe\",\"prenom\":\"John\",\"tache\":\"INVITE\"}")))
    public Participant addParticipant(@RequestBody Participant participant) {
        return eventServices.addParticipant(participant);
    }

    @PostMapping("/addEvent/{id}")
    @Operation(summary = "Add an event and attach to a participant", description = "Create an event and link it to an existing participant by id.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class), examples = @ExampleObject(value = "{\"description\":\"Workshop\",\"dateDebut\":\"2025-12-15\",\"dateFin\":\"2025-12-16\",\"cout\":500.0}")))
    public Event addEventPart(@RequestBody Event event, @PathVariable("id") int idPart) {
        return eventServices.addAffectEvenParticipant(event, idPart);
    }

    @PostMapping("/addEvent")
    @Operation(summary = "Add an event", description = "Create a new event. Dates must be in yyyy-MM-dd format.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class), examples = @ExampleObject(value = "{\"description\":\"Workshop\",\"dateDebut\":\"2025-12-15\",\"dateFin\":\"2025-12-16\",\"cout\":500.0}")))
    public Event addEvent(@RequestBody Event event) {
        return eventServices.addAffectEvenParticipant(event);
    }

    @PutMapping("/addAffectLog/{description}")
    @Operation(summary = "Add logistics to an event", description = "Attach a logistics item to an event identified by its description.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Logistics.class), examples = @ExampleObject(value = "{\"description\":\"Projector\",\"reserve\":true,\"prixUnit\":75.0,\"quantite\":1}")))
    public Logistics addAffectLog(@RequestBody Logistics logistics,
            @PathVariable("description") String descriptionEvent) {
        return eventServices.addAffectLog(logistics, descriptionEvent);
    }

    @GetMapping("/getLogs/{d1}/{d2}")
    public List<Logistics> getLogistiquesDates(@PathVariable("d1") LocalDate dateDebut,
            @PathVariable("d2") LocalDate dateFin) {
        return eventServices.getLogisticsDates(dateDebut, dateFin);
    }
}
