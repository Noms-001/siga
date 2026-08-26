package mg.bank.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departement")
    private Integer idDepartement;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "description")
    private String description;
}