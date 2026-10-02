package mg.bank.backend.repository.projection;

/**
 * Statut courant d une activite, tel que la base le voit.
 *
 * L activite ne porte pas son statut : celui-ci est la derniere ligne de
 * historique_activite. Cette projection sert a le relire seul, sans charger
 * le detail, et sert donc de reference aux decisions d ecriture -- "cette
 * activite est-elle encore un brouillon" -- comme au diagnostic apres un
 * refus.
 *
 * Deux colonnes et non une : id est l identifiant de l activite, pas de la
 * ligne d historique. Il est non nul par construction, donc une projection
 * presente signale une activite trouvee, meme si statutCode est nul parce
 * qu aucune ligne d historique n a encore ete ecrite. Sans cette colonne, un
 * Optional vide confondrait "hors perimetre" et "sans historique", et les
 * deux doivent repondre differemment.
 */
public interface StatutCourantRow {

    Integer getId();

    String getStatutCode();

}
