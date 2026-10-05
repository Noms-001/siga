-- =============================================================================
-- SAGA — Système d'Alerte et de Gestion des Activités
-- Département Études, Sûreté et Sécurité (TSS) — BFM
-- Fichier de données fictives réalistes — Année 2027
-- =============================================================================
-- IMPORTANT : les colonnes SERIAL ne sont PAS renseignées.
-- Les séquences sont réinitialisées en fin de fichier.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. RÉFÉRENTIELS DE BASE
-- =============================================================================

-- 1.1 Département
INSERT INTO departement (code, nom, description) VALUES
('TSS', 'Département Études, Sûreté et Sécurité',
 'Département chargé des études, de la sûreté et de la sécurité des biens et des personnes au sein de la BFM.');

-- 1.2 Services (référence au département via sous-requête)
INSERT INTO service (nom, description, actif, date_desactivation, id_departement)
SELECT 'Gestion des Risques et Procédures',
       'Service chargé de l''identification, l''évaluation et la maîtrise des risques, ainsi que de l''élaboration et la diffusion des procédures de sûreté et de sécurité.',
       TRUE, NULL, d.id_departement
FROM departement d WHERE d.code = 'TSS';

INSERT INTO service (nom, description, actif, date_desactivation, id_departement)
SELECT 'Gestion des Systèmes de Sécurité',
       'Service chargé de la disponibilité, la maintenance et l''amélioration des systèmes de sécurité (vidéosurveillance, alarme, contrôle d''accès, etc.).',
       TRUE, NULL, d.id_departement
FROM departement d WHERE d.code = 'TSS';

-- 1.3 Sites (indépendants, pas de FK)
INSERT INTO site (nom, actif, date_creation, date_desactivation) VALUES
('Siège BFM — Tunis', TRUE, '2020-01-15 08:00:00', NULL),
('Agence BFM — Ariana', TRUE, '2021-03-10 08:00:00', NULL),
('Agence BFM — Sfax', TRUE, '2021-06-20 08:00:00', NULL),
('Bâtiment administratif BFM — Lac', TRUE, '2022-02-01 08:00:00', NULL),
('Site technique BFM — Ben Arous', TRUE, '2022-09-15 08:00:00', NULL),
('Centre de stockage BFM — Mornag', TRUE, '2023-04-05 08:00:00', NULL),
('Agence BFM — Sousse', TRUE, '2023-11-12 08:00:00', NULL),
('Agence BFM — Bizerte', FALSE, '2020-05-01 08:00:00', '2026-12-31 17:00:00');

-- 1.4 Types d'activité (indépendants)
INSERT INTO type_activite (designation, description, actif, date_creation, date_desactivation) VALUES
('Étude', 'Études techniques, audits, analyses de risques et diagnostics.', TRUE, '2020-01-15 08:00:00', NULL),
('Installation', 'Installation de nouveaux équipements ou systèmes de sécurité.', TRUE, '2020-01-15 08:00:00', NULL),
('Maintenance préventive', 'Maintenance planifiée et périodique des équipements.', TRUE, '2020-01-15 08:00:00', NULL),
('Maintenance curative', 'Interventions correctives suite à une panne ou défaillance.', TRUE, '2020-01-15 08:00:00', NULL),
('Inspection', 'Inspections, contrôles et vérifications sur site.', TRUE, '2020-01-15 08:00:00', NULL),
('Procédure', 'Élaboration, mise à jour et diffusion de procédures.', TRUE, '2020-01-15 08:00:00', NULL),
('Exercice de sécurité', 'Exercices, simulations et tests de sécurité.', TRUE, '2020-01-15 08:00:00', NULL),
('Inventaire', 'Inventaires et recensements des équipements.', TRUE, '2020-01-15 08:00:00', NULL),
('Intervention urgente', 'Interventions non planifiées suite à un incident.', TRUE, '2020-01-15 08:00:00', NULL),
('Amélioration', 'Projets d''amélioration et de renforcement des dispositifs.', TRUE, '2020-01-15 08:00:00', NULL);

-- 1.5 Statuts (indépendants)
INSERT INTO statut (code, libelle, description, actif, date_creation, date_modification, date_desactivation) VALUES
('BROUILLON', 'Brouillon', 'Activité en cours de rédaction, non soumise.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('EN_ATTENTE_VALIDATION', 'En attente de validation', 'Activité soumise pour validation.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('VALIDEE', 'Validée', 'Activité validée par le responsable hiérarchique.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('REJETE', 'Rejetée', 'Activité rejetée définitivement par le responsable hiérarchique.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('NON_COMMENCEE', 'Non commencée', 'Activité validée mais pas encore démarrée.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('EN_COURS', 'En cours', 'Activité en cours de réalisation.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('TERMINEE', 'Terminée', 'Activité achevée.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('EN_RETARD', 'En retard', 'Activité dont la date de fin prévue est dépassée.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('SUSPENDUE', 'Suspendue', 'Activité temporairement suspendue.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('REPORTEE', 'Reportée', 'Activité reportée à une date ultérieure.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('ANNULEE', 'Annulée', 'Activité annulée.', TRUE, '2020-01-15 08:00:00', NULL, NULL);

-- 1.6 Priorités (indépendants)
INSERT INTO priorite (code, description, actif, date_creation, date_desactivation, libelle) VALUES
('FAIBLE', 'Priorité faible, traitement différé possible.', TRUE, '2020-01-15 08:00:00', NULL, 'Faible'),
('NORMALE', 'Priorité normale, traitement dans les délais standards.', TRUE, '2020-01-15 08:00:00', NULL, 'Normale'),
('HAUTE', 'Priorité haute, traitement rapide requis.', TRUE, '2020-01-15 08:00:00', NULL, 'Haute'),
('CRITIQUE', 'Priorité critique, traitement immédiat.', TRUE, '2020-01-15 08:00:00', NULL, 'Critique');

-- 1.7 Rôles (indépendants)
INSERT INTO role (code, designation, description, actif, date_creation, date_modification, date_desactivation) VALUES
('RESPONSABLE', 'Responsable', 'Responsable principal de la sous-activité.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('SUPPLEANT', 'Suppléant', 'Suppléant du responsable en cas d''absence.', TRUE, '2020-01-15 08:00:00', NULL, NULL),
('PARTICIPANT', 'Participant', 'Participant à la réalisation de la sous-activité.', TRUE, '2020-01-15 08:00:00', NULL, NULL);

-- 1.8 Types de token (indépendants)
INSERT INTO type_token (code, libelle, description) VALUES
('RESET_MOT_DE_PASSE', 'Réinitialisation du mot de passe', 'Token utilisé pour réinitialiser un mot de passe oublié.'),
('ACTIVATION_COMPTE', 'Activation de compte', 'Token utilisé pour activer un nouveau compte utilisateur.'),
('AUTHENTIFICATION', 'Authentification', 'Token de session après authentification réussie.');


-- 1.9 Procédure de validation (indépendante)
INSERT INTO "procedure" (designation, description) VALUES
('Procédure de validation des activités TSS',
 'Procédure standard de validation des activités du département TSS : validation par le chef de service puis par le chef de département.');

-- 1.10 Étapes de validation (référence à "procedure" via sous-requête)
INSERT INTO etape_validation (designation, description, niveau, obligatoire, actif, date_desactivation, id_procedure, retour)
SELECT 'Validation Chef de service',
    'Validation de l''activité par le chef de service concerné.',
    1, TRUE, TRUE, NULL, p.id_procedure, FALSE
FROM "procedure" p WHERE p.designation = 'Procédure de validation des activités TSS';


INSERT INTO etape_validation (designation, description, niveau, obligatoire, actif, date_desactivation, id_procedure, retour)
SELECT 'Validation Chef de département',
    'Validation finale de l''activité par le chef de département TSS.',
    2, TRUE, TRUE, NULL, p.id_procedure,TRUE
FROM "procedure" p WHERE p.designation = 'Procédure de validation des activités TSS';

-- 1.11 Objectifs spécifiques (indépendants)
-- code : BCT/<n° de l'objectif spécifique> (BCT/1 .. BCT/10)
INSERT INTO objectif_specifique (code, designation, annee, date_creation) VALUES
('BCT/1', 'Identifier et évaluer les risques liés à la sûreté et à la sécurité des biens et des personnes', 2027, '2027-01-05 08:00:00'),
('BCT/2', 'Élaborer, actualiser et diffuser les procédures de sûreté et de sécurité', 2027, '2027-01-05 08:00:00'),
('BCT/3', 'Assurer le suivi et la maîtrise des risques identifiés au sein des sites de la BFM', 2027, '2027-01-05 08:00:00'),
('BCT/4', 'Mettre en place et suivre les plans d''actions visant à réduire les risques de sûreté et de sécurité', 2027, '2027-01-05 08:00:00'),
('BCT/5', 'Contrôler l''application des procédures et proposer des mesures d''amélioration', 2027, '2027-01-05 08:00:00'),
('BCT/6', 'Assurer la disponibilité et le bon fonctionnement des systèmes de sécurité', 2027, '2027-01-05 08:00:00'),
('BCT/7', 'Planifier et réaliser la maintenance préventive des équipements et installations de sécurité', 2027, '2027-01-05 08:00:00'),
('BCT/8', 'Assurer les interventions de maintenance curative et le rétablissement des systèmes défaillants', 2027, '2027-01-05 08:00:00'),
('BCT/9', 'Suivre les installations, les équipements et leur évolution sur l''ensemble des sites', 2027, '2027-01-05 08:00:00'),
('BCT/10', 'Améliorer et renforcer les systèmes de sécurité en fonction des besoins et des risques identifiés', 2027, '2027-01-05 08:00:00');

-- 1.12 Postes (indépendants)
INSERT INTO poste (nom, effectif_prevu, effectif_reel, actif, date_creation, date_modification, date_desactivation, is_metier) VALUES
('Chef de département TSS', 1, 1, TRUE, '2020-01-15 08:00:00', NULL, NULL, TRUE),
('Chef de service — Gestion des Risques et Procédures', 1, 1, TRUE, '2020-01-15 08:00:00', NULL, NULL, TRUE),
('Chef de service — Gestion des Systèmes de Sécurité', 1, 1, TRUE, '2020-01-15 08:00:00', NULL, NULL, TRUE),
('Gestionnaire risques sûreté/sécurité', 2, 2, TRUE, '2020-01-15 08:00:00', NULL, NULL, TRUE),
('Chargé procédures sûreté/sécurité', 2, 2, TRUE, '2020-01-15 08:00:00', NULL, NULL, TRUE),
('Superviseur installation systèmes de sécurité', 1, 1, TRUE, '2020-01-15 08:00:00', NULL, NULL, TRUE),
('Technicien spécialisé systèmes de sécurité', 4, 4, TRUE, '2020-01-15 08:00:00', NULL, NULL, TRUE),
('Gestionnaire systèmes sécurité', 2, 2, TRUE, '2020-01-15 08:00:00', NULL, NULL, TRUE),
('Administrateur', 1, 1, TRUE, '2020-01-15 08:00:00', NULL, NULL, FALSE);

-- Mapping étape de validation ↔ poste décideur.
--
-- On référence par designation d'étape et par nom de poste, jamais par
-- identifiant : les séquences SERIAL ne sont pas renseignées dans ce
-- fichier (elles sont repositionnées en fin de chargement), donc un
-- identifiant écrit à la main n'a aucune garantie d'être celui attendu
-- au moment de l'exécution.
--
-- La jointure par VALUES + JOIN suit le schéma du reste du fichier :
-- c'est la même construction que pour poste_utilisateur, poste_permission
-- ou type_activite_service, ce qui évite d'inventer une seconde manière
-- de faire la même chose.
INSERT INTO etape_validation_decideur (id_etape_validation, id_poste)
SELECT ev.id_etape_validation, p.id_poste
FROM (VALUES
    -- Niveau 1 : le chef de service (Risques et Procédures) valide.
    ('Validation Chef de service', 'Chef de service — Gestion des Risques et Procédures'),
    -- Niveau 2 : le chef de département valide en dernier ressort.
    ('Validation Chef de département', 'Chef de département TSS')
) AS v(etape_designation, poste_nom)
JOIN etape_validation ev ON ev.designation = v.etape_designation
JOIN poste p ON p.nom = v.poste_nom;


-- 1.13 Permissions (indépendantes)
INSERT INTO permission (designation, description, ressource, action, actif, date_desactivation) VALUES
('Créer une activité', 'Permet de créer une nouvelle activité.', 'activite', 'CREATE', TRUE, NULL),
('Modifier une activité', 'Permet de modifier une activité existante.', 'activite', 'UPDATE', TRUE, NULL),
('Supprimer une activité', 'Permet de supprimer une activité.', 'activite', 'DELETE', TRUE, NULL),
('Consulter une activité', 'Permet de consulter les activités.', 'activite', 'READ', TRUE, NULL),
('Valider une activité', 'Permet de valider ou rejeter une activité.', 'validation_activite', 'VALIDATE', TRUE, NULL),
('Créer une sous-activité', 'Permet de créer une sous-activité.', 'sous_activite', 'CREATE', TRUE, NULL),
('Modifier une sous-activité', 'Permet de modifier une sous-activité.', 'sous_activite', 'UPDATE', TRUE, NULL),
('Affecter une sous-activité', 'Permet d''affecter une sous-activité à un utilisateur.', 'affectation_sous_activite', 'CREATE', TRUE, NULL),
('Gérer les utilisateurs', 'Permet de gérer les comptes utilisateurs.', 'utilisateur', 'MANAGE', TRUE, NULL),
('Gérer les rôles et permissions', 'Permet de gérer les rôles et permissions.', 'role', 'MANAGE', TRUE, NULL),
('Gérer les plans d''action', 'Permet de gérer les plans d''action.', 'plan_action', 'MANAGE', TRUE, NULL),
('Consulter les indicateurs', 'Permet de consulter les indicateurs.', 'indicateur', 'READ', TRUE, NULL),
('Saisir une valeur d''indicateur', 'Permet de saisir une valeur d''indicateur.', 'valeur_indicateur', 'CREATE', TRUE, NULL),
('Gérer les paramètres', 'Permet de gérer les paramètres applicatifs.', 'parametre', 'MANAGE', TRUE, NULL);

-- =============================================================================
-- 2. UTILISATEURS
-- =============================================================================
-- Chef de département → id_service NULL, id_departement = TSS
-- Membres de service → id_departement = TSS, id_service = (Risques) ou (Systèmes)

-- Chef de département (id_service NULL)
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Ben Salah', 'Karim', 'karim.bensalah@bfm.tn', '+216 71 100 001',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2020-01-15 08:00:00', NULL, NULL, '2027-03-15 09:12:00',
       d.id_departement, NULL
FROM departement d WHERE d.code = 'TSS';

-- Chef de service 1 — Gestion des Risques et Procédures
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Mansouri', 'Leila', 'leila.mansouri@bfm.tn', '+216 71 100 002',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2020-02-01 08:00:00', NULL, NULL, '2027-03-14 10:30:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Risques et Procédures'
WHERE d.code = 'TSS';

-- Chef de service 2 — Gestion des Systèmes de Sécurité
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Trabelsi', 'Hatem', 'hatem.trabelsi@bfm.tn', '+216 71 100 003',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2020-02-01 08:00:00', NULL, NULL, '2027-03-14 11:00:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Systèmes de Sécurité'
WHERE d.code = 'TSS';

-- Gestionnaire risques 1
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Gharbi', 'Sonia', 'sonia.gharbi@bfm.tn', '+216 71 100 004',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2021-01-10 08:00:00', NULL, NULL, '2027-03-13 08:45:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Risques et Procédures'
WHERE d.code = 'TSS';

-- Gestionnaire risques 2
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Khelifi', 'Mohamed', 'mohamed.khelifi@bfm.tn', '+216 71 100 005',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2021-05-20 08:00:00', NULL, NULL, '2027-03-12 14:20:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Risques et Procédures'
WHERE d.code = 'TSS';

-- Chargé procédures 1
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Bouazizi', 'Nadia', 'nadia.bouazizi@bfm.tn', '+216 71 100 006',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2021-09-01 08:00:00', NULL, NULL, '2027-03-11 09:00:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Risques et Procédures'
WHERE d.code = 'TSS';

-- Chargé procédures 2
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Jlassi', 'Rami', 'rami.jlassi@bfm.tn', '+216 71 100 007',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2022-03-15 08:00:00', NULL, NULL, '2027-03-10 16:30:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Risques et Procédures'
WHERE d.code = 'TSS';

-- Superviseur installation
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Ayari', 'Slim', 'slim.ayari@bfm.tn', '+216 71 100 008',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2020-06-01 08:00:00', NULL, NULL, '2027-03-15 07:55:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Systèmes de Sécurité'
WHERE d.code = 'TSS';

-- Technicien 1
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Mejri', 'Anis', 'anis.mejri@bfm.tn', '+216 71 100 009',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2021-02-01 08:00:00', NULL, NULL, '2027-03-14 08:10:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Systèmes de Sécurité'
WHERE d.code = 'TSS';

-- Technicien 2
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Ferchichi', 'Ines', 'ines.ferchichi@bfm.tn', '+216 71 100 010',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2021-07-01 08:00:00', NULL, NULL, '2027-03-13 15:45:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Systèmes de Sécurité'
WHERE d.code = 'TSS';

-- Technicien 3
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Zaidi', 'Yassine', 'yassine.zaidi@bfm.tn', '+216 71 100 011',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2022-01-10 08:00:00', NULL, NULL, '2027-03-12 11:30:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Systèmes de Sécurité'
WHERE d.code = 'TSS';

-- Technicien 4
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Hamdi', 'Mariem', 'mariem.hamdi@bfm.tn', '+216 71 100 012',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2022-09-01 08:00:00', NULL, NULL, '2027-03-11 13:00:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Systèmes de Sécurité'
WHERE d.code = 'TSS';

-- Gestionnaire systèmes 1
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Sassi', 'Walid', 'walid.sassi@bfm.tn', '+216 71 100 013',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2021-04-01 08:00:00', NULL, NULL, '2027-03-14 09:25:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Systèmes de Sécurité'
WHERE d.code = 'TSS';

-- Gestionnaire systèmes 2
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Riahi', 'Faten', 'faten.riahi@bfm.tn', '+216 71 100 014',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2022-06-01 08:00:00', NULL, NULL, '2027-03-13 10:50:00',
       d.id_departement, s.id_service
FROM departement d
JOIN service s ON s.id_departement = d.id_departement AND s.nom = 'Gestion des Systèmes de Sécurité'
WHERE d.code = 'TSS';

-- Administrateur (id_service NULL, rattaché au département)
INSERT INTO utilisateur (nom, prenom, email, telephone, mot_de_passe, actif, date_creation, date_desactivation, date_modification, date_derniere_connexion, id_departement, id_service)
SELECT 'Baccouche', 'Tarek', 'tarek.baccouche@bfm.tn', '+216 71 100 015',
       '$2a$12$ky63.0qcZctkDoSrGVjlbuoAlGYTQJVeBFB.s5UzgNXv/IB1C9Yim', TRUE, '2020-01-15 08:00:00', NULL, NULL, '2027-03-15 08:00:00',
       d.id_departement, NULL
FROM departement d WHERE d.code = 'TSS';

-- =============================================================================
-- 3. RELATIONS UTILISATEURS / POSTES
-- =============================================================================
-- Référencement par email (unique) et par nom de poste

INSERT INTO poste_utilisateur (id_poste, id_utilisateur)
SELECT p.id_poste, u.id_utilisateur
FROM poste p, utilisateur u
WHERE (p.nom = 'Chef de département TSS' AND u.email = 'karim.bensalah@bfm.tn')
   OR (p.nom = 'Chef de service — Gestion des Risques et Procédures' AND u.email = 'leila.mansouri@bfm.tn')
   OR (p.nom = 'Chef de service — Gestion des Systèmes de Sécurité' AND u.email = 'hatem.trabelsi@bfm.tn')
   OR (p.nom = 'Gestionnaire risques sûreté/sécurité' AND u.email IN ('sonia.gharbi@bfm.tn', 'mohamed.khelifi@bfm.tn'))
   OR (p.nom = 'Chargé procédures sûreté/sécurité' AND u.email IN ('nadia.bouazizi@bfm.tn', 'rami.jlassi@bfm.tn'))
   OR (p.nom = 'Superviseur installation systèmes de sécurité' AND u.email = 'slim.ayari@bfm.tn')
   OR (p.nom = 'Technicien spécialisé systèmes de sécurité' AND u.email IN ('anis.mejri@bfm.tn', 'ines.ferchichi@bfm.tn', 'yassine.zaidi@bfm.tn', 'mariem.hamdi@bfm.tn'))
   OR (p.nom = 'Gestionnaire systèmes sécurité' AND u.email IN ('walid.sassi@bfm.tn', 'faten.riahi@bfm.tn'))
   OR (p.nom = 'Administrateur' AND u.email = 'tarek.baccouche@bfm.tn');

-- =============================================================================
-- 4. PERMISSIONS PAR POSTE
-- =============================================================================

INSERT INTO poste_permission (id_poste, id_permission)
SELECT p.id_poste, perm.id_permission
FROM poste p, permission perm
WHERE
  -- Chef de département : supervision + validation finale
  (p.nom = 'Chef de département TSS' AND perm.designation IN ('Consulter une activité', 'Valider une activité', 'Gérer les plans d''action', 'Consulter les indicateurs'))
  -- Chef service 1 : gestion activités + validation niveau 1 + affectations
  OR (p.nom = 'Chef de service — Gestion des Risques et Procédures' AND perm.designation IN ('Créer une activité', 'Modifier une activité', 'Consulter une activité', 'Valider une activité', 'Créer une sous-activité', 'Modifier une sous-activité', 'Affecter une sous-activité', 'Gérer les plans d''action', 'Consulter les indicateurs'))
  -- Chef service 2 : idem
  OR (p.nom = 'Chef de service — Gestion des Systèmes de Sécurité' AND perm.designation IN ('Créer une activité', 'Modifier une activité', 'Consulter une activité', 'Valider une activité', 'Créer une sous-activité', 'Modifier une sous-activité', 'Affecter une sous-activité', 'Gérer les plans d''action', 'Consulter les indicateurs'))
  -- Gestionnaire risques
  OR (p.nom = 'Gestionnaire risques sûreté/sécurité' AND perm.designation IN ('Créer une activité', 'Modifier une activité', 'Consulter une activité', 'Consulter les indicateurs', 'Saisir une valeur d''indicateur'))
  -- Chargé procédures
  OR (p.nom = 'Chargé procédures sûreté/sécurité' AND perm.designation IN ('Créer une activité', 'Modifier une activité', 'Consulter une activité', 'Consulter les indicateurs'))
  -- Superviseur installation
  OR (p.nom = 'Superviseur installation systèmes de sécurité' AND perm.designation IN ('Consulter une activité', 'Créer une sous-activité', 'Modifier une sous-activité', 'Affecter une sous-activité', 'Consulter les indicateurs'))
  -- Techniciens
  OR (p.nom = 'Technicien spécialisé systèmes de sécurité' AND perm.designation IN ('Consulter une activité', 'Saisir une valeur d''indicateur'))
  -- Gestionnaire systèmes
  OR (p.nom = 'Gestionnaire systèmes sécurité' AND perm.designation IN ('Consulter une activité', 'Créer une sous-activité', 'Modifier une sous-activité', 'Consulter les indicateurs', 'Saisir une valeur d''indicateur'))
  -- Administrateur : toutes
  OR (p.nom = 'Administrateur');

-- =============================================================================
-- 5. TYPES D'ACTIVITÉ PAR SERVICE
-- =============================================================================

INSERT INTO type_activite_service (id_service, id_type_activite)
SELECT s.id_service, t.id_type_activite
FROM service s, type_activite t
WHERE
  (s.nom = 'Gestion des Risques et Procédures' AND t.designation IN ('Étude', 'Inspection', 'Procédure', 'Inventaire', 'Amélioration'))
  OR (s.nom = 'Gestion des Systèmes de Sécurité' AND t.designation IN ('Installation', 'Maintenance préventive', 'Maintenance curative', 'Exercice de sécurité', 'Intervention urgente', 'Amélioration'));

-- =============================================================================
-- 6. ACTIVITÉS PTA (50 activités — 5 par objectif spécifique)
-- =============================================================================
-- code      = A-<année>-<n° objectif>-<n° activité pour cet objectif>  ex. A-2027-01-01
-- reference = <code objectif>_<n° activité>                             ex. BCT/1_1
-- On référence les FK via sous-requêtes sur les codes/noms.

INSERT INTO activite (code, reference, designation, date_debut_prevue, date_fin_prevue, date_debut_reelle, date_fin_reelle, id_objectif_specifique, id_type_activite, id_site, id_priorite, id_service, id_procedure)
SELECT v.code, v.reference, v.designation, v.dd, v.df, v.ddr, v.dfr,
       (SELECT id_objectif_specifique FROM objectif_specifique WHERE code = v.os_code),
       (SELECT id_type_activite FROM type_activite WHERE designation = v.type_designation),
       (SELECT id_site FROM site WHERE nom = v.site_nom),
       (SELECT id_priorite FROM priorite WHERE code = v.priorite_code),
       (SELECT id_service FROM service WHERE nom = v.service_nom),
       (SELECT id_procedure FROM "procedure" WHERE designation = v.procedure_designation)
FROM (VALUES
-- BCT/1
('A-2027-01-01', 'BCT/1_1', 'Audit d''évaluation des risques de sûreté du siège BFM', DATE '2027-01-10', DATE '2027-02-28', DATE '2027-01-12', DATE '2027-02-25', 'BCT/1', 'Étude', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-01-02', 'BCT/1_2', 'Analyse des risques liés aux accès non autorisés sur le site technique', DATE '2027-02-01', DATE '2027-03-31', DATE '2027-02-05', NULL, 'BCT/1', 'Étude', 'Site technique BFM — Ben Arous', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-01-03', 'BCT/1_3', 'Évaluation des risques de défaillance des systèmes d''alarme du centre de stockage', DATE '2027-03-01', DATE '2027-04-30', NULL, NULL, 'BCT/1', 'Étude', 'Centre de stockage BFM — Mornag', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-01-04', 'BCT/1_4', 'Diagnostic des risques de sûreté du bâtiment administratif du Lac', DATE '2027-04-01', DATE '2027-05-31', NULL, NULL, 'BCT/1', 'Étude', 'Bâtiment administratif BFM — Lac', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-01-05', 'BCT/1_5', 'Étude de vulnérabilité des sites face aux intrusions', DATE '2027-05-01', DATE '2027-06-30', NULL, NULL, 'BCT/1', 'Étude', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),

-- BCT/2
('A-2027-02-01', 'BCT/2_1', 'Mise à jour de la procédure de contrôle d''accès des visiteurs', DATE '2027-01-15', DATE '2027-02-28', DATE '2027-01-20', DATE '2027-02-20', 'BCT/2', 'Procédure', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-02-02', 'BCT/2_2', 'Rédaction de la procédure de gestion des incidents de sécurité', DATE '2027-02-01', DATE '2027-03-31', DATE '2027-02-10', NULL, 'BCT/2', 'Procédure', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-02-03', 'BCT/2_3', 'Actualisation de la procédure de ronde et de surveillance des sites', DATE '2027-03-01', DATE '2027-04-30', NULL, NULL, 'BCT/2', 'Procédure', 'Agence BFM — Sfax', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-02-04', 'BCT/2_4', 'Diffusion et formation sur la nouvelle procédure d''évacuation d''urgence', DATE '2027-04-01', DATE '2027-05-31', NULL, NULL, 'BCT/2', 'Procédure', 'Siège BFM — Tunis', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-02-05', 'BCT/2_5', 'Révision de la procédure de gestion des clés et badges d''accès', DATE '2027-05-01', DATE '2027-06-30', NULL, NULL, 'BCT/2', 'Procédure', 'Bâtiment administratif BFM — Lac', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),

-- BCT/3
('A-2027-03-01', 'BCT/3_1', 'Suivi trimestriel des risques identifiés au siège BFM', DATE '2027-01-05', DATE '2027-03-31', DATE '2027-01-10', DATE '2027-03-28', 'BCT/3', 'Inspection', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-03-02', 'BCT/3_2', 'Contrôle de la maîtrise des risques sur l''agence d''Ariana', DATE '2027-02-01', DATE '2027-04-30', DATE '2027-02-15', NULL, 'BCT/3', 'Inspection', 'Agence BFM — Ariana', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-03-03', 'BCT/3_3', 'Vérification des mesures de maîtrise des risques au centre de stockage', DATE '2027-03-01', DATE '2027-05-31', NULL, NULL, 'BCT/3', 'Inspection', 'Centre de stockage BFM — Mornag', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-03-04', 'BCT/3_4', 'Audit de suivi des risques sur le site technique de Ben Arous', DATE '2027-04-01', DATE '2027-06-30', NULL, NULL, 'BCT/3', 'Inspection', 'Site technique BFM — Ben Arous', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-03-05', 'BCT/3_5', 'Évaluation semestrielle de l''efficacité des mesures de maîtrise des risques', DATE '2027-05-01', DATE '2027-07-31', NULL, NULL, 'BCT/3', 'Inspection', 'Siège BFM — Tunis', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),

-- BCT/4
('A-2027-04-01', 'BCT/4_1', 'Élaboration du plan d''action de réduction des risques d''intrusion au siège', DATE '2027-01-20', DATE '2027-03-15', DATE '2027-01-25', DATE '2027-03-10', 'BCT/4', 'Amélioration', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-04-02', 'BCT/4_2', 'Mise en œuvre du plan d''action de sécurisation de l''agence de Sfax', DATE '2027-02-15', DATE '2027-05-31', DATE '2027-02-20', NULL, 'BCT/4', 'Amélioration', 'Agence BFM — Sfax', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-04-03', 'BCT/4_3', 'Suivi du plan d''action de réduction des risques au bâtiment administratif', DATE '2027-03-15', DATE '2027-06-30', NULL, NULL, 'BCT/4', 'Amélioration', 'Bâtiment administratif BFM — Lac', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-04-04', 'BCT/4_4', 'Plan d''action de renforcement de la sûreté du centre de stockage', DATE '2027-04-15', DATE '2027-07-31', NULL, NULL, 'BCT/4', 'Amélioration', 'Centre de stockage BFM — Mornag', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-04-05', 'BCT/4_5', 'Évaluation et ajustement des plans d''action de réduction des risques', DATE '2027-06-01', DATE '2027-08-31', NULL, NULL, 'BCT/4', 'Amélioration', 'Siège BFM — Tunis', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),

-- BCT/5
('A-2027-05-01', 'BCT/5_1', 'Contrôle de l''application de la procédure de contrôle d''accès au siège', DATE '2027-01-10', DATE '2027-03-31', DATE '2027-01-15', DATE '2027-03-25', 'BCT/5', 'Inspection', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-05-02', 'BCT/5_2', 'Inspection de l''application de la procédure de ronde sur l''agence de Sousse', DATE '2027-02-01', DATE '2027-04-30', DATE '2027-02-10', NULL, 'BCT/5', 'Inspection', 'Agence BFM — Sousse', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-05-03', 'BCT/5_3', 'Audit de conformité des procédures de sécurité au site technique', DATE '2027-03-01', DATE '2027-05-31', NULL, NULL, 'BCT/5', 'Inspection', 'Site technique BFM — Ben Arous', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-05-04', 'BCT/5_4', 'Contrôle du respect des procédures de gestion des clés et badges', DATE '2027-04-01', DATE '2027-06-30', NULL, NULL, 'BCT/5', 'Inspection', 'Bâtiment administratif BFM — Lac', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-05-05', 'BCT/5_5', 'Proposition de mesures d''amélioration suite aux contrôles de procédures', DATE '2027-05-01', DATE '2027-07-31', NULL, NULL, 'BCT/5', 'Inspection', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),

-- BCT/6
('A-2027-06-01', 'BCT/6_1', 'Vérification du bon fonctionnement du système de vidéosurveillance du siège', DATE '2027-01-05', DATE '2027-02-28', DATE '2027-01-08', DATE '2027-02-22', 'BCT/6', 'Maintenance préventive', 'Siège BFM — Tunis', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-06-02', 'BCT/6_2', 'Contrôle de la disponibilité du système d''alarme de l''agence d''Ariana', DATE '2027-02-01', DATE '2027-03-31', DATE '2027-02-05', NULL, 'BCT/6', 'Maintenance préventive', 'Agence BFM — Ariana', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-06-03', 'BCT/6_3', 'Test de bon fonctionnement du contrôle d''accès du bâtiment administratif', DATE '2027-03-01', DATE '2027-04-30', NULL, NULL, 'BCT/6', 'Maintenance préventive', 'Bâtiment administratif BFM — Lac', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-06-04', 'BCT/6_4', 'Vérification de la disponibilité des systèmes de sécurité du centre de stockage', DATE '2027-04-01', DATE '2027-05-31', NULL, NULL, 'BCT/6', 'Maintenance préventive', 'Centre de stockage BFM — Mornag', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-06-05', 'BCT/6_5', 'Audit de disponibilité des systèmes de sécurité sur l''ensemble des sites', DATE '2027-05-01', DATE '2027-06-30', NULL, NULL, 'BCT/6', 'Maintenance préventive', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),

-- BCT/7
('A-2027-07-01', 'BCT/7_1', 'Maintenance préventive du système de vidéosurveillance du siège BFM', DATE '2027-01-15', DATE '2027-03-15', DATE '2027-01-20', DATE '2027-03-10', 'BCT/7', 'Maintenance préventive', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-07-02', 'BCT/7_2', 'Maintenance préventive des caméras de l''agence de Sfax', DATE '2027-02-01', DATE '2027-04-15', DATE '2027-02-10', NULL, 'BCT/7', 'Maintenance préventive', 'Agence BFM — Sfax', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-07-03', 'BCT/7_3', 'Maintenance préventive du système d''alarme du site technique', DATE '2027-03-01', DATE '2027-05-15', NULL, NULL, 'BCT/7', 'Maintenance préventive', 'Site technique BFM — Ben Arous', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-07-04', 'BCT/7_4', 'Maintenance préventive des lecteurs de badges du bâtiment administratif', DATE '2027-04-01', DATE '2027-06-15', NULL, NULL, 'BCT/7', 'Maintenance préventive', 'Bâtiment administratif BFM — Lac', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-07-05', 'BCT/7_5', 'Maintenance préventive annuelle des équipements de sécurité du centre de stockage', DATE '2027-05-01', DATE '2027-07-15', NULL, NULL, 'BCT/7', 'Maintenance préventive', 'Centre de stockage BFM — Mornag', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),

-- BCT/8
('A-2027-08-01', 'BCT/8_1', 'Intervention curative sur la caméra défaillante du hall principal', DATE '2027-01-20', DATE '2027-01-25', DATE '2027-01-20', DATE '2027-01-24', 'BCT/8', 'Maintenance curative', 'Siège BFM — Tunis', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-08-02', 'BCT/8_2', 'Réparation du lecteur de badge défectueux de l''entrée du personnel', DATE '2027-02-10', DATE '2027-02-15', DATE '2027-02-10', DATE '2027-02-14', 'BCT/8', 'Maintenance curative', 'Siège BFM — Tunis', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-08-03', 'BCT/8_3', 'Rétablissement du système d''alarme après panne au centre de stockage', DATE '2027-03-05', DATE '2027-03-12', NULL, NULL, 'BCT/8', 'Maintenance curative', 'Centre de stockage BFM — Mornag', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-08-04', 'BCT/8_4', 'Remplacement d''un équipement de contrôle d''accès défaillant à Sousse', DATE '2027-04-10', DATE '2027-04-20', NULL, NULL, 'BCT/8', 'Maintenance curative', 'Agence BFM — Sousse', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-08-05', 'BCT/8_5', 'Intervention curative sur le système de détection d''intrusion du site technique', DATE '2027-05-05', DATE '2027-05-15', NULL, NULL, 'BCT/8', 'Maintenance curative', 'Site technique BFM — Ben Arous', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),

-- BCT/9
('A-2027-09-01', 'BCT/9_1', 'Inventaire des équipements de sécurité du siège BFM', DATE '2027-01-10', DATE '2027-02-28', DATE '2027-01-15', DATE '2027-02-25', 'BCT/9', 'Inventaire', 'Siège BFM — Tunis', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-09-02', 'BCT/9_2', 'Suivi de l''évolution des installations de sécurité de l''agence d''Ariana', DATE '2027-02-01', DATE '2027-03-31', DATE '2027-02-10', NULL, 'BCT/9', 'Inventaire', 'Agence BFM — Ariana', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-09-03', 'BCT/9_3', 'Recensement des équipements de sécurité du bâtiment administratif', DATE '2027-03-01', DATE '2027-04-30', NULL, NULL, 'BCT/9', 'Inventaire', 'Bâtiment administratif BFM — Lac', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-09-04', 'BCT/9_4', 'Suivi des installations de sécurité du centre de stockage', DATE '2027-04-01', DATE '2027-05-31', NULL, NULL, 'BCT/9', 'Inventaire', 'Centre de stockage BFM — Mornag', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-09-05', 'BCT/9_5', 'Mise à jour du registre des équipements de sécurité de tous les sites', DATE '2027-05-01', DATE '2027-06-30', NULL, NULL, 'BCT/9', 'Inventaire', 'Siège BFM — Tunis', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),

-- BCT/10
('A-2027-10-01', 'BCT/10_1', 'Renforcement du système de vidéosurveillance du siège par ajout de caméras', DATE '2027-01-15', DATE '2027-04-30', DATE '2027-01-20', NULL, 'BCT/10', 'Amélioration', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-10-02', 'BCT/10_2', 'Amélioration du contrôle d''accès de l''agence de Sfax par technologie biométrique', DATE '2027-02-01', DATE '2027-05-31', DATE '2027-02-05', NULL, 'BCT/10', 'Amélioration', 'Agence BFM — Sfax', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-10-03', 'BCT/10_3', 'Mise à niveau du système d''alarme du site technique de Ben Arous', DATE '2027-03-01', DATE '2027-06-30', NULL, NULL, 'BCT/10', 'Amélioration', 'Site technique BFM — Ben Arous', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-10-04', 'BCT/10_4', 'Renforcement de la sûreté périmétrique du centre de stockage', DATE '2027-04-01', DATE '2027-07-31', NULL, NULL, 'BCT/10', 'Amélioration', 'Centre de stockage BFM — Mornag', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-10-05', 'BCT/10_5', 'Étude d''amélioration des systèmes de sécurité sur l''ensemble des sites', DATE '2027-06-01', DATE '2027-08-31', NULL, NULL, 'BCT/10', 'Amélioration', 'Siège BFM — Tunis', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS')
) AS v(code, reference, designation, dd, df, ddr, dfr, os_code, type_designation, site_nom, priorite_code, service_nom, procedure_designation);

-- =============================================================================
-- 7. ACTIVITÉS NON PTA (50 activités — id_objectif_specifique = NULL)
-- =============================================================================
-- code = A-<année>-<n° d'activité> (A-2027-01 .. A-2027-50)
-- reference = NULL (aucun objectif spécifique rattaché)
-- La numérotation continue après celle des activités PTA (A-2027-01-01 .. A-2027-10-05).
--
-- La procédure est la même que pour les activités PTA : une NON PTA n'est pas
-- rattachée à un objectif, mais elle passe bien par le circuit de validation.
-- Sans id_procedure, elle resterait orpheline du circuit, et le calcul de
-- l'étape suivante échouerait à la soumission (voir getPremiereEtape).

INSERT INTO activite (code, reference, designation, date_debut_prevue, date_fin_prevue, date_debut_reelle, date_fin_reelle, id_objectif_specifique, id_type_activite, id_site, id_priorite, id_service, id_procedure)
SELECT v.code, v.reference, v.designation, v.dd, v.df, v.ddr, v.dfr,
       NULL,
       (SELECT id_type_activite FROM type_activite WHERE designation = v.type_designation),
       (SELECT id_site FROM site WHERE nom = v.site_nom),
       (SELECT id_priorite FROM priorite WHERE code = v.priorite_code),
       (SELECT id_service FROM service WHERE nom = v.service_nom),
       (SELECT id_procedure FROM "procedure" WHERE designation = v.procedure_designation)
FROM (VALUES
('A-2027-01', NULL, 'Intervention urgente suite à une alarme intrusion déclenchée au siège', DATE '2027-01-05', DATE '2027-01-06', DATE '2027-01-05', DATE '2027-01-05', 'Intervention urgente', 'Siège BFM — Tunis', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-02', NULL, 'Remplacement d''une caméra défectueuse à l''entrée principale de l''agence d''Ariana', DATE '2027-01-12', DATE '2027-01-15', DATE '2027-01-12', DATE '2027-01-14', 'Maintenance curative', 'Agence BFM — Ariana', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-03', NULL, 'Dépannage du lecteur de badge de l''accès parking du siège', DATE '2027-01-18', DATE '2027-01-20', DATE '2027-01-18', DATE '2027-01-19', 'Maintenance curative', 'Siège BFM — Tunis', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-04', NULL, 'Inspection ponctuelle des issues de secours du bâtiment administratif', DATE '2027-01-25', DATE '2027-01-27', DATE '2027-01-25', DATE '2027-01-26', 'Inspection', 'Bâtiment administratif BFM — Lac', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-05', NULL, 'Vérification technique des extincteurs du centre de stockage', DATE '2027-02-01', DATE '2027-02-03', DATE '2027-02-01', DATE '2027-02-02', 'Inspection', 'Centre de stockage BFM — Mornag', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-06', NULL, 'Intervention après anomalie détectée sur le système d''alarme de Sfax', DATE '2027-02-05', DATE '2027-02-08', DATE '2027-02-05', DATE '2027-02-07', 'Maintenance curative', 'Agence BFM — Sfax', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-07', NULL, 'Sécurisation temporaire de l''accès au site technique suite à un incident', DATE '2027-02-10', DATE '2027-02-15', DATE '2027-02-10', DATE '2027-02-14', 'Intervention urgente', 'Site technique BFM — Ben Arous', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-08', NULL, 'Inventaire ponctuel des badges d''accès de l''agence de Sousse', DATE '2027-02-15', DATE '2027-02-17', DATE '2027-02-15', DATE '2027-02-16', 'Inventaire', 'Agence BFM — Sousse', 'FAIBLE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-09', NULL, 'Contrôle exceptionnel des caméras de la salle des coffres du siège', DATE '2027-02-20', DATE '2027-02-22', DATE '2027-02-20', DATE '2027-02-21', 'Inspection', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-10', NULL, 'Assistance technique pour la remise en service du portail automatique du siège', DATE '2027-02-25', DATE '2027-02-28', DATE '2027-02-25', DATE '2027-02-27', 'Maintenance curative', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-11', NULL, 'Intervention urgente suite à une coupure d''alimentation du système de sécurité', DATE '2027-03-01', DATE '2027-03-02', DATE '2027-03-01', DATE '2027-03-01', 'Intervention urgente', 'Siège BFM — Tunis', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-12', NULL, 'Remplacement d''un détecteur de mouvement défaillant à l''agence d''Ariana', DATE '2027-03-05', DATE '2027-03-08', DATE '2027-03-05', DATE '2027-03-07', 'Maintenance curative', 'Agence BFM — Ariana', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-13', NULL, 'Dépannage du système de vidéosurveillance du parking du bâtiment administratif', DATE '2027-03-10', DATE '2027-03-13', DATE '2027-03-10', DATE '2027-03-12', 'Maintenance curative', 'Bâtiment administratif BFM — Lac', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-14', NULL, 'Inspection ponctuelle des clôtures du centre de stockage', DATE '2027-03-15', DATE '2027-03-17', DATE '2027-03-15', DATE '2027-03-16', 'Inspection', 'Centre de stockage BFM — Mornag', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-15', NULL, 'Vérification technique du système d''extinction automatique du site technique', DATE '2027-03-20', DATE '2027-03-23', DATE '2027-03-20', DATE '2027-03-22', 'Inspection', 'Site technique BFM — Ben Arous', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-16', NULL, 'Intervention après détection d''un accès non autorisé à l''agence de Sfax', DATE '2027-03-25', DATE '2027-03-28', DATE '2027-03-25', DATE '2027-03-27', 'Intervention urgente', 'Agence BFM — Sfax', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-17', NULL, 'Sécurisation temporaire de l''entrée de service du siège', DATE '2027-04-01', DATE '2027-04-05', DATE '2027-04-01', DATE '2027-04-04', 'Intervention urgente', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-18', NULL, 'Inventaire ponctuel du matériel de sécurité de l''agence de Sousse', DATE '2027-04-05', DATE '2027-04-07', DATE '2027-04-05', DATE '2027-04-06', 'Inventaire', 'Agence BFM — Sousse', 'FAIBLE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-19', NULL, 'Contrôle exceptionnel des accès au centre de stockage', DATE '2027-04-10', DATE '2027-04-12', DATE '2027-04-10', DATE '2027-04-11', 'Inspection', 'Centre de stockage BFM — Mornag', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-20', NULL, 'Assistance technique pour la réparation du système d''alarme du siège', DATE '2027-04-15', DATE '2027-04-18', DATE '2027-04-15', DATE '2027-04-17', 'Maintenance curative', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-21', NULL, 'Intervention urgente suite à un dysfonctionnement du contrôle d''accès', DATE '2027-04-20', DATE '2027-04-22', DATE '2027-04-20', DATE '2027-04-21', 'Intervention urgente', 'Agence BFM — Ariana', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-22', NULL, 'Remplacement d''un câble défectueux du système de vidéosurveillance', DATE '2027-04-25', DATE '2027-04-28', DATE '2027-04-25', DATE '2027-04-27', 'Maintenance curative', 'Agence BFM — Sfax', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-23', NULL, 'Dépannage du système de détection incendie du bâtiment administratif', DATE '2027-05-01', DATE '2027-05-04', DATE '2027-05-01', DATE '2027-05-03', 'Maintenance curative', 'Bâtiment administratif BFM — Lac', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-24', NULL, 'Inspection ponctuelle des éclairages de sécurité du site technique', DATE '2027-05-05', DATE '2027-05-07', DATE '2027-05-05', DATE '2027-05-06', 'Inspection', 'Site technique BFM — Ben Arous', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-25', NULL, 'Vérification technique des portails automatiques du centre de stockage', DATE '2027-05-10', DATE '2027-05-12', DATE '2027-05-10', DATE '2027-05-11', 'Inspection', 'Centre de stockage BFM — Mornag', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-26', NULL, 'Intervention après anomalie sur le système de contrôle d''accès de Sousse', DATE '2027-05-15', DATE '2027-05-18', DATE '2027-05-15', DATE '2027-05-17', 'Maintenance curative', 'Agence BFM — Sousse', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-27', NULL, 'Sécurisation temporaire du parking du siège suite à un incident', DATE '2027-05-20', DATE '2027-05-24', DATE '2027-05-20', DATE '2027-05-23', 'Intervention urgente', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-28', NULL, 'Inventaire ponctuel des équipements de sécurité de l''agence d''Ariana', DATE '2027-05-25', DATE '2027-05-27', DATE '2027-05-25', DATE '2027-05-26', 'Inventaire', 'Agence BFM — Ariana', 'FAIBLE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-29', NULL, 'Contrôle exceptionnel des caméras du site technique', DATE '2027-06-01', DATE '2027-06-03', DATE '2027-06-01', DATE '2027-06-02', 'Inspection', 'Site technique BFM — Ben Arous', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-30', NULL, 'Assistance technique pour la remise en service du système d''alarme de Sfax', DATE '2027-06-05', DATE '2027-06-08', DATE '2027-06-05', DATE '2027-06-07', 'Maintenance curative', 'Agence BFM — Sfax', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-31', NULL, 'Intervention urgente suite à une panne du système de vidéosurveillance', DATE '2027-06-10', DATE '2027-06-12', DATE '2027-06-10', DATE '2027-06-11', 'Intervention urgente', 'Siège BFM — Tunis', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-32', NULL, 'Remplacement d''un module de communication défaillant du site technique', DATE '2027-06-15', DATE '2027-06-18', NULL, NULL, 'Maintenance curative', 'Site technique BFM — Ben Arous', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-33', NULL, 'Dépannage du système de sonorisation d''évacuation du bâtiment administratif', DATE '2027-06-20', DATE '2027-06-23', NULL, NULL, 'Maintenance curative', 'Bâtiment administratif BFM — Lac', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-34', NULL, 'Inspection ponctuelle des dispositifs anti-intrusion du centre de stockage', DATE '2027-06-25', DATE '2027-06-27', NULL, NULL, 'Inspection', 'Centre de stockage BFM — Mornag', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-35', NULL, 'Vérification technique des systèmes de sécurité de l''agence de Sousse', DATE '2027-07-01', DATE '2027-07-03', NULL, NULL, 'Inspection', 'Agence BFM — Sousse', 'NORMALE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-36', NULL, 'Intervention après détection d''une anomalie sur le réseau de caméras', DATE '2027-07-05', DATE '2027-07-08', NULL, NULL, 'Maintenance curative', 'Agence BFM — Ariana', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-37', NULL, 'Sécurisation temporaire de l''accès principal de l''agence de Sfax', DATE '2027-07-10', DATE '2027-07-14', NULL, NULL, 'Intervention urgente', 'Agence BFM — Sfax', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-38', NULL, 'Inventaire ponctuel des clés et badges du siège', DATE '2027-07-15', DATE '2027-07-17', NULL, NULL, 'Inventaire', 'Siège BFM — Tunis', 'FAIBLE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-39', NULL, 'Contrôle exceptionnel du système d''alarme du bâtiment administratif', DATE '2027-07-20', DATE '2027-07-22', NULL, NULL, 'Inspection', 'Bâtiment administratif BFM — Lac', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-40', NULL, 'Assistance technique pour le remplacement d''un lecteur de badge', DATE '2027-07-25', DATE '2027-07-28', NULL, NULL, 'Maintenance curative', 'Siège BFM — Tunis', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-41', NULL, 'Intervention urgente suite à un déclenchement intempestif d''alarme', DATE '2027-08-01', DATE '2027-08-02', NULL, NULL, 'Intervention urgente', 'Siège BFM — Tunis', 'CRITIQUE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-42', NULL, 'Remplacement d''une caméra endommagée au centre de stockage', DATE '2027-08-05', DATE '2027-08-08', NULL, NULL, 'Maintenance curative', 'Centre de stockage BFM — Mornag', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-43', NULL, 'Dépannage du système de contrôle d''accès du site technique', DATE '2027-08-10', DATE '2027-08-13', NULL, NULL, 'Maintenance curative', 'Site technique BFM — Ben Arous', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-44', NULL, 'Inspection ponctuelle des caméras de l''agence de Sousse', DATE '2027-08-15', DATE '2027-08-17', NULL, NULL, 'Inspection', 'Agence BFM — Sousse', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-45', NULL, 'Vérification technique des alarmes anti-intrusion de Sfax', DATE '2027-08-20', DATE '2027-08-22', NULL, NULL, 'Inspection', 'Agence BFM — Sfax', 'NORMALE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-46', NULL, 'Intervention après anomalie sur le système de détection incendie', DATE '2027-08-25', DATE '2027-08-28', NULL, NULL, 'Maintenance curative', 'Bâtiment administratif BFM — Lac', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-47', NULL, 'Sécurisation temporaire du centre de stockage après incident', DATE '2027-09-01', DATE '2027-09-05', NULL, NULL, 'Intervention urgente', 'Centre de stockage BFM — Mornag', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS'),
('A-2027-48', NULL, 'Inventaire ponctuel des équipements de sécurité du site technique', DATE '2027-09-05', DATE '2027-09-07', NULL, NULL, 'Inventaire', 'Site technique BFM — Ben Arous', 'FAIBLE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-49', NULL, 'Contrôle exceptionnel des accès au bâtiment administratif', DATE '2027-09-10', DATE '2027-09-12', NULL, NULL, 'Inspection', 'Bâtiment administratif BFM — Lac', 'HAUTE', 'Gestion des Risques et Procédures', 'Procédure de validation des activités TSS'),
('A-2027-50', NULL, 'Assistance technique pour la réparation du système de vidéosurveillance de Sousse', DATE '2027-09-15', DATE '2027-09-18', NULL, NULL, 'Maintenance curative', 'Agence BFM — Sousse', 'HAUTE', 'Gestion des Systèmes de Sécurité', 'Procédure de validation des activités TSS')
) AS v(code, reference, designation, dd, df, ddr, dfr, type_designation, site_nom, priorite_code, service_nom, procedure_designation);

-- =============================================================================
-- 8. RÉSULTATS INTERMÉDIAIRES
-- =============================================================================
-- Un résultat intermédiaire pour un sous-ensemble d'activités (pertinentes)

INSERT INTO resultat_intermediaire (designation, id_activite)
SELECT v.designation, a.id_activite
FROM (VALUES
-- A-2027-01-01
('État des lieux des risques du siège établi', 'A-2027-01-01'),
('Cartographie préliminaire des risques réalisée', 'A-2027-01-01'),
-- A-2027-01-02
('Risques liés aux accès non autorisés identifiés', 'A-2027-01-02'),
-- A-2027-02-01
('Version actualisée de la procédure de contrôle d''accès rédigée', 'A-2027-02-01'),
-- A-2027-02-02
('Trame de la procédure de gestion des incidents validée', 'A-2027-02-02'),
-- A-2027-03-01
('Tableau de suivi trimestriel des risques mis à jour', 'A-2027-03-01'),
-- A-2027-04-01
('Plan d''action de réduction des risques d''intrusion élaboré', 'A-2027-04-01'),
-- A-2027-05-01
('Rapport de contrôle de l''application de la procédure d''accès établi', 'A-2027-05-01'),
-- A-2027-06-01
('État de fonctionnement du système de vidéosurveillance vérifié', 'A-2027-06-01'),
-- A-2027-07-01
('Rapport de maintenance préventive du système de vidéosurveillance établi', 'A-2027-07-01'),
-- A-2027-08-01
('Caméra défaillante remise en service', 'A-2027-08-01'),
-- A-2027-09-01
('Inventaire des équipements de sécurité du siège actualisé', 'A-2027-09-01'),
-- A-2027-10-01
('Plan de renforcement de la vidéosurveillance validé', 'A-2027-10-01'),
-- A-2027-01
('Incident d''intrusion qualifié et rapport établi', 'A-2027-01'),
-- A-2027-02
('Caméra remplacée et testée', 'A-2027-02'),
-- A-2027-06
('Anomalie du système d''alarme identifiée et corrigée', 'A-2027-06'),
-- A-2027-11
('Alimentation du système de sécurité rétablie', 'A-2027-11'),
-- A-2027-16
('Accès non autorisé documenté et mesures prises', 'A-2027-16'),
-- A-2027-31
('Système de vidéosurveillance remis en service', 'A-2027-31')
) AS v(designation, code_activite)
JOIN activite a ON a.code = v.code_activite;

-- =============================================================================
-- 9. SOUS-ACTIVITÉS (3 à 5 par activité)
-- =============================================================================
-- code = SA-<année>-<n° d'activité>-<n° de la sous-activité>
--   ex. A-2027-01-01 -> SA-2027-01-01, SA-2027-01-02, SA-2027-01-03
-- Le n° d'activité est GLOBAL (ordre de id_activite) et non remis à zéro
-- par objectif, sans quoi deux objectifs produiraient le même code.
-- Génération robuste : la date de fin réelle est TOUJOURS >= date de début réelle.
-- On borne la progression pour ne jamais dépasser la fenêtre de l'activité.

WITH activite_numero AS (
    SELECT a.id_activite,
           ROW_NUMBER() OVER (ORDER BY a.id_activite) AS numero_activite
    FROM activite a
),
activite_numerotee AS (
    SELECT an.id_activite,
           an.numero_activite,
           -- LPAD tronque à la longueur demandée : on n'élargit que si nécessaire,
           -- sinon le n° 100 deviendrait « 10 » et entrerait en collision.
           LPAD(an.numero_activite::text,
                GREATEST(2, length(an.numero_activite::text)),
                '0') AS numero_texte
    FROM activite_numero an
)
INSERT INTO sous_activite (code, designation, date_debut_prevue, date_fin_prevue, date_debut_reelle, date_fin_reelle, id_activite)
SELECT
    'SA-2027-' || an.numero_texte || '-' || LPAD(gs.n::text, 2, '0'),
    CASE gs.n
        WHEN 1 THEN 'Préparation et cadrage : ' || a.designation
        WHEN 2 THEN 'Réalisation technique : ' || a.designation
        WHEN 3 THEN 'Contrôle et vérification : ' || a.designation
        WHEN 4 THEN 'Documentation et reporting : ' || a.designation
        WHEN 5 THEN 'Validation et clôture : ' || a.designation
    END,
    -- date_debut_prevue : début de l'activité + décalage
    a.date_debut_prevue + ((gs.n - 1) * 3),
    -- date_fin_prevue : bornée par la fin prévue de l'activité,
    -- mais TOUJOURS >= début prévu de la sous-activité
    GREATEST(
        a.date_debut_prevue + ((gs.n - 1) * 3),
        LEAST(
            a.date_debut_prevue + ((gs.n - 1) * 3) + 7,
            COALESCE(a.date_fin_prevue, a.date_debut_prevue + 60)
        )
    ),
    -- date_debut_reelle : décalage depuis le début réel
    CASE WHEN a.date_debut_reelle IS NOT NULL
         THEN a.date_debut_reelle + ((gs.n - 1) * 2)
         ELSE NULL END,
    -- date_fin_reelle : TOUJOURS >= date_debut_reelle de la sous-activité
    CASE
        WHEN a.date_fin_reelle IS NOT NULL THEN
            -- fin réelle de la sous-activité = min(fin réelle activité, début réel + (n-1)*2 + 2)
            -- mais TOUJOURS >= début réel de la sous-activité
            GREATEST(
                a.date_debut_reelle + ((gs.n - 1) * 2),
                LEAST(
                    a.date_fin_reelle,
                    a.date_debut_reelle + ((gs.n - 1) * 2) + 2
                )
            )
        ELSE NULL
    END,
    a.id_activite
FROM activite_numerotee an
JOIN activite a ON a.id_activite = an.id_activite
CROSS JOIN LATERAL generate_series(1,
    CASE (an.id_activite % 3)
        WHEN 0 THEN 5
        WHEN 1 THEN 4
        ELSE 3
    END
) AS gs(n);

-- =============================================================================
-- 10. LIVRABLES (au moins un par sous-activité)
-- =============================================================================

INSERT INTO livrable_sous_activite (designation, description, id_sous_activite)
SELECT
    'Livrable — ' || sa.designation,
    'Livrable associé à la sous-activité : ' || sa.designation,
    sa.id_sous_activite
FROM sous_activite sa;

-- =============================================================================
-- 11. FICHIERS (au moins un par livrable)
-- =============================================================================

INSERT INTO fichier_sous_activite (nom_fichier, nom_original, chemin_fichier, version, extension, type_mime, taille, date_depot, id_utilisateur, id_livrable_sous_activite)
SELECT
    'livrable_' || l.id_livrable_sous_activite || '_v1.pdf',
    'Livrable ' || l.id_livrable_sous_activite || ' — version 1.pdf',
    '/data/saga/livrables/' || l.id_livrable_sous_activite || '/livrable_v1.pdf',
    1,
    'pdf',
    'application/pdf',
    150000 + (l.id_livrable_sous_activite * 137),
    '2027-01-15 08:00:00'::timestamp + ((l.id_livrable_sous_activite % 200) || ' days')::interval,
    -- Depot par un membre de service (rotation sur les emails, jamais sur les ids)
    (SELECT id_utilisateur FROM utilisateur
     WHERE email IN ('sonia.gharbi@bfm.tn', 'mohamed.khelifi@bfm.tn',
                     'nadia.bouazizi@bfm.tn', 'rami.jlassi@bfm.tn',
                     'slim.ayari@bfm.tn', 'anis.mejri@bfm.tn',
                     'ines.ferchichi@bfm.tn', 'yassine.zaidi@bfm.tn',
                     'mariem.hamdi@bfm.tn', 'walid.sassi@bfm.tn')
     ORDER BY email
     LIMIT 1 OFFSET (l.id_livrable_sous_activite % 10)),
    l.id_livrable_sous_activite
FROM livrable_sous_activite l;

-- =============================================================================
-- 12. INDICATEURS (au moins un par activité)
-- =============================================================================
-- Chaque activité reçoit au moins un indicateur. Certaines activités (1 sur 3)
-- en reçoivent un second pour plus de réalisme.

-- 12.1 Indicateurs principaux (un par activité)
INSERT INTO indicateur (code, code_hopex, indicateur_hopex, actif, unite_mesure, frequence_verification, frequence_aggregation, definition, methode_determination, objectif, valeur_cible, seuil_min, seuil_max, designation, type_indicateur)
SELECT
    'IND-' || LPAD(a.id_activite::text, 4, '0'),
    NULL, NULL, TRUE,
    CASE (a.id_activite % 4)
        WHEN 0 THEN '%'
        WHEN 1 THEN 'nombre'
        WHEN 2 THEN 'taux'
        ELSE 'jours'
    END,
    CASE (a.id_activite % 3)
        WHEN 0 THEN 'Mensuelle'
        WHEN 1 THEN 'Trimestrielle'
        ELSE 'Semestrielle'
    END,
    CASE (a.id_activite % 2)
        WHEN 0 THEN 'Trimestrielle'
        ELSE 'Annuelle'
    END,
    'Indicateur de suivi de l''activité : ' || a.designation,
    'Calcul basé sur les données de suivi de l''activité ' || a.code,
    'Atteindre la cible définie pour l''activité ' || a.code,
    CASE (a.id_activite % 4)
        WHEN 0 THEN 90.00
        WHEN 1 THEN 10.00
        WHEN 2 THEN 95.00
        ELSE 30.00
    END,
    CASE (a.id_activite % 4)
        WHEN 0 THEN 70.00
        WHEN 1 THEN 0.00
        WHEN 2 THEN 80.00
        ELSE 0.00
    END,
    CASE (a.id_activite % 4)
        WHEN 0 THEN 100.00
        WHEN 1 THEN 50.00
        WHEN 2 THEN 100.00
        ELSE 90.00
    END,
    'Indicateur principal — ' || a.designation,
    CASE (a.id_activite % 3)
        WHEN 0 THEN 'Quantitatif'
        WHEN 1 THEN 'Qualitatif'
        ELSE 'Mixte'
    END
FROM activite a;

-- 12.2 Indicateurs secondaires (une activité sur trois)
INSERT INTO indicateur (code, code_hopex, indicateur_hopex, actif, unite_mesure, frequence_verification, frequence_aggregation, definition, methode_determination, objectif, valeur_cible, seuil_min, seuil_max, designation, type_indicateur)
SELECT
    'IND-' || LPAD(a.id_activite::text, 4, '0') || '-B',
    NULL, NULL, TRUE,
    CASE (a.id_activite % 3)
        WHEN 0 THEN 'nombre'
        WHEN 1 THEN '%'
        ELSE 'jours'
    END,
    'Mensuelle', 'Trimestrielle',
    'Indicateur secondaire de suivi de l''activité : ' || a.designation,
    'Calcul complémentaire basé sur les données de l''activité ' || a.code,
    'Compléter le suivi de l''activité ' || a.code,
    20.00, 0.00, 100.00,
    'Indicateur secondaire — ' || a.designation,
    'Quantitatif'
FROM activite a
WHERE (a.id_activite % 3) = 0;

-- =============================================================================
-- 13. ACTIVITE_INDICATEUR (liaison activité ↔ indicateur)
-- =============================================================================

-- 13.1 Liaison avec les indicateurs principaux
INSERT INTO activite_indicateur (id_activite, id_indicateur)
SELECT a.id_activite, i.id_indicateur
FROM activite a
JOIN indicateur i ON i.code = 'IND-' || LPAD(a.id_activite::text, 4, '0');

-- 13.2 Liaison avec les indicateurs secondaires
INSERT INTO activite_indicateur (id_activite, id_indicateur)
SELECT a.id_activite, i.id_indicateur
FROM activite a
JOIN indicateur i ON i.code = 'IND-' || LPAD(a.id_activite::text, 4, '0') || '-B'
WHERE (a.id_activite % 3) = 0;

-- =============================================================================
-- 14. VALEURS D'INDICATEURS (pour un sous-ensemble d'indicateurs)
-- =============================================================================

INSERT INTO valeur_indicateur (valeur, periode_debut, periode_fin, commentaire, date_saisie, id_utilisateur, id_indicateur)
SELECT
    CASE (i.id_indicateur % 4)
        WHEN 0 THEN 85.50
        WHEN 1 THEN 12.00
        WHEN 2 THEN 92.00
        ELSE 28.00
    END,
    '2027-01-01 00:00:00'::timestamp,
    '2027-03-31 23:59:59'::timestamp,
    'Valeur du premier trimestre 2027 pour l''indicateur ' || i.code,
    '2027-04-05 10:00:00'::timestamp,
    (SELECT id_utilisateur FROM utilisateur
     WHERE email IN ('sonia.gharbi@bfm.tn', 'mohamed.khelifi@bfm.tn',
                     'nadia.bouazizi@bfm.tn', 'rami.jlassi@bfm.tn',
                     'slim.ayari@bfm.tn', 'anis.mejri@bfm.tn',
                     'ines.ferchichi@bfm.tn', 'yassine.zaidi@bfm.tn',
                     'mariem.hamdi@bfm.tn', 'walid.sassi@bfm.tn')
     ORDER BY email
     LIMIT 1 OFFSET (i.id_indicateur % 10)),
    i.id_indicateur
FROM indicateur i
WHERE (i.id_indicateur % 5) = 0;

INSERT INTO valeur_indicateur (valeur, periode_debut, periode_fin, commentaire, date_saisie, id_utilisateur, id_indicateur)
SELECT
    CASE (i.id_indicateur % 4)
        WHEN 0 THEN 88.00
        WHEN 1 THEN 15.00
        WHEN 2 THEN 94.00
        ELSE 25.00
    END,
    '2027-04-01 00:00:00'::timestamp,
    '2027-06-30 23:59:59'::timestamp,
    'Valeur du deuxième trimestre 2027 pour l''indicateur ' || i.code,
    '2027-07-05 10:00:00'::timestamp,
    (SELECT id_utilisateur FROM utilisateur
     WHERE email IN ('sonia.gharbi@bfm.tn', 'mohamed.khelifi@bfm.tn',
                     'nadia.bouazizi@bfm.tn', 'rami.jlassi@bfm.tn',
                     'slim.ayari@bfm.tn', 'anis.mejri@bfm.tn',
                     'ines.ferchichi@bfm.tn', 'yassine.zaidi@bfm.tn',
                     'mariem.hamdi@bfm.tn', 'walid.sassi@bfm.tn')
     ORDER BY email
     LIMIT 1 OFFSET (i.id_indicateur % 10)),
    i.id_indicateur
FROM indicateur i
WHERE (i.id_indicateur % 5) = 0;

-- =============================================================================
-- 15. AFFECTATIONS DES SOUS-ACTIVITÉS
-- =============================================================================
-- Chaque sous-activité reçoit un RESPONSABLE, et parfois un SUPPLEANT
-- et/ou un PARTICIPANT.

-- 15.1 Responsables (toutes les sous-activités)
INSERT INTO affectation_sous_activite (date_affectation, date_desaffectation, id_role, id_sous_activite, id_utilisateur)
SELECT
    '2027-01-05 08:00:00'::timestamp,
    NULL,
    (SELECT id_role FROM role WHERE code = 'RESPONSABLE'),
    sa.id_sous_activite,
    (SELECT id_utilisateur FROM utilisateur
     WHERE email IN ('sonia.gharbi@bfm.tn', 'mohamed.khelifi@bfm.tn',
                     'nadia.bouazizi@bfm.tn', 'rami.jlassi@bfm.tn',
                     'slim.ayari@bfm.tn', 'anis.mejri@bfm.tn',
                     'ines.ferchichi@bfm.tn', 'yassine.zaidi@bfm.tn',
                     'mariem.hamdi@bfm.tn', 'walid.sassi@bfm.tn')
     ORDER BY email
     LIMIT 1 OFFSET (sa.id_sous_activite % 10))
FROM sous_activite sa;

-- 15.2 Suppléants (une sous-activité sur deux)
INSERT INTO affectation_sous_activite (date_affectation, date_desaffectation, id_role, id_sous_activite, id_utilisateur)
SELECT
    '2027-01-05 08:00:00'::timestamp,
    NULL,
    (SELECT id_role FROM role WHERE code = 'SUPPLEANT'),
    sa.id_sous_activite,
    (SELECT id_utilisateur FROM utilisateur
     WHERE email IN ('sonia.gharbi@bfm.tn', 'mohamed.khelifi@bfm.tn',
                     'nadia.bouazizi@bfm.tn', 'rami.jlassi@bfm.tn',
                     'slim.ayari@bfm.tn', 'anis.mejri@bfm.tn',
                     'ines.ferchichi@bfm.tn', 'yassine.zaidi@bfm.tn',
                     'mariem.hamdi@bfm.tn', 'walid.sassi@bfm.tn')
     ORDER BY email
     LIMIT 1 OFFSET (sa.id_sous_activite % 9))
FROM sous_activite sa
WHERE (sa.id_sous_activite % 2) = 0;

-- 15.3 Participants (une sous-activité sur trois)
INSERT INTO affectation_sous_activite (date_affectation, date_desaffectation, id_role, id_sous_activite, id_utilisateur)
SELECT
    '2027-01-10 08:00:00'::timestamp,
    NULL,
    (SELECT id_role FROM role WHERE code = 'PARTICIPANT'),
    sa.id_sous_activite,
    (SELECT id_utilisateur FROM utilisateur
     WHERE email IN ('sonia.gharbi@bfm.tn', 'mohamed.khelifi@bfm.tn',
                     'nadia.bouazizi@bfm.tn', 'rami.jlassi@bfm.tn',
                     'slim.ayari@bfm.tn', 'anis.mejri@bfm.tn',
                     'ines.ferchichi@bfm.tn', 'yassine.zaidi@bfm.tn',
                     'mariem.hamdi@bfm.tn', 'walid.sassi@bfm.tn')
     ORDER BY email
     LIMIT 1 OFFSET (sa.id_sous_activite % 7))
FROM sous_activite sa
WHERE (sa.id_sous_activite % 3) = 0;

-- =============================================================================
-- 16. HISTORIQUE DES ACTIVITÉS
-- =============================================================================
-- Chaque activité reçoit un historique initial cohérent avec son état.
-- Les statuts sont référencés par leur code.

-- 16.1 Ligne initiale (BROUILLON ou EN_ATTENTE_VALIDATION)
INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Création de l''activité ' || a.code,
    '2027-01-02 08:00:00'::timestamp,
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_statut FROM statut WHERE code = 'BROUILLON'),
    a.id_activite
FROM activite a;

-- 16.2 Statut suivant selon l'état réel de l'activité
-- Activités terminées → VALIDEE puis TERMINEE
INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Validation de l''activité ' || a.code,
    a.date_debut_reelle - INTERVAL '2 days',
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_statut FROM statut WHERE code = 'VALIDEE'),
    a.id_activite
FROM activite a
WHERE a.date_debut_reelle IS NOT NULL;

INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Activité ' || a.code || ' démarrée',
    a.date_debut_reelle,
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_statut FROM statut WHERE code = 'EN_COURS'),
    a.id_activite
FROM activite a
WHERE a.date_debut_reelle IS NOT NULL;

INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Activité ' || a.code || ' terminée',
    a.date_fin_reelle,
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_statut FROM statut WHERE code = 'TERMINEE'),
    a.id_activite
FROM activite a
WHERE a.date_fin_reelle IS NOT NULL;

-- Activités en cours (date_debut_reelle NOT NULL, date_fin_reelle NULL)
INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Validation de l''activité ' || a.code,
    a.date_debut_reelle - INTERVAL '2 days',
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_statut FROM statut WHERE code = 'VALIDEE'),
    a.id_activite
FROM activite a
WHERE a.date_debut_reelle IS NOT NULL AND a.date_fin_reelle IS NULL;

INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Activité ' || a.code || ' en cours de réalisation',
    a.date_debut_reelle,
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_statut FROM statut WHERE code = 'EN_COURS'),
    a.id_activite
FROM activite a
WHERE a.date_debut_reelle IS NOT NULL AND a.date_fin_reelle IS NULL;

-- Activités non commencées (date_debut_reelle NULL)
INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Validation de l''activité ' || a.code,
    '2027-01-05 08:00:00'::timestamp,
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_statut FROM statut WHERE code = 'VALIDEE'),
    a.id_activite
FROM activite a
WHERE a.date_debut_reelle IS NULL;

INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Activité ' || a.code || ' non commencée',
    '2027-01-06 08:00:00'::timestamp,
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
     (SELECT id_statut FROM statut WHERE code = 'NON_COMMENCEE'),
     a.id_activite
FROM activite a
WHERE a.date_debut_reelle IS NULL;

-- 16.4 Activités soumises et en attente de décision
-- Ces activités portent une demande EN_ATTENTE_VALIDATION en 23.3 : sans cette
-- ligne d'historique, leur statut courant resterait NON_COMMENCEE et la page
-- des validateurs ne les verrait pas, alors que le circuit les attend. Les deux
-- écritures sont donc posees ensemble, à la même date.
INSERT INTO historique_activite (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
SELECT
    'Soumission à la validation de ' || a.code,
    '2027-03-01 08:00:00'::timestamp,
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_statut FROM statut WHERE code = 'EN_ATTENTE_VALIDATION'),
    a.id_activite
FROM activite a
WHERE a.code IN ('A-2027-01-03', 'A-2027-02-03', 'A-2027-03-03', 'A-2027-04-03', 'A-2027-05-03',
                 'A-2027-06-03', 'A-2027-07-03', 'A-2027-08-03', 'A-2027-09-03', 'A-2027-10-03');

-- =============================================================================
-- 17. AVANCEMENT DES SOUS-ACTIVITÉS
-- =============================================================================
-- Chaque sous-activité reçoit un avancement initial (0%, 50% ou 100%)
-- selon l'état de l'activité parente.

INSERT INTO avancement_sous_activite (valeur_pourcentage, commentaire, date_changement, id_utilisateur, id_sous_activite, id_statut)
SELECT
    CASE
        WHEN sa.date_fin_reelle IS NOT NULL THEN 100.00
        WHEN sa.date_debut_reelle IS NOT NULL THEN 50.00
        ELSE 0.00
    END,
    CASE
        WHEN sa.date_fin_reelle IS NOT NULL THEN 'Sous-activité terminée'
        WHEN sa.date_debut_reelle IS NOT NULL THEN 'Sous-activité en cours'
        ELSE 'Sous-activité non commencée'
    END,
    COALESCE(sa.date_debut_reelle, '2027-01-05 08:00:00'::timestamp),
    (SELECT id_utilisateur FROM utilisateur
     WHERE email IN ('sonia.gharbi@bfm.tn', 'mohamed.khelifi@bfm.tn',
                     'nadia.bouazizi@bfm.tn', 'rami.jlassi@bfm.tn',
                     'slim.ayari@bfm.tn', 'anis.mejri@bfm.tn',
                     'ines.ferchichi@bfm.tn', 'yassine.zaidi@bfm.tn',
                     'mariem.hamdi@bfm.tn', 'walid.sassi@bfm.tn')
     ORDER BY email
     LIMIT 1 OFFSET (sa.id_sous_activite % 10)),
    sa.id_sous_activite,
    CASE
        WHEN sa.date_fin_reelle IS NOT NULL THEN (SELECT id_statut FROM statut WHERE code = 'TERMINEE')
        WHEN sa.date_debut_reelle IS NOT NULL THEN (SELECT id_statut FROM statut WHERE code = 'EN_COURS')
        ELSE (SELECT id_statut FROM statut WHERE code = 'NON_COMMENCEE')
    END
FROM sous_activite sa;

-- =============================================================================
-- 18. ORIGINES — 10 RISQUES ET 10 INCIDENTS
-- =============================================================================

-- 18.1 Risques (type_origine = 'RISQUE')
INSERT INTO origine (code, designation, type_origine, description, date_creation, id_utilisateur)
SELECT v.code, v.designation, v.type_origine, v.description, v.date_creation::timestamp, u.id_utilisateur
FROM (VALUES
('RSQ-001', 'Risque de défaillance du système de vidéosurveillance', 'RISQUE', 'Risque de panne ou de dysfonctionnement des caméras et du système d''enregistrement vidéo.', '2027-01-10 08:00:00', 'sonia.gharbi@bfm.tn'),
('RSQ-002', 'Risque d''accès non autorisé', 'RISQUE', 'Risque d''intrusion ou d''accès non autorisé dans les locaux ou zones sensibles.', '2027-01-10 08:00:00', 'sonia.gharbi@bfm.tn'),
('RSQ-003', 'Risque de panne du système d''alarme', 'RISQUE', 'Risque de défaillance du système d''alarme anti-intrusion.', '2027-01-10 08:00:00', 'mohamed.khelifi@bfm.tn'),
('RSQ-004', 'Risque de défaillance du contrôle d''accès', 'RISQUE', 'Risque de panne des lecteurs de badges, portails et dispositifs de contrôle d''accès.', '2027-01-10 08:00:00', 'mohamed.khelifi@bfm.tn'),
('RSQ-005', 'Risque de dégradation des équipements de sécurité', 'RISQUE', 'Risque de détérioration physique des équipements de sûreté et de sécurité.', '2027-01-10 08:00:00', 'sonia.gharbi@bfm.tn'),
('RSQ-006', 'Risque de non-conformité des procédures de sûreté', 'RISQUE', 'Risque que les procédures ne soient pas conformes aux normes en vigueur.', '2027-01-10 08:00:00', 'nadia.bouazizi@bfm.tn'),
('RSQ-007', 'Risque de défaut de maintenance préventive', 'RISQUE', 'Risque que la maintenance préventive ne soit pas réalisée dans les délais.', '2027-01-10 08:00:00', 'sonia.gharbi@bfm.tn'),
('RSQ-008', 'Risque de perte de communication avec les équipements de sécurité', 'RISQUE', 'Risque de perte de liaison réseau avec les équipements de sécurité.', '2027-01-10 08:00:00', 'mohamed.khelifi@bfm.tn'),
('RSQ-009', 'Risque de défaillance du système de détection incendie', 'RISQUE', 'Risque de panne du système de détection et d''extinction incendie.', '2027-01-10 08:00:00', 'sonia.gharbi@bfm.tn'),
('RSQ-010', 'Risque de vulnérabilité périmétrique des sites', 'RISQUE', 'Risque lié à la faiblesse des clôtures et dispositifs périmétriques.', '2027-01-10 08:00:00', 'mohamed.khelifi@bfm.tn')
) AS v(code, designation, type_origine, description, date_creation, email)
JOIN utilisateur u ON u.email = v.email;

-- 18.2 Incidents (type_origine = 'INCIDENT')
INSERT INTO origine (code, designation, type_origine, description, date_creation, id_utilisateur)
SELECT v.code, v.designation, v.type_origine, v.description, v.date_creation::timestamp, u.id_utilisateur
FROM (VALUES
('INC-001', 'Panne d''une caméra de surveillance au hall principal', 'INCIDENT', 'La caméra du hall principal du siège a cessé de fonctionner le 20 janvier 2027.', '2027-01-20 09:00:00', 'slim.ayari@bfm.tn'),
('INC-002', 'Déclenchement intempestif d''une alarme au siège', 'INCIDENT', 'Le système d''alarme du siège s''est déclenché sans raison apparente le 5 janvier 2027.', '2027-01-05 22:00:00', 'slim.ayari@bfm.tn'),
('INC-003', 'Défaillance d''un lecteur de badge à l''entrée du personnel', 'INCIDENT', 'Le lecteur de badge de l''entrée du personnel du siège est tombé en panne le 10 février 2027.', '2027-02-10 07:30:00', 'anis.mejri@bfm.tn'),
('INC-004', 'Perte de communication avec un équipement de sécurité au site technique', 'INCIDENT', 'Perte de communication avec le détecteur d''intrusion du site technique le 5 mars 2027.', '2027-03-05 14:00:00', 'slim.ayari@bfm.tn'),
('INC-005', 'Détection d''un accès non autorisé à l''agence de Sfax', 'INCIDENT', 'Une personne non autorisée a tenté d''accéder à la zone sécurisée de l''agence de Sfax le 25 mars 2027.', '2027-03-25 10:15:00', 'slim.ayari@bfm.tn'),
('INC-006', 'Coupure d''alimentation du système de sécurité du siège', 'INCIDENT', 'Le système de sécurité du siège a été privé d''alimentation électrique le 1er mars 2027.', '2027-03-01 03:00:00', 'slim.ayari@bfm.tn'),
('INC-007', 'Dysfonctionnement du contrôle d''accès de l''agence d''Ariana', 'INCIDENT', 'Le portail automatique de l''agence d''Ariana est resté bloqué le 20 avril 2027.', '2027-04-20 08:45:00', 'anis.mejri@bfm.tn'),
('INC-008', 'Détection d''une anomalie sur le réseau de caméras', 'INCIDENT', 'Plusieurs caméras du réseau ont perdu leur connexion le 5 juillet 2027.', '2027-07-05 16:30:00', 'ines.ferchichi@bfm.tn'),
('INC-009', 'Panne du système de détection incendie du bâtiment administratif', 'INCIDENT', 'Le système de détection incendie du bâtiment administratif a signalé une panne le 1er mai 2027.', '2027-05-01 06:00:00', 'slim.ayari@bfm.tn'),
('INC-010', 'Incident de sûreté au centre de stockage', 'INCIDENT', 'Tentative d''intrusion détectée au centre de stockage le 1er septembre 2027.', '2027-09-01 23:30:00', 'anis.mejri@bfm.tn')
) AS v(code, designation, type_origine, description, date_creation, email)
JOIN utilisateur u ON u.email = v.email;

-- =============================================================================
-- 19. PLANS D'ACTION (20 plans — un par origine)
-- =============================================================================

INSERT INTO plan_action (code, designation, date_debut_prevue, date_fin_prevue, date_debut_reelle, date_fin_reelle) VALUES
-- Plans pour les risques
('PA-RSQ-001', 'Remise en état et renforcement du système de vidéosurveillance', '2027-02-01', '2027-04-30', '2027-02-05', NULL),
('PA-RSQ-002', 'Renforcement des dispositifs de lutte contre les accès non autorisés', '2027-02-01', '2027-05-31', '2027-02-10', NULL),
('PA-RSQ-003', 'Fiabilisation du système d''alarme anti-intrusion', '2027-02-15', '2027-05-15', '2027-02-20', NULL),
('PA-RSQ-004', 'Modernisation du contrôle d''accès des sites sensibles', '2027-03-01', '2027-06-30', NULL, NULL),
('PA-RSQ-005', 'Protection et maintenance des équipements de sécurité', '2027-03-01', '2027-06-30', NULL, NULL),
('PA-RSQ-006', 'Mise en conformité des procédures de sûreté', '2027-03-15', '2027-07-31', NULL, NULL),
('PA-RSQ-007', 'Renforcement du plan de maintenance préventive', '2027-04-01', '2027-07-31', NULL, NULL),
('PA-RSQ-008', 'Amélioration de la communication réseau des équipements de sécurité', '2027-04-01', '2027-08-31', NULL, NULL),
('PA-RSQ-009', 'Fiabilisation du système de détection incendie', '2027-04-15', '2027-08-31', NULL, NULL),
('PA-RSQ-010', 'Renforcement de la sûreté périmétrique des sites', '2027-05-01', '2027-09-30', NULL, NULL),
-- Plans pour les incidents
('PA-INC-001', 'Remplacement de la caméra défaillante du hall principal', '2027-01-21', '2027-01-31', '2027-01-21', '2027-01-25'),
('PA-INC-002', 'Diagnostic et correction du déclenchement intempestif d''alarme', '2027-01-06', '2027-01-20', '2027-01-06', '2027-01-15'),
('PA-INC-003', 'Réparation du lecteur de badge de l''entrée du personnel', '2027-02-11', '2027-02-28', '2027-02-11', '2027-02-20'),
('PA-INC-004', 'Rétablissement de la communication avec l''équipement de sécurité', '2027-03-06', '2027-03-20', '2027-03-06', '2027-03-15'),
('PA-INC-005', 'Enquête et renforcement de la sûreté de l''agence de Sfax', '2027-03-26', '2027-04-30', '2027-03-26', NULL),
('PA-INC-006', 'Fiabilisation de l''alimentation électrique du système de sécurité', '2027-03-02', '2027-03-31', '2027-03-02', '2027-03-25'),
('PA-INC-007', 'Réparation et test du contrôle d''accès de l''agence d''Ariana', '2027-04-21', '2027-05-15', '2027-04-21', NULL),
('PA-INC-008', 'Diagnostic et correction de l''anomalie du réseau de caméras', '2027-07-06', '2027-07-31', NULL, NULL),
('PA-INC-009', 'Réparation du système de détection incendie du bâtiment administratif', '2027-05-02', '2027-05-31', '2027-05-02', '2027-05-20'),
('PA-INC-010', 'Renforcement de la sûreté du centre de stockage après incident', '2027-09-02', '2027-10-31', NULL, NULL);

-- =============================================================================
-- 20. PLAN_ACTION_ORIGINE (liaison plan ↔ origine)
-- =============================================================================

INSERT INTO plan_action_origine (id_plan_action, id_origine, est_principale)
SELECT p.id_plan_action, o.id_origine, TRUE
FROM plan_action p
JOIN origine o
  ON o.code = regexp_replace(p.code, '^PA-', '');

-- =============================================================================
-- 21. AFFECTATION_PLAN_ACTION
-- =============================================================================

INSERT INTO affectation_plan_action (date_affectation, date_desaffectation, id_plan_action, id_utilisateur, id_role)
SELECT v.date_affectation::timestamp, NULL, p.id_plan_action, u.id_utilisateur, r.id_role
FROM (VALUES
-- Plans risques : affectés au chef de service 1 ou 2 selon le type
('2027-02-01 08:00:00', 'PA-RSQ-001', 'leila.mansouri@bfm.tn', 'RESPONSABLE'), ('2027-02-01 08:00:00', 'PA-RSQ-001', 'sonia.gharbi@bfm.tn', 'PARTICIPANT'),
('2027-02-01 08:00:00', 'PA-RSQ-002', 'leila.mansouri@bfm.tn', 'RESPONSABLE'), ('2027-02-01 08:00:00', 'PA-RSQ-002', 'mohamed.khelifi@bfm.tn', 'PARTICIPANT'),
('2027-02-15 08:00:00', 'PA-RSQ-003', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-02-15 08:00:00', 'PA-RSQ-003', 'slim.ayari@bfm.tn', 'PARTICIPANT'),
('2027-03-01 08:00:00', 'PA-RSQ-004', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-03-01 08:00:00', 'PA-RSQ-004', 'anis.mejri@bfm.tn', 'PARTICIPANT'),
('2027-03-01 08:00:00', 'PA-RSQ-005', 'leila.mansouri@bfm.tn', 'RESPONSABLE'), ('2027-03-01 08:00:00', 'PA-RSQ-005', 'nadia.bouazizi@bfm.tn', 'PARTICIPANT'),
('2027-03-15 08:00:00', 'PA-RSQ-006', 'leila.mansouri@bfm.tn', 'RESPONSABLE'), ('2027-03-15 08:00:00', 'PA-RSQ-006', 'rami.jlassi@bfm.tn', 'PARTICIPANT'),
('2027-04-01 08:00:00', 'PA-RSQ-007', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-04-01 08:00:00', 'PA-RSQ-007', 'ines.ferchichi@bfm.tn', 'PARTICIPANT'),
('2027-04-01 08:00:00', 'PA-RSQ-008', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-04-01 08:00:00', 'PA-RSQ-008', 'yassine.zaidi@bfm.tn', 'PARTICIPANT'),
('2027-04-15 08:00:00', 'PA-RSQ-009', 'leila.mansouri@bfm.tn', 'RESPONSABLE'), ('2027-04-15 08:00:00', 'PA-RSQ-009', 'mariem.hamdi@bfm.tn', 'PARTICIPANT'),
('2027-05-01 08:00:00', 'PA-RSQ-010', 'leila.mansouri@bfm.tn', 'RESPONSABLE'), ('2027-05-01 08:00:00', 'PA-RSQ-010', 'walid.sassi@bfm.tn', 'PARTICIPANT'),
-- Plans incidents
('2027-01-21 08:00:00', 'PA-INC-001', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-01-21 08:00:00', 'PA-INC-001', 'anis.mejri@bfm.tn', 'PARTICIPANT'),
('2027-01-06 08:00:00', 'PA-INC-002', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-01-06 08:00:00', 'PA-INC-002', 'ines.ferchichi@bfm.tn', 'PARTICIPANT'),
('2027-02-11 08:00:00', 'PA-INC-003', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-02-11 08:00:00', 'PA-INC-003', 'yassine.zaidi@bfm.tn', 'PARTICIPANT'),
('2027-03-06 08:00:00', 'PA-INC-004', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-03-06 08:00:00', 'PA-INC-004', 'mariem.hamdi@bfm.tn', 'PARTICIPANT'),
('2027-03-26 08:00:00', 'PA-INC-005', 'leila.mansouri@bfm.tn', 'RESPONSABLE'), ('2027-03-26 08:00:00', 'PA-INC-005', 'sonia.gharbi@bfm.tn', 'PARTICIPANT'),
('2027-03-02 08:00:00', 'PA-INC-006', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-03-02 08:00:00', 'PA-INC-006', 'slim.ayari@bfm.tn', 'PARTICIPANT'),
('2027-04-21 08:00:00', 'PA-INC-007', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-04-21 08:00:00', 'PA-INC-007', 'walid.sassi@bfm.tn', 'PARTICIPANT'),
('2027-07-06 08:00:00', 'PA-INC-008', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-07-06 08:00:00', 'PA-INC-008', 'faten.riahi@bfm.tn', 'PARTICIPANT'),
('2027-05-02 08:00:00', 'PA-INC-009', 'hatem.trabelsi@bfm.tn', 'RESPONSABLE'), ('2027-05-02 08:00:00', 'PA-INC-009', 'anis.mejri@bfm.tn', 'PARTICIPANT'),
('2027-09-02 08:00:00', 'PA-INC-010', 'leila.mansouri@bfm.tn', 'RESPONSABLE'), ('2027-09-02 08:00:00', 'PA-INC-010', 'mohamed.khelifi@bfm.tn', 'PARTICIPANT')
) AS v(date_affectation, code_plan, email, code_role)
JOIN plan_action p ON p.code = v.code_plan
JOIN utilisateur u ON u.email = v.email
JOIN role r ON r.code = v.code_role;

-- =============================================================================
-- 22. AVANCEMENT_PLAN_ACTION
-- =============================================================================

INSERT INTO avancement_plan_action (valeur_pourcentage, commentaire, date_changement, id_utilisateur, id_statut, id_plan_action)
SELECT v.valeur_pourcentage, v.commentaire, v.date_changement::timestamp, u.id_utilisateur, s.id_statut, p.id_plan_action
FROM (VALUES
-- Plans risques
(30.00, 'Diagnostic en cours', '2027-03-01 10:00:00', 'leila.mansouri@bfm.tn', 'EN_COURS', 'PA-RSQ-001'),
(25.00, 'Analyse des vulnérabilités réalisée', '2027-03-15 10:00:00', 'leila.mansouri@bfm.tn', 'EN_COURS', 'PA-RSQ-002'),
(20.00, 'Audit du système d''alarme en cours', '2027-03-20 10:00:00', 'hatem.trabelsi@bfm.tn', 'EN_COURS', 'PA-RSQ-003'),
(0.00, 'Plan non encore démarré', '2027-03-01 08:00:00', 'hatem.trabelsi@bfm.tn', 'NON_COMMENCEE', 'PA-RSQ-004'),
(0.00, 'Plan non encore démarré', '2027-03-01 08:00:00', 'leila.mansouri@bfm.tn', 'NON_COMMENCEE', 'PA-RSQ-005'),
(0.00, 'Plan non encore démarré', '2027-03-15 08:00:00', 'leila.mansouri@bfm.tn', 'NON_COMMENCEE', 'PA-RSQ-006'),
(0.00, 'Plan non encore démarré', '2027-04-01 08:00:00', 'hatem.trabelsi@bfm.tn', 'NON_COMMENCEE', 'PA-RSQ-007'),
(0.00, 'Plan non encore démarré', '2027-04-01 08:00:00', 'hatem.trabelsi@bfm.tn', 'NON_COMMENCEE', 'PA-RSQ-008'),
(0.00, 'Plan non encore démarré', '2027-04-15 08:00:00', 'leila.mansouri@bfm.tn', 'NON_COMMENCEE', 'PA-RSQ-009'),
(0.00, 'Plan non encore démarré', '2027-05-01 08:00:00', 'leila.mansouri@bfm.tn', 'NON_COMMENCEE', 'PA-RSQ-010'),
-- Plans incidents
(100.00, 'Caméra remplacée et testée', '2027-01-25 16:00:00', 'hatem.trabelsi@bfm.tn', 'TERMINEE', 'PA-INC-001'),
(100.00, 'Alarme diagnostiquée et corrigée', '2027-01-15 16:00:00', 'hatem.trabelsi@bfm.tn', 'TERMINEE', 'PA-INC-002'),
(100.00, 'Lecteur réparé et testé', '2027-02-20 16:00:00', 'hatem.trabelsi@bfm.tn', 'TERMINEE', 'PA-INC-003'),
(100.00, 'Communication rétablie', '2027-03-15 16:00:00', 'hatem.trabelsi@bfm.tn', 'TERMINEE', 'PA-INC-004'),
(60.00, 'Enquête en cours et mesures prises', '2027-04-15 10:00:00', 'leila.mansouri@bfm.tn', 'EN_COURS', 'PA-INC-005'),
(100.00, 'Alimentation fiabilisée', '2027-03-25 16:00:00', 'hatem.trabelsi@bfm.tn', 'TERMINEE', 'PA-INC-006'),
(50.00, 'Réparation en cours', '2027-05-01 10:00:00', 'hatem.trabelsi@bfm.tn', 'EN_COURS', 'PA-INC-007'),
(0.00, 'Plan non encore démarré', '2027-07-06 08:00:00', 'hatem.trabelsi@bfm.tn', 'NON_COMMENCEE', 'PA-INC-008'),
(100.00, 'Système réparé et testé', '2027-05-20 16:00:00', 'hatem.trabelsi@bfm.tn', 'TERMINEE', 'PA-INC-009'),
(0.00, 'Plan non encore démarré', '2027-09-02 08:00:00', 'leila.mansouri@bfm.tn', 'NON_COMMENCEE', 'PA-INC-010')
) AS v(valeur_pourcentage, commentaire, date_changement, email, code_statut, code_plan)
JOIN utilisateur u ON u.email = v.email
JOIN statut s ON s.code = v.code_statut
JOIN plan_action p ON p.code = v.code_plan;

-- =============================================================================
-- 23. VALIDATION_ACTIVITE (workflow Chef de service → Chef de département)
-- =============================================================================

-- Validation des activités par le chef de service (niveau 1)
INSERT INTO validation_activite (decision, commentaire, date_demande, date_decision, id_activite, id_etape_validation, id_demandeur, id_decideur)
SELECT
    'VALIDE',
    'Activité validée par le chef de service.',
    a.date_debut_prevue - INTERVAL '5 days',
    a.date_debut_prevue - INTERVAL '3 days',
    a.id_activite,
    (SELECT id_etape_validation FROM etape_validation WHERE niveau = 1),
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1)
FROM activite a;

-- Validation finale par le chef de département (niveau 2) pour les activités validées
INSERT INTO validation_activite (decision, commentaire, date_demande, date_decision, id_activite, id_etape_validation, id_demandeur, id_decideur)
SELECT
    'VALIDE',
    'Activité validée par le chef de département.',
    a.date_debut_prevue - INTERVAL '2 days',
    a.date_debut_prevue - INTERVAL '1 day',
    a.id_activite,
    (SELECT id_etape_validation FROM etape_validation WHERE niveau = 2),
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'karim.bensalah@bfm.tn')
FROM activite a;

-- Activités en attente de validation (une sélection)
INSERT INTO validation_activite (decision, commentaire, date_demande, date_decision, id_activite, id_etape_validation, id_demandeur, id_decideur)
SELECT
    'EN_ATTENTE_VALIDATION',
    'En attente de validation par le chef de département.',
    '2027-03-01 08:00:00'::timestamp,
    NULL,
    a.id_activite,
    (SELECT id_etape_validation FROM etape_validation WHERE niveau = 2),
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'karim.bensalah@bfm.tn')
FROM activite a
WHERE a.code IN ('A-2027-01-03', 'A-2027-02-03', 'A-2027-03-03', 'A-2027-04-03', 'A-2027-05-03',
                 'A-2027-06-03', 'A-2027-07-03', 'A-2027-08-03', 'A-2027-09-03', 'A-2027-10-03');

-- Activité retournée pour modification (une sélection)
INSERT INTO validation_activite (decision, commentaire, date_demande, date_decision, id_activite, id_etape_validation, id_demandeur, id_decideur)
SELECT
    'RETOUR_MODIFICATION',
    'Activité retournée pour modification du planning.',
    '2027-03-05 08:00:00'::timestamp,
    '2027-03-07 10:00:00'::timestamp,
    a.id_activite,
    (SELECT id_etape_validation FROM etape_validation WHERE niveau = 2),
    (SELECT u.id_utilisateur
     FROM utilisateur u
     JOIN service s ON s.id_service = u.id_service
     WHERE s.nom = (SELECT nom FROM service WHERE id_service = a.id_service)
       AND u.email IN ('leila.mansouri@bfm.tn', 'hatem.trabelsi@bfm.tn')
     ORDER BY u.email
     LIMIT 1),
    (SELECT id_utilisateur FROM utilisateur WHERE email = 'karim.bensalah@bfm.tn')
FROM activite a
WHERE a.code IN ('A-2027-01-05', 'A-2027-06-05', 'A-2027-05', 'A-2027-30');

-- =============================================================================
-- 24. VALIDATION_PLAN_ACTION
-- =============================================================================

INSERT INTO validation_plan_action (decision, commentaire, date_demande, date_decision, id_plan_action, id_etape_validation, id_demandeur, id_decideur)
SELECT v.decision::decision_validation, v.commentaire, v.date_demande::timestamp, v.date_decision::timestamp, p.id_plan_action,
       (SELECT id_etape_validation FROM etape_validation WHERE niveau = 2),
       u.id_utilisateur,
       (SELECT id_utilisateur FROM utilisateur WHERE email = 'karim.bensalah@bfm.tn')
FROM (VALUES
('VALIDE', 'Plan d''action validé.', '2027-02-01 08:00:00', '2027-02-03 10:00:00', 'PA-RSQ-001', 'leila.mansouri@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-02-01 08:00:00', '2027-02-03 10:00:00', 'PA-RSQ-002', 'leila.mansouri@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-02-15 08:00:00', '2027-02-17 10:00:00', 'PA-RSQ-003', 'hatem.trabelsi@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-03-01 08:00:00', NULL, 'PA-RSQ-004', 'hatem.trabelsi@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-03-01 08:00:00', NULL, 'PA-RSQ-005', 'leila.mansouri@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-03-15 08:00:00', NULL, 'PA-RSQ-006', 'leila.mansouri@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-04-01 08:00:00', NULL, 'PA-RSQ-007', 'hatem.trabelsi@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-04-01 08:00:00', NULL, 'PA-RSQ-008', 'hatem.trabelsi@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-04-15 08:00:00', NULL, 'PA-RSQ-009', 'leila.mansouri@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-05-01 08:00:00', NULL, 'PA-RSQ-010', 'leila.mansouri@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-01-21 08:00:00', '2027-01-22 10:00:00', 'PA-INC-001', 'hatem.trabelsi@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-01-06 08:00:00', '2027-01-07 10:00:00', 'PA-INC-002', 'hatem.trabelsi@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-02-11 08:00:00', '2027-02-12 10:00:00', 'PA-INC-003', 'hatem.trabelsi@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-03-06 08:00:00', '2027-03-07 10:00:00', 'PA-INC-004', 'hatem.trabelsi@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-03-26 08:00:00', '2027-03-28 10:00:00', 'PA-INC-005', 'leila.mansouri@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-03-02 08:00:00', '2027-03-03 10:00:00', 'PA-INC-006', 'hatem.trabelsi@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-04-21 08:00:00', '2027-04-23 10:00:00', 'PA-INC-007', 'hatem.trabelsi@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-07-06 08:00:00', NULL, 'PA-INC-008', 'hatem.trabelsi@bfm.tn'),
('VALIDE', 'Plan d''action validé.', '2027-05-02 08:00:00', '2027-05-04 10:00:00', 'PA-INC-009', 'hatem.trabelsi@bfm.tn'),
('EN_ATTENTE_VALIDATION', 'En attente de validation.', '2027-09-02 08:00:00', NULL, 'PA-INC-010', 'leila.mansouri@bfm.tn')
) AS v(decision, commentaire, date_demande, date_decision, code_plan, email)
JOIN plan_action p ON p.code = v.code_plan
JOIN utilisateur u ON u.email = v.email;

-- =============================================================================
-- 25. NOTIFICATIONS
-- =============================================================================

INSERT INTO notification (titre, message, date_creation, date_lecture, id_priorite, id_utilisateur)
SELECT v.titre, v.message, v.date_creation::timestamp, v.date_lecture::timestamp, pr.id_priorite, u.id_utilisateur
FROM (VALUES
('Nouvelle activité assignée', 'Une nouvelle activité vous a été assignée.', '2027-01-10 08:00:00', NULL, 'NORMALE', 'sonia.gharbi@bfm.tn'),
('Validation en attente', 'Une activité est en attente de votre validation.', '2027-01-12 09:00:00', NULL, 'HAUTE', 'karim.bensalah@bfm.tn'),
('Sous-activité terminée', 'La sous-activité a été marquée comme terminée.', '2027-02-25 16:00:00', '2027-02-26 08:00:00', 'NORMALE', 'leila.mansouri@bfm.tn'),
('Incident signalé', 'Un incident de sécurité a été signalé.', '2027-01-05 22:30:00', NULL, 'CRITIQUE', 'hatem.trabelsi@bfm.tn'),
('Plan d''action validé', 'Votre plan d''action a été validé.', '2027-02-03 10:30:00', '2027-02-04 09:00:00', 'NORMALE', 'leila.mansouri@bfm.tn'),
('Maintenance préventive à planifier', 'La maintenance préventive doit être planifiée.', '2027-03-01 08:00:00', NULL, 'HAUTE', 'hatem.trabelsi@bfm.tn'),
('Rappel de saisie d''indicateur', 'Veuillez saisir la valeur de l''indicateur.', '2027-04-01 08:00:00', NULL, 'NORMALE', 'sonia.gharbi@bfm.tn'),
('Nouvelle procédure disponible', 'Une nouvelle version de procédure est disponible.', '2027-02-20 10:00:00', '2027-02-21 09:00:00', 'FAIBLE', 'nadia.bouazizi@bfm.tn'),
('Compte activé', 'Votre compte a été activé avec succès.', '2027-01-01 08:00:00', '2027-01-01 08:05:00', 'NORMALE', 'sonia.gharbi@bfm.tn'),
('Token de réinitialisation', 'Votre token de réinitialisation de mot de passe.', '2027-03-01 10:00:00', NULL, 'HAUTE', 'tarek.baccouche@bfm.tn')
) AS v(titre, message, date_creation, date_lecture, code_priorite, email)
JOIN priorite pr ON pr.code = v.code_priorite
JOIN utilisateur u ON u.email = v.email;

-- =============================================================================
-- 26. TOKEN_AUTH (exemples)
-- =============================================================================

INSERT INTO token_auth (token, date_creation, date_expiration, date_utilisation, id_type_token, id_utilisateur)
SELECT v.token, v.date_creation::timestamp, v.date_expiration::timestamp, v.date_utilisation::timestamp, tt.id_type_token, u.id_utilisateur
FROM (VALUES
('tok_reset_001_abc123', '2027-03-01 10:00:00', '2027-03-01 10:30:00', NULL, 'RESET_MOT_DE_PASSE', 'sonia.gharbi@bfm.tn'),
('tok_activation_001_def456', '2027-01-01 08:00:00', '2027-01-01 10:00:00', '2027-01-01 08:05:00', 'ACTIVATION_COMPTE', 'sonia.gharbi@bfm.tn'),
('tok_reset_002_ghi789', '2027-03-05 14:00:00', '2027-03-05 14:30:00', NULL, 'RESET_MOT_DE_PASSE', 'rami.jlassi@bfm.tn'),
('tok_auth_001_jkl012', '2027-03-15 09:00:00', '2027-03-15 17:00:00', '2027-03-15 09:12:00', 'AUTHENTIFICATION', 'karim.bensalah@bfm.tn')
) AS v(token, date_creation, date_expiration, date_utilisation, code_type_token, email)
JOIN type_token tt ON tt.code = v.code_type_token
JOIN utilisateur u ON u.email = v.email;

-- =============================================================================
-- 27. RÉINITIALISATION DES SÉQUENCES
-- =============================================================================

SELECT setval(pg_get_serial_sequence('departement', 'id_departement'), COALESCE((SELECT MAX(id_departement) FROM departement), 1));
SELECT setval(pg_get_serial_sequence('service', 'id_service'), COALESCE((SELECT MAX(id_service) FROM service), 1));
SELECT setval(pg_get_serial_sequence('objectif_specifique', 'id_objectif_specifique'), COALESCE((SELECT MAX(id_objectif_specifique) FROM objectif_specifique), 1));
SELECT setval(pg_get_serial_sequence('site', 'id_site'), COALESCE((SELECT MAX(id_site) FROM site), 1));
SELECT setval(pg_get_serial_sequence('type_activite', 'id_type_activite'), COALESCE((SELECT MAX(id_type_activite) FROM type_activite), 1));
SELECT setval(pg_get_serial_sequence('statut', 'id_statut'), COALESCE((SELECT MAX(id_statut) FROM statut), 1));
SELECT setval(pg_get_serial_sequence('priorite', 'id_priorite'), COALESCE((SELECT MAX(id_priorite) FROM priorite), 1));
SELECT setval(pg_get_serial_sequence('poste', 'id_poste'), COALESCE((SELECT MAX(id_poste) FROM poste), 1));
SELECT setval(pg_get_serial_sequence('activite', 'id_activite'), COALESCE((SELECT MAX(id_activite) FROM activite), 1));
SELECT setval(pg_get_serial_sequence('sous_activite', 'id_sous_activite'), COALESCE((SELECT MAX(id_sous_activite) FROM sous_activite), 1));
SELECT setval(pg_get_serial_sequence('resultat_intermediaire', 'id_resultat_intermediaire'), COALESCE((SELECT MAX(id_resultat_intermediaire) FROM resultat_intermediaire), 1));
SELECT setval(pg_get_serial_sequence('livrable_sous_activite', 'id_livrable_sous_activite'), COALESCE((SELECT MAX(id_livrable_sous_activite) FROM livrable_sous_activite), 1));
SELECT setval(pg_get_serial_sequence('utilisateur', 'id_utilisateur'), COALESCE((SELECT MAX(id_utilisateur) FROM utilisateur), 1));
SELECT setval(pg_get_serial_sequence('type_token', 'id_type_token'), COALESCE((SELECT MAX(id_type_token) FROM type_token), 1));
SELECT setval(pg_get_serial_sequence('role', 'id_role'), COALESCE((SELECT MAX(id_role) FROM role), 1));
SELECT setval(pg_get_serial_sequence('permission', 'id_permission'), COALESCE((SELECT MAX(id_permission) FROM permission), 1));
SELECT setval(pg_get_serial_sequence('affectation_sous_activite', 'id_affectation_sous_activite'), COALESCE((SELECT MAX(id_affectation_sous_activite) FROM affectation_sous_activite), 1));
SELECT setval(pg_get_serial_sequence('"procedure"', 'id_procedure'), COALESCE((SELECT MAX(id_procedure) FROM "procedure"), 1));
SELECT setval(pg_get_serial_sequence('etape_validation', 'id_etape_validation'), COALESCE((SELECT MAX(id_etape_validation) FROM etape_validation), 1));
SELECT setval(pg_get_serial_sequence('validation_activite', 'id_validation_activite'), COALESCE((SELECT MAX(id_validation_activite) FROM validation_activite), 1));
SELECT setval(pg_get_serial_sequence('indicateur', 'id_indicateur'), COALESCE((SELECT MAX(id_indicateur) FROM indicateur), 1));
SELECT setval(pg_get_serial_sequence('valeur_indicateur', 'id_valeur_indicateur'), COALESCE((SELECT MAX(id_valeur_indicateur) FROM valeur_indicateur), 1));
SELECT setval(pg_get_serial_sequence('plan_action', 'id_plan_action'), COALESCE((SELECT MAX(id_plan_action) FROM plan_action), 1));
SELECT setval(pg_get_serial_sequence('origine', 'id_origine'), COALESCE((SELECT MAX(id_origine) FROM origine), 1));
SELECT setval(pg_get_serial_sequence('affectation_plan_action', 'id_affectation_plan_action'), COALESCE((SELECT MAX(id_affectation_plan_action) FROM affectation_plan_action), 1));
SELECT setval(pg_get_serial_sequence('avancement_plan_action', 'id_avancement_plan_action'), COALESCE((SELECT MAX(id_avancement_plan_action) FROM avancement_plan_action), 1));
SELECT setval(pg_get_serial_sequence('validation_plan_action', 'id_validation_plan_action'), COALESCE((SELECT MAX(id_validation_plan_action) FROM validation_plan_action), 1));
SELECT setval(pg_get_serial_sequence('notification', 'id_notification'), COALESCE((SELECT MAX(id_notification) FROM notification), 1));
SELECT setval(pg_get_serial_sequence('historique_activite', 'id_historique_activite'), COALESCE((SELECT MAX(id_historique_activite) FROM historique_activite), 1));
SELECT setval(pg_get_serial_sequence('avancement_sous_activite', 'id_historique_sous_activite'), COALESCE((SELECT MAX(id_historique_sous_activite) FROM avancement_sous_activite), 1));
SELECT setval(pg_get_serial_sequence('fichier_sous_activite', 'id_fichier_sous_activite'), COALESCE((SELECT MAX(id_fichier_sous_activite) FROM fichier_sous_activite), 1));
SELECT setval(pg_get_serial_sequence('token_auth', 'id_token_auth'), COALESCE((SELECT MAX(id_token_auth) FROM token_auth), 1));
SELECT setval(pg_get_serial_sequence('parametre', 'id_parametre'), COALESCE((SELECT MAX(id_parametre) FROM parametre), 1));

COMMIT;

-- =============================================================================
-- 28. REQUÊTES DE CONTRÔLE
-- =============================================================================

-- 28.1 Objectifs spécifiques : doit retourner 10
SELECT 'Objectifs spécifiques' AS controle, COUNT(*) AS total FROM objectif_specifique;

-- 28.2 Activités PTA : doit retourner 50
SELECT 'Activités PTA' AS controle, COUNT(*) AS total FROM activite WHERE id_objectif_specifique IS NOT NULL;

-- 28.3 Activités NON PTA : doit retourner 50
SELECT 'Activités NON PTA' AS controle, COUNT(*) AS total FROM activite WHERE id_objectif_specifique IS NULL;

-- 28.4 Activités totales : doit retourner 100
SELECT 'Activités totales' AS controle, COUNT(*) AS total FROM activite;

-- 28.5 Chaque objectif possède exactement 5 activités PTA
SELECT o.code, COUNT(a.id_activite) AS nb_activites
FROM objectif_specifique o
LEFT JOIN activite a ON a.id_objectif_specifique = o.id_objectif_specifique
GROUP BY o.code
ORDER BY o.code;

-- 28.6 Sous-activités par activité : doit être entre 3 et 5
SELECT a.code, COUNT(sa.id_sous_activite) AS nb_sous_activites
FROM activite a
LEFT JOIN sous_activite sa ON sa.id_activite = a.id_activite
GROUP BY a.code
HAVING COUNT(sa.id_sous_activite) < 3 OR COUNT(sa.id_sous_activite) > 5;

-- 28.7 Sous-activités totales
SELECT 'Sous-activités totales' AS controle, COUNT(*) AS total FROM sous_activite;

-- 28.8 Chaque sous-activité possède au moins un livrable
SELECT COUNT(*) AS sous_activites_sans_livrable
FROM sous_activite sa
LEFT JOIN livrable_sous_activite l ON l.id_sous_activite = sa.id_sous_activite
WHERE l.id_livrable_sous_activite IS NULL;

-- 28.9 Chaque livrable possède au moins un fichier
SELECT COUNT(*) AS livrables_sans_fichier
FROM livrable_sous_activite l
LEFT JOIN fichier_sous_activite f ON f.id_livrable_sous_activite = l.id_livrable_sous_activite
WHERE f.id_fichier_sous_activite IS NULL;

-- 28.10 Chaque activité possède au moins un indicateur
SELECT COUNT(*) AS activites_sans_indicateur
FROM activite a
LEFT JOIN activite_indicateur ai ON ai.id_activite = a.id_activite
WHERE ai.id_indicateur IS NULL;

-- 28.11 Indicateurs totaux
SELECT 'Indicateurs totaux' AS controle, COUNT(*) AS total FROM indicateur;

-- 28.12 Risques : doit retourner 10
SELECT 'Risques' AS controle, COUNT(*) AS total FROM origine WHERE type_origine = 'RISQUE';

-- 28.13 Incidents : doit retourner 10
SELECT 'Incidents' AS controle, COUNT(*) AS total FROM origine WHERE type_origine = 'INCIDENT';

-- 28.14 Origines totales : doit retourner 20
SELECT 'Origines totales' AS controle, COUNT(*) AS total FROM origine;

-- 28.15 Plans d'action : doit retourner 20
SELECT 'Plans d''action' AS controle, COUNT(*) AS total FROM plan_action;

-- 28.16 Chaque plan d'action est relié à une origine
SELECT COUNT(*) AS plans_sans_origine
FROM plan_action pa
LEFT JOIN plan_action_origine pao ON pao.id_plan_action = pa.id_plan_action
WHERE pao.id_origine IS NULL;

INSERT INTO parametre (code, designation, valeur, type_valeur, description, categorie, modifiable, actif, date_creation)
VALUES
(
    'ID_PROCEDURE_PTA',
    'Procédure de validation — activités PTA',
    (SELECT id_procedure::text FROM "procedure"
     WHERE designation = 'Procédure de validation des activités TSS'),
    'INTEGER',
    'Identifiant de la procédure de validation appliquée aux activités PTA (rattachées à un objectif spécifique).',
    'VALIDATION',
    TRUE,
    TRUE,
    '2027-01-05 08:00:00'::timestamp
),
(
    'ID_PROCEDURE_NON_PTA',
    'Procédure de validation — activités NON PTA',
    (SELECT id_procedure::text FROM "procedure"
     WHERE designation = 'Procédure de validation des activités TSS'),
    'INTEGER',
    'Identifiant de la procédure de validation appliquée aux activités NON PTA (sans objectif spécifique).',
    'VALIDATION',
    TRUE,
    TRUE,
    '2027-01-05 08:00:00'::timestamp
);