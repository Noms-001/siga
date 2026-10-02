package mg.bank.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * Ce que renvoie la creation ou la modification d une activite.
 *
 * Le statut est renvoye parce que la creation le fixe (BROUILLON) et que la
 * modification ne le change pas : le front peut ainsi confirmer sans
 * relire l historique. Les sous-activites sont renvoyees avec leur
 * identifiant, generes a la creation, pour que le front puisse faire le
 * lien entre une ligne du formulaire et sa ligne en base.
 */
@Getter
@Builder
public class ActiviteEcritureResponse {

    private Integer idActivite;

    private String code;

    private String reference;

    private String designation;

    /**
     * Toujours BROUILLON : cree comme brouillon, ou modifie sans changer
     * d etat.
     */
    private String statut;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private LocalDateTime dateEnregistrement;

    private Integer nombreSousActivites;

    /**
     * Nombre de resultats intermediaires enregistres, comme
     * nombreSousActivites.
     *
     * Ajoute pour que le message de confirmation puisse dire ce qui a ete
     * enregistre, plutot que d'annoncer une reussite sans dire quoi. Reste
     * nul tant que le backend ne le renseigne pas.
     */
    private Integer nombreResultatsIntermediaires;

}
