package mg.bank.backend.enums;

/**
 * Decision d une etape de validation d une activite.
 *
 * Ces valeurs sont celles du type enum PostgreSQL decision_validation, utilise
 * par la colonne validation_activite.decision. La contrainte de verification
 * chk_validation_activite_decision_date en impose l association :
 *
 * - EN_ATTENTE_VALIDATION : date_decision doit rester nulle
 * - VALIDE, REJETE, RETOUR_MODIFICATION : date_decision doit etre renseignee
 *
 * A NE PAS confondre avec StatutsActivite, qui decrit l etat d une activite et
 * dont le code termine est VALIDEE, avec un E. Ici la decision est VALIDE, sans
 * E. Les deux notions ne se rejoignent pas : une activite peut etre validee par
 * une etape tout en restant, dans son historique, au statut EN_COURS.
 */
public enum DecisionValidation {

    EN_ATTENTE_VALIDATION,
    VALIDE,
    REJETE,
    RETOUR_MODIFICATION

}
