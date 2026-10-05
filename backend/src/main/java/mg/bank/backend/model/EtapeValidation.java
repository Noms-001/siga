package mg.bank.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "etape_validation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapeValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etape_validation")
    private Integer idEtapeValidation;

    @Column(name = "designation", nullable = false, length = 255)
    private String designation;

    @Column(name = "description")
    private String description;

    @Column(name = "niveau", nullable = false)
    private Integer niveau;

    @Column(name = "obligatoire", nullable = false)
    private Boolean obligatoire;

    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @Column(name = "retour", nullable = false)
    private Boolean retour;

    @Column(name = "date_desactivation")
    private LocalDateTime dateDesactivation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_procedure", nullable = false)
    private Procedure procedure;

    @ManyToMany
    @JoinTable(
        name = "etape_validation_decideur",
        joinColumns = @JoinColumn(name = "id_etape_validation"),
        inverseJoinColumns = @JoinColumn(name = "id_poste")
    )
    @Builder.Default
    private List<Poste> postesDecideurs = new ArrayList<>();
}
