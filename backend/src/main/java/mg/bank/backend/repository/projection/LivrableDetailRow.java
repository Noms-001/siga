package mg.bank.backend.repository.projection;

/**
 * Projection d un livrable de sous-activite.
 *
 * idSousActivite est la cle de regroupement : les livrables de toutes les
 * sous-activites sont lus en une seule requete.
 */
public interface LivrableDetailRow {

    Integer getId();

    /** Cle de regroupement, pas une donnee affichee. */
    Integer getIdSousActivite();

    String getDesignation();

    String getDescription();

}
