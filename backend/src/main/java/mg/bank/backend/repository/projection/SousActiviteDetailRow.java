package mg.bank.backend.repository.projection;

import java.time.LocalDate;

/**
 * Projection d une sous-activite.
 *
 * Volontairement sans avancement : celui-ci vit dans AvancementDetailRow, qui
 * est lu en une seule requete pour TOUTES les sous-activites de l activite.
 * L imbriquer ici obligerait soit a regrouper les deux lectures par identifiant
 * de sous-activite, soit a refaire une requete par sous-activite, ce qui
 * ramenerait le N+1 que le detail evite justement.
 */
public interface SousActiviteDetailRow {

    Integer getId();

    String getCode();

    String getDesignation();

    LocalDate getDateDebutPrevue();

    LocalDate getDateFinPrevue();

    LocalDate getDateDebutReelle();

    LocalDate getDateFinReelle();

}
