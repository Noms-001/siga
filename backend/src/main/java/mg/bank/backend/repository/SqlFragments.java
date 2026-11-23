package mg.bank.backend.repository;

import mg.bank.backend.enums.StatutsActivite;

public final class SqlFragments {

    private SqlFragments() {
    }

    public static final String STATUT_COURANT = """
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

    public static final String AVANCEMENT_ACTIVITE_COURANT = """
            LEFT JOIN LATERAL (
                SELECT COALESCE(AVG(COALESCE(dernier.valeur_pourcentage, 0)), 0) AS avancement
                FROM sous_activite sa
                LEFT JOIN LATERAL (
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

    public static final String PLAN_SCOPE = """
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

    public static final String PORTEE_UTILISATEUR = """
            AND (:idUtilisateurPortee IS NULL OR EXISTS (
                SELECT 1
                FROM historique_activite hpu
                JOIN statut spu ON spu.id_statut = hpu.id_statut
                WHERE hpu.id_activite = act.id_activite
                  AND spu.code = '""" + StatutsActivite.BROUILLON + "'\n" + """
                  AND hpu.id_utilisateur = :idUtilisateurPortee
                  AND hpu.date_changement = (
                      SELECT MIN(hpu2.date_changement)
                      FROM historique_activite hpu2
                      WHERE hpu2.id_activite = hpu.id_activite)
            ) OR EXISTS (
                SELECT 1
                FROM affectation_activite aap
                WHERE aap.id_activite = act.id_activite
                  AND aap.id_utilisateur = :idUtilisateurPortee
                  AND aap.date_desaffectation IS NULL
            ))
            """;

    /**
     * Restreint une requete sur l'alias {@code i} (indicateur) aux indicateurs
     * rattaches a au moins une activite du perimetre. Les trois filtres a null
     * signifient « portee totale » et ne restreignent rien.
     */
    public static final String INDICATEUR_DANS_PERIMETRE = """
            AND (
                (CAST(:servicePerimetre AS integer) IS NULL
                 AND CAST(:departementPerimetre AS integer) IS NULL
                 AND CAST(:idUtilisateurPortee AS integer) IS NULL)
                OR EXISTS (
                    SELECT 1
                    FROM activite_indicateur ai
                    JOIN activite act ON act.id_activite = ai.id_activite
                    JOIN service srv ON srv.id_service = act.id_service
                    WHERE ai.id_indicateur = i.id_indicateur
                      AND (CAST(:servicePerimetre AS integer) IS NULL
                           OR act.id_service = CAST(:servicePerimetre AS integer))
                      AND (CAST(:departementPerimetre AS integer) IS NULL
                           OR srv.id_departement = CAST(:departementPerimetre AS integer))
            """ + PORTEE_UTILISATEUR + """
                )
            )
            """;

    public static final String PORTEE_UTILISATEUR_PLAN = """
            AND (:idUtilisateurPortee IS NULL OR EXISTS (
                SELECT 1
                FROM avancement_plan_action apc
                JOIN statut spc ON spc.id_statut = apc.id_statut
                WHERE apc.id_plan_action = pa.id_plan_action
                  AND spc.code = '""" + StatutsActivite.BROUILLON + "'\n" + """
                  AND apc.id_utilisateur = :idUtilisateurPortee
                  AND apc.date_changement = (
                      SELECT MIN(apc2.date_changement)
                      FROM avancement_plan_action apc2
                      WHERE apc2.id_plan_action = apc.id_plan_action)
            ) OR EXISTS (
                SELECT 1
                FROM affectation_plan_action apa
                WHERE apa.id_plan_action = pa.id_plan_action
                  AND apa.id_utilisateur = :idUtilisateurPortee
                  AND apa.date_desaffectation IS NULL
            ))
            """;

    public static final String FILTRES_PORTEE_PLAN = """
            WHERE (CAST(:servicePerimetre AS integer) IS NULL
                   OR (ps.nombre_services = 1
                       AND ps.id_service_principal = CAST(:servicePerimetre AS integer)))
              AND (CAST(:departementPerimetre AS integer) IS NULL
                   OR (ps.nombre_departements = 1
                       AND ps.id_departement_principal = CAST(:departementPerimetre AS integer)))
            """;

    public static final String AVANCEMENT_PLAN_ACTION_DERNIER = """
            LEFT JOIN LATERAL (
                SELECT a.valeur_pourcentage, a.id_statut
                FROM avancement_plan_action a
                WHERE a.id_plan_action = pa.id_plan_action
                  AND NOT EXISTS (
                      SELECT 1 FROM avancement_plan_action a2
                      WHERE a2.id_plan_action = pa.id_plan_action
                        AND (a2.date_changement, a2.id_avancement_plan_action)
                            > (a.date_changement, a.id_avancement_plan_action))
            ) av ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = av.id_statut
            """;

    /*
     * ------------------------------------------------------------------
     * Variantes historiques : le dernier etat connu A UNE DATE DONNEE.
     *
     * Le dashboard applique un filtre temporel global. Pour une periode
     * passee, afficher le statut courant fausserait la lecture : on
     * reconstitue l'etat a `dateReference` (= min(fin, aujourd'hui)).
     * ------------------------------------------------------------------
     */

    public static final String STATUT_ACTIVITE_A_DATE = """
            LEFT JOIN LATERAL (
                SELECT h.id_statut
                FROM historique_activite h
                WHERE h.id_activite = act.id_activite
                  AND h.date_changement::date <= CAST(:dateReference AS date)
                  AND NOT EXISTS (
                      SELECT 1 FROM historique_activite h2
                      WHERE h2.id_activite = act.id_activite
                        AND h2.date_changement::date <= CAST(:dateReference AS date)
                        AND (h2.date_changement, h2.id_historique_activite)
                            > (h.date_changement, h.id_historique_activite))
            ) hist ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = hist.id_statut
            """;

    public static final String AVANCEMENT_ACTIVITE_A_DATE = """
            LEFT JOIN LATERAL (
                SELECT COALESCE(AVG(COALESCE(dernier.valeur_pourcentage, 0)), 0) AS avancement
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
            ) av ON TRUE
            """;

    public static final String AVANCEMENT_PLAN_ACTION_A_DATE = """
            LEFT JOIN LATERAL (
                SELECT a.valeur_pourcentage, a.id_statut
                FROM avancement_plan_action a
                WHERE a.id_plan_action = pa.id_plan_action
                  AND a.date_changement::date <= CAST(:dateReference AS date)
                  AND NOT EXISTS (
                      SELECT 1 FROM avancement_plan_action a2
                      WHERE a2.id_plan_action = pa.id_plan_action
                        AND a2.date_changement::date <= CAST(:dateReference AS date)
                        AND (a2.date_changement, a2.id_avancement_plan_action)
                            > (a.date_changement, a.id_avancement_plan_action))
            ) av ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = av.id_statut
            """;
}