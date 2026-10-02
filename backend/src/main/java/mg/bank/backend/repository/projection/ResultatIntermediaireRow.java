package mg.bank.backend.repository.projection;

/**
 * Projection d un resultat intermediaire d une activite.
 *
 * Deux colonnes seulement : la table ne porte ni date ni commentaire, et le DTO
 * n invente pas de champ vide pour les representer.
 */
public interface ResultatIntermediaireRow {

    Integer getId();

    String getDesignation();

}
