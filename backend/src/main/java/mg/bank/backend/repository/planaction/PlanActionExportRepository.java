package mg.bank.backend.repository.planaction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mg.bank.backend.model.planaction.PlanAction;
import mg.bank.backend.repository.SqlFragments;
import mg.bank.backend.repository.projection.planaction.ActiviteLieeExportRow;
import mg.bank.backend.repository.projection.planaction.MotifReprisePlanExportRow;
import mg.bank.backend.repository.projection.planaction.OrigineExportRow;
import mg.bank.backend.repository.projection.planaction.PlanActionExportRow;
import mg.bank.backend.repository.projection.planaction.ResponsablePlanExportRow;
import mg.bank.backend.repository.projection.planaction.SuiviPlanActionExportRow;

@Repository
public interface PlanActionExportRepository extends JpaRepository<PlanAction, Integer> {

    String PLAN_SCOPE = """
            WITH plan_services AS (
                SELECT DISTINCT pao.id_plan_action,
                       srv.id_service,
                       srv.nom AS nom_service,
                       srv.id_departement
                FROM plan_action_origine pao
                JOIN origine o ON o.id_origine = pao.id_origine
                JOIN activite_origine ao ON ao.id_origine = o.id_origine
                JOIN activite act ON act.id_activite = ao.id_activite
                JOIN service srv ON srv.id_service = act.id_service
            ),
            plan_scope AS (
                SELECT id_plan_action,
                       COUNT(DISTINCT id_service) AS nombre_services,
                       COUNT(DISTINCT id_departement) AS nombre_departements,
                       MIN(id_service) AS id_service_principal,
                       MIN(nom_service) AS nom_service_principal,
                       MIN(id_departement) AS id_departement_principal,
                       STRING_AGG(DISTINCT nom_service, ', ' ORDER BY nom_service) AS services_lies
                FROM plan_services
                GROUP BY id_plan_action
            )
            """;

    String RESPONSABLE_LATERAL = """
            LEFT JOIN LATERAL (
                SELECT aff.id_utilisateur
                FROM affectation_plan_action aff
                JOIN role r ON r.id_role = aff.id_role
                WHERE aff.id_plan_action = pa.id_plan_action
                  AND aff.date_desaffectation IS NULL
                  AND r.code = 'RESPONSABLE'
                  AND NOT EXISTS (
                      SELECT 1 FROM affectation_plan_action aff2
                      JOIN role r2 ON r2.id_role = aff2.id_role
                      WHERE aff2.id_plan_action = pa.id_plan_action
                        AND aff2.date_desaffectation IS NULL
                        AND r2.code = 'RESPONSABLE'
                        AND (aff2.date_affectation, aff2.id_affectation_plan_action)
                            < (aff.date_affectation, aff.id_affectation_plan_action))
            ) resp ON TRUE
            """;

    @Query(value = PLAN_SCOPE + """
            SELECT pa.id_plan_action AS id,
                   pa.code AS code,
                   pa.designation AS designation,
                   pa.date_debut_prevue AS dateDebutPrevue,
                   pa.date_fin_prevue AS dateFinPrevue,
                   pa.date_debut_reelle AS dateDebutReelle,
                   pa.date_fin_reelle AS dateFinReelle,
                   sta.code AS statutCode,
                   sta.libelle AS statutLibelle,
                   COALESCE(av.valeur_pourcentage, 0) AS avancement,
                   ps.id_service_principal AS serviceId,
                   ps.nom_service_principal AS serviceNom,
                   ps.services_lies AS servicesLies
            FROM plan_action pa
            JOIN plan_scope ps ON ps.id_plan_action = pa.id_plan_action
            """
            + SqlFragments.AVANCEMENT_PLAN_ACTION_DERNIER
            + RESPONSABLE_LATERAL
            + """
            LEFT JOIN utilisateur u ON u.id_utilisateur = resp.id_utilisateur
            WHERE (CAST(:servicePerimetre AS integer) IS NULL
                   OR (ps.nombre_services = 1 AND ps.id_service_principal = CAST(:servicePerimetre AS integer)))
              AND (CAST(:departementPerimetre AS integer) IS NULL
                   OR (ps.nombre_departements = 1
                       AND ps.id_departement_principal = CAST(:departementPerimetre AS integer)))
              AND (CAST(:serviceDemande AS integer) IS NULL
                   OR (ps.nombre_services = 1 AND ps.id_service_principal = CAST(:serviceDemande AS integer)))
              AND (:statutCode IS NULL OR sta.code = :statutCode)
              AND (:statutsParDefautCsv IS NULL
                   OR sta.code = ANY(string_to_array(:statutsParDefautCsv, ',')))
              AND (:motif IS NULL OR pa.code ILIKE :motif OR pa.designation ILIKE :motif)
              AND (CAST(:annee AS integer) IS NULL
                   OR EXTRACT(YEAR FROM pa.date_debut_prevue) = CAST(:annee AS integer))
              AND (CAST(:dateDebut AS date) IS NULL OR pa.date_debut_prevue >= CAST(:dateDebut AS date))
              AND (CAST(:dateFin AS date) IS NULL OR pa.date_fin_prevue <= CAST(:dateFin AS date))
              AND (CAST(:origineId AS integer) IS NULL OR EXISTS (
                  SELECT 1 FROM plan_action_origine pao
                  WHERE pao.id_plan_action = pa.id_plan_action
                    AND pao.id_origine = CAST(:origineId AS integer)))
              AND (:typeOrigine IS NULL OR EXISTS (
                  SELECT 1 FROM plan_action_origine pao_t
                  JOIN origine o_t ON o_t.id_origine = pao_t.id_origine
                  WHERE pao_t.id_plan_action = pa.id_plan_action
                    AND o_t.type_origine = :typeOrigine))
              AND (CAST(:responsableId AS integer) IS NULL
                   OR resp.id_utilisateur = CAST(:responsableId AS integer))
            ORDER BY pa.date_debut_prevue DESC, pa.code
            """, nativeQuery = true)
    List<PlanActionExportRow> findPlansPourExport(
            @Param("servicePerimetre") Integer servicePerimetre,
            @Param("departementPerimetre") Integer departementPerimetre,
            @Param("serviceDemande") Integer serviceDemande,
            @Param("statutCode") String statutCode,
            @Param("statutsParDefautCsv") String statutsParDefautCsv,
            @Param("motif") String motif,
            @Param("annee") Integer annee,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("origineId") Integer origineId,
            @Param("typeOrigine") String typeOrigine,
            @Param("responsableId") Integer responsableId);

    @Query(value = """
            SELECT pao.id_plan_action AS idPlanAction,
                   o.code AS code,
                   o.designation AS designation,
                   o.type_origine AS typeOrigine,
                   pao.est_principale AS estPrincipale
            FROM plan_action_origine pao
            JOIN origine o ON o.id_origine = pao.id_origine
            WHERE pao.id_plan_action IN (:ids)
            ORDER BY pao.id_plan_action, pao.est_principale DESC, o.id_origine
            """, nativeQuery = true)
    List<OrigineExportRow> findOriginesPourExport(@Param("ids") List<Integer> ids);

    @Query(value = """
            SELECT DISTINCT pao.id_plan_action AS idPlanAction,
                   act.id_activite AS idActivite,
                   act.code AS code,
                   act.designation AS designation,
                   srv.id_service AS serviceId,
                   srv.nom AS serviceNom,
                   o.code AS origineCode,
                   o.type_origine AS typeOrigine
            FROM plan_action_origine pao
            JOIN origine o ON o.id_origine = pao.id_origine
            JOIN activite_origine ao ON ao.id_origine = o.id_origine
            JOIN activite act ON act.id_activite = ao.id_activite
            JOIN service srv ON srv.id_service = act.id_service
            WHERE pao.id_plan_action IN (:ids)
            ORDER BY pao.id_plan_action, act.code, o.code
            """, nativeQuery = true)
    List<ActiviteLieeExportRow> findActivitesLieesPourExport(@Param("ids") List<Integer> ids);

    @Query(value = """
            SELECT aff.id_plan_action AS idPlanAction,
                   u.nom AS nom,
                   u.prenom AS prenom,
                   r.code AS roleCode
            FROM affectation_plan_action aff
            JOIN utilisateur u ON u.id_utilisateur = aff.id_utilisateur
            JOIN role r ON r.id_role = aff.id_role
            WHERE aff.id_plan_action IN (:ids)
              AND aff.date_desaffectation IS NULL
              AND r.code IN ('RESPONSABLE', 'SUPPLEANT')
            ORDER BY aff.id_plan_action, r.code DESC, u.nom, u.prenom
            """, nativeQuery = true)
    List<ResponsablePlanExportRow> findResponsablesPourExport(@Param("ids") List<Integer> ids);

    @Query(value = """
            SELECT av.id_plan_action AS idPlanAction,
                   av.date_changement AS dateChangement,
                   u.nom AS nom,
                   u.prenom AS prenom,
                   av.valeur_pourcentage AS valeurPourcentage,
                   av.commentaire AS commentaire,
                   s.code AS statutCode,
                   s.libelle AS statutLibelle
            FROM avancement_plan_action av
            JOIN utilisateur u ON u.id_utilisateur = av.id_utilisateur
            JOIN statut s ON s.id_statut = av.id_statut
            WHERE av.id_plan_action IN (:ids)
            ORDER BY av.id_plan_action, av.date_changement DESC, av.id_avancement_plan_action DESC
            """, nativeQuery = true)
    List<SuiviPlanActionExportRow> findSuivisPourExport(@Param("ids") List<Integer> ids);

    @Query(value = """
            SELECT dr.id_plan_action AS idPlanAction,
                   dr.date_demande AS dateDemande,
                   dr.motif AS motif
            FROM demande_reprise_plan_action dr
            WHERE dr.id_plan_action IN (:ids)
              AND NOT EXISTS (
                  SELECT 1 FROM demande_reprise_plan_action dr2
                  WHERE dr2.id_plan_action = dr.id_plan_action
                    AND (dr2.date_demande, dr2.id_demande_reprise_plan_action)
                        > (dr.date_demande, dr.id_demande_reprise_plan_action))
            """, nativeQuery = true)
    List<MotifReprisePlanExportRow> findMotifsReprisePourExport(@Param("ids") List<Integer> ids);
}
