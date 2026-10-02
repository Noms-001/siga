package mg.bank.backend.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Ligne de sous-activite pre-remplie dans le formulaire de modification.
 *
 * Meme contour que SousActiviteEcritureRequest, plus l identifiant et les
 * dates reelles : les dates reelles sont renvoyees pour que le front puisse
 * afficher qu une sous-activite executee n a pas de date de fin prevue
 * modifiable sans effet visible, mais le backend ne les ecrase pas lors d
 * une relecture.
 */
@Getter
@Builder
public class SousActiviteFormulaireDTO {

    private Integer idSousActivite;

    private String code;

    private String designation;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private LocalDate dateDebutReelle;

    private LocalDate dateFinReelle;

    /**
     * Livrables deja declares pour cette sous-activite.
     *
     * Renvoyes pour que le formulaire de modification les propose : sans eux,
     * toute relecture d un brouillon effacerait ses livrables a
     * l'enregistrement suivant.
     *
     * `fichiersDeposes` n'est pas un simple affichage : c'est ce qui evite de
     * proposer la suppression d'un livrable que le backend refuserait.
     */
    private List<LivrableFormulaireDTO> livrables;

}
