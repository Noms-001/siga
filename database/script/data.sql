-- ============================================================
-- SAGA - data.sql
-- Données de démonstration - Département TSS
-- Organigramme réel du Département Études, Sûreté et Sécurité
-- ============================================================

BEGIN;

-- ============================================================
-- 1. DÉPARTEMENTS (1 seul département)
-- ============================================================

INSERT INTO departement (nom, description) VALUES
('Département Études, Sûreté et Sécurité (TSS)',
 'Département en charge des études, de la sûreté et de la sécurité');


-- ============================================================
-- 2. SERVICES (2 services)
-- ============================================================

INSERT INTO service (
    id_departement,
    nom,
    description,
    actif,
    date_creation
) VALUES
(
    (SELECT id_departement
     FROM departement
     WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),

    'Service Gestion des risques et Procédures',
    'Service chargé de la gestion des risques, sûreté, sécurité et des procédures',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_departement
     FROM departement
     WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),

    'Service Gestion des systèmes de sécurité',
    'Service chargé de la gestion, supervision et installation des systèmes de sécurité',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 3. POSTES (8 postes correspondant à l'organigramme)
-- ============================================================

INSERT INTO poste (
    libelle,
    type_contrat,
    categorie,
    effectif_prevu,
    effectif_reel,
    actif,
    date_creation
) VALUES
(
    'Chef du département',
    'CDI',
    'Direction',
    1,
    1,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Chef de service Gestion des risques et Procédures',
    'CDI',
    'Cadre',
    1,
    1,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Gestionnaire des risques sûreté/sécurité',
    'CDI',
    'Cadre',
    1,
    1,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Chargé des procédures en sûreté/sécurité',
    'CDI',
    'Cadre',
    1,
    1,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Chef de service Gestion des systèmes de sécurité',
    'CDI',
    'Cadre',
    1,
    1,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Superviseur d''installation des systèmes de sécurité',
    'CDI',
    'Cadre',
    2,
    2,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Technicien spécialisé en systèmes de sécurité',
    'CDD',
    'Technicien',
    4,
    4,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Gestionnaire des systèmes de sécurité',
    'CDI',
    'Cadre',
    1,
    1,
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. PRIORITÉS
-- ============================================================

INSERT INTO priorite (
    code,
    libelle,
    description,
    actif,
    date_creation
) VALUES
(
    'BASSE',
    'Basse',
    'Priorité faible',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'NORMALE',
    'Normale',
    'Priorité normale',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'HAUTE',
    'Haute',
    'Priorité importante',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'URGENTE',
    'Urgente',
    'Traitement prioritaire',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 5. TYPES D'ACTIVITÉ
-- ============================================================

INSERT INTO type_activite (
    libelle,
    actif,
    date_creation
) VALUES
('Projet', TRUE, CURRENT_TIMESTAMP),
('Action', TRUE, CURRENT_TIMESTAMP),
('Étude', TRUE, CURRENT_TIMESTAMP),
('Contrôle', TRUE, CURRENT_TIMESTAMP),
('Supervision', TRUE, CURRENT_TIMESTAMP),
('Maintenance', TRUE, CURRENT_TIMESTAMP);


-- ============================================================
-- 6. STATUTS
-- ============================================================

INSERT INTO statut (
    libelle,
    code,
    ordre,
    description,
    actif,
    date_creation
) VALUES
(
    'Créée',
    'CREEE',
    1,
    'Activité nouvellement créée',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'En attente de validation',
    'EN_ATTENTE_VALIDATION',
    2,
    'Activité soumise au circuit de validation',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Validée',
    'VALIDEE',
    3,
    'Validation finale obtenue',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Non commencée',
    'NON_COMMENCEE',
    4,
    'Activité validée mais non démarrée',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'En cours',
    'EN_COURS',
    5,
    'Activité en cours de réalisation',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Terminée',
    'TERMINEE',
    6,
    'Activité terminée',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 7. TYPES DE TOKEN
-- ============================================================

INSERT INTO type_token (
    code,
    libelle,
    description
) VALUES
(
    'ACTIVATION_COMPTE',
    'Activation de compte',
    'Token utilisé pour initialiser le mot de passe et activer un compte'
),

(
    'RESET_MOT_DE_PASSE',
    'Réinitialisation du mot de passe',
    'Token utilisé pour réinitialiser un mot de passe'
);


-- ============================================================
-- 8. PERMISSIONS
-- ============================================================

INSERT INTO permission (
    code,
    nom,
    description,
    ressource,
    action,
    actif
) VALUES
(
    'CONSULTATION_GLOBALE',
    'Consultation globale',
    'Accès à toutes les données du département',
    'GLOBAL',
    'CONSULTER',
    TRUE
),

(
    'GERER_UTILISATEUR',
    'Gérer les utilisateurs',
    'Créer, modifier et administrer les utilisateurs',
    'UTILISATEUR',
    'GERER',
    TRUE
),

(
    'CREER_ACTIVITE',
    'Créer une activité',
    'Permet de créer une activité',
    'ACTIVITE',
    'CREER',
    TRUE
),

(
    'MODIFIER_ACTIVITE',
    'Modifier une activité',
    'Permet de modifier une activité',
    'ACTIVITE',
    'MODIFIER',
    TRUE
),

(
    'VALIDER_ACTIVITE',
    'Valider une activité',
    'Permet de valider une activité',
    'ACTIVITE',
    'VALIDER',
    TRUE
),

(
    'AFFECTER_ACTIVITE',
    'Affecter une activité',
    'Permet d''affecter des utilisateurs à une activité',
    'ACTIVITE',
    'AFFECTER',
    TRUE
),

(
    'SUIVI_ACTIVITE',
    'Suivi des activités',
    'Permet de suivre l''avancement des activités',
    'ACTIVITE',
    'SUIVI',
    TRUE
),

(
    'CREER_INDICATEUR',
    'Créer un indicateur',
    'Permet de créer un indicateur',
    'INDICATEUR',
    'CREER',
    TRUE
),

(
    'SAISIR_VALEUR_INDICATEUR',
    'Saisir une valeur',
    'Permet de saisir une valeur d''indicateur',
    'INDICATEUR',
    'SAISIR',
    TRUE
),

(
    'CONSULTER_INDICATEUR',
    'Consulter les indicateurs',
    'Permet de consulter les indicateurs',
    'INDICATEUR',
    'CONSULTER',
    TRUE
),

(
    'CONSULTER_STATISTIQUES',
    'Consulter les statistiques',
    'Permet de consulter les statistiques du département',
    'STATISTIQUES',
    'CONSULTER',
    TRUE
),

(
    'GERER_PLAN_ACTION',
    'Gérer les plans d''action',
    'Permet de gérer les plans d''action',
    'PLAN_ACTION',
    'GERER',
    TRUE
),

(
    'MISE_A_JOUR_ACTIVITE',
    'Mise à jour des activités',
    'Permet de mettre à jour ses activités',
    'ACTIVITE',
    'MISE_A_JOUR',
    TRUE
);


-- ============================================================
-- 9. RÔLES
-- ============================================================

INSERT INTO role (
    code,
    libelle,
    description,
    actif,
    date_creation
) VALUES
(
    'RESPONSABLE',
    'Responsable',
    'Responsable principal de l''activité',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'SUPPLEANT',
    'Suppléant',
    'Remplace le responsable si nécessaire',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'PARTICIPANT',
    'Participant',
    'Participe à la réalisation de l''activité',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'CONTRIBUTEUR',
    'Contributeur',
    'Contribue à la réalisation du plan d''action',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'SUPERVISEUR',
    'Superviseur',
    'Supervise et coordonne les activités',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 10. OPÉRATIONS PTA
-- ============================================================

INSERT INTO operation_pta (
    code,
    libelle,
    description,
    annee,
    date_creation
) VALUES
(
    'PTA-2026-001',
    'Amélioration de la gestion des risques',
    'Actions liées à l''amélioration de la gestion des risques',
    2026,
    CURRENT_TIMESTAMP
),

(
    'PTA-2026-002',
    'Renforcement des systèmes de sécurité',
    'Actions liées au renforcement des systèmes de sécurité',
    2026,
    CURRENT_TIMESTAMP
),

(
    'PTA-2026-003',
    'Optimisation des procédures de sécurité',
    'Actions liées à l''optimisation des procédures de sécurité',
    2026,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 11. SITES
-- ============================================================

INSERT INTO site (
    code,
    nom,
    latitude,
    longitude,
    actif,
    date_creation
) VALUES
(
    'SITE-TANA',
    'Siège Antananarivo',
    -18.8792,
    47.5079,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'SITE-IT',
    'Centre de sécurité',
    -18.9150,
    47.5250,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'SITE-TOAM',
    'Site Toamasina',
    -18.1493,
    49.4023,
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 12. DÉCISIONS
-- ============================================================

INSERT INTO decision_validation (
    code,
    libelle,
    description,
    actif,
    date_creation
) VALUES
(
    'VALIDER',
    'Valider',
    'Approuver l''étape de validation',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'REFUSER',
    'Refuser',
    'Rejeter la demande',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'DEMANDER_MODIFICATION',
    'Demander une modification',
    'Retourner la demande pour correction',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 13. PROCÉDURES
-- ============================================================

INSERT INTO "procedure" (
    code,
    libelle,
    description,
    actif,
    date_creation
) VALUES
(
    'PROC-STD',
    'Validation standard',
    'Circuit standard avec chef de service puis chef de département',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'PROC-STRAT',
    'Validation stratégique',
    'Circuit renforcé avec chef de service, chef de département',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 14. ÉTAPES DE VALIDATION (adaptées aux nouveaux postes)
-- ============================================================

-- Pour la procédure standard (PROC-STD)
INSERT INTO etape_validation (
    id_procedure,
    libelle,
    description,
    niveau,
    id_poste,
    obligatoire,
    actif,
    date_creation
) VALUES
(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STD'),

    'Validation chef de service',
    'Validation par le chef de service concerné',
    1,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Chef de service Gestion des risques et Procédures'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STD'),

    'Validation chef de département',
    'Validation finale par le chef du département',
    2,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Chef du département'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
);

-- Pour la procédure stratégique (PROC-STRAT)
INSERT INTO etape_validation (
    id_procedure,
    libelle,
    description,
    niveau,
    id_poste,
    obligatoire,
    actif,
    date_creation
) VALUES
(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STRAT'),

    'Validation chef de service',
    'Première étape de validation stratégique',
    1,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Chef de service Gestion des systèmes de sécurité'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STRAT'),

    'Validation chef de département',
    'Deuxième étape de validation stratégique',
    2,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Chef du département'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 15. UTILISATEURS (12 utilisateurs correspondant à l'organigramme)
-- ============================================================

-- 15.1 Chef du département (id_service = NULL)
INSERT INTO utilisateur (
    id_service,
    id_poste,
    id_departement,
    nom,
    prenom,
    email,
    telephone,
    mot_de_passe,
    actif,
    date_creation,
    date_derniere_connexion
) VALUES
(
    NULL, -- Important : Le chef du département n'a pas de service
    (SELECT id_poste FROM poste WHERE libelle = 'Chef du département'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RAZAFINDRAMARY',
    'Jean-Louis',
    'jeanlouis.razafindramary@banky-foibe.mg',
    '032000001',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 15.2 Service Gestion des risques et Procédures (6 utilisateurs)
INSERT INTO utilisateur (
    id_service,
    id_poste,
    id_departement,
    nom,
    prenom,
    email,
    telephone,
    mot_de_passe,
    actif,
    date_creation,
    date_derniere_connexion
) VALUES
(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des risques et Procédures'),
    (SELECT id_poste FROM poste WHERE libelle = 'Chef de service Gestion des risques et Procédures'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RAKOTOMALALA',
    'Marie-Claire',
    'marieclaire.rakotomalala@banky-foibe.mg',
    '032000002',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des risques et Procédures'),
    (SELECT id_poste FROM poste WHERE libelle = 'Gestionnaire des risques sûreté/sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RAZANASOA',
    'Hery',
    'hery.razanasoa@banky-foibe.mg',
    '032000003',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des risques et Procédures'),
    (SELECT id_poste FROM poste WHERE libelle = 'Chargé des procédures en sûreté/sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'ANDRIANTSOA',
    'Lalao',
    'lalao.andriantsoa@banky-foibe.mg',
    '032000004',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 15.3 Service Gestion des systèmes de sécurité (7 utilisateurs)
INSERT INTO utilisateur (
    id_service,
    id_poste,
    id_departement,
    nom,
    prenom,
    email,
    telephone,
    mot_de_passe,
    actif,
    date_creation,
    date_derniere_connexion
) VALUES
(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_poste FROM poste WHERE libelle = 'Chef de service Gestion des systèmes de sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RAVELOJAONA',
    'Haja',
    'haja.ravelojaona@banky-foibe.mg',
    '032000005',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_poste FROM poste WHERE libelle = 'Superviseur d''installation des systèmes de sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RASOLOARISOA',
    'Mampionona',
    'mampionona.rasoloarisoa@banky-foibe.mg',
    '032000006',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_poste FROM poste WHERE libelle = 'Superviseur d''installation des systèmes de sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RAHARISON',
    'Tiana',
    'tiana.raharison@banky-foibe.mg',
    '032000007',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_poste FROM poste WHERE libelle = 'Technicien spécialisé en systèmes de sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RAFALIMANANA',
    'Tojo',
    'tojo.rafalimanana@banky-foibe.mg',
    '032000008',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_poste FROM poste WHERE libelle = 'Technicien spécialisé en systèmes de sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RANDRIANAVALONA',
    'Miora',
    'miora.randrianavalona@banky-foibe.mg',
    '032000009',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_poste FROM poste WHERE libelle = 'Technicien spécialisé en systèmes de sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RAKOTOARIVONY',
    'Nirina',
    'nirina.rakotoarivony@banky-foibe.mg',
    '032000010',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_poste FROM poste WHERE libelle = 'Technicien spécialisé en systèmes de sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'ANDRIAMAHEFA',
    'Faniry',
    'faniry.andriamahefa@banky-foibe.mg',
    '032000011',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_poste FROM poste WHERE libelle = 'Gestionnaire des systèmes de sécurité'),
    (SELECT id_departement FROM departement WHERE nom = 'Département Études, Sûreté et Sécurité (TSS)'),
    'RAZAFINDRAKOTO',
    'Tahiana',
    'tahiana.razafindrakoto@banky-foibe.mg',
    '032000012',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 16. PERMISSIONS DES POSTES (adaptées à l'organigramme)
-- ============================================================

-- 16.1 Chef du département - Accès complet
INSERT INTO poste_permission (
    id_poste,
    id_permission
)
SELECT
    (SELECT id_poste FROM poste WHERE libelle = 'Chef du département'),
    id_permission
FROM permission
WHERE code IN (
    'CONSULTATION_GLOBALE',
    'GERER_UTILISATEUR',
    'CREER_ACTIVITE',
    'MODIFIER_ACTIVITE',
    'VALIDER_ACTIVITE',
    'AFFECTER_ACTIVITE',
    'SUIVI_ACTIVITE',
    'CREER_INDICATEUR',
    'SAISIR_VALEUR_INDICATEUR',
    'CONSULTER_INDICATEUR',
    'CONSULTER_STATISTIQUES',
    'GERER_PLAN_ACTION'
);

-- 16.2 Chefs de service - Accès à leur service
INSERT INTO poste_permission (
    id_poste,
    id_permission
)
SELECT
    (SELECT id_poste FROM poste WHERE libelle = 'Chef de service Gestion des risques et Procédures'),
    id_permission
FROM permission
WHERE code IN (
    'CREER_ACTIVITE',
    'MODIFIER_ACTIVITE',
    'VALIDER_ACTIVITE',
    'AFFECTER_ACTIVITE',
    'SUIVI_ACTIVITE',
    'CREER_INDICATEUR',
    'SAISIR_VALEUR_INDICATEUR',
    'CONSULTER_INDICATEUR',
    'CONSULTER_STATISTIQUES',
    'GERER_PLAN_ACTION'
);

INSERT INTO poste_permission (
    id_poste,
    id_permission
)
SELECT
    (SELECT id_poste FROM poste WHERE libelle = 'Chef de service Gestion des systèmes de sécurité'),
    id_permission
FROM permission
WHERE code IN (
    'CREER_ACTIVITE',
    'MODIFIER_ACTIVITE',
    'VALIDER_ACTIVITE',
    'AFFECTER_ACTIVITE',
    'SUIVI_ACTIVITE',
    'CREER_INDICATEUR',
    'SAISIR_VALEUR_INDICATEUR',
    'CONSULTER_INDICATEUR',
    'CONSULTER_STATISTIQUES',
    'GERER_PLAN_ACTION'
);

-- 16.3 Gestionnaire risques - Consultation et gestion de ses activités
INSERT INTO poste_permission (
    id_poste,
    id_permission
)
SELECT
    (SELECT id_poste FROM poste WHERE libelle = 'Gestionnaire des risques sûreté/sécurité'),
    id_permission
FROM permission
WHERE code IN (
    'CREER_ACTIVITE',
    'MODIFIER_ACTIVITE',
    'SUIVI_ACTIVITE',
    'CONSULTER_INDICATEUR',
    'SAISIR_VALEUR_INDICATEUR'
);

-- 16.4 Chargé des procédures - Consultation et mise à jour
INSERT INTO poste_permission (
    id_poste,
    id_permission
)
SELECT
    (SELECT id_poste FROM poste WHERE libelle = 'Chargé des procédures en sûreté/sécurité'),
    id_permission
FROM permission
WHERE code IN (
    'CREER_ACTIVITE',
    'MODIFIER_ACTIVITE',
    'SUIVI_ACTIVITE',
    'CONSULTER_INDICATEUR',
    'SAISIR_VALEUR_INDICATEUR'
);

-- 16.5 Superviseurs - Supervision et coordination
INSERT INTO poste_permission (
    id_poste,
    id_permission
)
SELECT
    (SELECT id_poste FROM poste WHERE libelle = 'Superviseur d''installation des systèmes de sécurité'),
    id_permission
FROM permission
WHERE code IN (
    'CREER_ACTIVITE',
    'MODIFIER_ACTIVITE',
    'AFFECTER_ACTIVITE',
    'SUIVI_ACTIVITE',
    'CONSULTER_INDICATEUR',
    'SAISIR_VALEUR_INDICATEUR'
);

-- 16.6 Techniciens - Consultation et mise à jour de leurs activités
INSERT INTO poste_permission (
    id_poste,
    id_permission
)
SELECT
    (SELECT id_poste FROM poste WHERE libelle = 'Technicien spécialisé en systèmes de sécurité'),
    id_permission
FROM permission
WHERE code IN (
    'SUIVI_ACTIVITE',
    'MISE_A_JOUR_ACTIVITE',
    'CONSULTER_INDICATEUR'
);

-- 16.7 Gestionnaire des systèmes - Consultation et gestion
INSERT INTO poste_permission (
    id_poste,
    id_permission
)
SELECT
    (SELECT id_poste FROM poste WHERE libelle = 'Gestionnaire des systèmes de sécurité'),
    id_permission
FROM permission
WHERE code IN (
    'CREER_ACTIVITE',
    'MODIFIER_ACTIVITE',
    'SUIVI_ACTIVITE',
    'CONSULTER_INDICATEUR',
    'SAISIR_VALEUR_INDICATEUR'
);


-- ============================================================
-- 17. TOKENS
-- ============================================================

INSERT INTO token_auth (
    id_utilisateur,
    id_type_token,
    token,
    date_creation,
    date_expiration
) VALUES
(
    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'jeanlouis.razafindramary@banky-foibe.mg'),

    (SELECT id_type_token
     FROM type_token
     WHERE code = 'ACTIVATION_COMPTE'),

    'ACTIVATION-DEMO-001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '24 hours'
),

(
    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),

    (SELECT id_type_token
     FROM type_token
     WHERE code = 'ACTIVATION_COMPTE'),

    'ACTIVATION-DEMO-002',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '24 hours'
),

(
    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'haja.ravelojaona@banky-foibe.mg'),

    (SELECT id_type_token
     FROM type_token
     WHERE code = 'RESET_MOT_DE_PASSE'),

    'RESET-DEMO-001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 hour'
);


-- ============================================================
-- 18. ACTIVITÉS (cohérentes avec les services)
-- ============================================================

-- 18.1 Activités du Service Gestion des risques et Procédures
INSERT INTO activite (
    id_service,
    id_type_activite,
    id_priorite,
    id_site,
    id_operation_pta,
    id_procedure,
    code,
    libelle,
    date_debut_prevue,
    date_fin_prevue,
    date_debut_reelle,
    date_creation
) VALUES
(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des risques et Procédures'),
    (SELECT id_type_activite FROM type_activite WHERE libelle = 'Contrôle'),
    (SELECT id_priorite FROM priorite WHERE code = 'HAUTE'),
    (SELECT id_site FROM site WHERE code = 'SITE-TANA'),
    (SELECT id_operation_pta FROM operation_pta WHERE code = 'PTA-2026-001'),
    (SELECT id_procedure FROM "procedure" WHERE code = 'PROC-STD'),
    'ACT-2026-001',
    'Contrôle mensuel des risques opérationnels',
    '2026-08-01 08:00:00',
    '2026-08-30 17:00:00',
    '2026-08-05 08:30:00',
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des risques et Procédures'),
    (SELECT id_type_activite FROM type_activite WHERE libelle = 'Étude'),
    (SELECT id_priorite FROM priorite WHERE code = 'NORMALE'),
    (SELECT id_site FROM site WHERE code = 'SITE-TANA'),
    (SELECT id_operation_pta FROM operation_pta WHERE code = 'PTA-2026-003'),
    (SELECT id_procedure FROM "procedure" WHERE code = 'PROC-STD'),
    'ACT-2026-002',
    'Étude des procédures de sécurité existantes',
    '2026-08-15 08:00:00',
    '2026-09-15 17:00:00',
    NULL,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des risques et Procédures'),
    (SELECT id_type_activite FROM type_activite WHERE libelle = 'Action'),
    (SELECT id_priorite FROM priorite WHERE code = 'URGENTE'),
    (SELECT id_site FROM site WHERE code = 'SITE-TOAM'),
    (SELECT id_operation_pta FROM operation_pta WHERE code = 'PTA-2026-001'),
    (SELECT id_procedure FROM "procedure" WHERE code = 'PROC-STRAT'),
    'ACT-2026-003',
    'Action corrective suite à l''audit de sécurité',
    '2026-09-01 08:00:00',
    '2026-09-30 17:00:00',
    NULL,
    CURRENT_TIMESTAMP
);

-- 18.2 Activités du Service Gestion des systèmes de sécurité
INSERT INTO activite (
    id_service,
    id_type_activite,
    id_priorite,
    id_site,
    id_operation_pta,
    id_procedure,
    code,
    libelle,
    date_debut_prevue,
    date_fin_prevue,
    date_debut_reelle,
    date_creation
) VALUES
(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_type_activite FROM type_activite WHERE libelle = 'Supervision'),
    (SELECT id_priorite FROM priorite WHERE code = 'HAUTE'),
    (SELECT id_site FROM site WHERE code = 'SITE-IT'),
    (SELECT id_operation_pta FROM operation_pta WHERE code = 'PTA-2026-002'),
    (SELECT id_procedure FROM "procedure" WHERE code = 'PROC-STD'),
    'ACT-2026-004',
    'Supervision des systèmes de sécurité existants',
    '2026-08-01 08:00:00',
    '2026-08-31 17:00:00',
    '2026-08-02 09:00:00',
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_type_activite FROM type_activite WHERE libelle = 'Maintenance'),
    (SELECT id_priorite FROM priorite WHERE code = 'NORMALE'),
    (SELECT id_site FROM site WHERE code = 'SITE-TANA'),
    (SELECT id_operation_pta FROM operation_pta WHERE code = 'PTA-2026-002'),
    (SELECT id_procedure FROM "procedure" WHERE code = 'PROC-STD'),
    'ACT-2026-005',
    'Maintenance préventive des systèmes de sécurité',
    '2026-09-01 08:00:00',
    '2026-09-15 17:00:00',
    NULL,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service WHERE nom = 'Service Gestion des systèmes de sécurité'),
    (SELECT id_type_activite FROM type_activite WHERE libelle = 'Projet'),
    (SELECT id_priorite FROM priorite WHERE code = 'URGENTE'),
    (SELECT id_site FROM site WHERE code = 'SITE-TOAM'),
    (SELECT id_operation_pta FROM operation_pta WHERE code = 'PTA-2026-002'),
    (SELECT id_procedure FROM "procedure" WHERE code = 'PROC-STRAT'),
    'ACT-2026-006',
    'Installation des nouveaux équipements de sécurité',
    '2026-09-10 08:00:00',
    '2026-10-20 17:00:00',
    NULL,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 19. AFFECTATIONS ACTIVITÉ / UTILISATEUR
-- ============================================================

-- 19.1 Activité ACT-2026-001 (Contrôle des risques)
INSERT INTO activite_utilisateur (
    id_activite,
    id_utilisateur,
    id_role,
    date_affectation
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'hery.razanasoa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'lalao.andriantsoa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    CURRENT_TIMESTAMP
);

-- 19.2 Activité ACT-2026-002 (Étude des procédures)
INSERT INTO activite_utilisateur (
    id_activite,
    id_utilisateur,
    id_role,
    date_affectation
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-002'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-002'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'lalao.andriantsoa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'SUPPLEANT'),
    CURRENT_TIMESTAMP
);

-- 19.3 Activité ACT-2026-003 (Action corrective)
INSERT INTO activite_utilisateur (
    id_activite,
    id_utilisateur,
    id_role,
    date_affectation
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-003'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'hery.razanasoa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-003'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'SUPERVISEUR'),
    CURRENT_TIMESTAMP
);

-- 19.4 Activité ACT-2026-004 (Supervision)
INSERT INTO activite_utilisateur (
    id_activite,
    id_utilisateur,
    id_role,
    date_affectation
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-004'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'haja.ravelojaona@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-004'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'mampionona.rasoloarisoa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'SUPERVISEUR'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-004'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'tiana.raharison@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'SUPERVISEUR'),
    CURRENT_TIMESTAMP
);

-- 19.5 Activité ACT-2026-005 (Maintenance)
INSERT INTO activite_utilisateur (
    id_activite,
    id_utilisateur,
    id_role,
    date_affectation
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-005'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'tojo.rafalimanana@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-005'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'miora.randrianavalona@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-005'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'nirina.rakotoarivony@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    CURRENT_TIMESTAMP
);

-- 19.6 Activité ACT-2026-006 (Installation)
INSERT INTO activite_utilisateur (
    id_activite,
    id_utilisateur,
    id_role,
    date_affectation
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-006'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'tahiana.razafindrakoto@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-006'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'mampionona.rasoloarisoa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'SUPERVISEUR'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-006'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'tojo.rafalimanana@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-006'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'miora.randrianavalona@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-006'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'nirina.rakotoarivony@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    CURRENT_TIMESTAMP
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-006'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'faniry.andriamahefa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 20. HISTORIQUE
-- ============================================================

-- Historique pour ACT-2026-001
INSERT INTO historique_activite (
    id_activite,
    id_statut,
    id_utilisateur,
    date_debut,
    date_fin,
    commentaire
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT id_statut FROM statut WHERE code = 'CREEE'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    '2026-08-01 08:00:00',
    '2026-08-01 09:00:00',
    'Activité créée'
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT id_statut FROM statut WHERE code = 'EN_ATTENTE_VALIDATION'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    '2026-08-01 09:00:00',
    '2026-08-02 10:00:00',
    'Soumis à validation'
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT id_statut FROM statut WHERE code = 'VALIDEE'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'jeanlouis.razafindramary@banky-foibe.mg'),
    '2026-08-02 10:00:00',
    '2026-08-02 10:30:00',
    'Validé par le chef de département'
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT id_statut FROM statut WHERE code = 'NON_COMMENCEE'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    '2026-08-02 10:30:00',
    '2026-08-05 08:30:00',
    'Prêt à démarrer'
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT id_statut FROM statut WHERE code = 'EN_COURS'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    '2026-08-05 08:30:00',
    NULL,
    'En cours de réalisation'
);

-- Historique pour ACT-2026-004
INSERT INTO historique_activite (
    id_activite,
    id_statut,
    id_utilisateur,
    date_debut,
    date_fin,
    commentaire
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-004'),
    (SELECT id_statut FROM statut WHERE code = 'CREEE'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'haja.ravelojaona@banky-foibe.mg'),
    '2026-08-01 08:00:00',
    '2026-08-01 09:00:00',
    'Activité créée'
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-004'),
    (SELECT id_statut FROM statut WHERE code = 'EN_COURS'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'haja.ravelojaona@banky-foibe.mg'),
    '2026-08-02 09:00:00',
    NULL,
    'Supervision en cours'
);


-- ============================================================
-- 21. RESSOURCES
-- ============================================================

INSERT INTO ressource (
    id_activite,
    designation,
    type_ressource,
    quantite,
    cout
) VALUES
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    'Ordinateur portable',
    'Matériel',
    2,
    2500000
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    'Véhicule de service',
    'Transport',
    1,
    500000
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-004'),
    'Équipement de supervision',
    'Matériel',
    3,
    4500000
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-006'),
    'Équipement de sécurité réseau',
    'Matériel',
    5,
    12000000
),
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-006'),
    'Caméras de surveillance',
    'Matériel',
    10,
    8000000
);


-- ============================================================
-- 22. INDICATEURS
-- ============================================================

INSERT INTO indicateur (
    type_indicateur,
    details,
    libelle,
    code_hopex,
    indicateur_hopex,
    objectifs,
    methode_determination,
    frequence_verification,
    frequence_aggregation,
    seuil_min,
    seuil_max,
    unite,
    commentaire,
    date_creation
) VALUES
(
    'KPI',
    'Nombre de contrôles de risques réalisés',
    'Taux de réalisation des contrôles de risques',
    'KPI-RISK-001',
    'RISK_CONTROL_RATE',
    'Réaliser au moins 90% des contrôles prévus',
    'Contrôles réalisés / contrôles prévus',
    'Mensuelle',
    'Mensuelle',
    90,
    100,
    '%',
    'Indicateur de suivi des contrôles de risques',
    CURRENT_TIMESTAMP
),

(
    'KPI',
    'Taux de réalisation des actions correctives',
    'Taux de réalisation des actions correctives',
    'KPI-ACT-001',
    'CORRECTIVE_ACTION_RATE',
    'Atteindre au moins 90% de réalisation',
    'Actions terminées / actions planifiées',
    'Mensuelle',
    'Mensuelle',
    90,
    100,
    '%',
    'Indicateur de performance des actions correctives',
    CURRENT_TIMESTAMP
),

(
    'KPI',
    'Disponibilité des systèmes de sécurité',
    'Disponibilité des systèmes de sécurité',
    'KPI-SEC-001',
    'SEC_SYSTEM_AVAILABILITY',
    'Maintenir une disponibilité minimale de 99%',
    'Temps de fonctionnement / temps total',
    'Mensuelle',
    'Mensuelle',
    99,
    100,
    '%',
    'Indicateur de disponibilité des systèmes de sécurité',
    CURRENT_TIMESTAMP
),

(
    'KPI',
    'Nombre d''incidents de sécurité',
    'Nombre d''incidents de sécurité',
    'KPI-SEC-002',
    'SEC_INCIDENTS_COUNT',
    'Réduire le nombre d''incidents à moins de 5 par mois',
    'Comptage des incidents déclarés',
    'Mensuelle',
    'Mensuelle',
    0,
    5,
    'incident',
    'Indicateur de suivi des incidents de sécurité',
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 23. VALEURS DES INDICATEURS
-- ============================================================

INSERT INTO valeur_indicateur (
    id_indicateur,
    periode_debut,
    periode_fin,
    valeur,
    commentaire,
    date_saisie
) VALUES
(
    (SELECT id_indicateur FROM indicateur WHERE code_hopex = 'KPI-RISK-001'),
    '2026-06-01',
    '2026-06-30',
    92,
    '92% des contrôles réalisés en juin',
    '2026-07-01 09:00:00'
),
(
    (SELECT id_indicateur FROM indicateur WHERE code_hopex = 'KPI-RISK-001'),
    '2026-07-01',
    '2026-07-31',
    95,
    '95% des contrôles réalisés en juillet',
    '2026-08-01 09:00:00'
),
(
    (SELECT id_indicateur FROM indicateur WHERE code_hopex = 'KPI-SEC-001'),
    '2026-06-01',
    '2026-06-30',
    99.2,
    'Disponibilité de 99.2% en juin',
    '2026-07-01 09:00:00'
),
(
    (SELECT id_indicateur FROM indicateur WHERE code_hopex = 'KPI-SEC-001'),
    '2026-07-01',
    '2026-07-31',
    99.5,
    'Disponibilité de 99.5% en juillet',
    '2026-08-01 09:00:00'
),
(
    (SELECT id_indicateur FROM indicateur WHERE code_hopex = 'KPI-SEC-002'),
    '2026-06-01',
    '2026-06-30',
    4,
    '4 incidents en juin',
    '2026-07-01 09:00:00'
),
(
    (SELECT id_indicateur FROM indicateur WHERE code_hopex = 'KPI-SEC-002'),
    '2026-07-01',
    '2026-07-31',
    3,
    '3 incidents en juillet',
    '2026-08-01 09:00:00'
);


-- ============================================================
-- 24. VALIDATIONS
-- ============================================================

INSERT INTO validation_activite (
    id_activite,
    id_etape_validation,
    id_utilisateur_demandeur,
    id_utilisateur_validateur,
    id_decision,
    date_demande,
    date_decision,
    commentaire
) VALUES

-- Validation ACT-2026-001 étape 1 (Chef de service)
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT ev.id_etape_validation
     FROM etape_validation ev
     JOIN "procedure" p ON p.id_procedure = ev.id_procedure
     WHERE p.code = 'PROC-STD' AND ev.niveau = 1),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    (SELECT id_decision FROM decision_validation WHERE code = 'VALIDER'),
    '2026-08-01 09:00:00',
    '2026-08-02 10:00:00',
    'Validation par le chef de service'
),

-- Validation ACT-2026-001 étape 2 (Chef de département)
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-001'),
    (SELECT ev.id_etape_validation
     FROM etape_validation ev
     JOIN "procedure" p ON p.id_procedure = ev.id_procedure
     WHERE p.code = 'PROC-STD' AND ev.niveau = 2),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'jeanlouis.razafindramary@banky-foibe.mg'),
    (SELECT id_decision FROM decision_validation WHERE code = 'VALIDER'),
    '2026-08-02 10:15:00',
    '2026-08-02 10:30:00',
    'Validation finale par le chef de département'
),

-- Validation ACT-2026-004 étape 1 (Chef de service)
(
    (SELECT id_activite FROM activite WHERE code = 'ACT-2026-004'),
    (SELECT ev.id_etape_validation
     FROM etape_validation ev
     JOIN "procedure" p ON p.id_procedure = ev.id_procedure
     WHERE p.code = 'PROC-STD' AND ev.niveau = 1),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'haja.ravelojaona@banky-foibe.mg'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'haja.ravelojaona@banky-foibe.mg'),
    (SELECT id_decision FROM decision_validation WHERE code = 'VALIDER'),
    '2026-08-01 09:00:00',
    '2026-08-02 09:00:00',
    'Validation par le chef de service'
);


-- ============================================================
-- 25. NOTIFICATIONS
-- ============================================================

-- Notifications pour ACT-2026-001
INSERT INTO notification (
    id_utilisateur,
    id_priorite,
    titre,
    message,
    lu,
    date_creation,
    date_lecture
) VALUES
(
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    (SELECT id_priorite FROM priorite WHERE code = 'HAUTE'),
    'Validation requise',
    'L''activité ACT-2026-001 nécessite votre validation.',
    TRUE,
    '2026-08-02 09:30:00',
    '2026-08-02 09:45:00'
),
(
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'jeanlouis.razafindramary@banky-foibe.mg'),
    (SELECT id_priorite FROM priorite WHERE code = 'HAUTE'),
    'Validation requise',
    'L''activité ACT-2026-001 nécessite votre validation finale.',
    TRUE,
    '2026-08-02 10:15:00',
    '2026-08-02 10:20:00'
),
(
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'hery.razanasoa@banky-foibe.mg'),
    (SELECT id_priorite FROM priorite WHERE code = 'NORMALE'),
    'Nouvelle affectation',
    'Vous avez été affecté à l''activité ACT-2026-001.',
    FALSE,
    '2026-08-01 08:30:00',
    NULL
),
(
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'lalao.andriantsoa@banky-foibe.mg'),
    (SELECT id_priorite FROM priorite WHERE code = 'NORMALE'),
    'Nouvelle affectation',
    'Vous avez été affecté à l''activité ACT-2026-001.',
    FALSE,
    '2026-08-01 08:30:00',
    NULL
);

-- Notifications pour ACT-2026-006
INSERT INTO notification (
    id_utilisateur,
    id_priorite,
    titre,
    message,
    lu,
    date_creation,
    date_lecture
) VALUES
(
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'tahiana.razafindrakoto@banky-foibe.mg'),
    (SELECT id_priorite FROM priorite WHERE code = 'URGENTE'),
    'Projet prioritaire',
    'L''activité ACT-2026-006 est prioritaire. Veuillez préparer l''installation.',
    FALSE,
    '2026-08-25 08:20:00',
    NULL
),
(
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'mampionona.rasoloarisoa@banky-foibe.mg'),
    (SELECT id_priorite FROM priorite WHERE code = 'HAUTE'),
    'Supervision requise',
    'Vous êtes superviseur pour l''activité ACT-2026-006.',
    FALSE,
    '2026-08-25 08:25:00',
    NULL
),
(
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'tojo.rafalimanana@banky-foibe.mg'),
    (SELECT id_priorite FROM priorite WHERE code = 'NORMALE'),
    'Participation à un projet',
    'Vous participez à l''activité ACT-2026-006.',
    FALSE,
    '2026-08-25 08:30:00',
    NULL
);


-- ============================================================
-- 26. PLANS D'ACTION
-- ============================================================

INSERT INTO plan_action (
    code,
    intitule,
    commentaire,
    date_creation
) VALUES
(
    'PA-2026-001',
    'Renforcement de la sécurité des installations',
    'Plan issu des recommandations d''audit de sécurité',
    CURRENT_TIMESTAMP
),
(
    'PA-2026-002',
    'Optimisation des procédures de gestion des risques',
    'Plan d''amélioration continue des procédures',
    CURRENT_TIMESTAMP
),
(
    'PA-2026-003',
    'Formation des techniciens en sécurité',
    'Plan de formation des techniciens spécialisés',
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 27. ORIGINES
-- ============================================================

INSERT INTO origine (
    code,
    type,
    reference,
    intitule,
    description
) VALUES
(
    'ORI-001',
    'AUDIT',
    'AUD-2026-01',
    'Audit de sécurité interne',
    'Constats issus de l''audit de sécurité'
),
(
    'ORI-002',
    'RECOMMANDATION',
    'REC-2026-04',
    'Recommandation de la direction TSS',
    'Recommandation du chef de département'
),
(
    'ORI-003',
    'INCIDENT',
    'INC-2026-02',
    'Incident de sécurité majeur',
    'Origine liée à un incident de sécurité'
),
(
    'ORI-004',
    'AUDIT',
    'AUD-2026-02',
    'Audit des procédures',
    'Constats issus de l''audit des procédures'
);


-- ============================================================
-- 28. ORIGINES DES PLANS D'ACTION
-- ============================================================

INSERT INTO plan_action_origine (
    id_plan_action,
    id_origine,
    est_principale
) VALUES
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-001'),
    (SELECT id_origine FROM origine WHERE code = 'ORI-001'),
    TRUE
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-001'),
    (SELECT id_origine FROM origine WHERE code = 'ORI-003'),
    FALSE
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-002'),
    (SELECT id_origine FROM origine WHERE code = 'ORI-002'),
    TRUE
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-002'),
    (SELECT id_origine FROM origine WHERE code = 'ORI-004'),
    FALSE
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-003'),
    (SELECT id_origine FROM origine WHERE code = 'ORI-002'),
    TRUE
);


-- ============================================================
-- 29. UTILISATEURS DES PLANS D'ACTION
-- ============================================================

INSERT INTO plan_action_utilisateur (
    id_plan_action,
    id_utilisateur,
    id_role
) VALUES

-- PA-2026-001
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-001'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'jeanlouis.razafindramary@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE')
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-001'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'haja.ravelojaona@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'CONTRIBUTEUR')
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-001'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'mampionona.rasoloarisoa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT')
),

-- PA-2026-002
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-002'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'marieclaire.rakotomalala@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE')
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-002'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'hery.razanasoa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT')
),

-- PA-2026-003
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-003'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'haja.ravelojaona@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE')
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-003'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'tojo.rafalimanana@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT')
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-003'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'miora.randrianavalona@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT')
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-003'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'nirina.rakotoarivony@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT')
),
(
    (SELECT id_plan_action FROM plan_action WHERE code = 'PA-2026-003'),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'faniry.andriamahefa@banky-foibe.mg'),
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT')
);


-- ============================================================
-- 30. PARAMÈTRES
-- ============================================================

INSERT INTO parametre (
    code,
    valeur,
    description,
    actif
) VALUES
(
    'DUREE_TOKEN_RESET_MOT_DE_PASSE',
    '30',
    'Durée de validité du token de réinitialisation du mot de passe en minutes',
    TRUE
),
(
    'DUREE_TOKEN_ACTIVATION_COMPTE',
    '30',
    'Durée de validité du token d''activation du compte en minutes',
    TRUE
);


-- ============================================================
-- FIN
-- ============================================================

COMMIT;