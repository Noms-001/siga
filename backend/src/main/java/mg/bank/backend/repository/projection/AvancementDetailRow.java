package mg.bank.backend.repository.projection;

import java.time.LocalDateTime;

/**
 * Projection d un releve d avancement de sous-activite.
 *
 * Tous les releves de toutes les sous-activites d une activite sont lus par
 * une seule requete, ce qui impose de pouvoir rattacher chaque ligne a sa
 * sous-activite. idSousActivite est ce rattachement ; il ne doit pas disparaitre
 * au detriment d un sousActivite imbrique, sinon le regroupement en memoire
 * devient impossible.
 */
public interface AvancementDetailRow {

    Integer getId();

    /** Cle de regroupement, pas une donnee affichee. */
    Integer getIdSousActivite();

    Double getValeurPourcentage();

    String getCommentaire();

    LocalDateTime getDateChangement();

    Integer getStatutId();

    String getStatutCode();

    String getStatutLibelle();

    Integer getUtilisateurId();

    String getUtilisateurNom();

    String getUtilisateurPrenom();

}
