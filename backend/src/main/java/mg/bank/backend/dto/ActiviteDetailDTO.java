package mg.bank.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Detail complet d une activite.
 *
 * Regle applicable a toutes les collections : elles ne sont jamais nulles.
 * Une activite sans indicateur expose une liste vide, ce qui laisse le front
 * distinguer "rien a afficher" d "information manquante" sans tester null.
 *
 * Les listes imbriquees portent des references, pas des identifiants : le
 * detail est renvoye en une seule reponse, et le front n a pas a redemander
 * chaque sous-activite, chaque livrable ou chaque fichier.
 */
@Getter
@Builder
@AllArgsConstructor
public class ActiviteDetailDTO {

    private Integer id;

    private String code;

    private String reference;

    private String designation;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private LocalDate dateDebutReelle;

    private LocalDate dateFinReelle;

    /**
     * Premiere entree d historique : l activite ne porte pas sa date de creation.
     */
    private LocalDateTime dateCreation;

    /**
     * Vrai si l activite est rattachee a un objectif specifique.
     *
     * Redondant avec la presence de objectifSpecifique, mais explicite : la
     * liste distingue deja les vues PTA et NON PTA par ce booleen, et une
     * page de detail ne doit pas deduire la notion d un test de nullite.
     */
    private boolean pta;

    private ReferenceDTO objectifSpecifique;

    private ReferenceDTO service;

    private ReferenceDTO typeActivite;

    private ReferenceDTO site;

    private ReferenceDTO priorite;

    /**
     * Statut courant, reconstruit depuis l historique. Null si l historique est
     * vide.
     */
    private ReferenceDTO statut;

    /** Moyenne des derniers avancements des sous-activites, 0 si aucune ne suit. */
    private Double avancement;

    /**
     * Echeance depassee sans statut terminal.
     *
     * Meme definition que la card EN_RETARD de la liste, et pour la meme
     * raison : EN_RETARD n est pas un statut stocke, il se deduit de la date
     * de fin et du statut. Une activite peut donc porter un statut EN_COURS
     * et etre en retard.
     */
    private boolean enRetard;

    private List<SousActiviteDetailDTO> sousActivites;

    private List<IndicateurDetailDTO> indicateurs;

    private List<ResultatIntermediaireDTO> resultatsIntermediaires;

    /** Du plus recent au plus ancien. */
    private List<HistoriqueActiviteDTO> historique;

}
