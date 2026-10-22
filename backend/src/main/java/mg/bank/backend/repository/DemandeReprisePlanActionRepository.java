package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.DemandeReprisePlanAction;
import mg.bank.backend.repository.projection.DemandeReprisePlanRow;

public interface DemandeReprisePlanActionRepository
        extends JpaRepository<DemandeReprisePlanAction, Integer> {

    boolean existsByPlanActionIdPlanActionAndStatut(Integer idPlanAction, String statut);

    Optional<DemandeReprisePlanAction> findByIdDemandeReprisePlanActionAndStatut(
            Integer id, String statut);

    /** Historique des demandes d'un plan. */
    @Query(value = """
            SELECT d.id_demande_reprise_plan_action AS id,
                   d.date_demande                  AS dateDemande,
                   d.date_decision                 AS dateDecision,
                   d.statut                        AS statut,
                   d.motif                         AS motif,
                   d.commentaire_decision          AS commentaireDecision,
                   p.id_plan_action                AS planActionId,
                   p.code                          AS planActionCode,
                   p.designation                   AS planActionDesignation,
                   dem.id_utilisateur              AS demandeurId,
                   dem.nom                         AS demandeurNom,
                   dem.prenom                      AS demandeurPrenom,
                   resp.id_utilisateur             AS responsableId,
                   resp.nom                        AS responsableNom,
                   resp.prenom                     AS responsablePrenom,
                   dec.id_utilisateur              AS decideurId,
                   dec.nom                         AS decideurNom,
                   dec.prenom                      AS decideurPrenom
            FROM demande_reprise_plan_action d
            JOIN plan_action p ON p.id_plan_action = d.id_plan_action
            JOIN utilisateur dem ON dem.id_utilisateur = d.id_demandeur
            JOIN utilisateur resp ON resp.id_utilisateur = d.id_responsable_actuel
            LEFT JOIN utilisateur dec ON dec.id_utilisateur = d.id_decideur
            WHERE d.id_plan_action = :idPlanAction
            ORDER BY d.date_demande DESC, d.id_demande_reprise_plan_action DESC
            """, nativeQuery = true)
    List<DemandeReprisePlanRow> findHistoriquePlan(
            @Param("idPlanAction") Integer idPlanAction);

    /** Demandes envoyees par l'utilisateur courant. */
    @Query(value = """
            SELECT d.id_demande_reprise_plan_action AS id,
                   d.date_demande                  AS dateDemande,
                   d.date_decision                 AS dateDecision,
                   d.statut                        AS statut,
                   d.motif                         AS motif,
                   d.commentaire_decision          AS commentaireDecision,
                   p.id_plan_action                AS planActionId,
                   p.code                          AS planActionCode,
                   p.designation                   AS planActionDesignation,
                   dem.id_utilisateur              AS demandeurId,
                   dem.nom                         AS demandeurNom,
                   dem.prenom                      AS demandeurPrenom,
                   resp.id_utilisateur             AS responsableId,
                   resp.nom                        AS responsableNom,
                   resp.prenom                     AS responsablePrenom,
                   dec.id_utilisateur              AS decideurId,
                   dec.nom                         AS decideurNom,
                   dec.prenom                      AS decideurPrenom
            FROM demande_reprise_plan_action d
            JOIN plan_action p ON p.id_plan_action = d.id_plan_action
            JOIN utilisateur dem ON dem.id_utilisateur = d.id_demandeur
            JOIN utilisateur resp ON resp.id_utilisateur = d.id_responsable_actuel
            LEFT JOIN utilisateur dec ON dec.id_utilisateur = d.id_decideur
            WHERE d.id_demandeur = :idUtilisateur
            ORDER BY d.date_demande DESC, d.id_demande_reprise_plan_action DESC
            """, nativeQuery = true)
    List<DemandeReprisePlanRow> findMesDemandes(
            @Param("idUtilisateur") Integer idUtilisateur);

    /**
     * Demandes a traiter par l'utilisateur :
     * - il est le RESPONSABLE actuel du plan,
     * - ou il est chef de service du meme service que le RESPONSABLE actuel.
     */
    @Query(value = """
            SELECT d.id_demande_reprise_plan_action AS id,
                   d.date_demande                  AS dateDemande,
                   d.date_decision                 AS dateDecision,
                   d.statut                        AS statut,
                   d.motif                         AS motif,
                   d.commentaire_decision          AS commentaireDecision,
                   p.id_plan_action                AS planActionId,
                   p.code                          AS planActionCode,
                   p.designation                   AS planActionDesignation,
                   dem.id_utilisateur              AS demandeurId,
                   dem.nom                         AS demandeurNom,
                   dem.prenom                      AS demandeurPrenom,
                   resp.id_utilisateur             AS responsableId,
                   resp.nom                        AS responsableNom,
                   resp.prenom                     AS responsablePrenom,
                   dec.id_utilisateur              AS decideurId,
                   dec.nom                         AS decideurNom,
                   dec.prenom                      AS decideurPrenom
            FROM demande_reprise_plan_action d
            JOIN plan_action p ON p.id_plan_action = d.id_plan_action
            JOIN utilisateur dem ON dem.id_utilisateur = d.id_demandeur
            JOIN utilisateur resp ON resp.id_utilisateur = d.id_responsable_actuel
            LEFT JOIN utilisateur dec ON dec.id_utilisateur = d.id_decideur
            WHERE d.statut = 'EN_ATTENTE'
              AND (
                  d.id_responsable_actuel = :idUtilisateur
                  OR EXISTS (
                      SELECT 1 FROM utilisateur u
                      JOIN poste po ON po.id_poste = u.id_poste
                      WHERE u.id_utilisateur = :idUtilisateur
                        AND u.actif = TRUE
                        AND resp.id_service IS NOT NULL
                        AND u.id_service = resp.id_service
                        AND po.nom LIKE 'Chef de service%'
                  )
              )
            ORDER BY d.date_demande ASC, d.id_demande_reprise_plan_action ASC
            """, nativeQuery = true)
    List<DemandeReprisePlanRow> findATraiter(@Param("idUtilisateur") Integer idUtilisateur);
}