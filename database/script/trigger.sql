-- =============================================================================
-- SAGA — Fonctions + triggers + planification cron
--
-- À exécuter APRÈS data.sql.
-- Cron : 05h30 Madagascar (UTC+3) = 02h30 UTC.
-- =============================================================================
-- =============================================================================
-- 1. FONCTIONS DE NOTIFICATION
-- =============================================================================
CREATE OR REPLACE FUNCTION verifier_notifications_sous_activites() RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE v_jours_avant INTEGER;
v_seuil_hierarchie INTEGER;
BEGIN
SELECT valeur::INTEGER INTO v_jours_avant
FROM parametre
WHERE code = 'NOTIFICATION_SOUS_ACTIVITE_AVANT_ECHEANCE_JOURS'
    AND actif = TRUE
LIMIT 1;
IF v_jours_avant IS NULL THEN RAISE NOTICE 'Paramètre NOTIFICATION_SOUS_ACTIVITE_AVANT_ECHEANCE_JOURS non configuré.';
RETURN;
END IF;
SELECT valeur::INTEGER INTO v_seuil_hierarchie
FROM parametre
WHERE code = 'NOTIFICATION_SOUS_ACTIVITE_SEUIL_RETARD_HIERARCHIE_JOURS'
    AND actif = TRUE
LIMIT 1;
IF v_seuil_hierarchie IS NULL THEN v_seuil_hierarchie := 7;
END IF;
-- Échéance proche
WITH dernier_avancement AS (
    SELECT DISTINCT ON (av.id_sous_activite) av.id_sous_activite,
        av.valeur_pourcentage
    FROM avancement_sous_activite av
    ORDER BY av.id_sous_activite,
        av.date_changement DESC,
        av.id_historique_sous_activite DESC
),
etat AS (
    SELECT sa.id_sous_activite,
        sa.code,
        sa.designation,
        sa.date_fin_prevue,
        COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
        CASE
            WHEN sa.date_fin_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN TRUE
            ELSE FALSE
        END AS est_terminee
    FROM sous_activite sa
        LEFT JOIN dernier_avancement da ON da.id_sous_activite = sa.id_sous_activite
)
INSERT INTO notification (
        titre,
        message,
        date_creation,
        id_priorite,
        id_utilisateur,
        id_reference,
        resource
    )
SELECT 'Échéance proche — sous-activité',
    'La sous-activité "' || e.code || ' — ' || e.designation || '" arrive à échéance dans ' || v_jours_avant || ' jour(s).',
    CURRENT_TIMESTAMP,
    (
        SELECT id_priorite
        FROM priorite
        WHERE code = 'NORMALE'
    ),
    asa.id_utilisateur,
    e.id_sous_activite,
    'SOUS_ACTIVITE'
FROM etat e
    INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = e.id_sous_activite
    INNER JOIN role r ON r.id_role = asa.id_role
    AND r.code = 'RESPONSABLE'
WHERE e.date_fin_prevue = CURRENT_DATE + v_jours_avant
    AND e.est_terminee = FALSE
    AND asa.date_desaffectation IS NULL;
-- Démarrage en retard
WITH dernier_avancement AS (
    SELECT DISTINCT ON (av.id_sous_activite) av.id_sous_activite,
        av.valeur_pourcentage
    FROM avancement_sous_activite av
    ORDER BY av.id_sous_activite,
        av.date_changement DESC,
        av.id_historique_sous_activite DESC
),
etat AS (
    SELECT sa.id_sous_activite,
        sa.code,
        sa.designation,
        sa.date_debut_prevue,
        CASE
            WHEN sa.date_fin_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN FALSE
            WHEN sa.date_debut_reelle IS NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 0 THEN TRUE
            ELSE FALSE
        END AS est_non_commencee
    FROM sous_activite sa
        LEFT JOIN dernier_avancement da ON da.id_sous_activite = sa.id_sous_activite
)
INSERT INTO notification (
        titre,
        message,
        date_creation,
        id_priorite,
        id_utilisateur,
        id_reference,
        resource
    )
SELECT 'Démarrage en retard — sous-activité',
    'La sous-activité "' || e.code || ' — ' || e.designation || '" devait démarrer le ' || TO_CHAR(e.date_debut_prevue, 'DD/MM/YYYY') || ' mais n''a pas encore été démarrée (' || (CURRENT_DATE - e.date_debut_prevue) || ' jour(s) de retard).',
    CURRENT_TIMESTAMP,
    (
        SELECT id_priorite
        FROM priorite
        WHERE code = 'HAUTE'
    ),
    asa.id_utilisateur,
    e.id_sous_activite,
    'SOUS_ACTIVITE'
FROM etat e
    INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = e.id_sous_activite
    INNER JOIN role r ON r.id_role = asa.id_role
    AND r.code = 'RESPONSABLE'
WHERE e.date_debut_prevue < CURRENT_DATE
    AND e.est_non_commencee = TRUE
    AND asa.date_desaffectation IS NULL;
-- En retard
WITH dernier_avancement AS (
    SELECT DISTINCT ON (av.id_sous_activite) av.id_sous_activite,
        av.valeur_pourcentage
    FROM avancement_sous_activite av
    ORDER BY av.id_sous_activite,
        av.date_changement DESC,
        av.id_historique_sous_activite DESC
),
etat AS (
    SELECT sa.id_sous_activite,
        sa.code,
        sa.designation,
        sa.date_fin_prevue,
        COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
        CASE
            WHEN sa.date_fin_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN TRUE
            ELSE FALSE
        END AS est_terminee,
        CASE
            WHEN sa.date_fin_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN FALSE
            WHEN sa.date_debut_reelle IS NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 0 THEN TRUE
            ELSE FALSE
        END AS est_non_commencee
    FROM sous_activite sa
        LEFT JOIN dernier_avancement da ON da.id_sous_activite = sa.id_sous_activite
)
INSERT INTO notification (
        titre,
        message,
        date_creation,
        id_priorite,
        id_utilisateur,
        id_reference,
        resource
    )
SELECT 'Sous-activité en retard',
    'La sous-activité "' || e.code || ' — ' || e.designation || '" est en retard depuis ' || (CURRENT_DATE - e.date_fin_prevue) || ' jour(s)' || ' (avancement : ' || e.valeur_pourcentage || ' %).',
    CURRENT_TIMESTAMP,
    (
        SELECT id_priorite
        FROM priorite
        WHERE code = 'HAUTE'
    ),
    asa.id_utilisateur,
    e.id_sous_activite,
    'SOUS_ACTIVITE'
FROM etat e
    INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = e.id_sous_activite
    INNER JOIN role r ON r.id_role = asa.id_role
    AND r.code = 'RESPONSABLE'
WHERE e.date_fin_prevue < CURRENT_DATE
    AND e.est_terminee = FALSE
    AND e.est_non_commencee = FALSE
    AND asa.date_desaffectation IS NULL;
-- Marquage EN_RETARD
WITH dernier_avancement AS (
    SELECT DISTINCT ON (av.id_sous_activite) av.id_sous_activite,
        av.valeur_pourcentage,
        av.id_statut,
        av.id_utilisateur
    FROM avancement_sous_activite av
    ORDER BY av.id_sous_activite,
        av.date_changement DESC,
        av.id_historique_sous_activite DESC
),
sa_en_retard AS (
    SELECT DISTINCT ON (sa.id_sous_activite) sa.id_sous_activite,
        COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
        COALESCE(da.id_utilisateur, asa.id_utilisateur) AS id_utilisateur,
        COALESCE(s.code, 'NON_COMMENCEE') AS statut_code
    FROM sous_activite sa
        LEFT JOIN dernier_avancement da ON da.id_sous_activite = sa.id_sous_activite
        LEFT JOIN statut s ON s.id_statut = da.id_statut
        INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = sa.id_sous_activite
        AND asa.date_desaffectation IS NULL
        INNER JOIN role r ON r.id_role = asa.id_role
        AND r.code = 'RESPONSABLE'
    WHERE sa.date_fin_prevue < CURRENT_DATE
        AND sa.date_fin_reelle IS NULL
        AND COALESCE(da.valeur_pourcentage, 0) < 100
        AND (
            sa.date_debut_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) > 0
        )
        AND COALESCE(s.code, 'NON_COMMENCEE') <> 'EN_RETARD'
    ORDER BY sa.id_sous_activite,
        asa.id_affectation_sous_activite
)
INSERT INTO avancement_sous_activite (
        valeur_pourcentage,
        commentaire,
        date_changement,
        id_utilisateur,
        id_statut,
        id_sous_activite
    )
SELECT saer.valeur_pourcentage,
    'Passage automatique en retard.',
    CURRENT_TIMESTAMP,
    saer.id_utilisateur,
    (
        SELECT id_statut
        FROM statut
        WHERE code = 'EN_RETARD'
    ),
    saer.id_sous_activite
FROM sa_en_retard saer;
END;
$$;
CREATE OR REPLACE FUNCTION verifier_notifications_plans_action() RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE v_jours_avant INTEGER;
v_seuil_hierarchie INTEGER;
BEGIN
SELECT valeur::INTEGER INTO v_jours_avant
FROM parametre
WHERE code = 'NOTIFICATION_PLAN_ACTION_AVANT_ECHEANCE_JOURS'
    AND actif = TRUE
LIMIT 1;
IF v_jours_avant IS NULL THEN RAISE NOTICE 'Paramètre NOTIFICATION_PLAN_ACTION_AVANT_ECHEANCE_JOURS non configuré.';
RETURN;
END IF;
SELECT valeur::INTEGER INTO v_seuil_hierarchie
FROM parametre
WHERE code = 'NOTIFICATION_PLAN_ACTION_SEUIL_RETARD_HIERARCHIE_JOURS'
    AND actif = TRUE
LIMIT 1;
IF v_seuil_hierarchie IS NULL THEN v_seuil_hierarchie := 7;
END IF;
-- Échéance proche
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action) ap.id_plan_action,
        ap.valeur_pourcentage
    FROM avancement_plan_action ap
    ORDER BY ap.id_plan_action,
        ap.date_changement DESC,
        ap.id_avancement_plan_action DESC
),
etat AS (
    SELECT pa.id_plan_action,
        pa.code,
        pa.designation,
        pa.date_fin_prevue,
        COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
        CASE
            WHEN pa.date_fin_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN TRUE
            ELSE FALSE
        END AS est_termine
    FROM plan_action pa
        LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
)
INSERT INTO notification (
        titre,
        message,
        date_creation,
        id_priorite,
        id_utilisateur,
        id_reference,
        resource
    )
SELECT 'Échéance proche — plan d''action',
    'Le plan "' || e.code || ' — ' || e.designation || '" arrive à échéance dans ' || v_jours_avant || ' jour(s).',
    CURRENT_TIMESTAMP,
    (
        SELECT id_priorite
        FROM priorite
        WHERE code = 'NORMALE'
    ),
    apa.id_utilisateur,
    e.id_plan_action,
    'PLAN_ACTION'
FROM etat e
    INNER JOIN affectation_plan_action apa ON apa.id_plan_action = e.id_plan_action
    INNER JOIN role r ON r.id_role = apa.id_role
    AND r.code = 'RESPONSABLE'
WHERE e.date_fin_prevue = CURRENT_DATE + v_jours_avant
    AND e.est_termine = FALSE
    AND apa.date_desaffectation IS NULL;
-- Démarrage en retard
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action) ap.id_plan_action,
        ap.valeur_pourcentage
    FROM avancement_plan_action ap
    ORDER BY ap.id_plan_action,
        ap.date_changement DESC,
        ap.id_avancement_plan_action DESC
),
etat AS (
    SELECT pa.id_plan_action,
        pa.code,
        pa.designation,
        pa.date_debut_prevue,
        CASE
            WHEN pa.date_fin_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN FALSE
            WHEN pa.date_debut_reelle IS NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 0 THEN TRUE
            ELSE FALSE
        END AS est_non_commence
    FROM plan_action pa
        LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
)
INSERT INTO notification (
        titre,
        message,
        date_creation,
        id_priorite,
        id_utilisateur,
        id_reference,
        resource
    )
SELECT 'Démarrage en retard — plan d''action',
    'Le plan "' || e.code || ' — ' || e.designation || '" devait démarrer le ' || TO_CHAR(e.date_debut_prevue, 'DD/MM/YYYY') || ' mais n''a pas encore été démarré (' || (CURRENT_DATE - e.date_debut_prevue) || ' jour(s) de retard).',
    CURRENT_TIMESTAMP,
    (
        SELECT id_priorite
        FROM priorite
        WHERE code = 'HAUTE'
    ),
    apa.id_utilisateur,
    e.id_plan_action,
    'PLAN_ACTION'
FROM etat e
    INNER JOIN affectation_plan_action apa ON apa.id_plan_action = e.id_plan_action
    INNER JOIN role r ON r.id_role = apa.id_role
    AND r.code = 'RESPONSABLE'
WHERE e.date_debut_prevue < CURRENT_DATE
    AND e.est_non_commence = TRUE
    AND apa.date_desaffectation IS NULL;
-- En retard
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action) ap.id_plan_action,
        ap.valeur_pourcentage
    FROM avancement_plan_action ap
    ORDER BY ap.id_plan_action,
        ap.date_changement DESC,
        ap.id_avancement_plan_action DESC
),
etat AS (
    SELECT pa.id_plan_action,
        pa.code,
        pa.designation,
        pa.date_fin_prevue,
        COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
        CASE
            WHEN pa.date_fin_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN TRUE
            ELSE FALSE
        END AS est_termine,
        CASE
            WHEN pa.date_fin_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN FALSE
            WHEN pa.date_debut_reelle IS NULL
            OR COALESCE(da.valeur_pourcentage, 0) = 0 THEN TRUE
            ELSE FALSE
        END AS est_non_commence
    FROM plan_action pa
        LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
)
INSERT INTO notification (
        titre,
        message,
        date_creation,
        id_priorite,
        id_utilisateur,
        id_reference,
        resource
    )
SELECT 'Plan d''action en retard',
    'Le plan "' || e.code || ' — ' || e.designation || '" est en retard depuis ' || (CURRENT_DATE - e.date_fin_prevue) || ' jour(s)' || ' (avancement : ' || e.valeur_pourcentage || ' %).',
    CURRENT_TIMESTAMP,
    (
        SELECT id_priorite
        FROM priorite
        WHERE code = 'HAUTE'
    ),
    apa.id_utilisateur,
    e.id_plan_action,
    'PLAN_ACTION'
FROM etat e
    INNER JOIN affectation_plan_action apa ON apa.id_plan_action = e.id_plan_action
    INNER JOIN role r ON r.id_role = apa.id_role
    AND r.code = 'RESPONSABLE'
WHERE e.date_fin_prevue < CURRENT_DATE
    AND e.est_termine = FALSE
    AND e.est_non_commence = FALSE
    AND apa.date_desaffectation IS NULL;
-- Alerte hiérarchique
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action) ap.id_plan_action,
        ap.valeur_pourcentage
    FROM avancement_plan_action ap
    ORDER BY ap.id_plan_action,
        ap.date_changement DESC,
        ap.id_avancement_plan_action DESC
),
plans_critiques AS (
    SELECT DISTINCT pa.id_plan_action,
        pa.code,
        pa.designation,
        pa.date_fin_prevue,
        COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
        u_resp.id_service AS id_service,
        u_resp.id_departement AS id_departement
    FROM plan_action pa
        INNER JOIN affectation_plan_action apa ON apa.id_plan_action = pa.id_plan_action
        AND apa.date_desaffectation IS NULL
        INNER JOIN role r ON r.id_role = apa.id_role
        AND r.code = 'RESPONSABLE'
        INNER JOIN utilisateur u_resp ON u_resp.id_utilisateur = apa.id_utilisateur
        INNER JOIN plan_action_origine pao ON pao.id_plan_action = pa.id_plan_action
        INNER JOIN origine o ON o.id_origine = pao.id_origine
        AND o.type_origine = 'INCIDENT'
        LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
    WHERE pa.date_fin_prevue < CURRENT_DATE
        AND pa.date_fin_reelle IS NULL
        AND COALESCE(da.valeur_pourcentage, 0) < 100
        AND (
            pa.date_debut_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) > 0
        )
        AND (CURRENT_DATE - pa.date_fin_prevue) >= v_seuil_hierarchie
),
destinataires AS (
    SELECT DISTINCT pc.id_plan_action,
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
    SELECT DISTINCT pc.id_plan_action,
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
INSERT INTO notification (
        titre,
        message,
        date_creation,
        id_priorite,
        id_utilisateur,
        id_reference,
        resource
    )
SELECT 'Alerte : plan d''action en retard prolongé',
    'Le plan "' || d.code || ' — ' || d.designation || '" est en retard depuis ' || (CURRENT_DATE - d.date_fin_prevue) || ' jour(s)' || ' (avancement : ' || d.valeur_pourcentage || ' %).',
    CURRENT_TIMESTAMP,
    (
        SELECT id_priorite
        FROM priorite
        WHERE code = 'CRITIQUE'
    ),
    d.id_utilisateur,
    d.id_plan_action,
    'PLAN_ACTION'
FROM destinataires d;
-- Marquage EN_RETARD
WITH dernier_avancement AS (
    SELECT DISTINCT ON (ap.id_plan_action) ap.id_plan_action,
        ap.valeur_pourcentage,
        ap.id_statut,
        ap.id_utilisateur
    FROM avancement_plan_action ap
    ORDER BY ap.id_plan_action,
        ap.date_changement DESC,
        ap.id_avancement_plan_action DESC
),
plans_en_retard AS (
    SELECT DISTINCT ON (pa.id_plan_action) pa.id_plan_action,
        COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
        COALESCE(da.id_utilisateur, apa.id_utilisateur) AS id_utilisateur,
        COALESCE(s.code, 'NON_COMMENCEE') AS statut_code
    FROM plan_action pa
        LEFT JOIN dernier_avancement da ON da.id_plan_action = pa.id_plan_action
        LEFT JOIN statut s ON s.id_statut = da.id_statut
        INNER JOIN affectation_plan_action apa ON apa.id_plan_action = pa.id_plan_action
        AND apa.date_desaffectation IS NULL
        INNER JOIN role r ON r.id_role = apa.id_role
        AND r.code = 'RESPONSABLE'
    WHERE pa.date_fin_prevue < CURRENT_DATE
        AND pa.date_fin_reelle IS NULL
        AND COALESCE(da.valeur_pourcentage, 0) < 100
        AND (
            pa.date_debut_reelle IS NOT NULL
            OR COALESCE(da.valeur_pourcentage, 0) > 0
        )
        AND COALESCE(s.code, 'NON_COMMENCEE') <> 'EN_RETARD'
    ORDER BY pa.id_plan_action,
        apa.id_affectation_plan_action
)
INSERT INTO avancement_plan_action (
        valeur_pourcentage,
        commentaire,
        date_changement,
        id_utilisateur,
        id_statut,
        id_plan_action
    )
SELECT per.valeur_pourcentage,
    'Passage automatique en retard.',
    CURRENT_TIMESTAMP,
    per.id_utilisateur,
    (
        SELECT id_statut
        FROM statut
        WHERE code = 'EN_RETARD'
    ),
    per.id_plan_action
FROM plans_en_retard per;
END;
$$;
CREATE OR REPLACE FUNCTION verifier_retour_en_cours_activites() RETURNS VOID LANGUAGE plpgsql AS $$ BEGIN WITH statut_courant AS (
        SELECT DISTINCT ON (ha.id_activite) ha.id_activite,
            ha.id_utilisateur,
            s.code AS statut_code
        FROM historique_activite ha
            INNER JOIN statut s ON s.id_statut = ha.id_statut
        ORDER BY ha.id_activite,
            ha.date_changement DESC,
            ha.id_historique_activite DESC
    ),
    dernier_avancement AS (
        SELECT DISTINCT ON (av.id_sous_activite) av.id_sous_activite,
            av.valeur_pourcentage
        FROM avancement_sous_activite av
        ORDER BY av.id_sous_activite,
            av.date_changement DESC,
            av.id_historique_sous_activite DESC
    ),
    etat AS (
        SELECT sa.id_sous_activite,
            sa.id_activite,
            sa.date_debut_reelle,
            sa.date_fin_reelle,
            sa.date_fin_prevue,
            COALESCE(da.valeur_pourcentage, 0) AS valeur_pourcentage,
            CASE
                WHEN sa.date_fin_reelle IS NOT NULL
                OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN TRUE
                ELSE FALSE
            END AS est_terminee,
            CASE
                WHEN sa.date_fin_reelle IS NOT NULL
                OR COALESCE(da.valeur_pourcentage, 0) = 100 THEN FALSE
                WHEN sa.date_debut_reelle IS NULL
                OR COALESCE(da.valeur_pourcentage, 0) = 0 THEN TRUE
                ELSE FALSE
            END AS est_non_commencee
        FROM sous_activite sa
            LEFT JOIN dernier_avancement da ON da.id_sous_activite = sa.id_sous_activite
    ),
    avec_retard AS (
        SELECT DISTINCT e.id_activite
        FROM etat e
            INNER JOIN affectation_sous_activite asa ON asa.id_sous_activite = e.id_sous_activite
            AND asa.date_desaffectation IS NULL
            INNER JOIN role r ON r.id_role = asa.id_role
            AND r.code = 'RESPONSABLE'
        WHERE e.date_fin_prevue < CURRENT_DATE
            AND e.est_terminee = FALSE
            AND e.est_non_commencee = FALSE
    ),
    a_repasser AS (
        SELECT sc.id_activite,
            sc.id_utilisateur
        FROM statut_courant sc
        WHERE sc.statut_code = 'EN_RETARD'
            AND NOT EXISTS (
                SELECT 1
                FROM avec_retard ar
                WHERE ar.id_activite = sc.id_activite
            )
    )
INSERT INTO historique_activite (
        commentaire,
        date_changement,
        id_utilisateur,
        id_statut,
        id_activite
    )
SELECT 'Retour automatique en cours : plus aucune sous-activité en retard.',
    CURRENT_TIMESTAMP,
    ar.id_utilisateur,
    (
        SELECT id_statut
        FROM statut
        WHERE code = 'EN_COURS'
    ),
    ar.id_activite
FROM a_repasser ar;
END;
$$;
-- =============================================================================
-- 2. TRIGGERS
-- =============================================================================
CREATE OR REPLACE FUNCTION check_affectation_role_singulier() RETURNS TRIGGER AS $$
DECLARE v_role_code TEXT;
v_id_entity INTEGER;
BEGIN
SELECT code INTO v_role_code
FROM role
WHERE id_role = NEW.id_role;
IF v_role_code IS NULL
OR v_role_code NOT IN ('RESPONSABLE', 'SUPPLEANT') THEN RETURN NEW;
END IF;
IF NEW.date_desaffectation IS NOT NULL THEN RETURN NEW;
END IF;
IF TG_TABLE_NAME = 'affectation_activite' THEN v_id_entity := NEW.id_activite;
IF EXISTS (
    SELECT 1
    FROM affectation_activite
    WHERE id_activite = v_id_entity
        AND id_role = NEW.id_role
        AND date_desaffectation IS NULL
        AND id_affectation_activite <> COALESCE(NEW.id_affectation_activite, 0)
) THEN RAISE EXCEPTION 'Un rôle % est déjà affecté à cette activité',
v_role_code USING ERRCODE = 'unique_violation';
END IF;
ELSIF TG_TABLE_NAME = 'affectation_sous_activite' THEN v_id_entity := NEW.id_sous_activite;
IF EXISTS (
    SELECT 1
    FROM affectation_sous_activite
    WHERE id_sous_activite = v_id_entity
        AND id_role = NEW.id_role
        AND date_desaffectation IS NULL
        AND id_affectation_sous_activite <> COALESCE(NEW.id_affectation_sous_activite, 0)
) THEN RAISE EXCEPTION 'Un rôle % est déjà affecté à cette sous-activité',
v_role_code USING ERRCODE = 'unique_violation';
END IF;
ELSE RAISE EXCEPTION 'Trigger non supporté sur la table %',
TG_TABLE_NAME;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_affectation_activite_role_singulier ON affectation_activite;
CREATE TRIGGER trg_affectation_activite_role_singulier BEFORE
INSERT
    OR
UPDATE ON affectation_activite FOR EACH ROW EXECUTE FUNCTION check_affectation_role_singulier();
DROP TRIGGER IF EXISTS trg_affectation_sous_activite_role_singulier ON affectation_sous_activite;
CREATE TRIGGER trg_affectation_sous_activite_role_singulier BEFORE
INSERT
    OR
UPDATE ON affectation_sous_activite FOR EACH ROW EXECUTE FUNCTION check_affectation_role_singulier();
CREATE OR REPLACE FUNCTION notify_indicateur_seuil() RETURNS TRIGGER AS $$
DECLARE v_seuil_min NUMERIC(15, 2);
v_seuil_max NUMERIC(15, 2);
v_designation VARCHAR(250);
v_code VARCHAR(50);
v_type VARCHAR(20);
v_titre VARCHAR(255);
v_message TEXT;
v_hors_seuil BOOLEAN := FALSE;
v_priorite_id INTEGER;
v_recipient RECORD;
BEGIN
SELECT seuil_min,
    seuil_max,
    designation,
    code,
    type_indicateur INTO v_seuil_min,
    v_seuil_max,
    v_designation,
    v_code,
    v_type
FROM indicateur
WHERE id_indicateur = NEW.id_indicateur;
IF v_seuil_min IS NOT NULL
AND NEW.valeur < v_seuil_min THEN v_hors_seuil := TRUE;
v_titre := 'Alerte indicateur : valeur sous le seuil minimum';
v_message := 'L''indicateur « ' || COALESCE(v_code, '') || ' - ' || COALESCE(v_designation, '') || ' » a une valeur de ' || NEW.valeur || ', inférieure au seuil minimum de ' || v_seuil_min || ' pour la période du ' || TO_CHAR(NEW.periode_debut, 'DD/MM/YYYY') || ' au ' || TO_CHAR(NEW.periode_fin, 'DD/MM/YYYY') || '.';
ELSIF v_seuil_max IS NOT NULL
AND NEW.valeur > v_seuil_max THEN v_hors_seuil := TRUE;
v_titre := 'Alerte indicateur : valeur au-dessus du seuil maximum';
v_message := 'L''indicateur « ' || COALESCE(v_code, '') || ' - ' || COALESCE(v_designation, '') || ' » a une valeur de ' || NEW.valeur || ', supérieure au seuil maximum de ' || v_seuil_max || ' pour la période du ' || TO_CHAR(NEW.periode_debut, 'DD/MM/YYYY') || ' au ' || TO_CHAR(NEW.periode_fin, 'DD/MM/YYYY') || '.';
END IF;
IF NOT v_hors_seuil THEN RETURN NEW;
END IF;
SELECT id_priorite INTO v_priorite_id
FROM priorite
WHERE actif = TRUE
ORDER BY (
        CASE
            WHEN code = 'HAUTE' THEN 0
            ELSE 1
        END
    ),
    id_priorite
LIMIT 1;
IF v_priorite_id IS NULL THEN RAISE WARNING 'Aucune priorité active (indicateur %, valeur %)',
NEW.id_indicateur,
NEW.valeur;
RETURN NEW;
END IF;
FOR v_recipient IN
SELECT DISTINCT aa.id_utilisateur
FROM activite_indicateur ai
    JOIN affectation_activite aa ON aa.id_activite = ai.id_activite
    JOIN role r ON r.id_role = aa.id_role
WHERE ai.id_indicateur = NEW.id_indicateur
    AND aa.date_desaffectation IS NULL
    AND r.code IN ('RESPONSABLE', 'SUPPLEANT') LOOP
INSERT INTO notification (
        titre,
        message,
        id_priorite,
        id_utilisateur,
        id_reference,
        resource
    )
VALUES (
        v_titre,
        v_message,
        v_priorite_id,
        v_recipient.id_utilisateur,
        NEW.id_indicateur,
        'INDICATEUR'
    );
END LOOP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_notify_indicateur_seuil ON valeur_indicateur;
CREATE TRIGGER trg_notify_indicateur_seuil
AFTER
INSERT ON valeur_indicateur FOR EACH ROW EXECUTE FUNCTION notify_indicateur_seuil();
-- =============================================================================
-- 3. PLANIFICATION pg_cron — 05h30 Madagascar (02h30 UTC)
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS pg_cron;
SELECT cron.unschedule(jobid)
FROM cron.job
WHERE jobname IN (
        'notification-sous-activites',
        'notification-plans-action',
        'retour-en-cours-activites'
    );
-- 05h30 Madagascar = 02h30 UTC
SELECT cron.schedule(
        'notification-sous-activites',
        '30 2 * * *',
        $$SELECT verifier_notifications_sous_activites();
$$
);
-- 05h31 Madagascar = 02h31 UTC
SELECT cron.schedule(
        'notification-plans-action',
        '31 2 * * *',
        $$SELECT verifier_notifications_plans_action();
$$
);
-- 05h32 Madagascar = 02h32 UTC
SELECT cron.schedule(
        'retour-en-cours-activites',
        '32 2 * * *',
        $$SELECT verifier_retour_en_cours_activites();
$$
);
-- =============================================================================
-- 4. DÉMONSTRATION IMMÉDIATE (une seule fois, à l'installation)
-- =============================================================================
-- Insère une valeur 999 pour déclencher le trigger d'indicateur,
-- et appelle les 3 fonctions pour générer les notifications SA + plans,
-- sans attendre le cron de 05h30.
DO $$ BEGIN IF EXISTS (
    SELECT 1
    FROM indicateur
    WHERE code = 'TIND-01'
) THEN
INSERT INTO valeur_indicateur (
        valeur,
        periode_debut,
        periode_fin,
        id_utilisateur,
        id_indicateur
    )
SELECT 999,
    NOW() - INTERVAL '1 day',
    NOW(),
    u.id_utilisateur,
    i.id_indicateur
FROM utilisateur u,
    indicateur i
WHERE u.email = 'toky.andriambololona@example.mg'
    AND i.code = 'TIND-01';
RAISE NOTICE 'Démo trigger : valeur 999 insérée pour TIND-01 (2 notifications attendues).';
END IF;
END $$;
SELECT verifier_notifications_sous_activites();
SELECT verifier_notifications_plans_action();
SELECT verifier_retour_en_cours_activites();
-- =============================================================================
-- 5. VÉRIFICATIONS (résultats attendus ci-dessous)
-- =============================================================================
-- Attendu : 14 notifications
--   - 2 du trigger indicateur (valeur 999 → Toky + Haja)
--   - 1 SA échéance proche (TSA-A1-01)
--   - 1 SA démarrage retard (TSA-A1-02)
--   - 1 SA en retard (TSA-A1-03)
--   - 1 plan échéance proche (TPA-01)
--   - 1 plan en retard (TPA-02)
--   - 2 alertes hiérarchiques (Chef service + Chef département)
--   - 1 plan démarrage retard (TPA-03)
--   + 2 (valeur 5 + 150 chargées par data.sql n'ont PAS déclenché car trigger absent)
--   - 1 ligne EN_RETARD ajoutée pour TSA-A1-03 et TPA-02 (dans avancement)
SELECT titre,
    resource,
    id_reference,
    LEFT(message, 60) || '…' AS apercu
FROM notification
WHERE date_creation > NOW() - INTERVAL '10 minutes'
ORDER BY date_creation DESC;