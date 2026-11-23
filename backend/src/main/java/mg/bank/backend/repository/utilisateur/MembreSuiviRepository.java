package mg.bank.backend.repository.utilisateur;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.repository.SqlFragments;

@Repository
@RequiredArgsConstructor
public class MembreSuiviRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // Casts explicites sur les occurrences "IS NULL" : sans cela PostgreSQL
    // abandonne avec "could not determine data type of parameter $N" dès que
    // la valeur liée est null. Les comparaisons directes (srv.id_service = :x)
    // n'ont pas besoin de cast : le type est inféré depuis la colonne.
    private static final String SCOPED_CTES = """
            WITH activites AS (
                SELECT act.id_activite,
                       CASE WHEN act.id_objectif_specifique IS NOT NULL THEN 'PTA' ELSE 'NON_PTA' END AS categorie,
                       act.code,
                       act.designation,
                       act.date_debut_prevue,
                       act.date_fin_prevue,
                       srv.id_service,
                       srv.nom AS nom_service,
                       srv.id_departement,
                       sta.code AS statut,
                       COALESCE(av.avancement, 0)::numeric AS avancement
                FROM activite act
                JOIN service srv ON srv.id_service = act.id_service
                """ + SqlFragments.STATUT_COURANT + SqlFragments.AVANCEMENT_ACTIVITE_COURANT
            + """
                        WHERE act.date_debut_prevue <= :dateFin
                          AND COALESCE(act.date_fin_prevue, act.date_debut_prevue) >= :dateDebut
                                            AND (sta.code IS NULL OR sta.code NOT IN ('EN_ATTENTE_VALIDATION', 'VALIDEE', 'REJETE'))
                           AND (CAST(:idDepartement AS integer) IS NULL OR srv.id_departement = :idDepartement)
                           AND (CAST(:idService     AS integer) IS NULL OR srv.id_service     = :idService)
                     """
            + SqlFragments.PORTEE_UTILISATEUR
            + """
                     ), plan_services AS (
                        SELECT DISTINCT pao.id_plan_action,
                               srv.id_service,
                               srv.nom AS nom_service,
                               srv.id_departement
                        FROM plan_action_origine pao
                        JOIN origine o ON o.id_origine = pao.id_origine
                        JOIN activite_origine ao ON ao.id_origine = o.id_origine
                        JOIN activite act ON act.id_activite = ao.id_activite
                        JOIN service srv ON srv.id_service = act.id_service
                    ), plan_map AS (
                        SELECT id_plan_action,
                               COUNT(DISTINCT id_service) AS nombre_services,
                               COUNT(DISTINCT id_departement) AS nombre_departements,
                               MIN(id_service) AS id_service_unique,
                               MIN(nom_service) AS nom_service_unique,
                               MIN(id_departement) AS id_departement_unique,
                               STRING_AGG(DISTINCT nom_service, ', ') AS services_lies
                        FROM plan_services
                        GROUP BY id_plan_action
                    ), plans AS (
                        SELECT pa.id_plan_action,
                               pa.code,
                               pa.designation,
                               pa.date_debut_prevue,
                               pa.date_fin_prevue,
                               pm.nombre_services,
                               pm.id_service_unique AS id_service,
                               pm.nom_service_unique AS nom_service,
                               pm.id_departement_unique AS id_departement,
                               pm.services_lies,
                               sta.code AS statut,
                               COALESCE(av.valeur_pourcentage, 0)::numeric AS avancement
                        FROM plan_action pa
                        JOIN plan_map pm ON pm.id_plan_action = pa.id_plan_action
                        """
            + SqlFragments.AVANCEMENT_PLAN_ACTION_DERNIER + """
                         WHERE (CAST(:idDepartement AS integer) IS NULL
                                OR (pm.nombre_departements = 1
                                    AND pm.id_departement_unique = :idDepartement))
                           AND (CAST(:idService AS integer) IS NULL
                                OR (pm.nombre_services = 1 AND pm.id_service_unique = :idService))
                           AND pa.date_debut_prevue <= :dateFin
                           AND pa.date_fin_prevue >= :dateDebut
                     """
            + SqlFragments.PORTEE_UTILISATEUR_PLAN
            + """
                     )
                    """;

    private static final String TERMINAL_ACTIVITY_SQL = "('TERMINEE', 'ANNULEE', 'REPORTEE')";

    public List<Map<String, Object>> findMembres(MapSqlParameterSource params, String recherche) {
        params.addValue("rechercheMembre", recherche == null || recherche.isBlank()
                ? null
                : "%" + recherche.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        String sql = SCOPED_CTES + """
                , affectations_activites AS (
                    SELECT DISTINCT aff.id_utilisateur, a.id_activite, a.categorie,
                           a.avancement, a.statut, a.date_fin_prevue
                    FROM affectation_activite aff
                    JOIN activites a ON a.id_activite = aff.id_activite
                    WHERE aff.date_affectation < CAST(:dateFin AS timestamp) + INTERVAL '1 day'
                      AND (aff.date_desaffectation IS NULL
                           OR aff.date_desaffectation >= CAST(:dateDebut AS timestamp))
                    UNION
                    SELECT DISTINCT aff.id_utilisateur, a.id_activite, a.categorie,
                           a.avancement, a.statut, a.date_fin_prevue
                    FROM affectation_sous_activite aff
                    JOIN sous_activite sa ON sa.id_sous_activite = aff.id_sous_activite
                    JOIN activites a ON a.id_activite = sa.id_activite
                    WHERE aff.date_affectation < CAST(:dateFin AS timestamp) + INTERVAL '1 day'
                      AND (aff.date_desaffectation IS NULL
                           OR aff.date_desaffectation >= CAST(:dateDebut AS timestamp))
                ), affectations_plans AS (
                    SELECT DISTINCT aff.id_utilisateur, p.id_plan_action,
                           p.avancement, p.statut, p.date_fin_prevue
                    FROM affectation_plan_action aff
                    JOIN plans p ON p.id_plan_action = aff.id_plan_action
                    WHERE aff.date_affectation < CAST(:dateFin AS timestamp) + INTERVAL '1 day'
                      AND (aff.date_desaffectation IS NULL
                           OR aff.date_desaffectation >= CAST(:dateDebut AS timestamp))
                ), activites_membre AS (
                    SELECT id_utilisateur,
                           COUNT(*) AS total_activites,
                           COUNT(*) FILTER (WHERE categorie = 'PTA')     AS total_pta,
                           COUNT(*) FILTER (WHERE categorie = 'NON_PTA') AS total_non_pta,
                           AVG(avancement) AS avancement_activites,
                           AVG(avancement) FILTER (WHERE categorie = 'PTA')     AS avancement_pta,
                           AVG(avancement) FILTER (WHERE categorie = 'NON_PTA') AS avancement_non_pta,
                           SUM(avancement) AS somme_avancement_activites,
                           COUNT(*) FILTER (
                               WHERE date_fin_prevue < CURRENT_DATE
                                 AND (statut IS NULL OR statut NOT IN ('TERMINEE', 'ANNULEE', 'REPORTEE'))
                           ) AS activites_en_retard,
                           COUNT(*) FILTER (
                               WHERE date_fin_prevue < CURRENT_DATE
                                 AND (statut IS NULL OR statut NOT IN ('TERMINEE', 'ANNULEE', 'REPORTEE'))
                                 AND categorie = 'PTA'
                           ) AS activites_pta_en_retard,
                           COUNT(*) FILTER (
                               WHERE date_fin_prevue < CURRENT_DATE
                                 AND (statut IS NULL OR statut NOT IN ('TERMINEE', 'ANNULEE', 'REPORTEE'))
                                 AND categorie = 'NON_PTA'
                           ) AS activites_non_pta_en_retard
                    FROM affectations_activites
                    GROUP BY id_utilisateur
                ), plans_membre AS (
                    SELECT id_utilisateur,
                           COUNT(*) AS total_plans,
                           AVG(avancement) AS avancement_plans,
                           SUM(avancement) AS somme_avancement_plans,
                           COUNT(*) FILTER (
                               WHERE date_fin_prevue < CURRENT_DATE
                                 AND statut IS DISTINCT FROM 'TERMINEE'
                                 AND avancement < 100
                           ) AS plans_en_retard
                    FROM affectations_plans
                    GROUP BY id_utilisateur
                ), membres_scope AS (
                    SELECT u.id_utilisateur
                    FROM utilisateur u
                    WHERE u.actif = TRUE
                      AND (CAST(:idDepartement AS integer) IS NULL OR u.id_departement = :idDepartement)
                      AND (CAST(:idService AS integer) IS NULL OR u.id_service = :idService)
                 )
                SELECT u.id_utilisateur AS id_utilisateur,
                       u.nom AS nom,
                       u.prenom AS prenom,
                       COALESCE(srv.id_service, u.id_service) AS id_service,
                       COALESCE(srv.nom, 'Service non renseigné') AS service,
                       COALESCE(am.total_activites, 0) AS total_activites,
                       COALESCE(am.total_pta, 0)     AS total_pta,
                       COALESCE(am.total_non_pta, 0) AS total_non_pta,
                       am.avancement_pta        AS avancement_pta,
                       am.avancement_non_pta    AS avancement_non_pta,
                       am.avancement_activites  AS avancement_activites,
                       COALESCE(am.activites_pta_en_retard, 0)     AS activites_pta_en_retard,
                       COALESCE(am.activites_non_pta_en_retard, 0) AS activites_non_pta_en_retard,
                       COALESCE(pm.total_plans, 0) AS total_plans,
                       pm.avancement_plans         AS avancement_plans,
                       COALESCE(pm.plans_en_retard, 0) AS plans_actions_en_retard,
                       CASE WHEN COALESCE(am.total_activites, 0) + COALESCE(pm.total_plans, 0) = 0 THEN NULL
                            ELSE (COALESCE(am.somme_avancement_activites, 0)
                                + COALESCE(pm.somme_avancement_plans, 0))
                                / (COALESCE(am.total_activites, 0) + COALESCE(pm.total_plans, 0))
                       END AS avancement_global,
                       COALESCE(am.activites_en_retard, 0) AS activites_en_retard,
                       COALESCE(am.activites_en_retard, 0) + COALESCE(pm.plans_en_retard, 0) AS elements_en_retard
                FROM membres_scope ms
                JOIN utilisateur u ON u.id_utilisateur = ms.id_utilisateur
                LEFT JOIN service srv ON srv.id_service = u.id_service
                LEFT JOIN activites_membre am ON am.id_utilisateur = u.id_utilisateur
                LEFT JOIN plans_membre pm ON pm.id_utilisateur = u.id_utilisateur
                WHERE (CAST(:rechercheMembre AS text) IS NULL
                       OR LOWER(u.nom) LIKE :rechercheMembre
                       OR LOWER(u.prenom) LIKE :rechercheMembre
                       OR LOWER(u.prenom || ' ' || u.nom) LIKE :rechercheMembre
                       OR LOWER(u.nom || ' ' || u.prenom) LIKE :rechercheMembre)
                ORDER BY LOWER(u.nom), LOWER(u.prenom), u.id_utilisateur
                """;
        return jdbc.queryForList(sql, params);
    }

    public List<Map<String, Object>> findCategories(MapSqlParameterSource params) {
        String sql = SCOPED_CTES + """
                SELECT 'PTA' AS categorie, COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE statut = 'TERMINEE') AS termines,
                       COUNT(*) FILTER (WHERE statut = 'EN_COURS') AS en_cours,
                       COUNT(*) FILTER (WHERE date_fin_prevue < CURRENT_DATE
                           AND (statut IS NULL OR statut NOT IN ('TERMINEE', 'ANNULEE', 'REPORTEE'))) AS en_retard,
                       COALESCE(AVG(avancement), 0) AS avancement
                FROM activites WHERE categorie = 'PTA'
                UNION ALL
                SELECT 'NON_PTA', COUNT(*),
                       COUNT(*) FILTER (WHERE statut = 'TERMINEE'),
                       COUNT(*) FILTER (WHERE statut = 'EN_COURS'),
                       COUNT(*) FILTER (WHERE date_fin_prevue < CURRENT_DATE
                           AND (statut IS NULL OR statut NOT IN ('TERMINEE', 'ANNULEE', 'REPORTEE'))),
                       COALESCE(AVG(avancement), 0)
                FROM activites WHERE categorie = 'NON_PTA'
                UNION ALL
                SELECT 'PLANS_ACTIONS', COUNT(*),
                       COUNT(*) FILTER (WHERE statut = 'TERMINEE' OR avancement >= 100),
                       COUNT(*) FILTER (WHERE statut = 'EN_COURS' OR (avancement > 0 AND avancement < 100)),
                       COUNT(*) FILTER (WHERE date_fin_prevue < CURRENT_DATE
                           AND statut IS DISTINCT FROM 'TERMINEE' AND avancement < 100),
                       COALESCE(AVG(avancement), 0)
                FROM plans
                """;
        return jdbc.queryForList(sql, params);
    }

    public List<Map<String, Object>> findStatutsActivites(MapSqlParameterSource params) {
        String sql = SCOPED_CTES + """
                SELECT statut_code, libelle, SUM(nombre) AS nombre
                FROM (
                    SELECT CASE
                               WHEN date_fin_prevue < CURRENT_DATE
                                    AND (statut IS NULL OR statut NOT IN """ + TERMINAL_ACTIVITY_SQL + ")\n" + """
                         AND statut IS DISTINCT FROM 'SUSPENDUE'
                        THEN 'EN_RETARD'
                    ELSE COALESCE(statut, 'NON_COMMENCEE')
                END AS statut_code,
                CASE
                    WHEN date_fin_prevue < CURRENT_DATE
                         AND (statut IS NULL OR statut NOT IN """ + TERMINAL_ACTIVITY_SQL + ")\n"
                + """
                                            AND statut IS DISTINCT FROM 'SUSPENDUE'
                                           THEN 'En retard'
                                       WHEN statut = 'NON_COMMENCEE' OR statut IS NULL THEN 'Non commencée'
                                       WHEN statut = 'EN_COURS' THEN 'En cours'
                                       WHEN statut = 'TERMINEE' THEN 'Terminée'
                                       WHEN statut = 'SUSPENDUE' THEN 'Suspendue'
                                       WHEN statut = 'REPORTEE' THEN 'Reportée'
                                       ELSE statut
                                   END AS libelle,
                                   COUNT(*) AS nombre
                                                FROM activites
                                                WHERE (CAST(:categorie AS text) IS NULL OR categorie = :categorie)
                            GROUP BY 1, 2
                        ) s
                                        WHERE statut_code IN ('NON_COMMENCEE', 'EN_COURS', 'TERMINEE', 'EN_RETARD', 'SUSPENDUE', 'REPORTEE', 'ANNULEE')
                        GROUP BY statut_code, libelle
                        ORDER BY CASE statut_code
                            WHEN 'NON_COMMENCEE' THEN 1 WHEN 'EN_COURS' THEN 2 WHEN 'EN_RETARD' THEN 3
                            WHEN 'TERMINEE' THEN 4 WHEN 'SUSPENDUE' THEN 5 WHEN 'REPORTEE' THEN 6 WHEN 'ANNULEE' THEN 7 ELSE 8 END
                        """;
        return jdbc.queryForList(sql, params);
    }

    public List<Map<String, Object>> findServices(MapSqlParameterSource params) {
        String sql = SCOPED_CTES + """
                SELECT id_service, nom_service,
                       COALESCE(AVG(avancement) FILTER (WHERE categorie = 'PTA'), 0) AS pta,
                       COALESCE(AVG(avancement) FILTER (WHERE categorie = 'NON_PTA'), 0) AS non_pta,
                       COALESCE(AVG(avancement) FILTER (WHERE categorie = 'PLANS_ACTIONS'), 0) AS plans_actions
                FROM (
                    SELECT id_service, nom_service, categorie, avancement FROM activites
                    UNION ALL
                    SELECT id_service, nom_service, 'PLANS_ACTIONS', avancement FROM plans
                    WHERE nombre_services = 1
                ) elements
                GROUP BY id_service, nom_service
                ORDER BY nom_service
                """;
        return jdbc.queryForList(sql, params);
    }

    public long countPlansMultiServices(MapSqlParameterSource params) {
        String sql = """
                WITH plan_services AS (
                    SELECT DISTINCT pao.id_plan_action, srv.id_service, srv.id_departement
                    FROM plan_action_origine pao
                    JOIN origine o ON o.id_origine = pao.id_origine
                    JOIN activite_origine ao ON ao.id_origine = o.id_origine
                    JOIN activite act ON act.id_activite = ao.id_activite
                    JOIN service srv ON srv.id_service = act.id_service
                ), plan_map AS (
                    SELECT id_plan_action, COUNT(DISTINCT id_service) AS nombre_services,
                           COUNT(DISTINCT id_departement) AS nombre_departements,
                           MIN(id_departement) AS id_departement
                    FROM plan_services GROUP BY id_plan_action
                )
                SELECT COUNT(*) FROM plan_action pa
                JOIN plan_map pm ON pm.id_plan_action = pa.id_plan_action
                WHERE (CAST(:idDepartement AS integer) IS NULL
                       OR (pm.nombre_departements = 1 AND pm.id_departement = :idDepartement))
                  AND pm.nombre_services > 1
                  AND pa.date_debut_prevue <= :dateFin AND pa.date_fin_prevue >= :dateDebut
                """
                + SqlFragments.PORTEE_UTILISATEUR_PLAN
                + """
                """;
        Number count = jdbc.queryForObject(sql, params, Number.class);
        return count == null ? 0 : count.longValue();
    }

    public long countMembres(MapSqlParameterSource params) {
        String sql = SCOPED_CTES
                + """
                        SELECT COUNT(DISTINCT id_utilisateur)
                        FROM (
                            SELECT aff.id_utilisateur
                            FROM affectation_activite aff
                            JOIN activites act ON act.id_activite = aff.id_activite
                            WHERE aff.date_affectation < CAST(:dateFin AS timestamp) + INTERVAL '1 day'
                              AND (aff.date_desaffectation IS NULL OR aff.date_desaffectation >= CAST(:dateDebut AS timestamp))
                              AND (CAST(:categorie AS text) IS NULL OR act.categorie = :categorie)
                            UNION
                                                SELECT aff.id_utilisateur
                                                FROM affectation_sous_activite aff
                                                JOIN sous_activite sa ON sa.id_sous_activite = aff.id_sous_activite
                                                JOIN activites act ON act.id_activite = sa.id_activite
                                                WHERE aff.date_affectation < CAST(:dateFin AS timestamp) + INTERVAL '1 day'
                                                    AND (aff.date_desaffectation IS NULL OR aff.date_desaffectation >= CAST(:dateDebut AS timestamp))
                                                    AND (CAST(:categorie AS text) IS NULL OR act.categorie = :categorie)
                                                UNION
                            SELECT aff.id_utilisateur
                            FROM affectation_plan_action aff
                            JOIN plans pa ON pa.id_plan_action = aff.id_plan_action
                            WHERE aff.date_affectation < CAST(:dateFin AS timestamp) + INTERVAL '1 day'
                              AND (aff.date_desaffectation IS NULL OR aff.date_desaffectation >= CAST(:dateDebut AS timestamp))
                              AND (CAST(:categorie AS text) IS NULL OR CAST(:categorie AS text) = 'PLANS_ACTIONS')
                        ) membres
                        """;
        Number count = jdbc.queryForObject(sql, params, Number.class);
        return count == null ? 0 : count.longValue();
    }

    public List<Map<String, Object>> findEvolution(MapSqlParameterSource params) {
        String sql = """
                WITH mois AS (
                    SELECT generate_series(
                        date_trunc('month', CAST(:dateDebut AS date))::date,
                        date_trunc('month', LEAST(CAST(:dateFin AS date), CURRENT_DATE))::date,
                        INTERVAL '1 month')::date AS debut_mois
                ), activites_scope AS (
                    SELECT act.id_activite,
                           CASE WHEN act.id_objectif_specifique IS NOT NULL THEN 'PTA' ELSE 'NON_PTA' END AS categorie
                    FROM activite act JOIN service srv ON srv.id_service = act.id_service
                    WHERE act.date_debut_prevue <= :dateFin
                      AND COALESCE(act.date_fin_prevue, act.date_debut_prevue) >= :dateDebut
                      AND (CAST(:idDepartement AS integer) IS NULL OR srv.id_departement = :idDepartement)
                      AND (CAST(:idService     AS integer) IS NULL OR srv.id_service     = :idService)
                 """
                + SqlFragments.PORTEE_UTILISATEUR
                + """
                 ), plan_services AS (
                    SELECT DISTINCT pao.id_plan_action, srv.id_service, srv.id_departement
                    FROM plan_action_origine pao
                    JOIN origine o ON o.id_origine = pao.id_origine
                    JOIN activite_origine ao ON ao.id_origine = o.id_origine
                    JOIN activite act ON act.id_activite = ao.id_activite
                    JOIN service srv ON srv.id_service = act.id_service
                ), plan_map AS (
                    SELECT id_plan_action, COUNT(DISTINCT id_service) AS nombre_services,
                           COUNT(DISTINCT id_departement) AS nombre_departements,
                           MIN(id_service) AS id_service_unique,
                           MIN(id_departement) AS id_departement_unique
                    FROM plan_services GROUP BY id_plan_action
                ), plans_scope AS (
                    SELECT pa.id_plan_action FROM plan_action pa
                    JOIN plan_map pm ON pm.id_plan_action = pa.id_plan_action
                    WHERE (CAST(:idDepartement AS integer) IS NULL
                           OR (pm.nombre_departements = 1 AND pm.id_departement_unique = :idDepartement))
                      AND (CAST(:idService AS integer) IS NULL
                           OR (pm.nombre_services = 1 AND pm.id_service_unique = :idService))
                      AND pa.date_debut_prevue <= :dateFin AND pa.date_fin_prevue >= :dateDebut
                 """
                + SqlFragments.PORTEE_UTILISATEUR_PLAN
                + """
                 ), activity_month AS (
                    SELECT m.debut_mois, a.categorie,
                           AVG(COALESCE(progress.avancement, 0)) AS avancement,
                           COUNT(*) FILTER (WHERE progress.a_historique) AS historique_count
                    FROM mois m CROSS JOIN activites_scope a
                    LEFT JOIN LATERAL (
                        SELECT COALESCE(AVG(COALESCE(dernier.valeur_pourcentage, 0)), 0) AS avancement,
                               COALESCE(BOOL_OR(dernier.valeur_pourcentage IS NOT NULL), FALSE) AS a_historique
                        FROM sous_activite sa
                        LEFT JOIN LATERAL (
                            SELECT av.valeur_pourcentage
                            FROM avancement_sous_activite av
                            WHERE av.id_sous_activite = sa.id_sous_activite
                              AND av.date_changement < m.debut_mois + INTERVAL '1 month'
                            ORDER BY av.date_changement DESC, av.id_historique_sous_activite DESC
                            LIMIT 1
                        ) dernier ON TRUE
                        WHERE sa.id_activite = a.id_activite
                    ) progress ON TRUE
                    GROUP BY m.debut_mois, a.categorie
                ), plan_month AS (
                    SELECT m.debut_mois, 'PLANS_ACTIONS' AS categorie,
                           AVG(COALESCE(dernier.valeur_pourcentage, 0)) AS avancement,
                           COUNT(*) FILTER (WHERE dernier.valeur_pourcentage IS NOT NULL) AS historique_count
                    FROM mois m CROSS JOIN plans_scope p
                    LEFT JOIN LATERAL (
                        SELECT av.valeur_pourcentage
                        FROM avancement_plan_action av
                        WHERE av.id_plan_action = p.id_plan_action
                          AND av.date_changement < m.debut_mois + INTERVAL '1 month'
                        ORDER BY av.date_changement DESC, av.id_avancement_plan_action DESC
                        LIMIT 1
                    ) dernier ON TRUE
                    GROUP BY m.debut_mois
                ), toutes AS (
                    SELECT * FROM activity_month UNION ALL SELECT * FROM plan_month
                )
                SELECT debut_mois, categorie,
                       CASE WHEN SUM(historique_count) = 0 THEN NULL
                            ELSE AVG(avancement) END AS avancement
                FROM toutes
                GROUP BY debut_mois, categorie
                ORDER BY debut_mois, categorie
                """;
        return jdbc.queryForList(sql, params);
    }

    public List<Map<String, Object>> findElements(MapSqlParameterSource params) {
        String sql = SCOPED_CTES + """
                SELECT id, categorie, code, designation, service, statut, date_echeance, avancement
                FROM (
                    SELECT id_activite AS id, categorie, code, designation, nom_service AS service,
                           statut, date_fin_prevue AS date_echeance, avancement
                    FROM activites
                    WHERE (CAST(:categorie AS text) IS NULL OR categorie = :categorie)
                    UNION ALL
                    SELECT id_plan_action, 'PLANS_ACTIONS', code, designation, services_lies,
                           statut, date_fin_prevue, avancement
                    FROM plans
                    WHERE (CAST(:categorie AS text) IS NULL OR CAST(:categorie AS text) = 'PLANS_ACTIONS')
                ) elements
                ORDER BY categorie, date_echeance NULLS LAST, code
                """;
        return jdbc.queryForList(sql, params);
    }
}