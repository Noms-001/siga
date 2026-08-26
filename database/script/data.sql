-- ============================================================
-- SIGA - data.sql
-- Données de démonstration
-- ============================================================

BEGIN;

-- Départements
INSERT INTO departement (id_departement, nom, description) VALUES
(1, 'Direction des Systèmes', 'Département en charge des systèmes et du numérique'),
(2, 'Département Administratif', 'Département en charge de la gestion administrative');

-- Services
INSERT INTO service (id_service, id_departement, nom, description, actif, date_creation) VALUES
(1, 1, 'Service Études et Procédures', 'Service chargé des études et procédures', TRUE, CURRENT_TIMESTAMP),
(2, 1, 'Service Informatique', 'Service chargé des infrastructures et applications', TRUE, CURRENT_TIMESTAMP),
(3, 2, 'Service Administratif et Financier', 'Service chargé de la gestion administrative et financière', TRUE, CURRENT_TIMESTAMP);

-- Postes
INSERT INTO poste (id_poste, libelle, type_contrat, categorie, effectif_prevu, effectif_reel, actif, date_creation) VALUES
(1, 'Chef de département', 'CDI', 'Cadre', 3, 2, TRUE, CURRENT_TIMESTAMP),
(2, 'Chef de service', 'CDI', 'Cadre', 8, 6, TRUE, CURRENT_TIMESTAMP),
(3, 'Agent', 'CDI', 'Agent', 30, 24, TRUE, CURRENT_TIMESTAMP),
(4, 'Directeur', 'CDI', 'Direction', 3, 2, TRUE, CURRENT_TIMESTAMP);

-- Priorités
INSERT INTO priorite (id_priorite, code, libelle, description, actif, date_creation) VALUES
(1, 'BASSE', 'Basse', 'Priorité faible', TRUE, CURRENT_TIMESTAMP),
(2, 'NORMALE', 'Normale', 'Priorité normale', TRUE, CURRENT_TIMESTAMP),
(3, 'HAUTE', 'Haute', 'Priorité importante', TRUE, CURRENT_TIMESTAMP),
(4, 'URGENTE', 'Urgente', 'Traitement prioritaire', TRUE, CURRENT_TIMESTAMP);

-- Types d'activité
INSERT INTO type_activite (id_type_activite, libelle, actif, date_creation) VALUES
(1, 'Projet', TRUE, CURRENT_TIMESTAMP),
(2, 'Action', TRUE, CURRENT_TIMESTAMP),
(3, 'Étude', TRUE, CURRENT_TIMESTAMP),
(4, 'Contrôle', TRUE, CURRENT_TIMESTAMP);

-- Statuts
INSERT INTO statut (id_statut, libelle, code, ordre, description, actif, date_creation) VALUES
(1, 'Créée', 'CREEE', 1, 'Activité nouvellement créée', TRUE, CURRENT_TIMESTAMP),
(2, 'En attente de validation', 'EN_ATTENTE_VALIDATION', 2, 'Activité soumise au circuit de validation', TRUE, CURRENT_TIMESTAMP),
(3, 'Validée', 'VALIDEE', 3, 'Validation finale obtenue', TRUE, CURRENT_TIMESTAMP),
(4, 'Non commencée', 'NON_COMMENCEE', 4, 'Activité validée mais non démarrée', TRUE, CURRENT_TIMESTAMP),
(5, 'En cours', 'EN_COURS', 5, 'Activité en cours de réalisation', TRUE, CURRENT_TIMESTAMP),
(6, 'Terminée', 'TERMINEE', 6, 'Activité terminée', TRUE, CURRENT_TIMESTAMP);

-- Types de token
INSERT INTO type_token (id_type_token, code, libelle, description) VALUES
(1, 'ACTIVATION_COMPTE', 'Activation de compte', 'Token utilisé pour initialiser le mot de passe et activer un compte'),
(2, 'RESET_MOT_DE_PASSE', 'Réinitialisation du mot de passe', 'Token utilisé pour réinitialiser un mot de passe');

-- Permissions
INSERT INTO permission (id_permission, code, nom, description, ressource, action, actif) VALUES
(1, 'GERER_UTILISATEUR', 'Gérer les utilisateurs', 'Créer, modifier et administrer les utilisateurs', 'UTILISATEUR', 'GERER', TRUE),
(2, 'CREER_ACTIVITE', 'Créer une activité', 'Permet de créer une activité', 'ACTIVITE', 'CREER', TRUE),
(3, 'MODIFIER_ACTIVITE', 'Modifier une activité', 'Permet de modifier une activité', 'ACTIVITE', 'MODIFIER', TRUE),
(4, 'VALIDER_ACTIVITE', 'Valider une activité', 'Permet de valider une activité', 'ACTIVITE', 'VALIDER', TRUE),
(5, 'CREER_INDICATEUR', 'Créer un indicateur', 'Permet de créer un indicateur', 'INDICATEUR', 'CREER', TRUE),
(6, 'SAISIR_VALEUR_INDICATEUR', 'Saisir une valeur', 'Permet de saisir une valeur d''indicateur', 'INDICATEUR', 'SAISIR', TRUE);

-- Rôles
INSERT INTO role (id_role, code, libelle, description, actif, date_creation) VALUES
(1, 'RESPONSABLE', 'Responsable', 'Responsable principal de l''activité', TRUE, CURRENT_TIMESTAMP),
(2, 'SUPPLEANT', 'Suppléant', 'Remplace le responsable si nécessaire', TRUE, CURRENT_TIMESTAMP),
(3, 'PARTICIPANT', 'Participant', 'Participe à la réalisation de l''activité', TRUE, CURRENT_TIMESTAMP),
(4, 'CONTRIBUTEUR', 'Contributeur', 'Contribue à la réalisation du plan d''action', TRUE, CURRENT_TIMESTAMP);

-- Opérations PTA
INSERT INTO operation_pta (id_operation_pta, code, libelle, description, annee, date_creation) VALUES
(1, 'PTA-2026-001', 'Amélioration des processus', 'Amélioration continue des processus', 2026, CURRENT_TIMESTAMP),
(2, 'PTA-2026-002', 'Renforcement de la sécurité', 'Actions liées à la sécurité du système', 2026, CURRENT_TIMESTAMP);

-- Sites
INSERT INTO site (id_site, code, nom, latitude, longitude, actif, date_creation) VALUES
(1, 'SITE-TANA', 'Siège Antananarivo', -18.8792, 47.5079, TRUE, CURRENT_TIMESTAMP),
(2, 'SITE-IT', 'Centre informatique', -18.9150, 47.5250, TRUE, CURRENT_TIMESTAMP);

-- Décisions
INSERT INTO decision_validation (id_decision, code, libelle, description, actif, date_creation) VALUES
(1, 'VALIDER', 'Valider', 'Approuver l''étape de validation', TRUE, CURRENT_TIMESTAMP),
(2, 'REFUSER', 'Refuser', 'Rejeter la demande', TRUE, CURRENT_TIMESTAMP),
(3, 'DEMANDER_MODIFICATION', 'Demander une modification', 'Retourner la demande pour correction', TRUE, CURRENT_TIMESTAMP);

-- Procédures
INSERT INTO "procedure" (id_procedure, code, libelle, description, actif, date_creation) VALUES
(1, 'PROC-STD', 'Validation standard', 'Circuit standard avec chef de service puis chef de département', TRUE, CURRENT_TIMESTAMP),
(2, 'PROC-STRAT', 'Validation stratégique', 'Circuit renforcé avec direction', TRUE, CURRENT_TIMESTAMP);

-- Étapes de validation
INSERT INTO etape_validation (
    id_etape_validation, id_procedure, libelle, description,
    niveau, id_poste, obligatoire, actif, date_creation
) VALUES
(1, 1, 'Validation chef de service', 'Première étape de validation', 1, 2, TRUE, TRUE, CURRENT_TIMESTAMP),
(2, 1, 'Validation chef de département', 'Validation finale du circuit standard', 2, 1, TRUE, TRUE, CURRENT_TIMESTAMP),
(3, 2, 'Validation chef de service', 'Première étape de validation stratégique', 1, 2, TRUE, TRUE, CURRENT_TIMESTAMP),
(4, 2, 'Validation chef de département', 'Deuxième étape de validation stratégique', 2, 1, TRUE, TRUE, CURRENT_TIMESTAMP),
(5, 2, 'Validation direction', 'Validation finale stratégique', 3, 4, TRUE, TRUE, CURRENT_TIMESTAMP);

-- Utilisateurs
-- Remplacer les mots de passe de démonstration par de vrais hash BCrypt.
INSERT INTO utilisateur (
    id_utilisateur, id_service, id_poste, nom, prenom, email,
    telephone, mot_de_passe, actif, date_creation, date_derniere_connexion
) VALUES
(1, 1, 1, 'RAKOTO', 'Jean', 'jean.rakoto@siga.local',
 '032000001', '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(2, 1, 2, 'RABE', 'Marie', 'marie.rabe@siga.local',
 '032000002', '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, 1, 3, 'RANDRIA', 'Paul', 'paul.randria@siga.local',
 '032000003', '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(4, 2, 2, 'RAZAFI', 'Sophie', 'sophie.razafi@siga.local',
 '032000004', '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(5, 2, 3, 'RAKOTOARISOA', 'Hery', 'hery.rakotoarisoa@siga.local',
 '032000005', '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(6, 3, 3, 'RAVAO', 'Nina', 'nina.ravao@siga.local',
 '032000006', '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66', TRUE, CURRENT_TIMESTAMP, NULL);

-- Permissions des postes
INSERT INTO poste_permission (id_poste, id_permission) VALUES
(1, 4),
(1, 5),
(1, 6),
(2, 2),
(2, 3),
(2, 4),
(2, 5),
(2, 6),
(3, 2),
(3, 3);

-- Tokens
INSERT INTO token_auth (
    id_token, id_utilisateur, id_type_token, token,
    date_creation, date_expiration
) VALUES
(1, 6, 1, 'ACTIVATION-DEMO-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '24 hours'),
(2, 3, 2, 'RESET-DEMO-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 hour');

-- Activités
INSERT INTO activite (
    id_activite, id_service, id_type_activite, id_priorite,
    id_site, id_operation_pta, id_procedure, code, libelle,
    date_debut_prevue, date_fin_prevue, date_debut_reelle, date_creation
) VALUES
(
    1, 1, 2, 3, 1, 1, 1, 'ACT-2026-001',
    'Réaliser les tests mensuels du SAI',
    '2026-08-01 08:00:00', '2026-08-30 17:00:00',
    '2026-08-05 08:30:00', CURRENT_TIMESTAMP
),
(
    2, 2, 1, 4, 2, 2, 2, 'ACT-2026-002',
    'Renforcer la sécurité du système',
    '2026-08-10 08:00:00', '2026-09-15 17:00:00',
    NULL, CURRENT_TIMESTAMP
),
(
    3, 3, 3, 2, 1, 1, 1, 'ACT-2026-003',
    'Étude d''optimisation des processus',
    '2026-08-15 08:00:00', '2026-09-05 17:00:00',
    NULL, CURRENT_TIMESTAMP
);

-- Affectations activité / utilisateur
INSERT INTO activite_utilisateur (
    id_activite_utilisateur, id_activite, id_utilisateur,
    id_role, date_affectation
) VALUES
(1, 1, 2, 1, CURRENT_TIMESTAMP),
(2, 1, 3, 2, CURRENT_TIMESTAMP),
(3, 1, 4, 3, CURRENT_TIMESTAMP),
(4, 2, 4, 1, CURRENT_TIMESTAMP),
(5, 2, 5, 3, CURRENT_TIMESTAMP),
(6, 3, 1, 1, CURRENT_TIMESTAMP),
(7, 3, 6, 3, CURRENT_TIMESTAMP);

-- Historique
INSERT INTO historique_activite (
    id_historique_activite, id_activite, id_statut,
    id_utilisateur, date_debut, date_fin, commentaire
) VALUES
(1, 1, 1, 2, '2026-08-01 08:00:00', '2026-08-01 09:00:00', 'Activité créée'),
(2, 1, 2, 2, '2026-08-01 09:00:00', '2026-08-02 11:00:00', 'Activité soumise à la validation'),
(3, 1, 3, 1, '2026-08-02 11:00:00', '2026-08-02 11:05:00', 'Validation finale obtenue'),
(4, 1, 4, 2, '2026-08-02 11:05:00', '2026-08-05 08:30:00', 'Activité prête à démarrer'),
(5, 1, 5, 2, '2026-08-05 08:30:00', NULL, 'Réalisation en cours'),
(6, 2, 1, 4, '2026-08-10 08:00:00', '2026-08-10 09:00:00', 'Activité créée'),
(7, 3, 1, 1, '2026-08-15 08:00:00', NULL, 'Activité créée');

-- Ressources
INSERT INTO ressource (
    id_ressource, id_activite, designation, type_ressource, quantite, cout
) VALUES
(1, 1, 'Ordinateur portable', 'Matériel', 2, 4500000),
(2, 1, 'Véhicule de service', 'Transport', 1, 500000),
(3, 2, 'Équipement de sécurité réseau', 'Matériel', 1, 3200000);

-- Indicateurs
INSERT INTO indicateur (
    id_indicateur, type_indicateur, details, libelle, code_hopex,
    indicateur_hopex, objectifs, methode_determination,
    frequence_verification, frequence_aggregation, seuil_min,
    seuil_max, unite, commentaire, date_creation
) VALUES
(
    1, 'KPI',
    'Nombre de tests réalisés chaque mois',
    'Nombre de tests mensuels du SAI',
    'KPI-SAI-001',
    'SAI_TEST_MENSUEL',
    'Réaliser au moins un test mensuel',
    'Comptage des tests réalisés',
    'Mensuelle', 'Mensuelle',
    1, 10, 'test',
    'Indicateur de suivi des tests SAI',
    CURRENT_TIMESTAMP
),
(
    2, 'KPI',
    'Taux de réalisation des activités',
    'Taux de réalisation des activités',
    'KPI-ACT-001',
    'ACT_REALISATION',
    'Atteindre au moins 90 %',
    'Nombre d''activités terminées / nombre d''activités prévues',
    'Mensuelle', 'Mensuelle',
    90, 100, '%',
    'Indicateur de performance des activités',
    CURRENT_TIMESTAMP
);

-- Valeurs d'indicateurs
INSERT INTO valeur_indicateur (
    id_valeur_indicateur, id_indicateur, periode_debut,
    periode_fin, valeur, commentaire, date_saisie
) VALUES
(1, 1, '2026-06-01', '2026-06-30', 1, 'Test réalisé en juin', '2026-07-01 09:00:00'),
(2, 1, '2026-07-01', '2026-07-31', 2, 'Deux tests réalisés en juillet', '2026-08-01 09:00:00'),
(3, 1, '2026-08-01', '2026-08-31', 1, 'Un test réalisé en août', '2026-08-25 09:00:00'),
(4, 2, '2026-08-01', '2026-08-31', 75, 'Suivi intermédiaire du mois', '2026-08-25 09:10:00');

-- Validations
INSERT INTO validation_activite (
    id_validation, id_activite, id_etape_validation,
    id_utilisateur_demandeur, id_utilisateur_validateur,
    id_decision, date_demande, date_decision, commentaire
) VALUES
(1, 1, 1, 2, 2, 1, '2026-08-01 09:00:00', '2026-08-02 10:00:00', 'Validation du chef de service'),
(2, 1, 2, 2, 1, 1, '2026-08-02 10:15:00', '2026-08-02 11:00:00', 'Validation finale du chef de département'),
(3, 2, 3, 4, 4, 1, '2026-08-10 09:30:00', '2026-08-10 10:00:00', 'Validation chef de service');

-- Notifications
INSERT INTO notification (
    id_notification, id_utilisateur, id_priorite,
    titre, message, lu, date_creation, date_lecture
) VALUES
(1, 2, 3, 'Validation requise', 'L''activité ACT-2026-001 nécessite votre validation.', TRUE, '2026-08-02 09:30:00', '2026-08-02 09:45:00'),
(2, 1, 3, 'Activité validée', 'L''activité ACT-2026-001 a été validée.', TRUE, '2026-08-02 11:05:00', '2026-08-02 11:10:00'),
(3, 5, 4, 'Activité urgente', 'L''activité ACT-2026-002 est prioritaire.', FALSE, '2026-08-25 08:20:00', NULL),
(4, 3, 2, 'Nouvelle affectation', 'Vous avez été affecté à l''activité ACT-2026-001.', FALSE, '2026-08-25 08:25:00', NULL);

-- Plans d'action
INSERT INTO plan_action (id_plan_action, code, intitule, commentaire, date_creation) VALUES
(1, 'PA-2026-001', 'Amélioration de la sécurité informatique', 'Plan issu des recommandations d''audit', CURRENT_TIMESTAMP),
(2, 'PA-2026-002', 'Optimisation des procédures internes', 'Plan d''amélioration continue', CURRENT_TIMESTAMP);

-- Origines
INSERT INTO origine (id_origine, code, type, reference, intitule, description) VALUES
(1, 'ORI-001', 'AUDIT', 'AUD-2026-01', 'Audit interne', 'Constats issus de l''audit interne'),
(2, 'ORI-002', 'RECOMMANDATION', 'REC-2026-04', 'Recommandation de la direction', 'Recommandation de la direction'),
(3, 'ORI-003', 'INCIDENT', 'INC-2026-02', 'Incident de sécurité', 'Origine liée à un incident de sécurité');

-- Origines des plans d'action
INSERT INTO plan_action_origine (id_plan_action, id_origine, est_principale) VALUES
(1, 1, TRUE),
(1, 3, FALSE),
(2, 2, TRUE);

-- Utilisateurs des plans d'action
INSERT INTO plan_action_utilisateur (id_plan_action, id_utilisateur, id_role) VALUES
(1, 1, 1),
(1, 4, 4),
(2, 2, 1),
(2, 3, 3);

-- Remise en phase des séquences après insertion d'IDs explicites.
SELECT setval(pg_get_serial_sequence('departement', 'id_departement'), COALESCE(MAX(id_departement), 1), MAX(id_departement) IS NOT NULL) FROM departement;
SELECT setval(pg_get_serial_sequence('service', 'id_service'), COALESCE(MAX(id_service), 1), MAX(id_service) IS NOT NULL) FROM service;
SELECT setval(pg_get_serial_sequence('poste', 'id_poste'), COALESCE(MAX(id_poste), 1), MAX(id_poste) IS NOT NULL) FROM poste;
SELECT setval(pg_get_serial_sequence('priorite', 'id_priorite'), COALESCE(MAX(id_priorite), 1), MAX(id_priorite) IS NOT NULL) FROM priorite;
SELECT setval(pg_get_serial_sequence('type_activite', 'id_type_activite'), COALESCE(MAX(id_type_activite), 1), MAX(id_type_activite) IS NOT NULL) FROM type_activite;
SELECT setval(pg_get_serial_sequence('statut', 'id_statut'), COALESCE(MAX(id_statut), 1), MAX(id_statut) IS NOT NULL) FROM statut;
SELECT setval(pg_get_serial_sequence('type_token', 'id_type_token'), COALESCE(MAX(id_type_token), 1), MAX(id_type_token) IS NOT NULL) FROM type_token;
SELECT setval(pg_get_serial_sequence('permission', 'id_permission'), COALESCE(MAX(id_permission), 1), MAX(id_permission) IS NOT NULL) FROM permission;
SELECT setval(pg_get_serial_sequence('role', 'id_role'), COALESCE(MAX(id_role), 1), MAX(id_role) IS NOT NULL) FROM role;
SELECT setval(pg_get_serial_sequence('operation_pta', 'id_operation_pta'), COALESCE(MAX(id_operation_pta), 1), MAX(id_operation_pta) IS NOT NULL) FROM operation_pta;
SELECT setval(pg_get_serial_sequence('site', 'id_site'), COALESCE(MAX(id_site), 1), MAX(id_site) IS NOT NULL) FROM site;
SELECT setval(pg_get_serial_sequence('decision_validation', 'id_decision'), COALESCE(MAX(id_decision), 1), MAX(id_decision) IS NOT NULL) FROM decision_validation;
SELECT setval(pg_get_serial_sequence('utilisateur', 'id_utilisateur'), COALESCE(MAX(id_utilisateur), 1), MAX(id_utilisateur) IS NOT NULL) FROM utilisateur;
SELECT setval(pg_get_serial_sequence('token_auth', 'id_token'), COALESCE(MAX(id_token), 1), MAX(id_token) IS NOT NULL) FROM token_auth;
SELECT setval(pg_get_serial_sequence('procedure', 'id_procedure'), COALESCE(MAX(id_procedure), 1), MAX(id_procedure) IS NOT NULL) FROM "procedure";
SELECT setval(pg_get_serial_sequence('etape_validation', 'id_etape_validation'), COALESCE(MAX(id_etape_validation), 1), MAX(id_etape_validation) IS NOT NULL) FROM etape_validation;
SELECT setval(pg_get_serial_sequence('activite', 'id_activite'), COALESCE(MAX(id_activite), 1), MAX(id_activite) IS NOT NULL) FROM activite;
SELECT setval(pg_get_serial_sequence('activite_utilisateur', 'id_activite_utilisateur'), COALESCE(MAX(id_activite_utilisateur), 1), MAX(id_activite_utilisateur) IS NOT NULL) FROM activite_utilisateur;
SELECT setval(pg_get_serial_sequence('historique_activite', 'id_historique_activite'), COALESCE(MAX(id_historique_activite), 1), MAX(id_historique_activite) IS NOT NULL) FROM historique_activite;
SELECT setval(pg_get_serial_sequence('ressource', 'id_ressource'), COALESCE(MAX(id_ressource), 1), MAX(id_ressource) IS NOT NULL) FROM ressource;
SELECT setval(pg_get_serial_sequence('indicateur', 'id_indicateur'), COALESCE(MAX(id_indicateur), 1), MAX(id_indicateur) IS NOT NULL) FROM indicateur;
SELECT setval(pg_get_serial_sequence('valeur_indicateur', 'id_valeur_indicateur'), COALESCE(MAX(id_valeur_indicateur), 1), MAX(id_valeur_indicateur) IS NOT NULL) FROM valeur_indicateur;
SELECT setval(pg_get_serial_sequence('validation_activite', 'id_validation'), COALESCE(MAX(id_validation), 1), MAX(id_validation) IS NOT NULL) FROM validation_activite;
SELECT setval(pg_get_serial_sequence('notification', 'id_notification'), COALESCE(MAX(id_notification), 1), MAX(id_notification) IS NOT NULL) FROM notification;
SELECT setval(pg_get_serial_sequence('plan_action', 'id_plan_action'), COALESCE(MAX(id_plan_action), 1), MAX(id_plan_action) IS NOT NULL) FROM plan_action;
SELECT setval(pg_get_serial_sequence('origine', 'id_origine'), COALESCE(MAX(id_origine), 1), MAX(id_origine) IS NOT NULL) FROM origine;

COMMIT;
