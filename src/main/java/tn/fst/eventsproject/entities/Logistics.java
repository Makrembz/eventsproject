package tn.fst.eventsproject.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Logistics implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int idLog;
    String description;
    boolean reserve;
    float prixUnit;
    int quantite;

}
