package mg.bank.backend.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "nom", nullable = false)
    private String libelle;

    @Column(name = "effectif_prevu", nullable = false)
    private Integer effectifPrevu;

    @Column(name = "effectif_reel")
    private Integer effectifReel;

    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "date_desactivation")
    private LocalDateTime dateDesactivation;

    /**
     * Utilisateurs affectés à ce poste.
     *
     * La table de liaison poste_utilisateur est définie
     * côté Utilisateur.
     */
    @ManyToMany(mappedBy = "postes", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Utilisateur> utilisateurs = new HashSet<>();

    /**
     * Indique si le poste est un poste métier.
     */
    @Column(
        name = "is_metier",
        nullable = false,
        columnDefinition = "BOOLEAN DEFAULT TRUE"
    )
    private Boolean isMetier;
}