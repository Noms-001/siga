package mg.bank.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ligne de la liste des activites.
 *
 * Optimise pour l'affichage : uniquement les donnees necessaires, avec les
 * relations aplaties en objets de reference. Le statut et l'avancement sont
 * calcules en base (voir ActiviteRepository) et non laisses au front.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiviteListItemDTO {

    private Integer id;
    private String code;
    private String reference;
    private String designation;

    private LocalDate dateDebutPrevue;
    private LocalDate dateFinPrevue;
    private LocalDate dateDebutReelle;
    private LocalDate dateFinReelle;

    /**
     * Date de creation retenue pour le tri par recurrence.
     * = premiere entree d'historique de l'activite.
     */
    private LocalDateTime dateCreation;

    /**
     * Objectif specifique : null pour une activite NON PTA.
     */
    private ReferenceDTO objectifSpecifique;

    private ReferenceDTO service;
    private ReferenceDTO typeActivite;
    private ReferenceDTO site;
    private ReferenceDTO priorite;

    /**
     * Statut courant, issu de la derniere entree d'historique.
     * Null si l'activite n'a encore aucune entree d'historique.
     */
    private ReferenceDTO statut;

    /**
     * Pourcentage 0-100, moyenne du dernier avancement de chaque
     * sous-activite. 0 si l'activite n'a pas de sous-activite.
     */
    private Double avancement;

}
