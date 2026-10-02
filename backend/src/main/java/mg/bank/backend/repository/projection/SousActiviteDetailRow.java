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

    /**
     * Statut courant de l activite mere, pas de la sous-activite : celle-ci
     * n en a pas. Vient de la meme jointure d historique que le detail
     * d activite, et sert a autoriser ou non les ecritures de suivi.
     *
     * Nullable : une activite sans historique n a pas de statut courant.
     */
    String getStatutActivite();

}
