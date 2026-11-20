package mg.bank.backend.repository.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.repository.SqlFragments;
import mg.bank.backend.repository.projection.dashboard.DashboardAvancementRow;
import mg.bank.backend.repository.projection.dashboard.DashboardEcheanceRow;
import mg.bank.backend.repository.projection.dashboard.DashboardKpiRow;
import mg.bank.backend.repository.projection.dashboard.DashboardPlanActionOrigineRow;
import mg.bank.backend.repository.projection.dashboard.DashboardPlanActionRow;
import mg.bank.backend.repository.projection.dashboard.DashboardPtaRow;
import mg.bank.backend.repository.projection.dashboard.DashboardServiceRow;
import mg.bank.backend.repository.projection.dashboard.DashboardStatutPrioriteRow;
import mg.bank.backend.repository.projection.dashboard.DashboardStatutRow;
import mg.bank.backend.repository.projection.dashboard.DashboardTypeActiviteRow;

/**
 * Requetes du dashboard.
 *
 * Toutes les requetes portant sur les activites partagent le meme
 * filtrage : chevauchement avec la periode globale, perimetre service /
 * departement, et reconstitution des statuts / avancements a
 * `dateReference` (= min(fin, aujourd'hui)). Les brouillons sont exclus du
 * tableau de bord opérationnel ; les activités en attente de validation
 * restent visibles pour le pilotage des décisions à traiter.
 */
public interface DashboardRepository extends Repository<Activite, Integer> {

        /*
         * ------------------------------------------------------------------
         * 1. KPI
         * ------------------------------------------------------------------
         */
        @Query(value = """
                        SELECT COUNT(*) AS total,
                               COUNT(*) FILTER (WHERE sta.code = 'EN_COURS')          AS enCours,
                               COUNT(*) FILTER (WHERE sta.code = 'TERMINEE')          AS terminees,
                               COUNT(*) FILTER (WHERE sta.code = 'NON_COMMENCEE')     AS nonCommencees,
                               COUNT(*) FILTER (WHERE sta.code = 'SUSPENDUE')         AS suspendues,
                               COUNT(*) FILTER (WHERE sta.code = 'ANNULEE')           AS annulees,
                               COUNT(*) FILTER (WHERE sta.code = 'REPORTEE')          AS reportees,
                               COUNT(*) FILTER (WHERE sta.code = 'EN_ATTENTE_VALIDATION') AS enAttenteValidation,
                               COUNT(*) FILTER (
                                   WHERE act.date_fin_prevue IS NOT NULL
                                     AND act.date_fin_prevue < CAST(:dateReference AS date)
                                     AND (sta.code IS NULL
                                          OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ',')))))
                                                                                       AS enRetard,
                               COALESCE(AVG(av.avancement), 0)                        AS avancementGlobal
                        FROM activite act
                        LEFT JOIN service srv ON srv.id_service = act.id_service
                        """
                        + SqlFragments.STATUT_ACTIVITE_A_DATE
                        + SqlFragments.AVANCEMENT_ACTIVITE_A_DATE
                        + """
                                        WHERE act.date_debut_prevue <= CAST(:dateFin AS date)
                                          AND (act.date_fin_prevue IS NULL OR act.date_fin_prevue >= CAST(:dateDebut AS date))
                                          AND (:serviceFiltre IS NULL OR act.id_service = :serviceFiltre)
                                          AND (:departementFiltre IS NULL OR srv.id_departement = :departementFiltre)
                                          AND (sta.code IS DISTINCT FROM 'BROUILLON')
                                        """, nativeQuery = true)
        DashboardKpiRow kpi(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference,
                        @Param("serviceFiltre") Integer serviceFiltre,
                        @Param("departementFiltre") Integer departementFiltre,
                        @Param("codesTerminaux") String codesTerminaux);

        /*
         * ------------------------------------------------------------------
         * 2. Avancement
         * ------------------------------------------------------------------
         * avancementGlobal = moyenne simple des avancements moyens par activité.
         * Chaque activité a le même poids ; les sous-activités sans historique
         * valent 0 %, conformément au formulaire de saisie du suivi.
         */
        @Query(value = """
                        SELECT
                            COUNT(*)                                              AS totalActivites,
                            COALESCE(SUM(sous.total), 0)                          AS totalSousActivites,
                            COALESCE(SUM(sous.terminees), 0)                      AS sousActivitesTerminees,
                            COALESCE(AVG(av.avancement), 0)                       AS avancementGlobal
                        FROM activite act
                        LEFT JOIN service srv ON srv.id_service = act.id_service
                        """
                        + SqlFragments.STATUT_ACTIVITE_A_DATE
                        + SqlFragments.AVANCEMENT_ACTIVITE_A_DATE
                        + """
                                        LEFT JOIN LATERAL (
                                            SELECT COUNT(*)                                          AS total,
                                                  COUNT(*) FILTER (WHERE COALESCE(dernier.valeur_pourcentage, 0) = 100) AS terminees
                                            FROM sous_activite sa
                                                                  LEFT JOIN LATERAL (
                                                SELECT av2.valeur_pourcentage
                                                FROM avancement_sous_activite av2
                                                WHERE av2.id_sous_activite = sa.id_sous_activite
                                                                        AND av2.date_changement::date <= CAST(:dateReference AS date)
                                                  AND NOT EXISTS (
                                                      SELECT 1 FROM avancement_sous_activite av3
                                                      WHERE av3.id_sous_activite = sa.id_sous_activite
                                                                              AND av3.date_changement::date <= CAST(:dateReference AS date)
                                                        AND (av3.date_changement, av3.id_historique_sous_activite)
                                                            > (av2.date_changement, av2.id_historique_sous_activite))
                                            ) dernier ON TRUE
                                            WHERE sa.id_activite = act.id_activite
                                        ) sous ON TRUE
                                        WHERE act.date_debut_prevue <= CAST(:dateFin AS date)
                                          AND (act.date_fin_prevue IS NULL OR act.date_fin_prevue >= CAST(:dateDebut AS date))
                                          AND (:serviceFiltre IS NULL OR act.id_service = :serviceFiltre)
                                          AND (:departementFiltre IS NULL OR srv.id_departement = :departementFiltre)
                                          AND (sta.code IS DISTINCT FROM 'BROUILLON')
                                        """, nativeQuery = true)
        DashboardAvancementRow avancement(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference,
                        @Param("serviceFiltre") Integer serviceFiltre,
                        @Param("departementFiltre") Integer departementFiltre);

        /*
         * ------------------------------------------------------------------
         * 3. Répartition exhaustive par statut courant
         * ------------------------------------------------------------------
         */
        @Query(value = """
                        SELECT COALESCE(sta.code, 'SANS_STATUT') AS statut,
                             COALESCE(sta.libelle, 'Sans statut') AS libelle,
                             COUNT(*) AS nombre
                            FROM activite act
                            LEFT JOIN service srv ON srv.id_service = act.id_service
                            """
                        + SqlFragments.STATUT_ACTIVITE_A_DATE
                        + """
                                    WHERE act.date_debut_prevue <= CAST(:dateFin AS date)
                                      AND (act.date_fin_prevue IS NULL OR act.date_fin_prevue >= CAST(:dateDebut AS date))
                                      AND (:serviceFiltre IS NULL OR act.id_service = :serviceFiltre)
                                      AND (:departementFiltre IS NULL OR srv.id_departement = :departementFiltre)
                                                  AND (sta.code IS DISTINCT FROM 'BROUILLON')
                                GROUP BY sta.code, sta.libelle
                                ORDER BY CASE COALESCE(sta.code, 'SANS_STATUT')
                                  WHEN 'BROUILLON' THEN 1
                                  WHEN 'EN_ATTENTE_VALIDATION' THEN 2
                                  WHEN 'NON_COMMENCEE' THEN 3
                                  WHEN 'EN_COURS' THEN 4
                                  WHEN 'SUSPENDUE' THEN 5
                                  WHEN 'TERMINEE' THEN 6
                                  WHEN 'REPORTEE' THEN 7
                                  WHEN 'ANNULEE' THEN 8
                                  WHEN 'VALIDEE' THEN 9
                                  WHEN 'REJETE' THEN 10
                                  ELSE 11
                                END, statut
                                        """, nativeQuery = true)
        List<DashboardStatutRow> statuts(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference,
                        @Param("serviceFiltre") Integer serviceFiltre,
                        @Param("departementFiltre") Integer departementFiltre);

        /*
         * ------------------------------------------------------------------
         * 3b. Répartition croisée statut × priorité
         * ------------------------------------------------------------------
         *
         * Chaque ligne fournit le nombre d'activités pour un couple
         * (statut, priorité). Les activités sans priorité sont conservées
         * sous le code SANS_PRIORITE afin de ne pas fausser les totaux.
         */
        @Query(value = """
                        SELECT COALESCE(sta.code, 'SANS_STATUT')       AS statut,
                               COALESCE(sta.libelle, 'Sans statut')     AS statutLibelle,
                               COALESCE(pr.code, 'SANS_PRIORITE')       AS priorite,
                               COALESCE(pr.libelle, 'Sans priorité')    AS prioriteLibelle,
                               COUNT(*)                                 AS nombre
                        FROM activite act
                        LEFT JOIN service srv ON srv.id_service = act.id_service
                        LEFT JOIN priorite pr ON pr.id_priorite = act.id_priorite
                        """
                        + SqlFragments.STATUT_ACTIVITE_A_DATE
                        + """
                                        WHERE act.date_debut_prevue <= CAST(:dateFin AS date)
                                          AND (act.date_fin_prevue IS NULL OR act.date_fin_prevue >= CAST(:dateDebut AS date))
                                          AND (:serviceFiltre IS NULL OR act.id_service = :serviceFiltre)
                                          AND (:departementFiltre IS NULL OR srv.id_departement = :departementFiltre)
                                          AND (sta.code IS DISTINCT FROM 'BROUILLON')
                                        GROUP BY sta.code, sta.libelle, pr.code, pr.libelle
                                        ORDER BY CASE COALESCE(sta.code, 'SANS_STATUT')
                                          WHEN 'BROUILLON' THEN 1
                                          WHEN 'EN_ATTENTE_VALIDATION' THEN 2
                                          WHEN 'NON_COMMENCEE' THEN 3
                                          WHEN 'EN_COURS' THEN 4
                                          WHEN 'SUSPENDUE' THEN 5
                                          WHEN 'TERMINEE' THEN 6
                                          WHEN 'REPORTEE' THEN 7
                                          WHEN 'ANNULEE' THEN 8
                                          WHEN 'VALIDEE' THEN 9
                                          WHEN 'REJETE' THEN 10
                                          ELSE 11
                                        END, statut,
                                        CASE COALESCE(pr.code, 'SANS_PRIORITE')
                                          WHEN 'CRITIQUE' THEN 1
                                          WHEN 'HAUTE' THEN 2
                                          WHEN 'NORMALE' THEN 3
                                          WHEN 'FAIBLE' THEN 4
                                          ELSE 5
                                        END, priorite
                                        """, nativeQuery = true)
        List<DashboardStatutPrioriteRow> statutsPriorites(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference,
                        @Param("serviceFiltre") Integer serviceFiltre,
                        @Param("departementFiltre") Integer departementFiltre);

        /*
         * ------------------------------------------------------------------
         * 4. Services
         * ------------------------------------------------------------------
         */
        @Query(value = """
                        SELECT srv.id_service                                       AS idService,
                               srv.nom                                              AS service,
                               COUNT(*)                                             AS totalActivites,
                               COUNT(*) FILTER (WHERE sta.code = 'TERMINEE')        AS terminees,
                               COUNT(*) FILTER (WHERE sta.code = 'EN_COURS')        AS enCours,
                               COUNT(*) FILTER (
                                   WHERE act.date_fin_prevue IS NOT NULL
                                     AND act.date_fin_prevue < CAST(:dateReference AS date)
                                     AND (sta.code IS NULL
                                          OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ',')))))
                                                                                    AS enRetard,
                               COUNT(*) FILTER (WHERE sta.code = 'NON_COMMENCEE')   AS nonCommencees,
                               COALESCE(AVG(av.avancement), 0)                      AS avancement
                        FROM activite act
                        LEFT JOIN service srv ON srv.id_service = act.id_service
                        """
                        + SqlFragments.STATUT_ACTIVITE_A_DATE
                        + SqlFragments.AVANCEMENT_ACTIVITE_A_DATE
                        + """
                                        WHERE act.date_debut_prevue <= CAST(:dateFin AS date)
                                          AND (act.date_fin_prevue IS NULL OR act.date_fin_prevue >= CAST(:dateDebut AS date))
                                          AND (:serviceFiltre IS NULL OR act.id_service = :serviceFiltre)
                                          AND (:departementFiltre IS NULL OR srv.id_departement = :departementFiltre)
                                          AND (sta.code IS DISTINCT FROM 'BROUILLON')
                                        GROUP BY srv.id_service, srv.nom
                                        ORDER BY srv.nom
                                        """, nativeQuery = true)
        List<DashboardServiceRow> services(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference,
                        @Param("serviceFiltre") Integer serviceFiltre,
                        @Param("departementFiltre") Integer departementFiltre,
                        @Param("codesTerminaux") String codesTerminaux);

        /*
         * ------------------------------------------------------------------
         * 5. Échéances
         * ------------------------------------------------------------------
         */
        @Query(value = """
                        SELECT act.id_activite                  AS id,
                               act.code                         AS code,
                               act.designation                  AS designation,
                               srv.nom                          AS service,
                               act.date_fin_prevue              AS dateEcheance,
                               (CAST(:dateReference AS date) - act.date_fin_prevue) AS jours,
                               pr.code                          AS prioriteCode,
                               pr.libelle                       AS prioriteLibelle,
                               sta.code                         AS statutCode,
                               sta.libelle                      AS statutLibelle,
                               COALESCE(av.avancement, 0)       AS avancement
                        FROM activite act
                        LEFT JOIN service srv ON srv.id_service = act.id_service
                        LEFT JOIN priorite pr ON pr.id_priorite = act.id_priorite
                        """
                        + SqlFragments.STATUT_ACTIVITE_A_DATE
                        + SqlFragments.AVANCEMENT_ACTIVITE_A_DATE
                        + """
                                        WHERE act.date_fin_prevue IS NOT NULL
                                          AND act.date_debut_prevue <= CAST(:dateFin AS date)
                                          AND (act.date_fin_prevue IS NULL OR act.date_fin_prevue >= CAST(:dateDebut AS date))
                                          AND (:serviceFiltre IS NULL OR act.id_service = :serviceFiltre)
                                          AND (:departementFiltre IS NULL OR srv.id_departement = :departementFiltre)
                                          AND (sta.code IS DISTINCT FROM 'BROUILLON')
                                          AND (sta.code IS NULL
                                               OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ','))))
                                          AND act.date_fin_prevue <= CAST(:dateReference AS date) + :joursFenetre
                                        ORDER BY act.date_fin_prevue ASC,
                                                 CASE pr.code
                                                   WHEN 'CRITIQUE' THEN 1
                                                   WHEN 'HAUTE' THEN 2
                                                   WHEN 'NORMALE' THEN 3
                                                   WHEN 'FAIBLE' THEN 4
                                                   ELSE 5
                                                 END ASC,
                                                 pr.libelle ASC
                                        """, nativeQuery = true)
        List<DashboardEcheanceRow> echeances(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference,
                        @Param("serviceFiltre") Integer serviceFiltre,
                        @Param("departementFiltre") Integer departementFiltre,
                        @Param("codesTerminaux") String codesTerminaux,
                        @Param("joursFenetre") int joursFenetre);

        /*
         * ------------------------------------------------------------------
         * 8. PTA / NON-PTA
         * ------------------------------------------------------------------
         */
        @Query(value = """
                        SELECT COUNT(*)                                                AS total,
                               COUNT(*) FILTER (WHERE sta.code = 'TERMINEE')           AS terminees,
                               COUNT(*) FILTER (WHERE sta.code = 'EN_COURS')           AS enCours,
                               COUNT(*) FILTER (WHERE sta.code = 'NON_COMMENCEE')      AS nonCommencees,
                               COUNT(*) FILTER (
                                   WHERE act.date_fin_prevue IS NOT NULL
                                     AND act.date_fin_prevue < CAST(:dateReference AS date)
                                     AND (sta.code IS NULL
                                          OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ',')))))
                                                                                       AS enRetard,
                               COALESCE(AVG(av.avancement), 0)                         AS avancement
                        FROM activite act
                        LEFT JOIN service srv ON srv.id_service = act.id_service
                        """
                        + SqlFragments.STATUT_ACTIVITE_A_DATE
                        + SqlFragments.AVANCEMENT_ACTIVITE_A_DATE
                        + """
                                        WHERE act.date_debut_prevue <= CAST(:dateFin AS date)
                                          AND (act.date_fin_prevue IS NULL OR act.date_fin_prevue >= CAST(:dateDebut AS date))
                                          AND (:serviceFiltre IS NULL OR act.id_service = :serviceFiltre)
                                          AND (:departementFiltre IS NULL OR srv.id_departement = :departementFiltre)
                                          AND (sta.code IS DISTINCT FROM 'BROUILLON')
                                          AND ((CAST(:pta AS BOOLEAN) AND act.id_objectif_specifique IS NOT NULL)
                                               OR (NOT CAST(:pta AS BOOLEAN) AND act.id_objectif_specifique IS NULL))
                                        """, nativeQuery = true)
        DashboardPtaRow pta(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference,
                        @Param("serviceFiltre") Integer serviceFiltre,
                        @Param("departementFiltre") Integer departementFiltre,
                        @Param("pta") boolean pta,
                        @Param("codesTerminaux") String codesTerminaux);

        /*
         * ------------------------------------------------------------------
         * 9. Plans d'action (indicateur non expose dans le dashboard)
         * ------------------------------------------------------------------
         */
        @Query(value = """
                        SELECT COUNT(*) AS total,
                               COUNT(*) FILTER (
                                   WHERE ap.valeur_pourcentage IS NOT NULL
                                     AND ap.valeur_pourcentage > 0
                                     AND ap.valeur_pourcentage < 100) AS enCours,
                               COUNT(*) FILTER (WHERE ap.valeur_pourcentage = 100) AS termines,
                               COUNT(*) FILTER (
                                   WHERE pa.date_fin_prevue < CURRENT_DATE
                                     AND (ap.valeur_pourcentage IS NULL OR ap.valeur_pourcentage < 100))
                                                                                    AS enRetard
                        FROM plan_action pa
                        LEFT JOIN LATERAL (
                            SELECT a2.valeur_pourcentage
                            FROM avancement_plan_action a2
                            WHERE a2.id_plan_action = pa.id_plan_action
                              AND NOT EXISTS (
                                  SELECT 1 FROM avancement_plan_action a3
                                  WHERE a3.id_plan_action = pa.id_plan_action
                                    AND (a3.date_changement, a3.id_avancement_plan_action)
                                        > (a2.date_changement, a2.id_avancement_plan_action))
                        ) ap ON TRUE
                        WHERE (:annee IS NULL OR EXTRACT(YEAR FROM pa.date_debut_prevue) = :annee)
                        """, nativeQuery = true)
        DashboardPlanActionRow plansAction(@Param("annee") Integer annee);

        /*
         * Avancement moyen par type d'activité.
         *
         * Les types sont portés par activite.id_type_activite → type_activite.
         * Le regroupement se fait sur le type seul : le filtre Service est
         * appliqué en amont par le périmètre / la demande, pas dans la clé.
         *
         * Ne pas confondre avec le PTA : on ne regarde PAS id_objectif_specifique,
         * on regarde uniquement le type intrinsèque de l'activité.
         *
         * Colonne d'affichage : type_activite.designation (cf. DDL — la table
         * n'a ni `nom` ni `libelle`, elle a `designation`).
         */
        @Query(value = """
                        SELECT typ.id_type_activite                  AS idTypeActivite,
                               typ.designation                       AS typeActivite,
                               COUNT(*)                              AS totalActivites,
                               COALESCE(AVG(av.avancement), 0)       AS avancement
                        FROM activite act
                        LEFT JOIN service srv       ON srv.id_service       = act.id_service
                        LEFT JOIN type_activite typ ON typ.id_type_activite = act.id_type_activite
                        """
                        + SqlFragments.STATUT_ACTIVITE_A_DATE
                        + SqlFragments.AVANCEMENT_ACTIVITE_A_DATE
                        + """
                                        WHERE act.id_type_activite IS NOT NULL
                                          AND act.date_debut_prevue <= CAST(:dateFin AS date)
                                          AND (act.date_fin_prevue IS NULL OR act.date_fin_prevue >= CAST(:dateDebut AS date))
                                          AND (:serviceFiltre IS NULL OR act.id_service = :serviceFiltre)
                                          AND (:departementFiltre IS NULL OR srv.id_departement = :departementFiltre)
                                          AND (sta.code IS DISTINCT FROM 'BROUILLON')
                                        GROUP BY typ.id_type_activite, typ.designation
                                        ORDER BY typ.designation
                                        """, nativeQuery = true)
        List<DashboardTypeActiviteRow> typesActivite(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference,
                        @Param("serviceFiltre") Integer serviceFiltre,
                        @Param("departementFiltre") Integer departementFiltre);

        /*
         * ------------------------------------------------------------------
         * 10. Avancement des plans d'action par origine
         * ------------------------------------------------------------------
         *
         * Agrégat unique sur la période globale : pour chaque origine,
         *  - total      : plans qui chevauchent la période ;
         *  - avancement : moyenne des pourcentages saisis (0 si aucun
         *                 avancement n'est connu à `dateReference`).
         *
         * L'historique avancement_plan_action sert à reconstituer
         * l'avancement à la date de référence. Aucune donnée n'est
         * inventée : un plan sans historique compte pour 0.
         *
         * L'origine retenue est la principale si elle existe, sinon celle
         * de plus petit identifiant. Les types inconnus sont classés en AUTRE.
         * Seuls les plans NON_COMMENCEE, EN_COURS et TERMINEE sont inclus,
         * comme dans le suivi global des plans.
         */
        @Query(value = """
                        WITH plans AS (
                            SELECT pa.id_plan_action,
                                   pa.date_debut_prevue,
                                   pa.date_fin_prevue,
                                   COALESCE(av.valeur_pourcentage, 0) AS avancement,
                                   CASE
                                       WHEN UPPER(o.type_origine) LIKE 'INCIDENT%' THEN 'INCIDENT'
                                       WHEN UPPER(o.type_origine) LIKE 'RISQUE%'   THEN 'RISQUE'
                                       ELSE 'AUTRE'
                                   END AS origine
                            FROM plan_action pa
                          """
                          + SqlFragments.AVANCEMENT_PLAN_ACTION_A_DATE
                          + """
                            JOIN LATERAL (
                                SELECT o2.type_origine
                                FROM plan_action_origine pao
                                JOIN origine o2 ON o2.id_origine = pao.id_origine
                                WHERE pao.id_plan_action = pa.id_plan_action
                                ORDER BY (pao.est_principale IS TRUE) DESC, pao.id_origine
                                LIMIT 1
                            ) o ON TRUE
                            WHERE pa.date_debut_prevue <= CAST(:dateFin AS date)
                              AND pa.date_fin_prevue >= CAST(:dateDebut AS date)
                              AND sta.code IN ('NON_COMMENCEE', 'EN_COURS', 'TERMINEE')
                        )
                        SELECT p.origine AS origine,
                               COUNT(*) AS total,
                               COALESCE(AVG(p.avancement), 0) AS avancement
                        FROM plans p
                        GROUP BY p.origine
                        ORDER BY p.origine
                        """, nativeQuery = true)
        List<DashboardPlanActionOrigineRow> avancementPlansParOrigine(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("dateReference") LocalDate dateReference);
}
