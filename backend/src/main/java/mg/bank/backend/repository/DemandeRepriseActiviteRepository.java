package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.DemandeRepriseActivite;
import mg.bank.backend.repository.projection.DemandeRepriseRow;

public interface DemandeRepriseActiviteRepository
        extends JpaRepository<DemandeRepriseActivite, Integer> {

    boolean existsByActiviteIdActiviteAndStatut(Integer idActivite, String statut);

    Optional<DemandeRepriseActivite> findByIdDemandeRepriseActiviteAndStatut(
            Integer id, String statut);

    /** Historique des demandes d'une activite. */
    @Query(value = """
            SELECT d.id_demande_reprise_activite AS id,
                   d.date_demande               AS dateDemande,
                   d.date_decision              AS dateDecision,
                   d.statut                     AS statut,
                   d.motif                      AS motif,
                   d.commentaire_decision       AS commentaireDecision,
                   a.id_activite                AS activiteId,
                   a.code                       AS activiteCode,
                   a.designation                AS activiteDesignation,
                   dem.id_utilisateur           AS demandeurId,
                   dem.nom                      AS demandeurNom,
                   dem.prenom                   AS demandeurPrenom,
                   resp.id_utilisateur          AS responsableId,
                   resp.nom                     AS responsableNom,
                   resp.prenom                  AS responsablePrenom,
                   dec.id_utilisateur           AS decideurId,
                   dec.nom                      AS decideurNom,
                   dec.prenom                   AS decideurPrenom
            FROM demande_reprise_activite d
            JOIN activite a ON a.id_activite = d.id_activite
            JOIN utilisateur dem ON dem.id_utilisateur = d.id_demandeur
            JOIN utilisateur resp ON resp.id_utilisateur = d.id_responsable_actuel
            LEFT JOIN utilisateur dec ON dec.id_utilisateur = d.id_decideur
            WHERE d.id_activite = :idActivite
            ORDER BY d.date_demande DESC, d.id_demande_reprise_activite DESC
            """, nativeQuery = true)
    List<DemandeRepriseRow> findHistoriqueActivite(@Param("idActivite") Integer idActivite);

    /** Demandes envoyees par l'utilisateur courant. */
    @Query(value = """
            SELECT d.id_demande_reprise_activite AS id,
                   d.date_demande               AS dateDemande,
                   d.date_decision              AS dateDecision,
                   d.statut                     AS statut,
                   d.motif                      AS motif,
                   d.commentaire_decision       AS commentaireDecision,
                   a.id_activite                AS activiteId,
                   a.code                       AS activiteCode,
                   a.designation                AS activiteDesignation,
                   dem.id_utilisateur           AS demandeurId,
                   dem.nom                      AS demandeurNom,
                   dem.prenom                   AS demandeurPrenom,
                   resp.id_utilisateur          AS responsableId,
                   resp.nom                     AS responsableNom,
                   resp.prenom                  AS responsablePrenom,
                   dec.id_utilisateur           AS decideurId,
                   dec.nom                      AS decideurNom,
                   dec.prenom                   AS decideurPrenom
            FROM demande_reprise_activite d
            JOIN activite a ON a.id_activite = d.id_activite
            JOIN utilisateur dem ON dem.id_utilisateur = d.id_demandeur
            JOIN utilisateur resp ON resp.id_utilisateur = d.id_responsable_actuel
            LEFT JOIN utilisateur dec ON dec.id_utilisateur = d.id_decideur
            WHERE d.id_demandeur = :idUtilisateur
            ORDER BY d.date_demande DESC, d.id_demande_reprise_activite DESC
            """, nativeQuery = true)
    List<DemandeRepriseRow> findMesDemandes(@Param("idUtilisateur") Integer idUtilisateur);

    /**
     * Demandes a traiter par l'utilisateur :
     * - il est le RESPONSABLE actuel reference dans la demande
     * - ou il est chef de service du service de l'activite.
     */
    @Query(value = """
            SELECT d.id_demande_reprise_activite AS id,
                   d.date_demande               AS dateDemande,
                   d.date_decision              AS dateDecision,
                   d.statut                     AS statut,
                   d.motif                      AS motif,
                   d.commentaire_decision       AS commentaireDecision,
                   a.id_activite                AS activiteId,
                   a.code                       AS activiteCode,
                   a.designation                AS activiteDesignation,
                   dem.id_utilisateur           AS demandeurId,
                   dem.nom                      AS demandeurNom,
                   dem.prenom                   AS demandeurPrenom,
                   resp.id_utilisateur          AS responsableId,
                   resp.nom                     AS responsableNom,
                   resp.prenom                  AS responsablePrenom,
                   dec.id_utilisateur           AS decideurId,
                   dec.nom                      AS decideurNom,
                   dec.prenom                   AS decideurPrenom
            FROM demande_reprise_activite d
            JOIN activite a ON a.id_activite = d.id_activite
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
                        AND u.id_service = a.id_service
                        AND u.actif = TRUE
                        AND po.nom LIKE 'Chef de service%'
                  )
              )
            ORDER BY d.date_demande ASC, d.id_demande_reprise_activite ASC
            """, nativeQuery = true)
    List<DemandeRepriseRow> findATraiter(@Param("idUtilisateur") Integer idUtilisateur);
}