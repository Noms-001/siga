package mg.bank.backend.repository.projection;

import java.time.LocalDateTime;

import mg.bank.backend.enums.DecisionValidation;

/**
 * Projection d un passage d activite par une etape de validation.
 *
 * La decision est lue comme l enum DecisionValidation : ses quatre valeurs
 * sont celles du type decision_validation de PostgreSQL, et la contrainte
 * chk_validation_activite_decision_date interdit toute autre valeur.
 *
 * demandeur et decideur sont tous deux non nuls en base, y compris sur une
 * demande en attente, ou le decideur designe l utilisateur prevu. Ils sont
 * donc exposes sans traitement particulier, a l inverse du statut courant d
 * une activite, lui, derives de l historique et potentiellement absent.
 */
public interface ValidationActiviteRow {

    Integer getId();

    DecisionValidation getDecision();

    String getCommentaire();

    LocalDateTime getDateDemande();

    LocalDateTime getDateDecision();

    Integer getEtapeId();

    String getEtapeDesignation();

    String getEtapeDescription();

    Integer getEtapeNiveau();

    Boolean getEtapeObligatoire();

    String getEtapeProcedureLibelle();

    Integer getDemandeurId();

    String getDemandeurNom();

    String getDemandeurPrenom();

    Integer getDecideurId();

    String getDecideurNom();

    String getDecideurPrenom();

}
