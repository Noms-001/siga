package mg.bank.backend.repository.planaction;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.planaction.PlanAction;
import mg.bank.backend.repository.SqlFragments;
import mg.bank.backend.repository.projection.planaction.PlanActionListRow;
import mg.bank.backend.repository.projection.planaction.UtilisateurPlanActionAnneeRow;

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
            """
            + SqlFragments.AVANCEMENT_PLAN_ACTION_DERNIER
            + """
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
                    """, countQuery = """
                    SELECT COUNT(*)
                    FROM plan_action pa
                    """
                    + SqlFragments.AVANCEMENT_PLAN_ACTION_DERNIER
                    + """
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
                            """, nativeQuery = true)
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
                   COALESCE(av.valeur_pourcentage, 0)      AS avancement,
                   u.id_utilisateur            AS responsableId,
                   u.nom                       AS responsableNom,
                   u.prenom                    AS responsablePrenom
            FROM plan_action pa
            """
            + SqlFragments.AVANCEMENT_PLAN_ACTION_DERNIER
            + """
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
                    """, countQuery = """
                      SELECT COUNT(*)
                      FROM plan_action pa
                    """
                    + SqlFragments.AVANCEMENT_PLAN_ACTION_DERNIER
                    + """
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
                            """, nativeQuery = true)
    Page<PlanActionListRow> findMesBrouillons(
            @Param("idUtilisateur") Integer idUtilisateur,
            Pageable pageable);

    @Query(value = """
            SELECT pa.id_plan_action       AS id,
                   pa.code                 AS code,
                   pa.designation          AS designation,
                   pa.date_debut_prevue    AS dateDebutPrevue,
                   pa.date_fin_prevue      AS dateFinPrevue,
                   pa.date_debut_reelle    AS dateDebutReelle,
                   pa.date_fin_reelle      AS dateFinReelle,
                   apa.date_affectation    AS dateAffectation,
                   apa.date_desaffectation AS dateDesaffectation,
                   r.code                  AS roleCode,
                   r.designation           AS roleLibelle,
                   sta.code                AS statutCode,
                   sta.libelle             AS statutLibelle,
                   COALESCE(av.valeur_pourcentage, 0) AS avancement
            FROM affectation_plan_action apa
            JOIN role r           ON r.id_role = apa.id_role
            JOIN plan_action pa   ON pa.id_plan_action = apa.id_plan_action
            """
            + SqlFragments.AVANCEMENT_PLAN_ACTION_DERNIER
            + """
                    WHERE apa.id_utilisateur = :idUtilisateur
                      AND apa.date_desaffectation IS NULL
                      AND pa.date_debut_prevue <= CAST(:finAnnee AS DATE)
                      AND pa.date_fin_prevue >= CAST(:debutAnnee AS DATE)
                    ORDER BY
                        CASE
                            WHEN sta.code = 'EN_RETARD' THEN 0
                            WHEN pa.date_fin_prevue < CURRENT_DATE
                                 AND sta.code NOT IN ('TERMINEE', 'ANNULEE') THEN 0
                            WHEN sta.code = 'EN_COURS' THEN 1
                            WHEN sta.code IS NULL OR sta.code = 'NON_COMMENCEE' THEN 2
                            WHEN sta.code = 'TERMINEE' THEN 3
                            ELSE 4
                        END,
                        pa.date_debut_prevue DESC,
                        pa.id_plan_action DESC
                    """, nativeQuery = true)
    List<UtilisateurPlanActionAnneeRow> findPlansAnneePourUtilisateur(
            @Param("idUtilisateur") Integer idUtilisateur,
            @Param("debutAnnee") java.time.LocalDate debutAnnee,
            @Param("finAnnee") java.time.LocalDate finAnnee);

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
                   COALESCE(av.valeur_pourcentage, 0) AS avancement,
                   u.id_utilisateur            AS responsableId,
                   u.nom                       AS responsableNom,
                   u.prenom                    AS responsablePrenom
            FROM plan_action pa
            """
            + SqlFragments.AVANCEMENT_PLAN_ACTION_DERNIER
            + """
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
                    WHERE pa.id_plan_action = :id
                    """, nativeQuery = true)
    Optional<PlanActionListRow> findByIdWithDetails(@Param("id") Integer id);
}