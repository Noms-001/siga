package mg.bank.backend.repository.projection;

import java.time.LocalDateTime;

/**
 * Projection d une affectation d utilisateur a une sous-activite.
 *
 * role utilise designation comme libelle, conformement a ReferenceDTO, qui
 * n impose pas que code et libelle soient renseignes simultanement.
 */
public interface AffectationDetailRow {

    Integer getId();

    /** Cle de regroupement, pas une donnee affichee. */
    Integer getIdSousActivite();

    LocalDateTime getDateAffectation();

    LocalDateTime getDateDesaffectation();

    Integer getRoleId();

    String getRoleCode();

    String getRoleLibelle();

    Integer getUtilisateurId();

    String getUtilisateurNom();

    String getUtilisateurPrenom();

}
