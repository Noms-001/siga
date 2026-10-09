package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.Activite;
import mg.bank.backend.repository.projection.DashboardAvancementRow;
import mg.bank.backend.repository.projection.DashboardEcheanceRow;
import mg.bank.backend.repository.projection.DashboardKpiRow;
import mg.bank.backend.repository.projection.DashboardPlanActionRow;
import mg.bank.backend.repository.projection.DashboardPrioriteRow;
import mg.bank.backend.repository.projection.DashboardPtaRow;
import mg.bank.backend.repository.projection.DashboardServiceRow;
import mg.bank.backend.repository.projection.DashboardStatutRow;
import mg.bank.backend.repository.projection.DashboardValidationRow;

/**
 * Agrégats dédiés au dashboard.
 *
 * Ce repository ne charge JAMAIS d'entité : toutes ses requêtes renvoient
 * des compteurs ou des moyennes. C'est la condition pour que le dashboard
 * reste consultable sans dépendre du volume d'activités.
 *
 * Les fragments SQL (statut courant, avancement courant) sont les mêmes
 * que dans ActiviteRepository.SELECT_BASE : la définition du "statut
 * courant" et de "l'avancement courant" est unique dans le projet, elle
 * est recopiée ici pour que les agrégations restent lisibles. Une
 * divergence serait un bug — le jour où la règle change, il faudra la
 * changer aux deux endroits. Un commentaire le rappelle dans chaque
 * requête qui l'utilise.
 */
public interface DashboardRepository extends Repository<Activite, Integer> {

    /* ------------------------------------------------------------------
     * Fragments SQL partagés
     * ------------------------------------------------------------------ */

    /**
     * Statut courant d'une activité : dernière ligne d'historique.
     *
     * Le NOT EXISTS à comparaison de tuples est l'équivalent de
     * ORDER BY date_changement DESC, id DESC LIMIT 1, sans ORDER BY — et
     * sans ORDER BY, il reste compatible avec un agrégat qui utilise la
     * même jointure plusieurs fois.
     */
    String STATUT_COURANT = """
            LEFT JOIN LATERAL (
                SELECT h.id_statut
                FROM historique_activite h
                WHERE h.id_activite = act.id_activite
                  AND NOT EXISTS (
                      SELECT 1 FROM historique_activite h2
                      WHERE h2.id_activite = act.id_activite
                        AND (h2.date_changement, h2.id_historique_activite)
                            > (h.date_changement, h.id_historique_activite))
            ) hist ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = hist.id_statut
            """;

    /**
     * Avancement courant d'une activité : moyenne des derniers avancements
     * de ses sous-activités. Null si aucune sous-activité n'a de relevé.
     */
    String AVANCEMENT_COURANT = """
            LEFT JOIN LATERAL (
                SELECT AVG(dernier.valeur_pourcentage) AS avancement
                FROM sous_activite sa
                JOIN LATERAL (
                    SELECT av2.valeur_pourcentage
                    FROM avancement_sous_activite av2
                    WHERE av2.id_sous_activite = sa.id_sous_activite
                      AND NOT EXISTS (
                          SELECT 1 FROM avancement_sous_activite av3
                          WHERE av3.id_sous_activite = sa.id_sous_activite
                            AND (av3.date_changement, av3.id_historique_sous_activite)
                                > (av2.date_changement, av2.id_historique_sous_activite))
                ) dernier ON TRUE
                WHERE sa.id_activite = act.id_activite
            ) av ON TRUE
            """;

    /**
     * Codes terminaux, injectés sous forme de chaîne CSV.
     * Recopiés depuis ActiviteService.codesTerminaux(), sans dépendance
     * Java : un repository n'appelle pas un service.
     */
    String SQL_TERMINAUX = "'TERMINEE','ANNULEE','REJETE','VALIDEE'";

    /* ------------------------------------------------------------------
     * 1. KPI
     * ------------------------------------------------------------------ */

    /**
     * Compteurs pour l'écran d'accueil.
     *
     * Inclut EN_ATTENTE_VALIDATION et VALIDEE, contrairement à la liste
     * standard : le dashboard est une vue de supervision, il doit tout
     * voir. Le total ici est donc plus grand que celui de /api/activites.
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
                         AND act.date_fin_prevue < CURRENT_DATE
                         AND (sta.code IS NULL
                              OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ',')))))
                                                                          AS enRetard,
                   COALESCE(AVG(av.avancement), 0)                        AS avancementGlobal
            FROM activite act
            """
            + STATUT_COURANT
            + AVANCEMENT_COURANT
            + " WHERE (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)",
            nativeQuery = true)
    DashboardKpiRow kpi(@Param("annee") Integer annee,
                        @Param("codesTerminaux") String codesTerminaux);

    /* ------------------------------------------------------------------
     * 2. Avancement
     * ------------------------------------------------------------------ */

    /**
     * Avancement global + volumétrie.
     *
     * `sousActivitesTerminees` compte les sous-activités dont le DERNIER
     * relevé est 100. Il n'y a pas de colonne "terminée" sur
     * sous_activite : c'est la valeur courante qui fait foi, comme
     * partout ailleurs.
     */
    @Query(value = """
            SELECT
                COUNT(*)                                              AS totalActivites,
                COALESCE(SUM(sous.total), 0)                          AS totalSousActivites,
                COALESCE(SUM(sous.terminees), 0)                      AS sousActivitesTerminees,
                COALESCE(AVG(av.avancement), 0)                       AS avancementGlobal
            FROM activite act
            """
            + STATUT_COURANT
            + AVANCEMENT_COURANT
            + """
            LEFT JOIN LATERAL (
                SELECT COUNT(*)                                          AS total,
                       COUNT(*) FILTER (WHERE dernier.valeur_pourcentage = 100) AS terminees
                FROM sous_activite sa
                JOIN LATERAL (
                    SELECT av2.valeur_pourcentage
                    FROM avancement_sous_activite av2
                    WHERE av2.id_sous_activite = sa.id_sous_activite
                      AND NOT EXISTS (
                          SELECT 1 FROM avancement_sous_activite av3
                          WHERE av3.id_sous_activite = sa.id_sous_activite
                            AND (av3.date_changement, av3.id_historique_sous_activite)
                                > (av2.date_changement, av2.id_historique_sous_activite))
                ) dernier ON TRUE
                WHERE sa.id_activite = act.id_activite
            ) sous ON TRUE
            WHERE (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
            """,
            nativeQuery = true)
    DashboardAvancementRow avancement(@Param("annee") Integer annee);

    /* ------------------------------------------------------------------
     * 3. Statuts
     * ------------------------------------------------------------------ */

    /**
     * Répartition par statut courant.
     *
     * Les activités sans historique de statut sont ignorées (sta.code est
     * NULL et la ligne est filtrée) : elles ne sont pas encore entrées
     * dans le cycle de vie.
     */
    @Query(value = """
            SELECT sta.code    AS statut,
                   sta.libelle AS libelle,
                   COUNT(*)    AS nombre
            FROM activite act
            """
            + STATUT_COURANT
            + """
            WHERE sta.code IS NOT NULL
              AND (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
            GROUP BY sta.code, sta.libelle
            ORDER BY sta.code
            """,
            nativeQuery = true)
    List<DashboardStatutRow> statuts(@Param("annee") Integer annee);

    /* ------------------------------------------------------------------
     * 4. Services
     * ------------------------------------------------------------------ */

    /**
     * Agrégat par service. LEFT JOIN sur service pour ne pas perdre
     * d'activité rattachée à un service absent (cas anormal, mais qui ne
     * doit pas disparaître silencieusement d'un total).
     */
    @Query(value = """
            SELECT srv.id_service                                       AS idService,
                   srv.nom                                              AS service,
                   COUNT(*)                                             AS totalActivites,
                   COUNT(*) FILTER (WHERE sta.code = 'TERMINEE')        AS terminees,
                   COUNT(*) FILTER (WHERE sta.code = 'EN_COURS')        AS enCours,
                   COUNT(*) FILTER (
                       WHERE act.date_fin_prevue IS NOT NULL
                         AND act.date_fin_prevue < CURRENT_DATE
                         AND (sta.code IS NULL
                              OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ',')))))
                                                                        AS enRetard,
                   COUNT(*) FILTER (WHERE sta.code = 'NON_COMMENCEE')   AS nonCommencees,
                   COALESCE(AVG(av.avancement), 0)                      AS avancement
            FROM activite act
            LEFT JOIN service srv ON srv.id_service = act.id_service
            """
            + STATUT_COURANT
            + AVANCEMENT_COURANT
            + """
            WHERE (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
            GROUP BY srv.id_service, srv.nom
            ORDER BY srv.nom
            """,
            nativeQuery = true)
    List<DashboardServiceRow> services(@Param("annee") Integer annee,
                                       @Param("codesTerminaux") String codesTerminaux);

    /* ------------------------------------------------------------------
     * 5. Activités prioritaires
     * ------------------------------------------------------------------ */

    /**
     * Activités à traiter en premier.
     *
     * Ordre : priorité (CRITIQUE → HAUTE → NORMALE → FAIBLE), puis
     * en-retard avant échéance-proche, puis échéance la plus proche.
     * Le rang de priorité est calculé en SQL par un CASE : les codes
     * viennent de la base, on ne les suppose pas — si un code inconnu
     * apparaît, il est classé après les codes connus.
     */
    @Query(value = """
            SELECT act.id_activite                  AS id,
                   act.code                         AS code,
                   act.reference                    AS reference,
                   act.designation                  AS designation,
                   srv.nom                          AS service,
                   pr.code                          AS prioriteCode,
                   pr.libelle                       AS prioriteLibelle,
                   sta.code                         AS statutCode,
                   sta.libelle                      AS statutLibelle,
                   act.date_debut_prevue            AS dateDebut,
                   act.date_fin_prevue              AS dateEcheance,
                   COALESCE(av.avancement, 0)       AS avancement,
                   CASE
                       WHEN act.date_fin_prevue IS NOT NULL
                            AND act.date_fin_prevue < CURRENT_DATE
                            AND (sta.code IS NULL
                                 OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ','))))
                           THEN (CURRENT_DATE - act.date_fin_prevue)
                       ELSE 0
                   END                              AS joursRetard
            FROM activite act
            LEFT JOIN service srv ON srv.id_service = act.id_service
            LEFT JOIN priorite pr ON pr.id_priorite = act.id_priorite
            """
            + STATUT_COURANT
            + AVANCEMENT_COURANT
            + """
            WHERE (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
              AND sta.code IN ('EN_COURS', 'NON_COMMENCEE')
            ORDER BY
                joursRetard DESC,
                CASE UPPER(pr.code)
                    WHEN 'CRITIQUE' THEN 1
                    WHEN 'HAUTE'    THEN 2
                    WHEN 'NORMALE'  THEN 3
                    WHEN 'FAIBLE'   THEN 4
                    ELSE 5
                END,
                act.date_fin_prevue ASC NULLS LAST,
                act.id_activite
            LIMIT :limite
            """,
            nativeQuery = true)
    List<DashboardPrioriteRow> priorites(@Param("annee") Integer annee,
                                         @Param("codesTerminaux") String codesTerminaux,
                                         @Param("limite") int limite);

    /* ------------------------------------------------------------------
     * 6. Validations en attente
     * ------------------------------------------------------------------ */

    /**
     * Demandes de validation en attente.
     *
     * Le demandeur peut être null (contrainte non stricte sur la table) :
     * les deux noms sont exposés séparément, l'appelant compose.
     */
    @Query(value = """
            SELECT v.id_validation_activite AS idValidation,
                   act.id_activite           AS idActivite,
                   act.code                  AS codeActivite,
                   act.designation           AS designationActivite,
                   u.nom                     AS demandeurNom,
                   u.prenom                  AS demandeurPrenom,
                   e.designation             AS etapeDesignation,
                   e.niveau                  AS etapeNiveau,
                   v.date_demande            AS dateDemande,
                   CAST(v.decision AS VARCHAR) AS decision
            FROM validation_activite v
            JOIN activite act ON act.id_activite = v.id_activite
            LEFT JOIN utilisateur u ON u.id_utilisateur = v.id_demandeur
            LEFT JOIN etape_validation e ON e.id_etape_validation = v.id_etape_validation
            WHERE v.decision = CAST('EN_ATTENTE_VALIDATION' AS decision_validation)
              AND (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
            ORDER BY v.date_demande ASC
            """,
            nativeQuery = true)
    List<DashboardValidationRow> validationsEnAttente(@Param("annee") Integer annee);

    /* ------------------------------------------------------------------
     * 7. Échéances
     * ------------------------------------------------------------------ */

    /**
     * Activités à échéance dans la fenêtre, non terminées.
     *
     * `jours` est calculé en SQL (CURRENT_DATE - date_fin_prevue) : le
     * front le reçoit sans avoir à le recalculer, et le tri reste cohérent
     * avec la valeur affichée.
     *
     * Une seule requête, deux usages : l'appelant filtre ensuite sur
     * jours >= 0 (retard) ou 0 < jours <= N (proche). Le contraire
     * obligerait à trier deux fois la même chose.
     */
    @Query(value = """
            SELECT act.id_activite                  AS id,
                   act.code                         AS code,
                   act.designation                  AS designation,
                   srv.nom                          AS service,
                   act.date_fin_prevue              AS dateEcheance,
                   (CURRENT_DATE - act.date_fin_prevue) AS jours,
                   pr.code                          AS prioriteCode,
                   pr.libelle                       AS prioriteLibelle,
                   sta.code                         AS statutCode,
                   sta.libelle                      AS statutLibelle,
                   COALESCE(av.avancement, 0)       AS avancement
            FROM activite act
            LEFT JOIN service srv ON srv.id_service = act.id_service
            LEFT JOIN priorite pr ON pr.id_priorite = act.id_priorite
            """
            + STATUT_COURANT
            + AVANCEMENT_COURANT
            + """
            WHERE act.date_fin_prevue IS NOT NULL
              AND (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
              AND (sta.code IS NULL
                   OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ','))))
              AND (CURRENT_DATE - act.date_fin_prevue) <= :joursFenetre
            ORDER BY act.date_fin_prevue ASC
            """,
            nativeQuery = true)
    List<DashboardEcheanceRow> echeances(@Param("annee") Integer annee,
                                         @Param("codesTerminaux") String codesTerminaux,
                                         @Param("joursFenetre") int joursFenetre);

    /* ------------------------------------------------------------------
     * 8. PTA / NON-PTA
     * ------------------------------------------------------------------ */

    /**
     * Un seul agrégat, deux appels : l'appelant passe `pta=true` puis
     * `pta=false`. La contrainte sur id_objectif_specifique porte la
     * distinction — pas de nouvelle colonne, la règle du projet est
     * respectée.
     */
    @Query(value = """
            SELECT COUNT(*)                                                AS total,
                   COUNT(*) FILTER (WHERE sta.code = 'TERMINEE')           AS terminees,
                   COUNT(*) FILTER (WHERE sta.code = 'EN_COURS')           AS enCours,
                   COUNT(*) FILTER (WHERE sta.code = 'NON_COMMENCEE')      AS nonCommencees,
                   COUNT(*) FILTER (
                       WHERE act.date_fin_prevue IS NOT NULL
                         AND act.date_fin_prevue < CURRENT_DATE
                         AND (sta.code IS NULL
                              OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ',')))))
                                                                           AS enRetard,
                   COALESCE(AVG(av.avancement), 0)                         AS avancement
            FROM activite act
            """
            + STATUT_COURANT
            + AVANCEMENT_COURANT
            + """
            WHERE (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
              AND ((CAST(:pta AS BOOLEAN) AND act.id_objectif_specifique IS NOT NULL)
                   OR (NOT CAST(:pta AS BOOLEAN) AND act.id_objectif_specifique IS NULL))
            """,
            nativeQuery = true)
    DashboardPtaRow pta(@Param("annee") Integer annee,
                        @Param("pta") boolean pta,
                        @Param("codesTerminaux") String codesTerminaux);

    /* ------------------------------------------------------------------
     * 9. Plans d'action
     * ------------------------------------------------------------------ */

    /**
     * KPI des plans d'action.
     *
     * Un plan est "en retard" quand sa date_fin_prevue est dépassée et
     * qu'aucun avancement à 100 n'a été enregistré. Le "à 100" est lu sur
     * avancement_plan_action (dernier relevé), de la même manière que
     * pour les sous-activités.
     *
     * Le filtre année s'applique à date_debut_prevue du plan, comme pour
     * les activités.
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
            """,
            nativeQuery = true)
    DashboardPlanActionRow plansAction(@Param("annee") Integer annee);
}