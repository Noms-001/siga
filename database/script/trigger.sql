CREATE OR REPLACE FUNCTION verifier_notifications_sous_activites()
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
v_jours_avant INTEGER;
BEGIN

    /*
     * Récupération du paramètre
     */
SELECT valeur::INTEGER
INTO v_jours_avant
FROM parametre
WHERE code = 'NOTIFICATION_ACTIVITE_AVANT_ECHEANCE_JOURS'
  AND actif = TRUE
    LIMIT 1;

IF v_jours_avant IS NULL THEN
        RAISE NOTICE 'Paramètre NOTIFICATION_ACTIVITE_AVANT_ECHEANCE_JOURS non configuré.';
        RETURN;
END IF;

    /*
     * ============================================================
     * 1. SOUS-ACTIVITÉS ARRIVANT À ÉCHÉANCE
     * ============================================================
     */
WITH statut_courant AS (
    SELECT DISTINCT ON (ha.id_activite)
    ha.id_activite,
    s.code AS statut_code
FROM historique_activite ha
    INNER JOIN statut s ON s.id_statut = ha.id_statut
ORDER BY ha.id_activite,
    ha.date_changement DESC,
    ha.id_historique_activite DESC
    ),
    statut_courant_sous_activite AS (
SELECT DISTINCT ON (av.id_sous_activite)
    av.id_sous_activite,
    s.code AS statut_code
FROM avancement_sous_activite av
    INNER JOIN statut s ON s.id_statut = av.id_statut
ORDER BY av.id_sous_activite,
    av.date_changement DESC,
    av.id_historique_sous_activite DESC
    )
INSERT INTO notification (
    titre, message, date_creation, id_priorite,
    id_utilisateur, id_reference, resource
)
SELECT
    'Échéance proche',
    'La sous-activité "' || sa.designation ||
    '" de l''activité "' || a.designation ||
    '" arrive à échéance dans ' || v_jours_avant || ' jour(s).',
    CURRENT_TIMESTAMP,
    a.id_priorite,
    asa.id_utilisateur,
    sa.id_sous_activite,
    'SOUS_ACTIVITE'
FROM sous_activite sa
         INNER JOIN activite a ON a.id_activite = sa.id_activite
         INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = sa.id_sous_activite
         INNER JOIN statut_courant sc ON sc.id_activite = a.id_activite
         INNER JOIN statut_courant_sous_activite scsa ON scsa.id_sous_activite = sa.id_sous_activite
WHERE
    sa.date_fin_prevue = CURRENT_DATE + v_jours_avant
  AND asa.date_desaffectation IS NULL
  AND sc.statut_code IN ('NON_COMMENCEE', 'EN_COURS', 'EN_RETARD')
  AND scsa.statut_code = 'EN_COURS';

/*
 * ============================================================
 * 1bis. SOUS-ACTIVITÉS DONT LA DATE DE DÉBUT PRÉVUE EST DÉPASSÉE
 *       SANS DÉMARRAGE EFFECTIF
 * ============================================================
 */
WITH statut_courant AS (
    SELECT DISTINCT ON (ha.id_activite)
    ha.id_activite,
    s.code AS statut_code
FROM historique_activite ha
    INNER JOIN statut s ON s.id_statut = ha.id_statut
ORDER BY ha.id_activite,
    ha.date_changement DESC,
    ha.id_historique_activite DESC
    ),
    statut_courant_sous_activite AS (
SELECT DISTINCT ON (av.id_sous_activite)
    av.id_sous_activite,
    s.code AS statut_code
FROM avancement_sous_activite av
    INNER JOIN statut s ON s.id_statut = av.id_statut
ORDER BY av.id_sous_activite,
    av.date_changement DESC,
    av.id_historique_sous_activite DESC
    )
INSERT INTO notification (
    titre, message, date_creation, id_priorite,
    id_utilisateur, id_reference, resource
)
SELECT
    'Démarrage en retard',
    'La sous-activité "' || sa.designation ||
    '" de l''activité "' || a.designation ||
    '" devait démarrer le ' || TO_CHAR(sa.date_debut_prevue, 'DD/MM/YYYY') ||
    ' mais n''a pas encore été démarrée (' ||
    (CURRENT_DATE - sa.date_debut_prevue) || ' jour(s) de retard).',
    CURRENT_TIMESTAMP,
    a.id_priorite,
    asa.id_utilisateur,
    sa.id_sous_activite,
    'SOUS_ACTIVITE'
FROM sous_activite sa
         INNER JOIN activite a ON a.id_activite = sa.id_activite
         INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = sa.id_sous_activite
         INNER JOIN statut_courant sc ON sc.id_activite = a.id_activite
         INNER JOIN statut_courant_sous_activite scsa ON scsa.id_sous_activite = sa.id_sous_activite
WHERE
    sa.date_debut_prevue < CURRENT_DATE
  AND sa.date_debut_reelle IS NULL
  AND asa.date_desaffectation IS NULL
  AND sc.statut_code IN ('NON_COMMENCEE', 'EN_COURS', 'EN_RETARD')
  AND scsa.statut_code = 'NON_COMMENCEE';

/*
 * ============================================================
 * 2. SOUS-ACTIVITÉS EN RETARD
 * ============================================================
 */
WITH statut_courant AS (
    SELECT DISTINCT ON (ha.id_activite)
    ha.id_activite,
    s.code AS statut_code
FROM historique_activite ha
    INNER JOIN statut s ON s.id_statut = ha.id_statut
ORDER BY ha.id_activite,
    ha.date_changement DESC,
    ha.id_historique_activite DESC
    ),
    statut_courant_sous_activite AS (
SELECT DISTINCT ON (av.id_sous_activite)
    av.id_sous_activite,
    s.code AS statut_code
FROM avancement_sous_activite av
    INNER JOIN statut s ON s.id_statut = av.id_statut
ORDER BY av.id_sous_activite,
    av.date_changement DESC,
    av.id_historique_sous_activite DESC
    )
INSERT INTO notification (
    titre, message, date_creation, id_priorite,
    id_utilisateur, id_reference, resource
)
SELECT
    'Sous-activité en retard',
    'La sous-activité "' || sa.designation ||
    '" de l''activité "' || a.designation ||
    '" est en retard depuis ' ||
    (CURRENT_DATE - sa.date_fin_prevue) || ' jour(s).',
    CURRENT_TIMESTAMP,
    a.id_priorite,
    asa.id_utilisateur,
    sa.id_sous_activite,
    'SOUS_ACTIVITE'
FROM sous_activite sa
         INNER JOIN activite a ON a.id_activite = sa.id_activite
         INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = sa.id_sous_activite
         INNER JOIN statut_courant sc ON sc.id_activite = a.id_activite
         INNER JOIN statut_courant_sous_activite scsa ON scsa.id_sous_activite = sa.id_sous_activite
WHERE
    sa.date_fin_prevue < CURRENT_DATE
  AND asa.date_desaffectation IS NULL
  AND sc.statut_code IN ('NON_COMMENCEE', 'EN_COURS', 'EN_RETARD')
  AND scsa.statut_code = 'EN_COURS';

/*
 * ============================================================
 * 2bis. ALERTE HIÉRARCHIQUE — SOUS-ACTIVITÉ CRITIQUE EN RETARD
 *
 *    Pour chaque sous-activité en retard dont l'activité parente
 *    est de priorité CRITIQUE, on notifie :
 *      - le chef du service concerné (celui dont id_service
 *        correspond au service de l'activité),
 *      - le chef de département TSS.
 * ============================================================
 */
WITH statut_courant AS (
    SELECT DISTINCT ON (ha.id_activite)
    ha.id_activite,
    s.code AS statut_code
FROM historique_activite ha
    INNER JOIN statut s ON s.id_statut = ha.id_statut
ORDER BY ha.id_activite,
    ha.date_changement DESC,
    ha.id_historique_activite DESC
    ),
    statut_courant_sous_activite AS (
SELECT DISTINCT ON (av.id_sous_activite)
    av.id_sous_activite,
    s.code AS statut_code
FROM avancement_sous_activite av
    INNER JOIN statut s ON s.id_statut = av.id_statut
ORDER BY av.id_sous_activite,
    av.date_changement DESC,
    av.id_historique_sous_activite DESC
    ),
    sous_activites_critiques_en_retard AS (
SELECT DISTINCT
    sa.id_sous_activite,
    sa.designation      AS sa_designation,
    a.designation       AS a_designation,
    a.id_priorite,
    a.id_service,
    sa.date_fin_prevue
FROM sous_activite sa
    INNER JOIN activite a ON a.id_activite = sa.id_activite
    INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = sa.id_sous_activite
    INNER JOIN statut_courant sc ON sc.id_activite = a.id_activite
    INNER JOIN statut_courant_sous_activite scsa ON scsa.id_sous_activite = sa.id_sous_activite
    INNER JOIN priorite p ON p.id_priorite = a.id_priorite
WHERE
    sa.date_fin_prevue < CURRENT_DATE
  AND asa.date_desaffectation IS NULL
  AND sc.statut_code IN ('NON_COMMENCEE', 'EN_COURS', 'EN_RETARD')
  AND scsa.statut_code = 'EN_COURS'
  AND p.code = 'CRITIQUE'
    ),
    destinataires AS (
-- Chef du service concerné
SELECT
    sce.id_sous_activite,
    sce.sa_designation,
    sce.a_designation,
    sce.id_priorite,
    sce.date_fin_prevue,
    u.id_utilisateur
FROM sous_activites_critiques_en_retard sce
    INNER JOIN utilisateur u
ON u.id_service = sce.id_service
    INNER JOIN poste p
    ON p.id_poste = u.id_poste
WHERE p.nom IN (
    'Chef de service — Gestion des Risques et Procédures',
    'Chef de service — Gestion des Systèmes de Sécurité'
    )

UNION ALL

-- Chef de département TSS
SELECT
    sce.id_sous_activite,
    sce.sa_designation,
    sce.a_designation,
    sce.id_priorite,
    sce.date_fin_prevue,
    u.id_utilisateur
FROM sous_activites_critiques_en_retard sce
    INNER JOIN utilisateur u
ON u.id_poste = (
    SELECT p.id_poste
    FROM poste p
    WHERE p.nom = 'Chef de département TSS'
    )
    )
INSERT INTO notification (
    titre, message, date_creation, id_priorite,
    id_utilisateur, id_reference, resource
)
SELECT
    'Alerte : sous-activité CRITIQUE en retard',
    'La sous-activité critique "' || d.sa_designation ||
    '" de l''activité "' || d.a_designation ||
    '" est en retard depuis ' || (CURRENT_DATE - d.date_fin_prevue) || ' jour(s).',
    CURRENT_TIMESTAMP,
    d.id_priorite,
    d.id_utilisateur,
    d.id_sous_activite,
    'SOUS_ACTIVITE'
FROM destinataires d;

/*
 * ============================================================
 * 3. AJOUT DU STATUT EN_RETARD DANS L'HISTORIQUE
 * ============================================================
 */
WITH statut_courant AS (
    SELECT DISTINCT ON (ha.id_activite)
    ha.id_activite,
    s.code AS statut_code
FROM historique_activite ha
    INNER JOIN statut s ON s.id_statut = ha.id_statut
ORDER BY ha.id_activite,
    ha.date_changement DESC,
    ha.id_historique_activite DESC
    ),
    statut_courant_sous_activite AS (
SELECT DISTINCT ON (av.id_sous_activite)
    av.id_sous_activite,
    s.code AS statut_code
FROM avancement_sous_activite av
    INNER JOIN statut s ON s.id_statut = av.id_statut
ORDER BY av.id_sous_activite,
    av.date_changement DESC,
    av.id_historique_sous_activite DESC
    ),
    activites_en_retard AS (
SELECT DISTINCT ON (a.id_activite)
    a.id_activite,
    asa.id_utilisateur
FROM sous_activite sa
    INNER JOIN activite a ON a.id_activite = sa.id_activite
    INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = sa.id_sous_activite
    INNER JOIN statut_courant sc ON sc.id_activite = a.id_activite
    INNER JOIN statut_courant_sous_activite scsa ON scsa.id_sous_activite = sa.id_sous_activite
WHERE
    sa.date_fin_prevue < CURRENT_DATE
  AND asa.date_desaffectation IS NULL
  AND sc.statut_code IN ('NON_COMMENCEE', 'EN_COURS', 'EN_RETARD')
  AND scsa.statut_code = 'EN_COURS'
ORDER BY a.id_activite,
    asa.id_affectation_sous_activite
    )
INSERT INTO historique_activite (
    commentaire, date_changement, id_utilisateur, id_statut, id_activite
)
SELECT
    'Passage automatique en retard : au moins une sous-activité est en retard.',
    CURRENT_TIMESTAMP,
    aer.id_utilisateur,
    (SELECT id_statut FROM statut WHERE code = 'EN_RETARD'),
    aer.id_activite
FROM activites_en_retard aer;

END;
$$;

CREATE EXTENSION IF NOT EXISTS pg_cron;

SELECT cron.schedule(
               'notification-sous-activites',
               '0 7 * * *',
               $$SELECT verifier_notifications_sous_activites();$$
);