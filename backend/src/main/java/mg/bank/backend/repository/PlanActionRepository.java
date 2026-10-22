package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.PlanAction;
import mg.bank.backend.repository.projection.PlanActionListRow;

public interface PlanActionRepository extends JpaRepository<PlanAction, Integer> {

    Optional<PlanAction> findByCode(String code);
    boolean existsByCode(String code);

    /**
     * Code suivant pour l'annee : MAX sur le numero des codes PA-<annee>-<n>.
     * Identique au mecanisme utilise par Origine.dernierNumeroPour.
     */
    @Query(value = """
            SELECT COALESCE(
                MAX(CAST(SUBSTRING(pa.code FROM '^PA-[0-9]{4}-([0-9]+)$') AS INTEGER)),
                0
            )
            FROM plan_action pa
            WHERE pa.code ~ ('^PA-' || :annee || '-[0-9]+$')
            """, nativeQuery = true)
    int dernierNumeroPourAnnee(@Param("annee") int annee);

    /**
     * Liste paginee des plans, avec filtre statut (par defaut les 3 statuts
     * actifs). Le statut courant et l'avancement sont lus par LATERAL sur
     * avancement_plan_action, et le responsable par LATERAL sur
     * affectation_plan_action avec role RESPONSABLE.
     *
     * Aucun ORDER BY (regle du projet) : le tri est impose par le Pageable.
     */
    @Query(value = """
            SELECT pa.id_plan_action          AS id,
                   pa.code                     AS code,
                   pa.designation              AS designation,
                   pa.date_debut_prevue        AS dateDebutPrevue,
                   pa.date_fin_prevue          AS dateFinPrevue,
                   pa.date_debut_reelle        AS dateDebutReelle,
                   pa.date_fin_reelle          AS dateFinReelle,
                   sta.id_statut               AS statutId,
                   sta.code                    AS statutCode,
                   sta.libelle                 AS statutLibelle,
                   COALESCE(av.valeur, 0)      AS avancement,
                   u.id_utilisateur            AS responsableId,
                   u.nom                       AS responsableNom,
                   u.prenom                    AS responsablePrenom
            FROM plan_action pa
            LEFT JOIN LATERAL (
                SELECT a.valeur_pourcentage AS valeur, a.id_statut
                FROM avancement_plan_action a
                WHERE a.id_plan_action = pa.id_plan_action
                ORDER BY a.date_changement DESC, a.id_avancement_plan_action DESC
                LIMIT 1
            ) av ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = av.id_statut
            LEFT JOIN LATERAL (
                SELECT aff.id_utilisateur
                FROM affectation_plan_action aff
                JOIN role r ON r.id_role = aff.id_role
                WHERE aff.id_plan_action = pa.id_plan_action
                  AND aff.date_desaffectation IS NULL
                  AND r.code = 'RESPONSABLE'
                ORDER BY aff.date_affectation ASC
                LIMIT 1
            ) resp ON TRUE
            LEFT JOIN utilisateur u ON u.id_utilisateur = resp.id_utilisateur
            WHERE (:statutCode IS NULL
                   OR sta.code = :statutCode)
              AND (:statutsParDefautCsv IS NULL
                   OR sta.code = ANY(string_to_array(:statutsParDefautCsv, ',')))
              AND (:motif IS NULL
                   OR pa.code ILIKE :motif
                   OR pa.designation ILIKE :motif)
              AND (:annee IS NULL
                   OR EXTRACT(YEAR FROM pa.date_debut_prevue) = :annee)
              AND (:dateDebut IS NULL OR pa.date_debut_prevue >= :dateDebut)
              AND (:dateFin IS NULL OR pa.date_fin_prevue <= :dateFin)
              AND (:origineId IS NULL
                   OR EXISTS (
                       SELECT 1 FROM plan_action_origine pao
                       WHERE pao.id_plan_action = pa.id_plan_action
                         AND pao.id_origine = :origineId
                   ))
              AND (:responsableId IS NULL
                   OR resp.id_utilisateur = :responsableId)
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM plan_action pa
            LEFT JOIN LATERAL (
                SELECT a.id_statut
                FROM avancement_plan_action a
                WHERE a.id_plan_action = pa.id_plan_action
                ORDER BY a.date_changement DESC, a.id_avancement_plan_action DESC
                LIMIT 1
            ) av ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = av.id_statut
            LEFT JOIN LATERAL (
                SELECT aff.id_utilisateur
                FROM affectation_plan_action aff
                JOIN role r ON r.id_role = aff.id_role
                WHERE aff.id_plan_action = pa.id_plan_action
                  AND aff.date_desaffectation IS NULL
                  AND r.code = 'RESPONSABLE'
                ORDER BY aff.date_affectation ASC
                LIMIT 1
            ) resp ON TRUE
            WHERE (:statutCode IS NULL OR sta.code = :statutCode)
              AND (:statutsParDefautCsv IS NULL
                   OR sta.code = ANY(string_to_array(:statutsParDefautCsv, ',')))
              AND (:motif IS NULL
                   OR pa.code ILIKE :motif
                   OR pa.designation ILIKE :motif)
              AND (:annee IS NULL
                   OR EXTRACT(YEAR FROM pa.date_debut_prevue) = :annee)
              AND (:dateDebut IS NULL OR pa.date_debut_prevue >= :dateDebut)
              AND (:dateFin IS NULL OR pa.date_fin_prevue <= :dateFin)
              AND (:origineId IS NULL
                   OR EXISTS (
                       SELECT 1 FROM plan_action_origine pao
                       WHERE pao.id_plan_action = pa.id_plan_action
                         AND pao.id_origine = :origineId
                   ))
              AND (:responsableId IS NULL
                   OR resp.id_utilisateur = :responsableId)
            """,
            nativeQuery = true)
    Page<PlanActionListRow> findPlans(
            @Param("statutCode") String statutCode,
            @Param("statutsParDefautCsv") String statutsParDefautCsv,
            @Param("motif") String motif,
            @Param("annee") Integer annee,
            @Param("dateDebut") java.time.LocalDate dateDebut,
            @Param("dateFin") java.time.LocalDate dateFin,
            @Param("origineId") Integer origineId,
            @Param("responsableId") Integer responsableId,
            Pageable pageable);

    /**
     * Brouillons de l'utilisateur courant : plans BROUILLON dont il est
     * RESPONSABLE actif.
     */
    @Query(value = """
            SELECT pa.id_plan_action          AS id,
                   pa.code                     AS code,
                   pa.designation              AS designation,
                   pa.date_debut_prevue        AS dateDebutPrevue,
                   pa.date_fin_prevue          AS dateFinPrevue,
                   pa.date_debut_reelle        AS dateDebutReelle,
                   pa.date_fin_reelle          AS dateFinReelle,
                   sta.id_statut               AS statutId,
                   sta.code                    AS statutCode,
                   sta.libelle                 AS statutLibelle,
                   COALESCE(av.valeur, 0)      AS avancement,
                   u.id_utilisateur            AS responsableId,
                   u.nom                       AS responsableNom,
                   u.prenom                    AS responsablePrenom
            FROM plan_action pa
            LEFT JOIN LATERAL (
                SELECT a.valeur_pourcentage AS valeur, a.id_statut
                FROM avancement_plan_action a
                WHERE a.id_plan_action = pa.id_plan_action
                ORDER BY a.date_changement DESC, a.id_avancement_plan_action DESC
                LIMIT 1
            ) av ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = av.id_statut
            INNER JOIN LATERAL (
                SELECT aff.id_utilisateur
                FROM affectation_plan_action aff
                JOIN role r ON r.id_role = aff.id_role
                WHERE aff.id_plan_action = pa.id_plan_action
                  AND aff.date_desaffectation IS NULL
                  AND r.code = 'RESPONSABLE'
                  AND aff.id_utilisateur = :idUtilisateur
                LIMIT 1
            ) resp ON TRUE
            INNER JOIN utilisateur u ON u.id_utilisateur = resp.id_utilisateur
            WHERE sta.code = 'BROUILLON'
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM plan_action pa
            LEFT JOIN LATERAL (
                SELECT a.id_statut
                FROM avancement_plan_action a
                WHERE a.id_plan_action = pa.id_plan_action
                ORDER BY a.date_changement DESC, a.id_avancement_plan_action DESC
                LIMIT 1
            ) av ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = av.id_statut
            INNER JOIN LATERAL (
                SELECT aff.id_utilisateur
                FROM affectation_plan_action aff
                JOIN role r ON r.id_role = aff.id_role
                WHERE aff.id_plan_action = pa.id_plan_action
                  AND aff.date_desaffectation IS NULL
                  AND r.code = 'RESPONSABLE'
                  AND aff.id_utilisateur = :idUtilisateur
                LIMIT 1
            ) resp ON TRUE
            WHERE sta.code = 'BROUILLON'
            """,
            nativeQuery = true)
    Page<PlanActionListRow> findMesBrouillons(
            @Param("idUtilisateur") Integer idUtilisateur,
            Pageable pageable);
}