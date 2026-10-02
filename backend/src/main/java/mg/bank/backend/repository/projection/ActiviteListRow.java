package mg.bank.backend.repository.projection;

/**
 * Projection d'une ligne d'activite.
 *
 * Les noms de colonnes doivent correspondre exactement aux getters :
 * Spring Data fait le mapping par alias.
 *
 * dateCreationTri est une colonne de tri qui n'est jamais nulle. En effet,
 * en DESC Postgres place les NULL en premier, ce qui ferait passer une
 * activite sans historique pour la plus recente alors qu'elle n'est pas
 * datable. COALESCE evite d'avoir a ecrire NULLS LAST dans chaque requete.
 *
 * dateCreation reste la valeur reelle, nullable, pour l'affichage.
 */
public interface ActiviteListRow {

    Integer getId();

    String getCode();

    String getReference();

    String getDesignation();

    java.time.LocalDate getDateDebutPrevue();

    java.time.LocalDate getDateFinPrevue();

    java.time.LocalDate getDateDebutReelle();

    java.time.LocalDate getDateFinReelle();

    /** Premiere entree d'historique : date de creation retenue. */
    java.time.LocalDateTime getDateCreation();

    java.time.LocalDateTime getDateCreationTri();

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

}
