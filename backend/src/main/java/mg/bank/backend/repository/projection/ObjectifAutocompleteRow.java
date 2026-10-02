package mg.bank.backend.repository.projection;

/**
 * Suggestion d'autocomplete d'un objectif specifique.
 *
 * PROPRE A CETTE RECHERCHE, ET NON REUTILISEE
 *
 * OptionRow sert aussi aux services, priorites, sites et types d'activite.
 * Lui ajouter un nombre d'activites le rendrait present partout et absent
 * partout ailleurs : une projection se declare sur les seules requetes qui
 * ont besoin de toutes ses colonnes, sinon Hibernate echoue sur une colonne
 * qui n existe pas dans la requete.
 *
 * prochainNumero vaut le nombre d'activites deja rattachees a l'objectif, plus
 * un. C'est une proposition, pas une reservation : activite.code reste
 * UNIQUE et c'est le backend qui refuse un code deja pris. Le calcul sert
 * uniquement a pre-remplir un champ que l'utilisateur peut modifier.
 */
public interface ObjectifAutocompleteRow {

    Integer getId();

    String getCode();

    String getLibelle();

    /**
     * Année de l'objectif, rendue a part et non concatenee au libelle : deux
     * objectifs de meme code court et de meme designation ne se distinguent
     * que par elle, et le code d'activite genere doit reprendre cette annee.
     */
    Integer getAnnee();

    Integer getProchainNumero();

}
