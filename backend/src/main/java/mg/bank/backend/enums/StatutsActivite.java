package mg.bank.backend.enums;

/**
 * Codes de statut : source unique de verite.
 *
 * Volontairement dans une classe separee et non dans l'enum : une enum ne
 * peut pas referencer ses propres champs statiques declares plus bas
 * ("illegal forward reference"), alors que ces memes constantes doivent etre
 * injectables dans le SQL de ActiviteRepository.
 *
 * Ces valeurs doivent correspondre aux codes reellement inseres dans la
 * table statut par database/script/data.sql. C est le seul endroit a
 * modifier si elles changent.
 */
public final class StatutsActivite {

    public static final String EN_COURS = "EN_COURS";
    public static final String TERMINEE = "TERMINEE";
    public static final String ANNULEE = "ANNULEE";
    public static final String REPORTEE = "REPORTEE";
    public static final String NON_COMMENCEE = "NON_COMMENCEE";
    public static final String SUSPENDUE = "SUSPENDUE";

    /**
     * Statut de redaction : l'activite existe et n'a pas ete publiee.
     *
     * Il est absent de StatutActiviteEnum parce qu'il n'est pas terminal, ce
     * qui est une information, et non un oubli. C'est aussi le seul des quatre
     * codes ci-dessous a figurer dans SQL_NON_PUBLIEES : il doit rester
     * trouvable dans la liste, ou son auteur ne pourrait plus le relire.
     */
    public static final String BROUILLON = "BROUILLON";

    /**
     * Statuts du circuit de validation, avant publication au suivi.
     *
     * AUCUN CHEMIN DE CODE NE LES PRODUIT PLUS : le backend n'ecrit plus que
     * BROUILLON, et un brouillon ne quitte plus cet etat. Les trois codes sont
     * conserves pour une seule raison -- ils sont presents dans le jeu de
     * donnees de database/script/data.sql, et SQL_NON_PUBLIEES doit continuer
     * a les masquer dans la liste et l'autocomplete. Les retirer ferait
     * reapparaitre dans le suivi des activites de demonstration.
     *
     * Ils sont absents de StatutActiviteEnum : aucun n'est terminal au sens du
     * suivi.
     */
    public static final String EN_ATTENTE_VALIDATION = "EN_ATTENTE_VALIDATION";
    public static final String VALIDEE = "VALIDEE";
    public static final String REJETE = "REJETE";

    /**
     * "En retard" n'est pas un statut stocke en base : c'est une regle
     * metier (echeance depassee + statut non terminal). Ce pseudo-code est
     * transmis par la card et recoit un traitement SQL dedie.
     */
    public static final String EN_RETARD = "EN_RETARD";

    /**
     * Statuts terminaux, separes par une virgule : format attendu par
     * string_to_array puis unnest en SQL.
     */
    public static final String TERMINAUX = TERMINEE + "," + ANNULEE + "," + REPORTEE;

    private StatutsActivite() {
    }

}
