package mg.bank.backend.repository.projection;

/**
 * Projection d un depot de fichier sur un livrable.
 *
 * comme pour les avancements, tous les fichiers de tous les livrables de
 * toutes les sous-activites sont lus en une requete : idLivrable est la cle de
 * regroupement, et idSousActivite n est pas necessaire puisque le livrable
 * porte deja ce rattachement.
 */
public interface FichierDetailRow {

    Integer getId();

    /** Cle de regroupement, pas une donnee affichee. */
    Integer getIdLivrable();

    String getNomFichier();

    String getNomOriginal();

    String getExtension();

    String getTypeMime();

    Long getTaille();

    Integer getVersion();

    java.time.LocalDateTime getDateDepot();

    Integer getUtilisateurId();

    String getUtilisateurNom();

    String getUtilisateurPrenom();

}
