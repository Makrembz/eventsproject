package tn.fst.eventsproject.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Event implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int idEvent;
    String description;
    LocalDate dateDebut;
    LocalDate dateFin;
    float cout;
    @ManyToMany(mappedBy = "events")
    @JsonIgnore
    Set<Participant> participants;
    @OneToMany(fetch = FetchType.EAGER)
    Set<Logistics> logistics;

}
