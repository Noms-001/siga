package mg.bank.backend.repository.projection;

/**
 * Compteurs des 6 cards, calcules en une seule requete.
 *
 * Les compteurs sont produits par des FILTER sur la meme clause WHERE que
 * la liste, ce qui garantit qu'ils ne peuvent pas diverger du tableau.
 */
public interface ActiviteStatistiquesRow {

    Long getTotal();

    Long getNonCommencees();

    Long getEnCours();

    Long getTerminees();

    Long getEnRetard();

    Long getAnnulees();

    Long getReportees();

    Long getSuspendues();

}
