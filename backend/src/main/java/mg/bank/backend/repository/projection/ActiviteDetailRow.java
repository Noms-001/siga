package mg.bank.backend.repository.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projection de l activite mere du detail.
 *
 * Le statut et la date de creation sont reconstruits depuis historique_activite
 * avec les memes LATERAL que la liste : le detail ne peut pas diverger de ce que
 * la liste affiche.
 *
 * enRetard est calcule en base plutot que dans le service, car il depend du
 * statut courant et de la liste des codes terminaux, deux donnees deja
 * disponibles dans la requete.
 */
public interface ActiviteDetailRow {

    Integer getId();

    String getCode();

    String getReference();

    String getDesignation();

    LocalDate getDateDebutPrevue();

    LocalDate getDateFinPrevue();

    LocalDate getDateDebutReelle();

    LocalDate getDateFinReelle();

    /** Premiere entree d historique, ou null si l activite n en a aucune. */
    LocalDateTime getDateCreation();

    Integer getOsId();

    String getOsCode();

    String getOsDesignation();

    Integer getOsAnnee();

    Integer getServiceId();

    String getServiceLibelle();

    Integer getTypeActiviteId();

    String getTypeActiviteLibelle();

    Integer getSiteId();

    String getSiteLibelle();

    Integer getPrioriteId();

    String getPrioriteCode();

    String getPrioriteLibelle();

    Integer getStatutId();

    String getStatutCode();

    String getStatutLibelle();

    Double getAvancement();

    Boolean getEnRetard();

}
