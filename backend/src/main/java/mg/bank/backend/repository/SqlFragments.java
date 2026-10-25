package mg.bank.backend.repository;

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

    public static final String AVANCEMENT_PLAN_ACTION_DERNIER = """
            LEFT JOIN LATERAL (
                SELECT a.valeur_pourcentage, a.id_statut
                FROM avancement_plan_action a
                WHERE a.id_plan_action = pa.id_plan_action
                ORDER BY a.date_changement DESC, a.id_avancement_plan_action DESC
                LIMIT 1
            ) av ON TRUE
            LEFT JOIN statut sta ON sta.id_statut = av.id_statut
            """;
}