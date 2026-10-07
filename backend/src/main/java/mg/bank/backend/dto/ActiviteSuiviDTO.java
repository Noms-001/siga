package mg.bank.backend.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Activité telle qu'affichée dans Suivi.vue.
 *
 * Réutilise ReferenceDTO pour service / priorité / statut : même forme que
 * la liste standard, pour que le front n'ait pas deux conventions à gérer.
 *
 * Le champ `avancement` vient déjà calculé par la requête (AVG des derniers
 * avancements de sous-activités) — pas de recalcul côté front.
 */
@Getter
@Builder
public class ActiviteSuiviDTO {
    private Integer id;
    private String code;
    private String reference;
    private String designation;
    private LocalDate dateDebutPrevue;
    private LocalDate dateFinPrevue;
    private LocalDate dateDebutReelle;
    private LocalDate dateFinReelle;
    private ReferenceDTO service;
    private ReferenceDTO priorite;
    private ReferenceDTO statut;
    private Double avancement;

    /** Premier utilisateur affecté actif sur les sous-activités, ou null. */
    private ResponsableSuiviDTO responsable;

    private List<SousActiviteSuiviDTO> sousActivites;
}