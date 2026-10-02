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
     * Statut de redaction : l'activite existe mais n'a pas ete soumise.
     *
     * Comme les trois codes suivants, il est absent de
     * StatutActiviteEnum, mais pour une raison opposee : il n'est pas dans
     * le circuit de validation, il en est le point de depart. C est le seul
     * des quatre a etre visible dans la liste, et il se distingue des trois
     * autres parce qu'il n'est pas dans SQL_NON_PUBLIEES : un brouillon doit
     * rester trouvable, sinon son auteur ne pourrait plus le soumettre.
     */
    public static final String BROUILLON = "BROUILLON";

    /**
     * Statuts du circuit de validation, avant que l'activite ne soit
     * publiee au suivi.
     *
     * Ces trois codes sont volontairement absents de StatutActiviteEnum :
     * aucun n'est terminal au sens du suivi, et aucun ne doit apparaitre
     * dans la liste. Ils sont regroupes ici pour que la regle
     * "qu'est-ce qui est visible" tienne en un seul endroit.
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
