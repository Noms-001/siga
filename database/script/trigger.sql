CREATE OR REPLACE FUNCTION verifier_notifications_plans_action()
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
v_jours_avant      INTEGER;
    v_seuil_hierarchie INTEGER;
BEGIN

    /* ============================================================
     * Récupération des paramètres
     * ============================================================ */
SELECT valeur::INTEGER
INTO v_jours_avant
FROM parametre
WHERE code = 'NOTIFICATION_PLAN_ACTION_AVANT_ECHEANCE_JOURS'
  AND actif = TRUE
    LIMIT 1;

IF v_jours_avant IS NULL THEN
        RAISE NOTICE 'Paramètre NOTIFICATION_PLAN_ACTION_AVANT_ECHEANCE_JOURS non configuré.';
        RETURN;
END IF;

SELECT valeur::INTEGER
INTO v_seuil_hierarchie
FROM parametre
WHERE code = 'NOTIFICATION_PLAN_ACTION_SEUIL_RETARD_HIERARCHIE_JOURS'
  AND actif = TRUE
    LIMIT 1;

IF v_seuil_hierarchie IS NULL THEN
        v_seuil_hierarchie := 7;   -- valeur de repli
END IF;

    /* ============================================================
     * 1. PLANS D'ACTION ARRIVANT À ÉCHÉANCE
     * ============================================================ */
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action)
    ap.id_plan_action,
    ap.valeur_pourcentage
FROM avancement_plan_action ap
ORDER BY ap.id_plan_action, ap.date_changement DESC, ap.id_avancement_plan_action DESC
    ),
    etat_plan AS (
SELECT
    pa.id_plan_action,
    pa.code,
    pa.designation,
    pa.date_debut_prevue,
    pa.date_fin_prevue,
    pa.date_debut_reelle,
    pa.date_fin_reelle,
    COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
    CASE
    WHEN pa.date_fin_reelle IS NOT NULL OR COALESCE(da.valeur_pourcentage, 0) = 100
    THEN TRUE ELSE FALSE
    END AS est_termine
FROM plan_action pa
    LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
    )
INSERT INTO notification (titre, message, date_creation, id_priorite,
                          id_utilisateur, id_reference, resource)
SELECT
    'Échéance proche — plan d''action',
    'Le plan d''action "' || ep.code || ' — ' || ep.designation ||
    '" arrive à échéance dans ' || v_jours_avant || ' jour(s).',
    CURRENT_TIMESTAMP,
    (SELECT id_priorite FROM priorite WHERE code = 'NORMALE'),
    apa.id_utilisateur,
    ep.id_plan_action,
    'PLAN_ACTION'
FROM etat_plan ep
         INNER JOIN affectation_plan_action apa ON apa.id_plan_action = ep.id_plan_action
         INNER JOIN role r ON r.id_role = apa.id_role AND r.code = 'RESPONSABLE'
WHERE ep.date_fin_prevue = CURRENT_DATE + v_jours_avant
  AND ep.est_termine = FALSE
  AND apa.date_desaffectation IS NULL;

/* ============================================================
 * 1bis. PLANS D'ACTION NON DÉMARRÉS DONT LA DATE DE DÉBUT EST DÉPASSÉE
 * ============================================================ */
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action)
    ap.id_plan_action,
    ap.valeur_pourcentage
FROM avancement_plan_action ap
ORDER BY ap.id_plan_action, ap.date_changement DESC, ap.id_avancement_plan_action DESC
    ),
    etat_plan AS (
SELECT
    pa.id_plan_action,
    pa.code,
    pa.designation,
    pa.date_debut_prevue,
    pa.date_fin_prevue,
    pa.date_debut_reelle,
    pa.date_fin_reelle,
    COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
    CASE
    WHEN pa.date_fin_reelle IS NOT NULL OR COALESCE(da.valeur_pourcentage, 0) = 100
    THEN TRUE ELSE FALSE
    END AS est_termine,
    CASE
    WHEN pa.date_fin_reelle IS NOT NULL OR COALESCE(da.valeur_pourcentage, 0) = 100
    THEN FALSE
    WHEN pa.date_debut_reelle IS NULL OR COALESCE(da.valeur_pourcentage, 0) = 0
    THEN TRUE
    ELSE FALSE
    END AS est_non_commence
FROM plan_action pa
    LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
    )
INSERT INTO notification (titre, message, date_creation, id_priorite,
                          id_utilisateur, id_reference, resource)
SELECT
    'Démarrage en retard — plan d''action',
    'Le plan d''action "' || ep.code || ' — ' || ep.designation ||
    '" devait démarrer le ' || TO_CHAR(ep.date_debut_prevue, 'DD/MM/YYYY') ||
    ' mais n''a pas encore été démarré (' ||
    (CURRENT_DATE - ep.date_debut_prevue) || ' jour(s) de retard).',
    CURRENT_TIMESTAMP,
    (SELECT id_priorite FROM priorite WHERE code = 'HAUTE'),
    apa.id_utilisateur,
    ep.id_plan_action,
    'PLAN_ACTION'
FROM etat_plan ep
         INNER JOIN affectation_plan_action apa ON apa.id_plan_action = ep.id_plan_action
         INNER JOIN role r ON r.id_role = apa.id_role AND r.code = 'RESPONSABLE'
WHERE ep.date_debut_prevue < CURRENT_DATE
  AND ep.est_non_commence = TRUE
  AND apa.date_desaffectation IS NULL;

/* ============================================================
 * 2. PLANS D'ACTION EN RETARD
 * ============================================================ */
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action)
    ap.id_plan_action,
    ap.valeur_pourcentage
FROM avancement_plan_action ap
ORDER BY ap.id_plan_action, ap.date_changement DESC, ap.id_avancement_plan_action DESC
    ),
    etat_plan AS (
SELECT
    pa.id_plan_action,
    pa.code,
    pa.designation,
    pa.date_debut_prevue,
    pa.date_fin_prevue,
    pa.date_debut_reelle,
    pa.date_fin_reelle,
    COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
    CASE
    WHEN pa.date_fin_reelle IS NOT NULL OR COALESCE(da.valeur_pourcentage, 0) = 100
    THEN TRUE ELSE FALSE
    END AS est_termine,
    CASE
    WHEN pa.date_fin_reelle IS NOT NULL OR COALESCE(da.valeur_pourcentage, 0) = 100
    THEN FALSE
    WHEN pa.date_debut_reelle IS NULL OR COALESCE(da.valeur_pourcentage, 0) = 0
    THEN TRUE
    ELSE FALSE
    END AS est_non_commence
FROM plan_action pa
    LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
    )
INSERT INTO notification (titre, message, date_creation, id_priorite,
                          id_utilisateur, id_reference, resource)
SELECT
    'Plan d''action en retard',
    'Le plan d''action "' || ep.code || ' — ' || ep.designation ||
    '" est en retard depuis ' || (CURRENT_DATE - ep.date_fin_prevue) || ' jour(s)' ||
    ' (avancement : ' || ep.valeur_pourcentage || ' %).',
    CURRENT_TIMESTAMP,
    (SELECT id_priorite FROM priorite WHERE code = 'HAUTE'),
    apa.id_utilisateur,
    ep.id_plan_action,
    'PLAN_ACTION'
FROM etat_plan ep
         INNER JOIN affectation_plan_action apa ON apa.id_plan_action = ep.id_plan_action
         INNER JOIN role r ON r.id_role = apa.id_role AND r.code = 'RESPONSABLE'
WHERE ep.date_fin_prevue < CURRENT_DATE
  AND ep.est_termine = FALSE
  AND ep.est_non_commence = FALSE
  AND apa.date_desaffectation IS NULL;

/* ============================================================
 * 2bis. ALERTE HIÉRARCHIQUE — PLAN D'ACTION EN RETARD PROLONGÉ
 *
 *   Déclenchée si :
 *     - le plan est en retard (comme section 2)
 *     - retard >= v_seuil_hierarchie jours
 *     - plan rattaché à une origine de type INCIDENT
 *
 *   Destinataires (par RESPONSABLE du plan) :
 *     - chef du service du responsable
 *     - chef du département du responsable
 * ============================================================ */
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action)
    ap.id_plan_action,
    ap.valeur_pourcentage
FROM avancement_plan_action ap
ORDER BY ap.id_plan_action, ap.date_changement DESC, ap.id_avancement_plan_action DESC
    ),
    plans_critiques AS (
SELECT DISTINCT
    pa.id_plan_action,
    pa.code,
    pa.designation,
    pa.date_fin_prevue,
    COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
    u_resp.id_utilisateur AS id_responsable,
    u_resp.id_service     AS id_service,
    u_resp.id_departement AS id_departement
FROM plan_action pa
    INNER JOIN affectation_plan_action apa
ON apa.id_plan_action = pa.id_plan_action
    AND apa.date_desaffectation IS NULL
    INNER JOIN role r ON r.id_role = apa.id_role AND r.code = 'RESPONSABLE'
    INNER JOIN utilisateur u_resp ON u_resp.id_utilisateur = apa.id_utilisateur
    INNER JOIN plan_action_origine pao ON pao.id_plan_action = pa.id_plan_action
    INNER JOIN origine o ON o.id_origine = pao.id_origine
    AND o.type_origine = 'INCIDENT'
    LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
WHERE pa.date_fin_prevue < CURRENT_DATE
  AND pa.date_fin_reelle IS NULL
  AND COALESCE(da.valeur_pourcentage, 0) < 100
  AND (pa.date_debut_reelle IS NOT NULL
   OR COALESCE(da.valeur_pourcentage, 0) > 0)
  AND (CURRENT_DATE - pa.date_fin_prevue) >= v_seuil_hierarchie
    ),
    destinataires AS (
-- Chef du service du responsable
SELECT DISTINCT
    pc.id_plan_action,
    pc.code,
    pc.designation,
    pc.date_fin_prevue,
    pc.valeur_pourcentage,
    u.id_utilisateur
FROM plans_critiques pc
    INNER JOIN utilisateur u ON u.id_service = pc.id_service
    INNER JOIN poste po ON po.id_poste = u.id_poste
WHERE po.nom LIKE 'Chef de service%'
  AND u.actif = TRUE

UNION

-- Chef du département du responsable
SELECT DISTINCT
    pc.id_plan_action,
    pc.code,
    pc.designation,
    pc.date_fin_prevue,
    pc.valeur_pourcentage,
    u.id_utilisateur
FROM plans_critiques pc
    INNER JOIN utilisateur u ON u.id_departement = pc.id_departement
    INNER JOIN poste po ON po.id_poste = u.id_poste
WHERE po.nom LIKE 'Chef de département%'
  AND u.actif = TRUE
    )
INSERT INTO notification (titre, message, date_creation, id_priorite,
                          id_utilisateur, id_reference, resource)
SELECT
    'Alerte : plan d''action en retard prolongé',
    'Le plan d''action "' || d.code || ' — ' || d.designation ||
    '" est en retard depuis ' || (CURRENT_DATE - d.date_fin_prevue) || ' jour(s)' ||
    ' (avancement : ' || d.valeur_pourcentage || ' %).',
    CURRENT_TIMESTAMP,
    (SELECT id_priorite FROM priorite WHERE code = 'CRITIQUE'),
    d.id_utilisateur,
    d.id_plan_action,
    'PLAN_ACTION'
FROM destinataires d;

/* ============================================================
 * 3. AJOUT DU STATUT EN_RETARD DANS L'HISTORIQUE DU PLAN
 *
 *   Anti-doublon : on n'insère que si le dernier statut n'est
 *   PAS déjà EN_RETARD.
 *   L'utilisateur associé est le dernier à avoir avancé le plan,
 *   sinon un RESPONSABLE actif en repli.
 * ============================================================ */
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action)
    ap.id_plan_action,
    ap.valeur_pourcentage,
    ap.id_statut,
    ap.id_utilisateur
FROM avancement_plan_action ap
ORDER BY ap.id_plan_action, ap.date_changement DESC, ap.id_avancement_plan_action DESC
    ),
    plans_en_retard AS (
SELECT DISTINCT ON (pa.id_plan_action)
    pa.id_plan_action,
    COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
    COALESCE(da.id_utilisateur, apa.id_utilisateur) AS id_utilisateur,
    COALESCE(s.code, 'NON_COMMENCEE') AS statut_code
FROM plan_action pa
    LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
    LEFT JOIN statut s ON s.id_statut = da.id_statut
    INNER JOIN affectation_plan_action apa
    ON apa.id_plan_action = pa.id_plan_action
    AND apa.date_desaffectation IS NULL
    INNER JOIN role r ON r.id_role = apa.id_role AND r.code = 'RESPONSABLE'
WHERE pa.date_fin_prevue < CURRENT_DATE
  AND pa.date_fin_reelle IS NULL
  AND COALESCE(da.valeur_pourcentage, 0) < 100
  AND (pa.date_debut_reelle IS NOT NULL OR COALESCE(da.valeur_pourcentage, 0) > 0)
  AND COALESCE(s.code, 'NON_COMMENCEE') <> 'EN_RETARD'
ORDER BY pa.id_plan_action, apa.id_affectation_plan_action
    )
INSERT INTO avancement_plan_action
(valeur_pourcentage, commentaire, date_changement,
 id_utilisateur, id_statut, id_plan_action)
SELECT
    per.valeur_pourcentage,
    'Passage automatique en retard.',
    CURRENT_TIMESTAMP,
    per.id_utilisateur,
    (SELECT id_statut FROM statut WHERE code = 'EN_RETARD'),
    per.id_plan_action
FROM plans_en_retard per;

END;
$$;

CREATE OR REPLACE FUNCTION verifier_retour_en_cours_activites()
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN

    /* ============================================================
     * Activités dont le statut courant est EN_RETARD
     * mais qui n'ont PLUS aucune sous-activité en retard actif.
     *
     *   Une sous-activité est considérée « en retard » si :
     *     - elle n'est pas terminée
     *       (date_fin_reelle IS NULL ET valeur_pourcentage < 100)
     *     - elle a bien démarré
     *       (date_debut_reelle IS NOT NULL OU valeur_pourcentage > 0)
     *     - son échéance est dépassée
     *       (date_fin_prevue < CURRENT_DATE)
     *     - elle est toujours affectée à un RESPONSABLE actif
     *
     * On ne traite que les activités dont le DERNIER statut est
     * EN_RETARD : cela évite toute réinsertion en boucle de EN_COURS.
     * ============================================================ */
WITH statut_courant AS (
    SELECT DISTINCT ON (ha.id_activite)
    ha.id_activite,
    ha.id_utilisateur,
    s.code AS statut_code
FROM historique_activite ha
    INNER JOIN statut s ON s.id_statut = ha.id_statut
ORDER BY ha.id_activite,
    ha.date_changement DESC,
    ha.id_historique_activite DESC
    ),
    dernier_avancement AS (
SELECT DISTINCT ON (av.id_sous_activite)
    av.id_sous_activite,
    av.valeur_pourcentage
FROM avancement_sous_activite av
ORDER BY av.id_sous_activite,
    av.date_changement DESC,
    av.id_historique_sous_activite DESC
    ),
    sous_activite_etat AS (
SELECT
    sa.id_sous_activite,
    sa.id_activite,
    sa.date_debut_reelle,
    sa.date_fin_reelle,
    sa.date_fin_prevue,
    COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
    CASE
    WHEN sa.date_fin_reelle IS NOT NULL
    OR COALESCE(da.valeur_pourcentage, 0) = 100
    THEN TRUE ELSE FALSE
    END AS est_terminee,
    CASE
    WHEN sa.date_fin_reelle IS NOT NULL
    OR COALESCE(da.valeur_pourcentage, 0) = 100
    THEN FALSE
    WHEN sa.date_debut_reelle IS NULL
    OR COALESCE(da.valeur_pourcentage, 0) = 0
    THEN TRUE
    ELSE FALSE
    END AS est_non_commencee
FROM sous_activite sa
    LEFT JOIN dernier_avancement da
ON da.id_sous_activite = sa.id_sous_activite
    ),
    activites_avec_sous_activite_en_retard AS (
SELECT DISTINCT sea.id_activite
FROM sous_activite_etat sea
    INNER JOIN affectation_sous_activite asa
ON asa.id_sous_activite = sea.id_sous_activite
    AND asa.date_desaffectation IS NULL
    INNER JOIN role r
    ON r.id_role = asa.id_role
    AND r.code = 'RESPONSABLE'
WHERE sea.date_fin_prevue < CURRENT_DATE
  AND sea.est_terminee = FALSE
  AND sea.est_non_commencee = FALSE
    ),
    activites_a_repasser_en_cours AS (
SELECT
    sc.id_activite,
    sc.id_utilisateur
FROM statut_courant sc
WHERE sc.statut_code = 'EN_RETARD'
  AND NOT EXISTS (
    SELECT 1
    FROM activites_avec_sous_activite_en_retard aser
    WHERE aser.id_activite = sc.id_activite
    )
    )
INSERT INTO historique_activite (
    commentaire, date_changement, id_utilisateur, id_statut, id_activite
)
SELECT
    'Retour automatique en cours : plus aucune sous-activité en retard.',
    CURRENT_TIMESTAMP,
    arc.id_utilisateur,
    (SELECT id_statut FROM statut WHERE code = 'EN_COURS'),
    arc.id_activite
FROM activites_a_repasser_en_cours arc;

END;
$$;

CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Sous-activités : 23h20
SELECT cron.schedule(
               'notification-sous-activites',
               '0 7 * * *',
               $$SELECT verifier_notifications_sous_activites();$$
);

-- Plans d'action : 23h30
SELECT cron.schedule(
               'notification-plans-action',
               '1 7 * * *',
               $$SELECT verifier_notifications_plans_action();$$
);

SELECT cron.schedule(
               'retour-en-cours-activites',
               '2 7 * * *',
               $$SELECT verifier_retour_en_cours_activites();$$
);