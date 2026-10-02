package mg.bank.backend.repository.projection;

/**
 * Projection d un fichier destinee a son telechargement.
 *
 * Contrairement a FichierDetailRow, elle porte cheminFichier : celui-ci n a
 * pas a figurer dans le detail JSON, mais l endpoint de telechargement en a
 * besoin pour retrouver le depot sur le disque du serveur. Elle n est donc
 * lue que par ce chemin, jamais par le detail.
 */
public interface FichierTelechargementRow {

    Integer getId();

    /** Nom de stockage, differencie de nomOriginal apres un renommage. */
    String getNomFichier();

    /** Nom d origine, tel que fourni par l utilisateur. */
    String getNomOriginal();

    /** Chemin du depot sur le disque du serveur, jamais expose en JSON. */
    String getCheminFichier();

    String getExtension();

    String getTypeMime();

    Long getTaille();

    Integer getVersion();

    java.time.LocalDateTime getDateDepot();

}