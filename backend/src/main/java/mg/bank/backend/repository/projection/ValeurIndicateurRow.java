package mg.bank.backend.repository.projection;

import java.time.LocalDateTime;

/**
 * Projection d une mesure d indicateur.
 *
 * Comme pour l historique et l avancement, la designation de la valeur
 * courante se deduit de l ordre decroissant des periodes, pose par le service
 * sur la premiere ligne de chaque indicateur.
 */
public interface ValeurIndicateurRow {

    Integer getId();

    /** Cle de regroupement, pas une donnee affichee. */
    Integer getIdIndicateur();

    Double getValeur();

    LocalDateTime getPeriodeDebut();

    LocalDateTime getPeriodeFin();

    String getCommentaire();

    LocalDateTime getDateSaisie();

    Integer getUtilisateurId();

    String getUtilisateurNom();

    String getUtilisateurPrenom();

}
