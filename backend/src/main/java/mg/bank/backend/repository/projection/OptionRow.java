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

}
