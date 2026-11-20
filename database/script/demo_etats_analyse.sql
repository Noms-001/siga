-- =============================================================================
-- SAGA — Jeu de données fictives cohérent pour les états d'analyse/statistiques
--
-- À exécuter APRÈS schema.sql, data.sql et migration_code_generation.sql.
-- Peut être exécuté avant ou après trigger.sql : les valeurs d'indicateurs
-- restent dans leurs seuils pour éviter les alertes supplémentaires.
--
-- Données explicitement fictives, millésime courant, préfixes DEMO-.
-- Script réexécutable : il ajoute les données DEMO manquantes et renouvelle
-- uniquement les relevés d'indicateurs explicitement marqués comme DEMO.
-- Il ne modifie aucune donnée de production.
--
-- Contenu : 48 activités (PTA et courantes), 144 sous-activités avec
-- historique, livrables et affectations ; 18 plans d'action rattachés à des
-- origines et à des services ; 6 indicateurs rattachés chacun à plusieurs
-- activités, avec leurs séries mensuelles et les cas retard / jamais saisi.
-- Le schéma ne contient pas de table signalement : aucun signalement ne peut
-- être créé par ce script.
-- =============================================================================

BEGIN;
SET LOCAL TIME ZONE 'Indian/Antananarivo';

-- -----------------------------------------------------------------------------
-- 1. Référentiels de démonstration manquants du data.sql d'origine
-- -----------------------------------------------------------------------------
INSERT INTO site (nom, actif)
SELECT v.nom, TRUE
FROM (VALUES
    ('DEMO — Siège Antaninarenina'),
    ('DEMO — Centre technique Ankorondrano'),
    ('DEMO — Agence Toamasina'),
    ('DEMO — Agence Mahajanga')
) AS v(nom)
WHERE NOT EXISTS (
    SELECT 1 FROM site s WHERE LOWER(s.nom) = LOWER(v.nom)
);

INSERT INTO objectif_specifique (code, designation, annee)
SELECT v.code,
       v.designation,
       EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
FROM (VALUES
    ('DEMO-OS26-01', 'Améliorer la disponibilité des dispositifs de sécurité sur les sites prioritaires.'),
    ('DEMO-OS26-02', 'Réduire les risques opérationnels par des contrôles périodiques documentés.'),
    ('DEMO-OS26-03', 'Renforcer la conformité des procédures et la traçabilité des interventions.'),
    ('DEMO-OS26-04', 'Améliorer la prévention et la réponse aux incidents de sécurité.')
) AS v(code, designation)
WHERE NOT EXISTS (
    SELECT 1
    FROM objectif_specifique os
    WHERE os.code = v.code
      AND os.annee = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
);

-- -----------------------------------------------------------------------------
-- 2. Activités fictives réparties sur une fenêtre récente de ±20 jours,
--    avec deux services et plusieurs statuts, types, sites, priorités et objectifs.
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE demo_activite_seed ON COMMIT DROP AS
WITH numeros AS (
    SELECT n::INTEGER AS numero,
           ((n - 1) / 4 + 1)::INTEGER AS mois,
           ((n - 1) % 4 + 1)::INTEGER AS position_mois
    FROM generate_series(1, 48) AS gs(n)
), attributs AS (
    SELECT n.*,
           CASE WHEN n.numero % 2 = 1
                THEN 'Gestion des Risques et Procédures'
                ELSE 'Gestion des Systèmes de Sécurité'
           END AS service_nom,
           CASE
               WHEN EXTRACT(MONTH FROM CURRENT_DATE)::INTEGER < n.mois
                   THEN 'NON_COMMENCEE'
               WHEN n.position_mois = 1 THEN 'TERMINEE'
               WHEN n.position_mois = 2 THEN 'EN_COURS'
               WHEN n.position_mois = 3 THEN 'NON_COMMENCEE'
               WHEN n.mois % 4 = 0 THEN 'SUSPENDUE'
               WHEN n.mois % 4 = 1 THEN 'REPORTEE'
               WHEN n.mois % 4 = 2 THEN 'ANNULEE'
               ELSE 'EN_ATTENTE_VALIDATION'
           END AS statut_code,
           CASE
               WHEN n.position_mois = 4 THEN NULL
               ELSE 'DEMO-OS26-' || LPAD((((n.numero - 1) % 4) + 1)::TEXT, 2, '0')
           END AS objectif_code,
           CASE
               WHEN n.numero % 4 = 1 THEN 'DEMO — Siège Antaninarenina'
               WHEN n.numero % 4 = 2 THEN 'DEMO — Centre technique Ankorondrano'
               WHEN n.numero % 4 = 3 THEN 'DEMO — Agence Toamasina'
               ELSE 'DEMO — Agence Mahajanga'
           END AS site_nom,
           CASE
               WHEN n.numero % 2 = 1 THEN
                   CASE n.numero % 5
                       WHEN 0 THEN 'Inspection'
                       WHEN 1 THEN 'Étude'
                       WHEN 2 THEN 'Procédure'
                       WHEN 3 THEN 'Inventaire'
                       ELSE 'Amélioration'
                   END
               ELSE
                   CASE n.numero % 5
                       WHEN 0 THEN 'Installation'
                       WHEN 1 THEN 'Maintenance préventive'
                       WHEN 2 THEN 'Maintenance curative'
                       WHEN 3 THEN 'Exercice de sécurité'
                       ELSE 'Intervention urgente'
                   END
           END AS type_nom,
           CASE
               WHEN n.position_mois = 2 AND n.numero % 8 = 0 THEN 'CRITIQUE'
               WHEN n.position_mois = 2 OR n.numero % 7 = 0 THEN 'HAUTE'
               WHEN n.numero % 3 = 0 THEN 'NORMALE'
               ELSE 'FAIBLE'
           END AS priorite_code
    FROM numeros n
), dates AS (
    SELECT a.*,
           CASE a.numero
               WHEN 42 THEN CURRENT_DATE - 4
               WHEN 43 THEN CURRENT_DATE - 1
               ELSE CURRENT_DATE - 20 + ((a.numero - 1) * 33 / 47)
           END AS debut_prevu,
           CASE a.numero
               WHEN 42 THEN CURRENT_DATE + 3
               WHEN 43 THEN CURRENT_DATE + 6
               ELSE CURRENT_DATE - 20 + ((a.numero - 1) * 33 / 47) + 7
           END AS fin_prevue
    FROM attributs a
)
SELECT d.numero,
       'DEMO-A-' || LPAD(d.numero::TEXT, 3, '0') AS code,
       CASE
           WHEN d.position_mois = 1 THEN 'Contrôle périodique des dispositifs de sécurité — ' || TO_CHAR(d.debut_prevu, 'TMMonth')
           WHEN d.position_mois = 2 THEN 'Maintenance planifiée des équipements — ' || TO_CHAR(d.debut_prevu, 'TMMonth')
           WHEN d.position_mois = 3 THEN 'Mise à jour des consignes opérationnelles — ' || TO_CHAR(d.debut_prevu, 'TMMonth')
           ELSE 'Revue des risques et actions correctives — ' || TO_CHAR(d.debut_prevu, 'TMMonth')
       END AS designation,
       CASE WHEN d.objectif_code IS NULL THEN NULL
            ELSE 'DEMO-REF-' || LPAD(d.numero::TEXT, 3, '0')
       END AS reference,
       d.debut_prevu,
       d.fin_prevue,
       CASE WHEN d.statut_code IN ('EN_COURS', 'TERMINEE', 'SUSPENDUE')
            THEN LEAST(d.debut_prevu + 2, CURRENT_DATE)
            ELSE NULL
       END AS debut_reel,
       CASE WHEN d.statut_code = 'TERMINEE'
            THEN LEAST(d.fin_prevue, CURRENT_DATE)
            ELSE NULL
       END AS fin_reelle,
       d.service_nom,
       d.objectif_code,
       d.site_nom,
       d.type_nom,
       d.priorite_code,
       CASE WHEN d.objectif_code IS NULL
            THEN 'Procédure de validation des activités courantes TSS'
            ELSE 'Procédure de validation des activités PTA TSS'
       END AS procedure_nom,
       d.statut_code,
       d.mois,
       d.position_mois
FROM dates d;

INSERT INTO activite (
    code, reference, designation,
    date_debut_prevue, date_fin_prevue, date_debut_reelle, date_fin_reelle,
    id_objectif_specifique, id_type_activite, id_site, id_priorite,
    id_service, id_procedure
)
SELECT d.code,
       d.reference,
       d.designation,
       d.debut_prevu,
       d.fin_prevue,
       d.debut_reel,
       d.fin_reelle,
       os.id_objectif_specifique,
       ta.id_type_activite,
       si.id_site,
       pr.id_priorite,
       se.id_service,
       p.id_procedure
FROM demo_activite_seed d
JOIN service se ON se.nom = d.service_nom
LEFT JOIN objectif_specifique os
       ON os.code = d.objectif_code
      AND os.annee = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
JOIN type_activite ta ON ta.designation = d.type_nom
JOIN site si ON si.nom = d.site_nom
JOIN priorite pr ON pr.code = d.priorite_code
JOIN "procedure" p ON p.designation = d.procedure_nom
WHERE NOT EXISTS (
    SELECT 1 FROM activite a
    WHERE a.id_service = se.id_service
      AND LOWER(a.code) = LOWER(d.code)
);

-- Recalibre uniquement les dates des activités DEMO déjà présentes lors d'une
-- réexécution. Aucune activité hors préfixe DEMO n'est modifiée.
UPDATE activite a
SET date_debut_prevue = d.debut_prevu,
    date_fin_prevue = d.fin_prevue,
    date_debut_reelle = d.debut_reel,
    date_fin_reelle = d.fin_reelle
FROM demo_activite_seed d
WHERE LOWER(a.code) = LOWER(d.code);

CREATE TEMP TABLE demo_activite ON COMMIT DROP AS
SELECT d.*,
       a.id_activite,
       se.id_service,
       CASE
           WHEN d.service_nom = 'Gestion des Risques et Procédures' THEN
               (SELECT u.id_utilisateur
                FROM utilisateur u
                WHERE u.email = CASE WHEN d.numero % 3 = 0
                                     THEN 'hery.randrianarisoa@example.mg'
                                     ELSE 'miora.ravelomanantsoa@example.mg' END)
           ELSE
               (SELECT u.id_utilisateur
                FROM utilisateur u
                WHERE u.email = CASE d.numero % 4
                    WHEN 0 THEN 'haja.randriamampianina@example.mg'
                    WHEN 1 THEN 'toky.andriambololona@example.mg'
                    WHEN 2 THEN 'nantenaina.rakotomalala@example.mg'
                    ELSE 'feno.razanadrakoto@example.mg' END)
       END AS responsable_id,
       CASE
           WHEN d.service_nom = 'Gestion des Risques et Procédures' THEN
               (SELECT u.id_utilisateur
                FROM utilisateur u
                WHERE u.email = CASE WHEN d.numero % 2 = 0
                                     THEN 'fara.razafindrakoto@example.mg'
                                     ELSE 'hery.randrianarisoa@example.mg' END)
           ELSE
               (SELECT u.id_utilisateur
                FROM utilisateur u
                WHERE u.email = CASE WHEN d.numero % 2 = 0
                                     THEN 'solofo.andrianjafy@example.mg'
                                     ELSE 'voahirana.rakotonirina@example.mg' END)
       END AS participant_id
FROM demo_activite_seed d
JOIN service se ON se.nom = d.service_nom
JOIN activite a ON a.id_service = se.id_service
                AND LOWER(a.code) = LOWER(d.code);

-- Deux événements par activité pour que les historiques aient un sens à la
-- date sélectionnée, sans laisser les activités DEMO dans un état sans statut.
INSERT INTO historique_activite (
    commentaire, date_changement, id_utilisateur, id_statut, id_activite
)
SELECT 'DEMO_ETATS_INIT',
    (LEAST(d.debut_prevu - 2, CURRENT_DATE - 1)::TIMESTAMP + INTERVAL '9 hours'),
       d.responsable_id,
       st.id_statut,
       d.id_activite
FROM demo_activite d
JOIN statut st ON st.code = 'NON_COMMENCEE'
WHERE NOT EXISTS (
    SELECT 1 FROM historique_activite h
    WHERE h.id_activite = d.id_activite AND h.commentaire = 'DEMO_ETATS_INIT'
);

INSERT INTO historique_activite (
    commentaire, date_changement, id_utilisateur, id_statut, id_activite
)
SELECT 'DEMO_ETATS_STATUT_FINAL',
    (LEAST(d.debut_prevu + 12, CURRENT_DATE)::TIMESTAMP + INTERVAL '18 hours'),
       d.responsable_id,
       st.id_statut,
       d.id_activite
FROM demo_activite d
JOIN statut st ON st.code = d.statut_code
WHERE NOT EXISTS (
    SELECT 1 FROM historique_activite h
    WHERE h.id_activite = d.id_activite AND h.commentaire = 'DEMO_ETATS_STATUT_FINAL'
);

INSERT INTO affectation_activite (date_affectation, id_role, id_activite, id_utilisateur)
SELECT (d.debut_prevu::TIMESTAMP - INTERVAL '1 day'), r.id_role, d.id_activite,
       CASE WHEN r.code = 'RESPONSABLE' THEN d.responsable_id ELSE d.participant_id END
FROM demo_activite d
CROSS JOIN role r
WHERE r.code IN ('RESPONSABLE', 'PARTICIPANT')
  AND NOT EXISTS (
      SELECT 1 FROM affectation_activite aa
      WHERE aa.id_activite = d.id_activite
        AND aa.id_role = r.id_role
        AND aa.id_utilisateur = CASE WHEN r.code = 'RESPONSABLE'
                                     THEN d.responsable_id ELSE d.participant_id END
        AND aa.date_desaffectation IS NULL
  );

INSERT INTO resultat_intermediaire (designation, id_activite)
SELECT 'Rapport de contrôle trimestriel validé et archivé.', d.id_activite
FROM demo_activite d
WHERE NOT EXISTS (
    SELECT 1 FROM resultat_intermediaire ri
    WHERE ri.id_activite = d.id_activite
      AND ri.designation = 'Rapport de contrôle trimestriel validé et archivé.'
);

-- -----------------------------------------------------------------------------
-- 3. Sous-activités, affectations, livrables et historiques d'avancement
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE demo_sous_activite_seed ON COMMIT DROP AS
SELECT d.numero,
    k.position::INTEGER AS position_sa,
       d.id_activite,
       'DEMO-SA-' || LPAD(d.numero::TEXT, 3, '0') || '-' || k.position AS code,
       CASE k.position
           WHEN 1 THEN 'Préparation et vérification des équipements'
           WHEN 2 THEN 'Intervention sur site et contrôle fonctionnel'
           ELSE 'Compte rendu, validation et archivage'
       END AS designation,
       d.debut_prevu AS date_debut_prevue,
       d.fin_prevue AS date_fin_prevue,
       CASE d.statut_code
           WHEN 'TERMINEE' THEN 100
           WHEN 'EN_COURS' THEN 40 + (k.position::INTEGER * 15)
           WHEN 'SUSPENDUE' THEN 10 + (k.position::INTEGER * 10)
           WHEN 'REPORTEE' THEN k.position::INTEGER * 5
           ELSE 0
       END::NUMERIC(5,2) AS avancement_final,
       d.responsable_id,
       d.participant_id,
       d.statut_code AS statut_activite
FROM demo_activite d
CROSS JOIN generate_series(1, 3) AS k(position);

INSERT INTO sous_activite (
    code, designation, date_debut_prevue, date_fin_prevue,
    date_debut_reelle, date_fin_reelle, id_activite
)
SELECT s.code,
       s.designation,
       s.date_debut_prevue,
       s.date_fin_prevue,
       CASE WHEN s.avancement_final > 0
            THEN LEAST(s.date_debut_prevue + 1, CURRENT_DATE)
            ELSE NULL
       END,
       CASE WHEN s.avancement_final = 100
            THEN LEAST(s.date_fin_prevue, CURRENT_DATE)
            ELSE NULL
       END,
       s.id_activite
FROM demo_sous_activite_seed s
WHERE NOT EXISTS (
    SELECT 1 FROM sous_activite sa
    WHERE sa.id_activite = s.id_activite
      AND LOWER(sa.code) = LOWER(s.code)
);

CREATE TEMP TABLE demo_sous_activite ON COMMIT DROP AS
SELECT seed.*,
       sa.id_sous_activite
FROM demo_sous_activite_seed seed
JOIN sous_activite sa
  ON sa.id_activite = seed.id_activite
 AND LOWER(sa.code) = LOWER(seed.code);

INSERT INTO affectation_sous_activite (date_affectation, id_role, id_sous_activite, id_utilisateur)
SELECT (sa.date_debut_prevue::TIMESTAMP - INTERVAL '1 day'), r.id_role,
       sa.id_sous_activite,
       CASE WHEN r.code = 'RESPONSABLE' THEN sa.responsable_id ELSE sa.participant_id END
FROM demo_sous_activite sa
CROSS JOIN role r
WHERE r.code IN ('RESPONSABLE', 'PARTICIPANT')
  AND NOT EXISTS (
      SELECT 1 FROM affectation_sous_activite asa
      WHERE asa.id_sous_activite = sa.id_sous_activite
        AND asa.id_role = r.id_role
        AND asa.id_utilisateur = CASE WHEN r.code = 'RESPONSABLE'
                                      THEN sa.responsable_id ELSE sa.participant_id END
        AND asa.date_desaffectation IS NULL
  );

INSERT INTO livrable_sous_activite (designation, description, id_sous_activite)
SELECT CASE sa.position_sa
           WHEN 1 THEN 'Fiche de préparation et liste de contrôle'
           WHEN 2 THEN 'Procès-verbal de vérification sur site'
           ELSE 'Rapport de clôture et pièces justificatives'
       END,
       'Document fictif de démonstration — ' || sa.designation || '.',
       sa.id_sous_activite
FROM demo_sous_activite sa
WHERE NOT EXISTS (
    SELECT 1 FROM livrable_sous_activite l
    WHERE l.id_sous_activite = sa.id_sous_activite
    AND l.designation = CASE sa.position_sa
           WHEN 1 THEN 'Fiche de préparation et liste de contrôle'
           WHEN 2 THEN 'Procès-verbal de vérification sur site'
           ELSE 'Rapport de clôture et pièces justificatives'
      END
);

-- Deux relevés par sous-activité : une valeur intermédiaire puis la valeur
-- observée à la fin de la séquence. Les valeurs sont comprises entre 0 et 100.
INSERT INTO avancement_sous_activite (
    valeur_pourcentage, commentaire, date_changement,
    id_utilisateur, id_sous_activite, id_statut
)
SELECT CASE WHEN x.etape = 1
            THEN GREATEST(0, sa.avancement_final - 20)
            ELSE sa.avancement_final
       END,
       CASE WHEN x.etape = 1
            THEN 'DEMO_ETATS_AVANCEMENT_INTERMEDIAIRE'
            ELSE 'DEMO_ETATS_AVANCEMENT_FINAL'
       END,
       (LEAST(sa.date_debut_prevue + CASE WHEN x.etape = 1 THEN 5 ELSE 20 END,
              CURRENT_DATE)::TIMESTAMP
        + CASE WHEN x.etape = 1 THEN INTERVAL '10 hours' ELSE INTERVAL '17 hours' END),
       sa.responsable_id,
       sa.id_sous_activite,
       st.id_statut
FROM demo_sous_activite sa
CROSS JOIN (VALUES (1), (2)) AS x(etape)
JOIN statut st ON st.code = CASE
    WHEN (CASE WHEN x.etape = 1
               THEN GREATEST(0, sa.avancement_final - 20)
               ELSE sa.avancement_final END) = 100 THEN 'TERMINEE'
    WHEN (CASE WHEN x.etape = 1
               THEN GREATEST(0, sa.avancement_final - 20)
               ELSE sa.avancement_final END) = 0 THEN 'NON_COMMENCEE'
    WHEN sa.date_fin_prevue < CURRENT_DATE THEN 'EN_RETARD'
    ELSE 'EN_COURS'
END
WHERE NOT EXISTS (
    SELECT 1 FROM avancement_sous_activite av
    WHERE av.id_sous_activite = sa.id_sous_activite
      AND av.commentaire = CASE WHEN x.etape = 1
            THEN 'DEMO_ETATS_AVANCEMENT_INTERMEDIAIRE'
            ELSE 'DEMO_ETATS_AVANCEMENT_FINAL' END
);

-- -----------------------------------------------------------------------------
-- 4. Origines et plans d'action rattachés à des activités/services
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE demo_origine_seed ON COMMIT DROP AS
SELECT v.numero::INTEGER AS numero,
       v.code,
       v.designation,
       v.type_origine,
       v.activite_numero::INTEGER AS activite_numero
FROM (VALUES
    (1, 'DEMO-ORI-INC-01', 'Incident — indisponibilité du contrôle d’accès', 'INCIDENT', 1),
    (2, 'DEMO-ORI-INC-02', 'Incident — alarme de zone technique', 'INCIDENT', 3),
    (3, 'DEMO-ORI-RSK-01', 'Risque — couverture de vidéosurveillance', 'RISQUE', 2),
    (4, 'DEMO-ORI-RSK-02', 'Risque — obsolescence des équipements', 'RISQUE', 4),
    (5, 'DEMO-ORI-AUT-01', 'Audit interne — contrôle périodique', 'AUTRE', 5),
    (6, 'DEMO-ORI-AUT-02', 'Recommandation — mise à jour des consignes', 'AUTRE', 6)
) AS v(numero, code, designation, type_origine, activite_numero);

INSERT INTO origine (code, designation, type_origine, description, id_utilisateur)
SELECT o.code,
       o.designation,
       o.type_origine,
       'Origine fictive créée pour les états de démonstration SAGA.',
       u.id_utilisateur
FROM demo_origine_seed o
JOIN utilisateur u ON u.email = 'andry.rakotoarisoa@example.mg'
WHERE NOT EXISTS (SELECT 1 FROM origine old WHERE old.code = o.code);

CREATE TEMP TABLE demo_origine ON COMMIT DROP AS
SELECT seed.*, o.id_origine
FROM demo_origine_seed seed
JOIN origine o ON o.code = seed.code;

INSERT INTO activite_origine (id_activite, id_origine)
SELECT a.id_activite, o.id_origine
FROM demo_origine o
JOIN demo_activite a ON a.numero = o.activite_numero
ON CONFLICT (id_activite, id_origine) DO NOTHING;

CREATE TEMP TABLE demo_plan_seed ON COMMIT DROP AS
WITH numeros AS (
    SELECT n::INTEGER AS numero,
            -20 + ((n - 1) * 32 / 17)::INTEGER AS offset_jour,
           ((n - 1) % 3 + 1)::INTEGER AS position_cycle,
           ((n - 1) % 6 + 1)::INTEGER AS origine_numero
    FROM generate_series(1, 18) AS gs(n)
)
SELECT n.numero,
       'DEMO-PA-' || LPAD(n.numero::TEXT, 3, '0') AS code,
       CASE n.position_cycle
           WHEN 1 THEN 'Remise à niveau du dispositif — lot ' || LPAD(n.numero::TEXT, 2, '0')
           WHEN 2 THEN 'Correction prioritaire du risque — lot ' || LPAD(n.numero::TEXT, 2, '0')
           ELSE 'Contrôle de conformité et clôture — lot ' || LPAD(n.numero::TEXT, 2, '0')
       END AS designation,
       CASE
           WHEN n.numero IN (2, 3, 5, 6, 8, 9, 12, 14, 17, 18)
               THEN CURRENT_DATE - ((n.numero % 5) + 1) - 8
           WHEN n.numero = 11 THEN CURRENT_DATE - 4
           WHEN n.numero = 15 THEN CURRENT_DATE + 2
           ELSE CURRENT_DATE + n.offset_jour
       END AS debut_prevu,
       CASE
           WHEN n.numero IN (2, 3, 5, 6, 8, 9, 12, 14, 17, 18)
               THEN CURRENT_DATE - ((n.numero % 5) + 1)
           WHEN n.numero = 11 THEN CURRENT_DATE + 4
           WHEN n.numero = 15 THEN CURRENT_DATE + 10
           ELSE CURRENT_DATE + n.offset_jour + 8
       END AS fin_prevue,
       CASE
           WHEN CURRENT_DATE + n.offset_jour > CURRENT_DATE + 20 THEN 'NON_COMMENCEE'
           WHEN n.position_cycle = 1 THEN 'TERMINEE'
           WHEN n.position_cycle = 2 THEN 'EN_COURS'
           ELSE 'NON_COMMENCEE'
       END AS statut_code,
       CASE
           WHEN CURRENT_DATE + n.offset_jour > CURRENT_DATE + 20 THEN 0
           WHEN n.position_cycle = 1 THEN 100
           WHEN n.position_cycle = 2 THEN 35 + (n.numero % 5) * 10
           ELSE 0
       END::NUMERIC(5,2) AS avancement_final,
       n.origine_numero,
       a.id_service,
       a.responsable_id
FROM numeros n
JOIN demo_origine o ON o.numero = n.origine_numero
JOIN demo_activite a ON a.numero = o.activite_numero;

INSERT INTO plan_action (
    code, designation, date_debut_prevue, date_fin_prevue,
    date_debut_reelle, date_fin_reelle
)
SELECT p.code,
       p.designation,
       p.debut_prevu,
       p.fin_prevue,
       CASE WHEN p.avancement_final > 0
            THEN LEAST(p.debut_prevu + 1, CURRENT_DATE)
            ELSE NULL END,
       CASE WHEN p.avancement_final = 100
            THEN LEAST(p.fin_prevue, CURRENT_DATE)
            ELSE NULL END
FROM demo_plan_seed p
WHERE NOT EXISTS (SELECT 1 FROM plan_action old WHERE old.code = p.code);

-- Recalibre uniquement les dates des plans DEMO déjà présents.
UPDATE plan_action pa
SET date_debut_prevue = p.debut_prevu,
    date_fin_prevue = p.fin_prevue,
    date_debut_reelle = CASE WHEN p.avancement_final > 0
                             THEN LEAST(p.debut_prevu + 1, CURRENT_DATE)
                             ELSE NULL END,
    date_fin_reelle = CASE WHEN p.avancement_final = 100
                           THEN LEAST(p.fin_prevue, CURRENT_DATE)
                           ELSE NULL END
FROM demo_plan_seed p
WHERE pa.code = p.code;

CREATE TEMP TABLE demo_plan ON COMMIT DROP AS
SELECT seed.*, pa.id_plan_action, o.id_origine
FROM demo_plan_seed seed
JOIN plan_action pa ON pa.code = seed.code
JOIN demo_origine o ON o.numero = seed.origine_numero;

INSERT INTO plan_action_origine (id_plan_action, id_origine, est_principale)
SELECT p.id_plan_action, p.id_origine, TRUE
FROM demo_plan p
ON CONFLICT (id_plan_action, id_origine) DO UPDATE
SET est_principale = TRUE;

INSERT INTO affectation_plan_action (
    date_affectation, date_desaffectation,
    id_plan_action, id_utilisateur, id_role
)
SELECT (p.debut_prevu::TIMESTAMP - INTERVAL '1 day'),
       NULL,
       p.id_plan_action,
       p.responsable_id,
       r.id_role
FROM demo_plan p
JOIN role r ON r.code = 'RESPONSABLE'
WHERE NOT EXISTS (
    SELECT 1 FROM affectation_plan_action aff
    WHERE aff.id_plan_action = p.id_plan_action
      AND aff.id_role = r.id_role
      AND aff.id_utilisateur = p.responsable_id
      AND aff.date_desaffectation IS NULL
);

INSERT INTO avancement_plan_action (
    valeur_pourcentage, commentaire, date_changement,
    id_utilisateur, id_statut, id_plan_action
)
SELECT CASE WHEN x.etape = 1
            THEN GREATEST(0, p.avancement_final - 20)
            ELSE p.avancement_final END,
       CASE WHEN x.etape = 1
            THEN 'DEMO_ETATS_PLAN_INTERMEDIAIRE'
            ELSE 'DEMO_ETATS_PLAN_FINAL' END,
       (LEAST(p.debut_prevu + CASE WHEN x.etape = 1 THEN 5 ELSE 20 END,
              CURRENT_DATE)::TIMESTAMP
        + CASE WHEN x.etape = 1 THEN INTERVAL '10 hours' ELSE INTERVAL '17 hours' END),
       p.responsable_id,
       st.id_statut,
       p.id_plan_action
FROM demo_plan p
CROSS JOIN (VALUES (1), (2)) AS x(etape)
JOIN statut st ON st.code = CASE
    WHEN (CASE WHEN x.etape = 1
               THEN GREATEST(0, p.avancement_final - 20)
               ELSE p.avancement_final END) = 100 THEN 'TERMINEE'
    WHEN (CASE WHEN x.etape = 1
               THEN GREATEST(0, p.avancement_final - 20)
               ELSE p.avancement_final END) = 0 THEN 'NON_COMMENCEE'
    ELSE 'EN_COURS'
END
WHERE NOT EXISTS (
    SELECT 1 FROM avancement_plan_action ap
    WHERE ap.id_plan_action = p.id_plan_action
      AND ap.commentaire = CASE WHEN x.etape = 1
            THEN 'DEMO_ETATS_PLAN_INTERMEDIAIRE'
            ELSE 'DEMO_ETATS_PLAN_FINAL' END
);

-- -----------------------------------------------------------------------------
-- 5. KPI/KRI, valeurs mensuelles, cas en retard et cas jamais saisi
-- -----------------------------------------------------------------------------
INSERT INTO indicateur (
    code, designation, type_indicateur, unite_mesure,
    valeur_cible, seuil_min, seuil_max,
    frequence_verification, frequence_aggregation,
    definition, methode_determination, objectif, actif
)
SELECT v.code, v.designation, v.type_indicateur, v.unite_mesure,
       v.valeur_cible, v.seuil_min, v.seuil_max,
       v.frequence_verification, v.frequence_aggregation,
       v.definition, v.methode_determination, v.objectif, TRUE
FROM (VALUES
    ('DEMO-KPI-01', 'Disponibilité des systèmes de sécurité', 'KPI', '%', 90::NUMERIC, 60::NUMERIC, 100::NUMERIC, 'MENSUEL', 'MENSUELLE', 'Part des équipements disponibles pendant la période.', 'Équipements disponibles / équipements suivis × 100.', 'Maintenir une disponibilité élevée.'),
    ('DEMO-KPI-02', 'Taux de contrôles périodiques réalisés', 'KPI', '%', 95::NUMERIC, 50::NUMERIC, 100::NUMERIC, 'MENSUEL', 'MENSUELLE', 'Part des contrôles réalisés parmi ceux programmés.', 'Contrôles réalisés / contrôles programmés × 100.', 'Améliorer la couverture des contrôles.'),
    ('DEMO-KRI-01', 'Nombre d’incidents de sécurité', 'KRI', 'incident(s)', 10::NUMERIC, 0::NUMERIC, 15::NUMERIC, 'MENSUEL', 'MENSUELLE', 'Nombre d’incidents déclarés durant le mois.', 'Comptage des incidents qualifiés.', 'Réduire le nombre d’incidents.'),
    ('DEMO-KRI-02', 'Équipements critiques indisponibles', 'KRI', 'équipement(s)', 5::NUMERIC, 0::NUMERIC, 20::NUMERIC, 'MENSUEL', 'MENSUELLE', 'Nombre d’équipements critiques indisponibles.', 'Comptage des équipements hors service.', 'Réduire les indisponibilités critiques.'),
    ('DEMO-KPI-RETARD', 'Taux de recommandations clôturées', 'KPI', '%', 90::NUMERIC, 40::NUMERIC, 100::NUMERIC, 'MENSUEL', 'MENSUELLE', 'Taux de clôture des recommandations d’audit.', 'Recommandations clôturées / recommandations ouvertes × 100.', 'Clôturer les recommandations dans les délais.'),
    ('DEMO-KRI-VIDE', 'Alertes de sécurité non qualifiées', 'KRI', 'alerte(s)', 3::NUMERIC, 0::NUMERIC, 20::NUMERIC, 'TRIMESTRIEL', 'TRIMESTRIELLE', 'Volume d’alertes restant à qualifier.', 'Comptage des alertes non qualifiées.', 'Limiter les alertes non qualifiées.')
) AS v(code, designation, type_indicateur, unite_mesure,
       valeur_cible, seuil_min, seuil_max, frequence_verification,
       frequence_aggregation, definition, methode_determination, objectif)
WHERE NOT EXISTS (SELECT 1 FROM indicateur old WHERE old.code = v.code);

-- Répare uniquement les deux fréquences des indicateurs DEMO éventuellement
-- créés par une version antérieure du script. Les autres champs et valeurs
-- historiques de l'indicateur ne sont pas modifiés.
UPDATE indicateur
SET frequence_verification = CASE
    WHEN code = 'DEMO-KRI-VIDE' THEN 'TRIMESTRIEL'
    ELSE 'MENSUEL'
    END,
    frequence_aggregation = CASE
        WHEN code = 'DEMO-KRI-VIDE' THEN 'TRIMESTRIELLE'
        ELSE 'MENSUELLE'
    END
WHERE code IN (
    'DEMO-KPI-01', 'DEMO-KPI-02', 'DEMO-KRI-01',
    'DEMO-KRI-02', 'DEMO-KPI-RETARD', 'DEMO-KRI-VIDE'
);

CREATE TEMP TABLE demo_indicateur ON COMMIT DROP AS
SELECT i.id_indicateur, i.code
FROM indicateur i
WHERE i.code IN (
    'DEMO-KPI-01', 'DEMO-KPI-02', 'DEMO-KRI-01',
    'DEMO-KRI-02', 'DEMO-KPI-RETARD', 'DEMO-KRI-VIDE'
);

-- Remplace uniquement les relevés générés par l'ancienne version de ce jeu
-- DEMO, afin de ne pas conserver les fausses périodes quotidiennes sous une
-- fréquence de vérification mensuelle.
DELETE FROM valeur_indicateur v
USING demo_indicateur i
WHERE v.id_indicateur = i.id_indicateur
  AND (
      v.commentaire LIKE 'Donnée fictive DEMO — relevé récent %'
      OR v.commentaire LIKE 'Donnée fictive DEMO — relevé mensuel%'
      OR v.commentaire = 'Donnée fictive DEMO — mesure volontairement ancienne dans la fenêtre récente'
  );

-- Rattachement plusieurs-à-plusieurs : chaque indicateur est lié à des
-- activités réellement cohérentes avec sa nature, et parfois à plusieurs
-- activités, pour illustrer qu'une valeur est partagée par toutes les
-- activités liées. L'activité marquée est_auteur fournit l'auteur des saisies.
CREATE TEMP TABLE demo_indicateur_activite ON COMMIT DROP AS
SELECT v.code_indicateur, v.activite_numero, v.est_auteur
FROM (VALUES
    ('DEMO-KPI-01', 2, TRUE),
    ('DEMO-KPI-01', 6, FALSE),
    ('DEMO-KPI-01', 42, FALSE),
    ('DEMO-KPI-01', 46, FALSE),
    ('DEMO-KPI-02', 5, TRUE),
    ('DEMO-KPI-02', 13, FALSE),
    ('DEMO-KPI-02', 25, FALSE),
    ('DEMO-KRI-01', 4, TRUE),
    ('DEMO-KRI-01', 24, FALSE),
    ('DEMO-KRI-01', 44, FALSE),
    ('DEMO-KRI-02', 6, TRUE),
    ('DEMO-KRI-02', 2, FALSE),
    ('DEMO-KRI-02', 42, FALSE),
    ('DEMO-KPI-RETARD', 7, TRUE),
    ('DEMO-KPI-RETARD', 27, FALSE),
    ('DEMO-KPI-RETARD', 47, FALSE),
    ('DEMO-KRI-VIDE', 8, TRUE),
    ('DEMO-KRI-VIDE', 28, FALSE),
    ('DEMO-KRI-VIDE', 48, FALSE)
) AS v(code_indicateur, activite_numero, est_auteur);

-- Réconcilie uniquement les liaisons entre indicateurs DEMO et activités DEMO.
-- Les liaisons éventuelles vers des activités réelles sont conservées.
DELETE FROM activite_indicateur ai
USING demo_indicateur i, activite a
WHERE ai.id_indicateur = i.id_indicateur
  AND ai.id_activite = a.id_activite
  AND a.code LIKE 'DEMO-A-%';

INSERT INTO activite_indicateur (id_activite, id_indicateur)
SELECT a.id_activite, i.id_indicateur
FROM demo_indicateur_activite m
JOIN demo_indicateur i ON i.code = m.code_indicateur
JOIN demo_activite a ON a.numero = m.activite_numero
ON CONFLICT (id_activite, id_indicateur) DO NOTHING;

-- Trois relevés mensuels complets (août à octobre de l'année courante).
-- Les valeurs sont choisies pour illustrer l'atteinte/la non-atteinte KPI/KRI
-- tout en respectant les fréquences configurées dans le référentiel.
INSERT INTO valeur_indicateur (
    valeur, periode_debut, periode_fin, commentaire,
    date_saisie, id_utilisateur, id_indicateur
)
SELECT CASE i.code
           WHEN 'DEMO-KPI-01' THEN 92 + ((d.mois + 3) * 3)
           WHEN 'DEMO-KPI-02' THEN 82 + ((d.mois + 3) * 4)
           WHEN 'DEMO-KRI-01' THEN 13 - ((d.mois + 3) * 2)
           WHEN 'DEMO-KRI-02' THEN 8 - ((d.mois + 3) * 2)
       END::NUMERIC(15,2),
       (DATE_TRUNC('month', CURRENT_DATE) + (d.mois * INTERVAL '1 month'))::TIMESTAMP,
       (DATE_TRUNC('month', CURRENT_DATE) + ((d.mois + 1) * INTERVAL '1 month')
           - INTERVAL '1 second')::TIMESTAMP,
       'Donnée fictive DEMO — relevé mensuel ' ||
           TO_CHAR(DATE_TRUNC('month', CURRENT_DATE) + (d.mois * INTERVAL '1 month'), 'YYYY-MM'),
       (DATE_TRUNC('month', CURRENT_DATE) + ((d.mois + 1) * INTERVAL '1 month')
           - INTERVAL '1 second')::TIMESTAMP,
       a.responsable_id,
       i.id_indicateur
FROM demo_indicateur i
JOIN demo_indicateur_activite m
  ON m.code_indicateur = i.code AND m.est_auteur
JOIN demo_activite a ON a.numero = m.activite_numero
JOIN generate_series(-3, -1) AS d(mois)
    ON i.code IN ('DEMO-KPI-01', 'DEMO-KPI-02', 'DEMO-KRI-01', 'DEMO-KRI-02');

-- Dernier relevé mensuel de recommandations en août : son échéance suivante
-- est dépassée au 12 novembre, ce qui distingue un retard d'un indicateur
-- jamais saisi.
INSERT INTO valeur_indicateur (
    valeur, periode_debut, periode_fin, commentaire,
    date_saisie, id_utilisateur, id_indicateur
)
SELECT 86,
             (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '3 months')::TIMESTAMP,
             (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '2 months'
                     - INTERVAL '1 second')::TIMESTAMP,
             'Donnée fictive DEMO — relevé mensuel en retard',
             (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '2 months'
                     - INTERVAL '1 second')::TIMESTAMP,
       a.responsable_id,
       i.id_indicateur
FROM demo_indicateur i
JOIN demo_indicateur_activite m
  ON m.code_indicateur = i.code AND m.est_auteur
JOIN demo_activite a ON a.numero = m.activite_numero
WHERE i.code = 'DEMO-KPI-RETARD'
;

-- DEMO-KRI-VIDE ne reçoit volontairement aucune valeur : il matérialise le cas
-- « jamais saisi » dans le tableau de bord des indicateurs.

-- -----------------------------------------------------------------------------
-- 6. Contrôles de cohérence intégrés : aucun total n'est inventé dans le script.
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    nb_activites INTEGER;
    nb_sous_activites INTEGER;
    nb_plans INTEGER;
    nb_indicateurs INTEGER;
BEGIN
    SELECT COUNT(*) INTO nb_activites
    FROM activite WHERE code LIKE 'DEMO-A-%';
    SELECT COUNT(*) INTO nb_sous_activites
    FROM sous_activite sa
    JOIN activite a ON a.id_activite = sa.id_activite
    WHERE a.code LIKE 'DEMO-A-%';
    SELECT COUNT(*) INTO nb_plans
    FROM plan_action WHERE code LIKE 'DEMO-PA-%';
    SELECT COUNT(*) INTO nb_indicateurs
    FROM indicateur WHERE code LIKE 'DEMO-%';

    IF nb_activites <> 48 THEN
        RAISE EXCEPTION 'Jeu DEMO incomplet : % activités, 48 attendues', nb_activites;
    END IF;
    IF nb_sous_activites <> 144 THEN
        RAISE EXCEPTION 'Jeu DEMO incomplet : % sous-activités, 144 attendues', nb_sous_activites;
    END IF;
    IF nb_plans <> 18 THEN
        RAISE EXCEPTION 'Jeu DEMO incomplet : % plans, 18 attendus', nb_plans;
    END IF;
    IF nb_indicateurs < 6 THEN
        RAISE EXCEPTION 'Jeu DEMO incomplet : % indicateurs, au moins 6 attendus', nb_indicateurs;
    END IF;
END $$;

COMMIT;

-- Vérifications à consulter après exécution.
SELECT 'Activités DEMO' AS controle, COUNT(*) AS nombre
FROM activite WHERE code LIKE 'DEMO-A-%'
UNION ALL
SELECT 'Sous-activités DEMO', COUNT(*)
FROM sous_activite sa JOIN activite a ON a.id_activite = sa.id_activite
WHERE a.code LIKE 'DEMO-A-%'
UNION ALL
SELECT 'Plans DEMO', COUNT(*)
FROM plan_action WHERE code LIKE 'DEMO-PA-%'
UNION ALL
SELECT 'Indicateurs DEMO', COUNT(*)
FROM indicateur WHERE code LIKE 'DEMO-%';

-- Répartition des statuts courants des activités DEMO.
SELECT st.code AS statut, COUNT(*) AS nombre
FROM activite a
JOIN LATERAL (
    SELECT h.id_statut
    FROM historique_activite h
    WHERE h.id_activite = a.id_activite
    ORDER BY h.date_changement DESC, h.id_historique_activite DESC
    LIMIT 1
) hist ON TRUE
JOIN statut st ON st.id_statut = hist.id_statut
WHERE a.code LIKE 'DEMO-A-%'
GROUP BY st.code
ORDER BY st.code;
