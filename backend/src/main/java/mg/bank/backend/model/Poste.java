package mg.bank.backend.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "poste")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_poste")
    private Integer idPoste;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "type_contrat")
    private String typeContrat;

    @Column(name = "categorie")
    private String categorie;

    @Column(name = "effectif_prevu")
    private Integer effectifPrevu;

    @Column(name = "effectif_reel")
    private Integer effectifReel;

    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_desactivation")
    private LocalDateTime dateDesactivation;
}