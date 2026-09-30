package mg.bank.backend.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Activite du DTS-TSS.
 *
 * Le statut courant n'est PAS porte par cette table : il n'existe que dans
 * l'historique (historique_activite). De même il n'y a pas de date_creation ;
 * la date de creation retenue est celle de la premiere entree d'historique.
 *
 * Ces deux valeurs sont calculees cote requete, voir ActiviteRepository.
 */
@Entity
@Table(name = "activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_activite")
    private Integer idActivite;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "reference", nullable = false)
    private String reference;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "date_debut_prevue", nullable = false)
    private LocalDate dateDebutPrevue;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "date_debut_reelle")
    private LocalDate dateDebutReelle;

    @Column(name = "date_fin_reelle")
    private LocalDate dateFinReelle;

    /**
     * Null = activite NON PTA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_objectif_specifique")
    private ObjectifSpecifique objectifSpecifique;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type_activite")
    private TypeActivite typeActivite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_site")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_priorite", nullable = false)
    private Priorite priorite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false)
    private Service service;

}
