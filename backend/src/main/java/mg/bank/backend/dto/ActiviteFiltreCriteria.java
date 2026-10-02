package mg.bank.backend.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filtres de la liste, tels que recus du front.
 *
 * Aucun de ces champs n'est une regle de securite : le perimetre impose par
 * le backend est porte par PerimetreUtilisateur et ne peut pas etre fourni
 * par le client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiviteFiltreCriteria {

    /**
     * true = PTA, false = NON PTA, null = les deux.
     */
    private Boolean pta;

    /**
     * Recherche sur code, reference ou designation.
     */
    private String search;

    private Integer objectifSpecifiqueId;

    /**
     * Recherche sur code ou designation d'objectif specifique.
     */
    private String objectifSearch;

    /**
     * Selectionnable uniquement par un utilisateur de niveau departement.
     */
    private Integer serviceId;

    private Integer prioriteId;
    private Integer typeActiviteId;
    private Integer siteId;

    /**
     * Code du statut courant. Utilise par les cards cliquables.
     */
    private String statutCode;

    private Integer annee;

    /**
     * 1 a 4. Null = tous.
     */
    private Integer trimestre;

    /**
     * Borne sur date_debut_prevue.
     */
    private LocalDate dateDebut;

    /**
     * Borne sur date_fin_prevue.
     */
    private LocalDate dateFin;

}
