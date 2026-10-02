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
 * Sous-activite d une activite.
 *
 * Cette table n etait lue qu en SQL natif, via une projection
 * (SousActiviteDetailRow). Aucun mapping JPA n existait, parce que rien ne
 * l ecrivait. Le formulaire de creation et de modification d une activite
 * devant pouvoir Created / Updated / Deleted ses sous-activites dans la
 * meme transaction que l activite mere, le mapping est ajoute ici plutot
 * qu un INSERT/UPDATE en SQL brut : la transaction, le flush et la gestion
 * des ids restent ceux de JPA, comme pour Activite.
 *
 * CE QUI N EST PAS DANS LE MODELE, CONTRAIREMENT A L ATTENTE
 *
 * - pas de description : la colonne n existe pas dans schema.sql ;
 * - pas de responsable : une affectation passe par
 *   affectation_sous_activite, qui est un module a part entier (role,
 *   utilisateur, date de desaffectation). L exposer ici reviendrait a
 *   inventer une donnee qui n a pas de colonne ;
 * - pas de statut propre : le suivi d une sous-activite passe par
 *   avancement_sous_activite, pas par un statut porte par la ligne.
 *
 * date_fin_prevue est NOT NULL ici, alors qu elle est nullable sur
 * activite : une sous-activite a une fin, une activite peut ne pas en avoir.
 */
@Entity
@Table(name = "sous_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SousActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sous_activite")
    private Integer idSousActivite;

    /**
     * UNIQUE global (pas seulement par activite), VARCHAR(20).
     */
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "date_debut_prevue", nullable = false)
    private LocalDate dateDebutPrevue;

    @Column(name = "date_fin_prevue", nullable = false)
    private LocalDate dateFinPrevue;

    /**
     * Saisies par le suivi, pas par le formulaire de redaction. Exposees ici
     * uniquement parce que la table les porte, et remises a leur valeur
     * existante lors d une modification : une redaction ne doit pas ecraser
     * une execution.
     */
    @Column(name = "date_debut_reelle")
    private LocalDate dateDebutReelle;

    @Column(name = "date_fin_reelle")
    private LocalDate dateFinReelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_activite", nullable = false)
    private Activite activite;

}
