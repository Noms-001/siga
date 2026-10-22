package mg.bank.backend.dto.importactivite;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.bank.backend.enums.TypeImport;

/**
 * Racine du JSON d'import des activites.
 *
 * <p>Le service et la procedure sont portes au niveau de la requete : toutes
 * les activites du lot seront rattachees a ceux-ci. L'annee est egalement
 * portee au niveau du lot : un import correspond a un exercice (une annee),
 * pas a une date d'activite. C'est cette annee qui servira a creer les
 * objectifs specifiques absents.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiviteImportRequest {

    private String version;

    /** Identifiant du service (id_service). */
    private Integer service;
    
    private Integer procedure;

    private Integer annee;

    /** "PTA" ou "NON_PTA". */
    private TypeImport typeImport;

    /** true = statut initial NON_COMMENCE, false = BROUILLON. */
    private Boolean activitesDejaValidees;

    private List<ActiviteImportDTO> activites;
}