package mg.bank.backend.repository.projection;

/**
 * Suggestion d'autocomplete, ou ligne d'un referentiel de filtre.
 *
 * Projection volontairement minimale : une seule requete ne renvoie pas
 * toujours les memes colonnes, et une projection large echouerait sur les
 * colonnes absentes.
 */
public interface OptionRow {

    Integer getId();

    String getCode();

    String getLibelle();

    /**
     * Second axe de lecture d une suggestion.
     *
     * Facultatif par nature : seules les requetes qui ont un deuxieme axe a
     * montrer le selectionnent -- l annee d un objectif, la designation d un
     * type d activite. Celles qui n en ont pas le laissent a null, ce qui
     * evite d ecrire une seconde projection identique a la premiere.
     */
    String getLibelleSecondaire();

}
